package com.example.mapsapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.mapsapp.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ChatInterfaceViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _users = MutableLiveData<List<User>>()
    val users: LiveData<List<User>> get() = _users

    fun loadUsers() {
        firestore.collection("users")
            .get()
            .addOnSuccessListener { documents ->
                val userList = mutableListOf<User>()
                for (doc in documents) {
                    val user = doc.toObject(User::class.java)
                    if (user.uid != auth.currentUser?.uid) {
                        userList.add(user)
                    }
                }
                _users.value = userList
            }
            .addOnFailureListener {
                _users.value = emptyList()
            }
    }
}
