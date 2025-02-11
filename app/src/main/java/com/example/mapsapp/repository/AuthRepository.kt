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

    fun login(email: String, password: String, onComplete: (FirebaseUser?,String?) -> Unit) {
        auth.signInWithEmailAndPassword(email,password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful){
                    val user = auth.currentUser
                    onComplete(user,null)
                }
                else {
                    onComplete(null,task.exception?.message ?: "Login Failed")
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


    fun updateFirestoreUserStatus(status: String) {
        val user = auth.currentUser ?: return
        val userDocRef = firestore.collection(USERS_COLLECTION).document(user.uid)
        userDocRef.update("status", status)
    }

    fun setupUserPresence() {
        val user = auth.currentUser ?: return
        val userStatusDatabaseRef = FirebaseDatabase.getInstance()
            .getReference("presence/${user.uid}")

        val connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected")

        connectedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                if (connected) {
                    userStatusDatabaseRef.setValue("ONLINE")
                    userStatusDatabaseRef.onDisconnect().setValue("OFFLINE")
                }
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                // Log or handle the error
            }
        })
    }

    fun loadFriendRequests(userUid: String, onComplete: (List<User>) -> Unit) {
        val friendRequestsRef = firestore.collection("users")
            .document(userUid)
            .collection("friendRequests")

        friendRequestsRef.get()
            .addOnSuccessListener { documents ->
                val requests = mutableListOf<User>()
                for (document in documents) {
                    val fromUid = document.getString("from") ?: continue
                    firestore.collection("users").document(fromUid).get()
                        .addOnSuccessListener { userDocument ->
                            val user = User(
                                uid = fromUid,
                                name = userDocument.getString("name") ?: "Unknown",
                                photoUrl = userDocument.getString("photoUrl")
                            )
                            requests.add(user)
                            if (requests.size == documents.size()) {
                                onComplete(requests)
                            }
                        }
                }
            }
            .addOnFailureListener {
                onComplete(emptyList()) // Hata varsa boş liste döndür
            }
    }


    fun acceptFriendRequest(currentUserUid: String, friendUid: String, onComplete: (Boolean) -> Unit) {
        val currentUserRef = firestore.collection("users").document(currentUserUid)
        val friendRef = firestore.collection("users").document(friendUid)

        firestore.runBatch { batch ->
            // İsteği kabul eden kullanıcının arkadaş listesine ekle
            batch.update(currentUserRef, "friends", FieldValue.arrayUnion(friendUid))
            // Arkadaşın arkadaş listesine bu kullanıcıyı ekle
            batch.update(friendRef, "friends", FieldValue.arrayUnion(currentUserUid))
            // İsteği sil
            batch.delete(currentUserRef.collection("friendRequests").document(friendUid))
        }.addOnCompleteListener { task ->
            onComplete(task.isSuccessful)
        }
    }

    fun logout() {
        auth.signOut()
    }
}
