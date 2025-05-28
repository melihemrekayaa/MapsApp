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
class FriendRequestsViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _requests = MutableStateFlow<List<User>>(emptyList())
    val requests: StateFlow<List<User>> = _requests

    fun loadRequests() {
        viewModelScope.launch {
            val currentUserId = repository.getCurrentUser()?.uid ?: return@launch
            val requesters = repository.getFriendRequests(currentUserId)
            _requests.value = requesters
        }
    }

    fun acceptRequest(fromUser: User) {
        viewModelScope.launch {
            val currentUserId = repository.getCurrentUser()?.uid ?: return@launch
            repository.acceptFriendRequest(currentUserId, fromUser.uid) {
                loadRequests()
            }
        }
    }


    fun rejectRequest(fromUser: User) {
        viewModelScope.launch {
            val currentUserId = repository.getCurrentUser()?.uid ?: return@launch
            repository.removeFriendRequest(currentUserId, fromUser.uid)
            loadRequests()
        }
    }

}

