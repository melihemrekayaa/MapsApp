package com.example.mapsapp.repository

import com.example.mapsapp.model.User
import com.example.mapsapp.webrtc.firebaseClient.FirebaseClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val firebaseClient: FirebaseClient
) {

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    fun register(email: String, password: String, onComplete: (FirebaseUser?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    addUserToFirestore(user)
                    // WebRTC Firebase'e Kullanıcı Kaydı
                    firebaseClient.addUserToWebRTC(getUsernameFromEmail(email), password) { success ->
                        if (success) {
                            onComplete(user)
                        } else {
                            onComplete(null)
                        }
                    }
                } else {
                    onComplete(null)
                }
            }
    }

    fun login(email: String, password: String, onComplete: (FirebaseUser?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onComplete(auth.currentUser)
                } else {
                    onComplete(null)
                }
            }
    }

    private fun addUserToFirestore(firebaseUser: FirebaseUser?) {
        firebaseUser?.let { user ->
            val userData = hashMapOf(
                "uid" to user.uid,
                "email" to user.email
            )
            firestore.collection("users").document(user.uid).set(userData)
        }
    }

    private fun getUsernameFromEmail(email: String): String {
        return email.substringBefore("@")
    }

    fun loadUsers(onResult: (List<User>) -> Unit) {
        firestore.collection("users").get().addOnSuccessListener { snapshot ->
            val users = snapshot.documents.mapNotNull { doc ->
                doc.toObject(User::class.java)
            }
            onResult(users)
        }
    }

    fun loadFriends(onResult: (List<User>) -> Unit) {
        val currentUser = getCurrentUser()?.uid ?: return
        firestore.collection("users").document(currentUser).collection("friends")
            .get().addOnSuccessListener { snapshot ->
                val friends = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(User::class.java)
                }
                onResult(friends)
            }
    }

    fun sendFriendRequest(receiverId: String, onComplete: (Boolean) -> Unit) {
        val currentUser = getCurrentUser()?.uid ?: return
        val requestData = mapOf("from" to currentUser)
        firestore.collection("users").document(receiverId)
            .collection("friendRequests").document(currentUser).set(requestData)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun cancelFriendRequest(receiverId: String, onComplete: (Boolean) -> Unit) {
        val currentUser = getCurrentUser()?.uid ?: return
        firestore.collection("users").document(receiverId)
            .collection("friendRequests").document(currentUser).delete()
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun logout() {
        auth.signOut()
    }
}
