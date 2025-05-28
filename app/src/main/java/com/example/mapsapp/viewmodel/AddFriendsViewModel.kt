package com.example.mapsapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mapsapp.model.User
import com.example.mapsapp.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddFriendsViewModel @Inject constructor(
    val repository: AuthRepository
) : ViewModel() {

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> get() = _users

    private val _operationStatus = MutableStateFlow<String?>(null)
    val operationStatus: StateFlow<String?> get() = _operationStatus

    fun loadUsers() {
        viewModelScope.launch {
            try {
                val loadedUsers = repository.loadUsers()
                _users.value = loadedUsers
            } catch (e: Exception) {
                Log.e("AddFriendsViewModel", "Error loading users: ${e.message}")
            }
        }
    }

    fun sendFriendRequest(receiverId: String) {
        viewModelScope.launch {
            val success = repository.sendFriendRequest(receiverId)
            _operationStatus.value = if (success) {
                loadUsers()
                "Arkadaşlık isteği gönderildi"
            } else {
                "Zaten gönderilmiş"
            }
        }
    }

    fun cancelFriendRequest(receiverId: String) {
        viewModelScope.launch {
            val success = repository.cancelFriendRequest(receiverId)
            _operationStatus.value = if (success) {
                loadUsers()
                "Arkadaşlık isteği iptal edildi"
            } else {
                "İptal başarısız"
            }
        }
    }

    fun clearStatus() {
        _operationStatus.value = null
    }
}
