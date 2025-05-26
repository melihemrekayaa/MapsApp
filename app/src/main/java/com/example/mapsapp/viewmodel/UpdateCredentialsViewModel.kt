package com.example.mapsapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mapsapp.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpdateCredentialsViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _updateStatus = MutableStateFlow<String?>(null)
    val updateStatus: StateFlow<String?> = _updateStatus

    fun updateEmail(currentPassword: String, newEmail: String) {
        viewModelScope.launch {
            val result = authRepository.reauthenticateAndChangeEmail(currentPassword, newEmail)
            _updateStatus.value = if (result.isSuccess) {
                "Email updated successfully"
            } else {
                "Error: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun updatePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            val result = authRepository.reauthenticateAndChangePassword(currentPassword, newPassword)
            _updateStatus.value = if (result.isSuccess) {
                "Password updated successfully"
            } else {
                "Error: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun clearStatus() {
        _updateStatus.value = null
    }
}
