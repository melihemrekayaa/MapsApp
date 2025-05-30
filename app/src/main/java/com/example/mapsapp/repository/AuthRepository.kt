package com.example.mapsapp.repository

import android.util.Log
import com.example.mapsapp.model.User
import com.example.mapsapp.webrtc.UserRTC
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.onSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    companion object {
        private const val USERS_COLLECTION = "Users"
        private const val FRIEND_REQUESTS_COLLECTION = "friendRequests"
        private const val FRIENDS_COLLECTION = "friends"
    }

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    suspend fun register(name: String, email: String, password: String): Result<String> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = result.user?.uid ?: throw Exception("User ID is null")

            val user = User(
                uid = userId,
                name = name,
                email = email,
                friends = listOf(),
            )
            firestore.collection("users").document(userId).set(user).await()
            Result.success("User registered successfully")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    fun login(email: String, password: String, onComplete: (FirebaseUser?, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        setUserOnline(it.uid) // Set user as online after login
                    }
                    onComplete(user, null)
                } else {
                    onComplete(null, task.exception?.message ?: "Login Failed")
                }
            }
    }



    suspend fun loadUsers(): List<User> {
        return try {
            val currentUser = getCurrentUser() ?: return emptyList()
            val currentUserId = currentUser.uid

            val snapshot = firestore.collection("users").get().await()
            snapshot.toObjects(User::class.java).mapNotNull { user ->
                if (user.uid == currentUserId || user.friends.contains(currentUserId)) {
                    null
                } else {
                    // isRequestSent kontrolü artık alt koleksiyon üzerinden
                    val requestDoc = firestore.collection("users")
                        .document(user.uid)
                        .collection("friendRequests")
                        .document(currentUserId)
                        .get()
                        .await()

                    val isRequestSent = requestDoc.exists()
                    user.copy(isRequestSent = isRequestSent)
                }
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "loadUsers error: ${e.message}")
            emptyList()
        }
    }







    suspend fun loadFriends(): List<User> {
        val currentUser = getCurrentUser()?.uid ?: return emptyList()
        return try {
            val snapshot = firestore.collection(USERS_COLLECTION)
                .document(currentUser)
                .collection(FRIENDS_COLLECTION).get().await()

            snapshot.documents.mapNotNull { it.toObject(User::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun sendFriendRequest(receiverId: String): Boolean {
        return try {
            val currentUser = getCurrentUser()?.uid ?: return false
            val ref = firestore.collection("users")
                .document(receiverId)
                .collection("friendRequests")
                .document(currentUser)

            val existing = ref.get().await()
            if (existing.exists()) return false // zaten gönderilmiş

            val requestData = mapOf("from" to currentUser)
            ref.set(requestData).await()
            true
        } catch (e: Exception) {
            Log.e("AuthRepository", "sendFriendRequest error: ${e.message}")
            false
        }
    }

    suspend fun cancelFriendRequest(receiverId: String): Boolean {
        return try {
            val currentUser = getCurrentUser()?.uid ?: return false
            firestore.collection("users").document(receiverId)
                .collection("friendRequests").document(currentUser).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun setUserOnline(userId: String) {
        firestore.collection("users")
            .document(userId)
            .update("isOnline", true)
    }

    fun setUserOffline(userId: String) {
        firestore.collection("users")
            .document(userId)
            .update("isOnline", false)
    }

    fun getFriendsListRealtime(userId: String): Flow<List<User>> = callbackFlow {
        val userDocRef = firestore.collection("users").document(userId)
        val realtimeDb = FirebaseDatabase.getInstance().getReference("usersOnlineStatus")

        val liveFriendsMap = mutableMapOf<String, User>()
        val lastSeenListeners = mutableMapOf<String, ValueEventListener>()

        val firestoreListener = userDocRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) {
                cancel("Snapshot error", error)
                return@addSnapshotListener
            }

            val friendsList = snapshot.get("friends") as? List<String> ?: emptyList()

            // 🔄 Silinen arkadaşları temizle
            val removedIds = liveFriendsMap.keys - friendsList
            removedIds.forEach { removedId ->
                lastSeenListeners[removedId]?.let {
                    realtimeDb.child(removedId).removeEventListener(it)
                }
                lastSeenListeners.remove(removedId)
                liveFriendsMap.remove(removedId)
            }

            friendsList.forEach { friendId ->
                if (lastSeenListeners.containsKey(friendId)) return@forEach

                firestore.collection("users").document(friendId)
                    .get()
                    .addOnSuccessListener { friendDoc ->
                        val user = friendDoc.toObject(User::class.java) ?: return@addOnSuccessListener

                        val listener = object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val lastSeen = snapshot.child("lastSeen").getValue(Long::class.java)
                                val fullUser = user.copy(lastSeenTimestamp = lastSeen)
                                liveFriendsMap[friendId] = fullUser
                                trySend(liveFriendsMap.values.toList()).onFailure {
                                    cancel("Send failed", it)
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {}
                        }

                        realtimeDb.child(friendId).addValueEventListener(listener)
                        lastSeenListeners[friendId] = listener
                    }
            }
        }

        awaitClose {
            firestoreListener.remove()
            lastSeenListeners.forEach { (id, listener) ->
                realtimeDb.child(id).removeEventListener(listener)
            }
        }
    }




    fun getUsersInCall(): Flow<List<UserRTC>> = callbackFlow {
        val ref = FirebaseDatabase.getInstance().getReference("users")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val inCallList = snapshot.children.mapNotNull { userSnap ->
                    val uid = userSnap.key ?: return@mapNotNull null
                    val name = userSnap.child("displayName").getValue(String::class.java) ?: return@mapNotNull null
                    val photoUrl = userSnap.child("photoUrl").getValue(String::class.java)
                    val isInCall = userSnap.child("inCall").getValue(Boolean::class.java) ?: false

                    if (isInCall) {
                        UserRTC(uid, name, photoUrl, true)
                    } else null
                }

                trySend(inCallList)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun loadFriendRequests(userUid: String): Flow<List<User>> = callbackFlow {
        val requestRef = firestore.collection("users")
            .document(userUid)
            .collection("friendRequests")

        val listener = requestRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val senderIds = snapshot?.documents?.mapNotNull { it.getString("from") }.orEmpty()
            if (senderIds.isEmpty()) {
                trySend(emptyList())
                return@addSnapshotListener
            }

            val users = mutableListOf<User>()
            var loaded = 0
            senderIds.forEach { senderId ->
                firestore.collection("users").document(senderId)
                    .get()
                    .addOnSuccessListener { doc ->
                        doc.toObject(User::class.java)?.let { users.add(it) }
                        loaded++
                        if (loaded == senderIds.size) trySend(users.toList())
                    }
                    .addOnFailureListener { e ->
                        Log.e("AuthRepository", "Error loading sender user: ${e.message}")
                        loaded++
                        if (loaded == senderIds.size) trySend(users.toList())
                    }
            }
        }

        awaitClose { listener.remove() }
    }

    suspend fun getFriendRequests(currentUserId: String): List<User> {
        return try {
            val snapshot = firestore.collection("users")
                .document(currentUserId)
                .collection("friendRequests")
                .get()
                .await()

            val senderIds = snapshot.documents.mapNotNull { it.getString("from") }
            senderIds.mapNotNull { uid ->
                firestore.collection("users").document(uid).get().await().toObject(User::class.java)
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "getFriendRequests error: ${e.message}")
            emptyList()
        }
    }






    fun acceptFriendRequest(currentUserUid: String, friendUid: String, onComplete: (Boolean) -> Unit) {
        val currentUserRef = firestore.collection("users").document(currentUserUid)
        val friendRef = firestore.collection("users").document(friendUid)

        firestore.runTransaction { transaction ->
            // Fetch both users' documents
            val currentUserDoc = transaction.get(currentUserRef)
            val friendDoc = transaction.get(friendRef)

            // Add friendUid to the current user's friends list
            val currentUserFriends = currentUserDoc.get("friends") as? MutableList<String> ?: mutableListOf()
            if (!currentUserFriends.contains(friendUid)) {
                currentUserFriends.add(friendUid)
                transaction.update(currentUserRef, "friends", currentUserFriends)
            }

            // Add currentUserUid to the friend's friends list
            val friendFriends = friendDoc.get("friends") as? MutableList<String> ?: mutableListOf()
            if (!friendFriends.contains(currentUserUid)) {
                friendFriends.add(currentUserUid)
                transaction.update(friendRef, "friends", friendFriends)
            }

            // Delete the friend request from the current user's requests
            transaction.delete(currentUserRef.collection("friendRequests").document(friendUid))
        }.addOnCompleteListener { task ->
            onComplete(task.isSuccessful)
        }
    }

    suspend fun sendVerificationToNewEmail(newEmail: String): Result<Unit> {
        return try {
            // Geçici kullanıcı oluşturup sadece doğrulama maili atmak için
            val tempPassword = "Temp123456!"
            val result = auth.createUserWithEmailAndPassword(newEmail, tempPassword).await()
            result.user?.sendEmailVerification()?.await()

            // Doğrulama gönderildi, hesap sonra manuel silinir ya da kullanıcı login olmaz
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun removeFriend(currentUserId: String, friendId: String): Boolean {
        return try {
            val userRef = firestore.collection("users").document(currentUserId)
            val friendRef = firestore.collection("users").document(friendId)

            firestore.runTransaction { transaction ->
                val currentUserDoc = transaction.get(userRef)
                val friendDoc = transaction.get(friendRef)

                val currentUserFriends = currentUserDoc.get("friends") as? MutableList<String> ?: mutableListOf()
                val friendFriends = friendDoc.get("friends") as? MutableList<String> ?: mutableListOf()

                currentUserFriends.remove(friendId)
                friendFriends.remove(currentUserId)

                transaction.update(userRef, "friends", currentUserFriends)
                transaction.update(friendRef, "friends", friendFriends)
            }.await()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun removeFriendRequest(userUid: String, friendUid: String) {
        firestore.collection("users")
            .document(userUid)
            .collection("friendRequests")
            .document(friendUid)
            .delete()
            .addOnSuccessListener {
                Log.d("AuthRepository", "Friend request from $friendUid removed for $userUid")
            }
            .addOnFailureListener { e ->
                Log.e("AuthRepository", "Failed to remove friend request: ${e.message}")
            }
    }

    suspend fun reauthenticateAndChangeEmail(currentPassword: String, newEmail: String): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("Kullanıcı oturumu yok"))

        return try {
            val credential = EmailAuthProvider.getCredential(user.email ?: "", currentPassword)
            user.reauthenticate(credential).await()

            user.updateEmail(newEmail).await()
            updateEmailInFirestore(user.uid, newEmail)

            user.sendEmailVerification().await()
            logout()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }



    suspend fun reauthenticateAndChangePassword(currentPassword: String, newPassword: String): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("Kullanıcı oturumu yok"))

        return try {
            val credential = EmailAuthProvider.getCredential(user.email ?: "", currentPassword)
            user.reauthenticate(credential).await()

            user.updatePassword(newPassword).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }



    suspend fun updateEmailInFirestore(uid: String, newEmail: String) {
        firestore.collection("users")
            .document(uid)
            .update("email", newEmail)
            .await()
    }



    fun logout() {
        val user = getCurrentUser()
        user?.let {
            val uid = it.uid

            // Firestore offline
            setUserOffline(uid)

            // Realtime Database temizliği
            val realtimeDb = FirebaseDatabase.getInstance().getReference("users/$uid")
            realtimeDb.child("online").setValue(false)
            realtimeDb.child("inCall").setValue(false)
            realtimeDb.child("lastSeen").setValue(System.currentTimeMillis())

            // Aktif callRequest temizliği
            FirebaseDatabase.getInstance().getReference("callRequests/$uid").removeValue()
            FirebaseDatabase.getInstance().getReference("calls/$uid").removeValue()
            FirebaseDatabase.getInstance().getReference("calls").child(uid).removeValue()

            auth.signOut()
        }
    }

}
