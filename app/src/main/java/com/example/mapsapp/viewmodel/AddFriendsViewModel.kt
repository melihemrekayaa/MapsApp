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

    private val _friendRequests = MutableStateFlow<List<User>>(emptyList())
    val friendRequests: StateFlow<List<User>> get() = _friendRequests

    fun loadUsers() {
        viewModelScope.launch {
            Log.d("AddFriendsViewModel", "loadUsers() started")
            try {
                val loadedUsers = repository.loadUsers()
                Log.d("AddFriendsViewModel", "Loaded users from repository: $loadedUsers")

                val filteredUsers = loadedUsers.filter { it.uid != repository.getCurrentUser()?.uid }
                Log.d("AddFriendsViewModel", "Filtered users: $filteredUsers")

                _users.value = filteredUsers
            } catch (e: Exception) {
                Log.e("AddFriendsViewModel", "Error loading users: ${e.message}")
            }
        }
    }

    fun sendFriendRequest(receiverId: String) {
        viewModelScope.launch {
            Log.d("AddFriendsViewModel", "Sending friend request to $receiverId")
            try {
                val success = repository.sendFriendRequest(receiverId)
                if (success) {
                    Log.d("AddFriendsViewModel", "Friend request sent to $receiverId successfully")
                } else {
                    Log.e("AddFriendsViewModel", "Failed to send friend request to $receiverId")
                }
            } catch (e: Exception) {
                Log.e("AddFriendsViewModel", "Error sending friend request: ${e.message}")
            }
        }
    }

    fun cancelFriendRequest(receiverId: String) {
        viewModelScope.launch {
            Log.d("AddFriendsViewModel", "Cancelling friend request for $receiverId")
            try {
                val success = repository.cancelFriendRequest(receiverId)
                if (success) {
                    Log.d("AddFriendsViewModel", "Friend request cancelled for $receiverId successfully")
                } else {
                    Log.e("AddFriendsViewModel", "Failed to cancel friend request for $receiverId")
                }
            } catch (e: Exception) {
                Log.e("AddFriendsViewModel", "Error cancelling friend request: ${e.message}")
            }
        }
    }



}
