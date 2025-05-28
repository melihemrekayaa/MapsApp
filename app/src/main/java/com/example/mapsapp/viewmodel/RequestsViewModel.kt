package com.example.mapsapp.viewmodel

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
class RequestsViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _incomingRequests = MutableStateFlow<List<User>>(emptyList())
    val incomingRequests: StateFlow<List<User>> = _incomingRequests

    init {
        loadIncomingRequests()
    }

    fun loadIncomingRequests() {
        viewModelScope.launch {
            val currentUserId = repository.getCurrentUser()?.uid ?: return@launch
            repository.loadFriendRequests(currentUserId).collect {
                _incomingRequests.value = it
            }
        }
    }

    fun acceptRequest(fromUserId: String) {
        val currentUserId = repository.getCurrentUser()?.uid ?: return
        repository.acceptFriendRequest(currentUserId, fromUserId) {
            loadIncomingRequests()
        }
    }

    fun rejectRequest(fromUserId: String) {
        val currentUserId = repository.getCurrentUser()?.uid ?: return
        repository.removeFriendRequest(currentUserId, fromUserId)
        loadIncomingRequests()
    }
}
