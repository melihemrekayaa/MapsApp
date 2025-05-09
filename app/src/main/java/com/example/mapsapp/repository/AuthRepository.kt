package com.example.mapsapp.repository

import android.util.Log
import com.example.mapsapp.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import kotlinx.coroutines.channels.awaitClose
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
                friendRequests = listOf()
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
            val currentUserId = getCurrentUser()?.uid
            val snapshot = firestore.collection("users").get().await()
            val users = snapshot.toObjects(User::class.java).map { user ->
                val isRequestSent = user.friendRequests.contains(currentUserId)
                user.copy(isRequestSent = isRequestSent)
            }
            users
        } catch (e: Exception) {
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
            val requestData = mapOf("from" to currentUser)
            firestore.collection("users").document(receiverId)
                .collection("friendRequests").document(currentUser)
                .set(requestData).await()
            true
        } catch (e: Exception) {
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

    fun getFriendsList(userId: String): Flow<List<User>> = callbackFlow {
        val userDocRef = firestore.collection("users").document(userId)

        val listener = userDocRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val friendsList = snapshot.get("friends") as? List<String> ?: emptyList()

                if (friendsList.isEmpty()) {
                    trySend(emptyList())
                } else {
                    val friends = mutableListOf<User>()
                    friendsList.forEach { friendId ->
                        firestore.collection("users").document(friendId)
                            .get()
                            .addOnSuccessListener { friendSnapshot ->
                                val friend = friendSnapshot.toObject(User::class.java)
                                friend?.let {
                                    friends.add(it)
                                    trySend(friends.toList())
                                }
                            }
                            .addOnFailureListener { Log.e("AuthRepo", "Friend fetch error: ${it.message}") }
                    }
                }
            }
        }

        awaitClose { listener.remove() }
    }







    fun loadFriendRequests(userUid: String): Flow<List<User>> = callbackFlow {
        val listener = firestore.collection("users")
            .document(userUid)
            .collection("friendRequests")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val requests = snapshot.documents.mapNotNull { doc ->
                        val senderUid = doc.getString("from") ?: return@mapNotNull null
                        firestore.collection("users").document(senderUid)
                            .get()
                            .addOnSuccessListener { senderSnapshot ->
                                if (senderSnapshot.exists()) {
                                    val user = senderSnapshot.toObject(User::class.java)
                                    if (user != null) {
                                        trySend(listOf(user)) // Güncellenen listeyi gönder
                                    }
                                }
                            }
                        null
                    }
                    trySend(requests)
                }
            }

        awaitClose { listener.remove() }
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



    fun logout() {
        val user = getCurrentUser()
        user?.let {
            setUserOffline(it.uid) // Set user as offline before logging out
        }
        auth.signOut()
    }
}
