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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatInterfaceViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _friendsList = MutableStateFlow<List<User>>(emptyList())
    val friendsList: StateFlow<List<User>> = _friendsList.asStateFlow()

    private val _friendRequests = MutableStateFlow<List<User>>(emptyList())
    val friendRequests: StateFlow<List<User>> = _friendRequests.asStateFlow()

    private val _operationStatus = MutableStateFlow<String?>(null)
    val operationStatus: StateFlow<String?> = _operationStatus.asStateFlow()



    fun fetchFriendsList(userId: String) {
        viewModelScope.launch {
            repository.getFriendsList(userId)
                .collect { friends ->
                    _friendsList.value = emptyList() // Clear first to force refresh
                    _friendsList.value = friends // Now set the updated list
                }
        }
    }






    fun loadFriendRequests(userUid: String) {
        viewModelScope.launch {
            repository.loadFriendRequests(userUid)
                .collect { requests ->
                    _friendRequests.value = requests
                }
        }
    }

    fun acceptFriendRequest(currentUserUid: String, friendUid: String) {
        viewModelScope.launch {
            try {
                repository.acceptFriendRequest(currentUserUid, friendUid) { success ->
                    if (success) {
                        _operationStatus.value = "Friend request accepted."
                        loadFriendRequests(currentUserUid) // Refresh the list
                    } else {
                        _operationStatus.value = "Error: Failed to accept friend request."
                    }
                }
            } catch (e: Exception) {
                _operationStatus.value = "Error: ${e.message}"
            }
        }
    }

    fun clearOperationStatus() {
        _operationStatus.value = null
    }
}
