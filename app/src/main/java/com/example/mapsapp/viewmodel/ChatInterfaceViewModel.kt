package com.example.mapsapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mapsapp.model.User
import com.example.mapsapp.repository.AuthRepository
import com.example.mapsapp.webrtc.UserRTC
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
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

    private val _inCallMap = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val inCallMap: StateFlow<Map<String, Boolean>> = _inCallMap.asStateFlow()

    fun fetchFriendsList(userId: String) {
        viewModelScope.launch {
            repository.getFriendsList(userId)
                .catch { e -> Log.e("ChatInterfaceViewModel", "Error fetching friends: ${e.message}") }
                .collectLatest { friends ->
                    _friendsList.value = friends
                }
        }
    }

    fun observeInCallStatuses() {
        viewModelScope.launch {
            FirebaseDatabase.getInstance().getReference("users")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val updatedMap = snapshot.children.associate { snap ->
                            val uid = snap.key ?: return@associate "" to false
                            val inCall = snap.child("inCall").getValue(Boolean::class.java) ?: false
                            uid to inCall
                        }
                        _inCallMap.value = updatedMap
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }
    }

    fun resetInCallState() {
        viewModelScope.launch {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("inCall")
                .setValue(false)
        }
    }


    fun loadFriendRequests(userUid: String) {
        viewModelScope.launch {
            repository.loadFriendRequests(userUid)
                .collect { requests ->
                    Log.d("ChatInterfaceViewModel", "Friend requests fetched: ${requests.map { it.name }}")
                    _friendRequests.value = requests
                }
        }
    }

    fun acceptFriendRequest(currentUserUid: String, friendUid: String) {
        viewModelScope.launch {
            repository.acceptFriendRequest(currentUserUid, friendUid) { success ->
                if (success) {
                    _operationStatus.value = "Friend request accepted."
                    repository.removeFriendRequest(currentUserUid, friendUid)
                    repository.removeFriendRequest(friendUid, currentUserUid)
                    fetchFriendsList(currentUserUid) // Güncelleme
                } else {
                    _operationStatus.value = "Error: Failed to accept friend request."
                }
            }
        }
    }

    fun removeFriendRequest(userUid: String, friendUid: String) {
        viewModelScope.launch {
            repository.removeFriendRequest(userUid, friendUid)
            _friendRequests.value = _friendRequests.value.filter { it.uid != friendUid } // UI'den de kaldır
        }
    }


    fun removeFriend(currentUserId: String, friendId: String) {
        viewModelScope.launch {
            val result = repository.removeFriend(currentUserId, friendId)
            if (result) {
                _operationStatus.value = "Friend removed successfully"
                fetchFriendsList(currentUserId)
            } else {
                _operationStatus.value = "Error: Failed to remove friend"
            }
        }
    }

    fun clearOperationStatus() {
        _operationStatus.value = null
    }
}

