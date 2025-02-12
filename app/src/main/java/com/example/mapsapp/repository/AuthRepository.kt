package com.example.mapsapp.repository

import android.util.Log
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mapsapp.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

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

            // Kullanıcı verilerini Firestore'a kaydet
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
            Log.d("AuthRepository", "Fetching users from Firestore")
            val snapshot = firestore.collection("users").get().await()
            val users = snapshot.toObjects(User::class.java)
            Log.d("AuthRepository", "Fetched users: $users")
            users
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error fetching users: ${e.message}")
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

        Log.d("AuthRepository", "Getting friends list for user: $userId")

        val listener = userDocRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("AuthRepository", "Error fetching friends: ${error.message}")
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val friendsList = snapshot.get("friends") as? List<String>
                if (!friendsList.isNullOrEmpty()) {
                    Log.d("AuthRepository", "Friend IDs found: $friendsList")

                    val friends = mutableListOf<User>()

                    // Fetch details of each friend
                    for (friendId in friendsList) {
                        firestore.collection("users").document(friendId)
                            .get()
                            .addOnSuccessListener { friendSnapshot ->
                                if (friendSnapshot.exists()) {
                                    val friend = friendSnapshot.toObject(User::class.java)
                                    if (friend != null) {
                                        friends.add(friend)
                                        trySend(friends)
                                        Log.d("AuthRepository", "Friend added: ${friend.name}")
                                    }
                                } else {
                                    Log.w("AuthRepository", "Friend document missing: $friendId")
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e("AuthRepository", "Error fetching friend details: ${e.message}")
                            }
                    }
                } else {
                    Log.w("AuthRepository", "User $userId has no friends in Firestore.")
                    trySend(emptyList())
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
                    val requests = snapshot.toObjects(User::class.java)
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

    fun logout() {
        val user = getCurrentUser()
        user?.let {
            setUserOffline(it.uid) // Set user as offline before logging out
        }
        auth.signOut()
    }
}
