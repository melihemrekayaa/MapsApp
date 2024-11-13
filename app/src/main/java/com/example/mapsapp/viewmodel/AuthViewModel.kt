package com.example.mapsapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.mapsapp.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.GeoPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _user = MutableLiveData<FirebaseUser?>()
    val user: LiveData<FirebaseUser?> get() = _user

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    fun register(email: String, password: String) {
        authRepository.register(email, password) { firebaseUser ->
            if (firebaseUser != null) {
                _user.postValue(firebaseUser)
            } else {
                _error.postValue("Registration failed. Please try again.")
            }
        }
    }

    fun login(email: String, password: String) {
        authRepository.login(email, password) { firebaseUser ->
            _user.value = firebaseUser
        }
    }

    fun isLogin(): Boolean {
        return authRepository.getCurrentUser() != null
    }

    fun logout() {
        authRepository.logout()
        _user.value = null
    }

    fun clearError() {
        _error.value = null
    }


}
