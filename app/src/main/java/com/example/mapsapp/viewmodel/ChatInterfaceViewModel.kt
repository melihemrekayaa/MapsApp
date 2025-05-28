package com.example.mapsapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mapsapp.model.User
import com.example.mapsapp.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatInterfaceViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _friendsWithFullStatus = MutableStateFlow<List<User>>(emptyList())
    val friendsWithFullStatus: StateFlow<List<User>> = _friendsWithFullStatus.asStateFlow()

    private val _friendRequests = MutableStateFlow<List<User>>(emptyList())
    val friendRequests: StateFlow<List<User>> = _friendRequests.asStateFlow()

    private val _operationStatus = MutableStateFlow<String?>(null)
    val operationStatus: StateFlow<String?> = _operationStatus.asStateFlow()

    private val onlineMap = mutableMapOf<String, Boolean>()
    private val inCallMap = mutableMapOf<String, Boolean>()
    private val lastSeenMap = mutableMapOf<String, Long?>()
    private var latestFriends: List<User> = emptyList()

    init {
        observeRealtimeData()
    }

    private fun observeRealtimeData() {
        val currentUser = repository.getCurrentUser()?.uid ?: return

        viewModelScope.launch {
            repository.getFriendsListRealtime(currentUser).collect { friends ->
                latestFriends = friends
                updateCombinedList()
            }
        }

        FirebaseDatabase.getInstance().getReference("usersOnlineStatus")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (child in snapshot.children) {
                        val uid = child.key ?: continue
                        val isOnline = child.child("isOnline").getValue(Boolean::class.java) ?: false
                        val lastSeen = child.child("lastSeen").getValue(Long::class.java)
                        onlineMap[uid] = isOnline
                        lastSeenMap[uid] = lastSeen
                    }
                    updateCombinedList()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ChatInterfaceVM", "Online status listener failed", error.toException())
                }
            })

        FirebaseDatabase.getInstance().getReference("users")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (child in snapshot.children) {
                        val uid = child.key ?: continue
                        inCallMap[uid] = child.child("inCall").getValue(Boolean::class.java) ?: false
                    }
                    updateCombinedList()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ChatInterfaceVM", "In call status listener failed", error.toException())
                }
            })
    }

    private fun updateCombinedList() {
        val updated = latestFriends.map { user ->
            user.lastSeenTimestamp = lastSeenMap[user.uid]
            user.isInCall = inCallMap[user.uid] == true
            user.copy().apply {
                isInCall = inCallMap[user.uid] == true
                lastSeenTimestamp = lastSeenMap[user.uid]
            }.also {
                onlineMap[it.uid]?.let { online -> it.isOnline = online }
            }
        }
        _friendsWithFullStatus.value = updated
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
            repository.loadFriendRequests(userUid).collect {
                _friendRequests.value = it
            }
        }
    }

    fun removeFriend(currentUserId: String, friendId: String) {
        viewModelScope.launch {
            val result = repository.removeFriend(currentUserId, friendId)
            if (result) {
                val updatedList = _friendsWithFullStatus.value.filterNot { it.uid == friendId }
                _friendsWithFullStatus.value = updatedList
                _operationStatus.value = "Friend removed successfully"
            } else {
                _operationStatus.value = "Failed to remove friend"
            }
        }
    }

    fun clearOperationStatus() {
        _operationStatus.value = null
    }
}

