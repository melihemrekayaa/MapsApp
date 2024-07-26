package com.example.mapsapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.mapsapp.repo.AuthRepository
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.GeoPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


class AuthViewModel constructor(private val authRepository: AuthRepository) : ViewModel() {

    private val _user = MutableLiveData<FirebaseUser?>()
    val user: LiveData<FirebaseUser?> get() = _user

    fun isLogin(): Boolean {
        return authRepository.getCurrentUser() != null
    }

    fun login(email: String, password: String) {
        authRepository.login(email, password) { firebaseUser ->
            _user.postValue(firebaseUser)
        }
    }

    fun register(email: String, password: String, location: GeoPoint) {
        authRepository.register(email, password, location) { firebaseUser ->
            _user.postValue(firebaseUser)
        }
    }

    fun logout() {
        authRepository.logout()
        _user.postValue(null)
    }
}
