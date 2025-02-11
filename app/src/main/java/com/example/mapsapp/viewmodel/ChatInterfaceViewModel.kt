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

    private val _operationStatus = MutableLiveData<String?>()
    val operationStatus: LiveData<String?> get() = _operationStatus

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

    fun loadFriendRequests(userUid: String) {
        repository.loadFriendRequests(userUid) { requests ->
            _friendRequests.value = requests
        }
    }

    fun acceptFriendRequest(currentUserUid: String, friendUid: String) {
        repository.acceptFriendRequest(currentUserUid, friendUid) { success ->
            if (success) {
                _operationStatus.postValue("Arkadaşlık isteği kabul edildi.")
                loadFriendRequests(currentUserUid) // Listeyi güncelle
            } else {
                _operationStatus.postValue("Bir hata oluştu.")
            }
        }
    }

    fun clearOperationStatus() {
        _operationStatus.postValue(null)
    }
}
