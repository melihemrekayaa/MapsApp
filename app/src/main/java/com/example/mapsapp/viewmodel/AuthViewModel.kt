package com.example.mapsapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class AuthViewModel : ViewModel() {
    private val _user = MutableLiveData<FirebaseUser?>()
    val user: LiveData<FirebaseUser?> get() = _user

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    fun register(email:String,password:String){
        auth.createUserWithEmailAndPassword(email,password)
            .addOnCompleteListener{task ->
                if(task.isSuccessful){
                    _user.value = auth.currentUser
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

    fun logout(){
        auth.signOut()
        _user.value = null
    }
}