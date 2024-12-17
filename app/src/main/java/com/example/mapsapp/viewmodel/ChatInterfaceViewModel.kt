package com.example.mapsapp.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
class ChatInterfaceViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _friends = MutableStateFlow<List<User>>(emptyList())
    val friends: StateFlow<List<User>> get() = _friends

    private val _friendRequests = MutableStateFlow<List<User>>(emptyList())
    val friendRequests: StateFlow<List<User>> get() = _friendRequests

    fun loadFriends() {
        viewModelScope.launch {
            try {
                val loadedFriends = repository.loadFriends()
                _friends.value = loadedFriends
            } catch (e: Exception){
                _friends.value = emptyList()
            }
        }
    }

    fun loadFriendRequests() {
        viewModelScope.launch {
            try {
                val requests = repository.loadFriendRequests()
                _friendRequests.value = requests
                Log.d("AddFriendsViewModel", "Friend requests: $requests")
            } catch (e: Exception) {
                Log.e("AddFriendsViewModel", "Error loading friend requests: ${e.message}")
            }
        }
    }

    fun acceptFriendRequest(senderId: String) {
        viewModelScope.launch {
            try {
                val success = repository.acceptFriendRequest(senderId)
                if (success) {
                    Log.d("ChatInterfaceViewModel", "Friend request accepted from $senderId")
                    loadFriends() // Arkadaş listesini güncelle
                    loadFriendRequests() // İstek listesini güncelle
                } else {
                    Log.e("ChatInterfaceViewModel", "Failed to accept friend request from $senderId")
                }
            } catch (e: Exception) {
                Log.e("ChatInterfaceViewModel", "Error accepting friend request: ${e.message}")
            }
        }
    }
}
