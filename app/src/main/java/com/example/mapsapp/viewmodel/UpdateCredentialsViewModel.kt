package com.example.mapsapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mapsapp.repository.AuthRepository
import com.example.mapsapp.util.SecurePreferences
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpdateCredentialsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val securePreferences: SecurePreferences
) : ViewModel() {

    private val _updateStatus = MutableStateFlow<String?>(null)
    val updateStatus: StateFlow<String?> get() = _updateStatus

    fun reauthenticateAndChangeEmail(currentPassword: String, newEmail: String) {
        viewModelScope.launch {
            val result = authRepository.reauthenticateAndChangeEmail(currentPassword, newEmail)
            if (result.isSuccess) {
                _updateStatus.value = "Email updated successfully. Please verify your new email and log in again."
            } else {
                _updateStatus.value = "Email update failed: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun updatePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            val result = authRepository.reauthenticateAndChangePassword(currentPassword, newPassword)
            if (result.isSuccess) {
                _updateStatus.value = "Password updated successfully. Please log in again."
            } else {
                _updateStatus.value = "Password update failed: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun logoutAndClearCredentials() {
        viewModelScope.launch {
            authRepository.logout()
            // securePreferences da burada temizlenmeli
            securePreferences.clearCredentials()
        }
    }


    fun getCurrentUser(): FirebaseUser? {
        return authRepository.getCurrentUser()
    }

    fun clearStaySignedIn() {
        authRepository.clearStaySignedIn()
    }


    fun clearStatus() {
        _updateStatus.value = null
    }
}
