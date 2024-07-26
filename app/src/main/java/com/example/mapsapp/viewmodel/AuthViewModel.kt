package com.example.mapsapp.viewmodel

import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {
    private val _user = MutableLiveData<FirebaseUser?>()
    val user: LiveData<FirebaseUser?> get() = _user

    fun register(email: String, password: String, location: GeoPoint) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _user.value = auth.currentUser
                    addUserToFirestore(auth.currentUser, location)
                } else {
                    _user.value = null
                }
            }
    }

    fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    _user.value = auth.currentUser
                } else {
                    _user.value = null
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

    fun isLogin(): Boolean {
        return auth.currentUser != null
    }

    fun logout() {
        auth.signOut()
        _user.value = null
    }
}
