package com.example.mapsapp.viewmodel

import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class AuthViewModel : ViewModel() {
    private val _user = MutableLiveData<FirebaseUser?>()
    val user: LiveData<FirebaseUser?> get() = _user

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy {FirebaseFirestore.getInstance()}

    fun register(email:String,password:String){
        auth.createUserWithEmailAndPassword(email,password)
            .addOnCompleteListener{task ->
                if(task.isSuccessful){
                    _user.value = auth.currentUser
                    addUserToFirestore(auth.currentUser)
                }else{
                    _user.value = null
                }
            }
    }

    fun login(email: String,password: String){
        auth.signInWithEmailAndPassword(email,password)
            .addOnCompleteListener {
                if(it.isSuccessful){
                    _user.value = auth.currentUser
                }else{
                    _user.value = null
                }
            }
    }

    fun addUserToFirestore(firebaseUser: FirebaseUser?){
        firebaseUser?.let { user ->
            val userData = hashMapOf(
                "uid" to user.uid,
                "email" to user.email
            )
            firestore.collection("users").document(user.uid).set(userData)
        }

    }

    fun isLogin() : Boolean{
        return  null != auth.currentUser
    }

    fun logout(){
        auth.signOut()
        _user.value = null
    }
}