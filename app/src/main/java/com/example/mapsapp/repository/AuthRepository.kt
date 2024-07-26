package com.example.mapsapp.repo

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
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
                    onComplete(user)
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

    fun logout() {
        auth.signOut()
    }
}
