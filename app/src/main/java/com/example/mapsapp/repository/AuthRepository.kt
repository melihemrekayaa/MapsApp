package com.example.mapsapp.repository

import com.example.mapsapp.webrtc.firebaseClient.FirebaseClient
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
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


    fun register(email: String, password: String, location: GeoPoint, onComplete: (FirebaseUser?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    addUserToFirestore(user, location)
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

    private fun addUserToFirestore(firebaseUser: FirebaseUser?, location: GeoPoint) {
        firebaseUser?.let { user ->
            val userData = hashMapOf(
                "uid" to user.uid,
                "email" to user.email,
                "location" to location
            )
            firestore.collection("users").document(user.uid).set(userData)
        }
    }


    private fun getUsernameFromEmail(email: String): String {
        return email.substringBefore("@")
    }

    fun logout() {
        auth.signOut()
    }
}
