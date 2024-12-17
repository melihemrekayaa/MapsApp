package com.example.mapsapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mapsapp.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    // Kullanıcı oturum durumu
    private val _currentUser = MutableStateFlow(authRepository.getCurrentUser())
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    // Auth sonucu için StateFlow
    private val _authResult = MutableStateFlow<AuthResult?>(null)
    val authResult: StateFlow<AuthResult?> = _authResult.asStateFlow()

    private val _isUserLoggedIn = MutableStateFlow(false)
    val isUserLoggedIn: StateFlow<Boolean> = _isUserLoggedIn.asStateFlow()

    // Ortak giriş/kayıt işlemleri için yardımcı metot
    private fun performAuthOperation(
        operation: suspend () -> Result<String>,
        successMessage: String
    ) {
        viewModelScope.launch {
            _authResult.value = AuthResult.Loading
            try {
                val result = operation()
                result.onSuccess {
                    _authResult.value = AuthResult.Success(it) // Başarı mesajını döndür
                    _currentUser.value = authRepository.getCurrentUser() // Kullanıcıyı güncelle
                }.onFailure { e ->
                    _authResult.value = AuthResult.Error(e.message ?: "An error occurred")
                }
            } catch (e: Exception) {
                _authResult.value = AuthResult.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

    // Kullanıcı kayıt işlemi
    fun register(name: String, email: String, password: String) {
        performAuthOperation(
            operation = { authRepository.register(name, email, password) },
            successMessage = "Registration successful"
        )
    }

    // Kullanıcı giriş işlemi
    fun login(email: String, password: String) {
        performAuthOperation(
            operation = { authRepository.login(email, password) },
            successMessage = "Login successful"
        )
    }

    // Kullanıcı çıkış işlemi
    fun logout() {
        authRepository.logout()
        _currentUser.value = null
        _authResult.value = AuthResult.Success("Logged out successfully")
    }

    // Sealed class: Auth işlemlerinin durumları
    sealed class AuthResult {
        data class Success(val message: String) : AuthResult()
        data class Error(val message: String) : AuthResult()
        object Loading : AuthResult()
    }
}
