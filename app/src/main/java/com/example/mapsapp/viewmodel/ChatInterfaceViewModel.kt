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

    private val _friendsWithFullStatus = MutableStateFlow<List<Triple<User, Boolean, Boolean>>>(emptyList())
    val friendsWithFullStatus: StateFlow<List<Triple<User, Boolean, Boolean>>> = _friendsWithFullStatus.asStateFlow()

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

        // Arkadaş listesi canlı olarak dinlenir
        viewModelScope.launch {
            repository.getFriendsListRealtime(currentUser).collect { friends ->
                latestFriends = friends
                updateCombinedList()
            }
        }

        // Online + Last Seen listener
        FirebaseDatabase.getInstance().getReference("usersOnlineStatus")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d("ChatInterfaceVM", "OnlineStatus snapshot received")

                    for (child in snapshot.children) {
                        val uid = child.key ?: continue
                        val isOnline = child.child("isOnline").getValue(Boolean::class.java) ?: false
                        val lastSeen = child.child("lastSeen").getValue(Long::class.java)

                        Log.d("ChatInterfaceVM", "[$uid] isOnline: $isOnline, lastSeen: $lastSeen")

                        onlineMap[uid] = isOnline
                        lastSeenMap[uid] = lastSeen
                    }

                    updateCombinedList()
                }


                override fun onCancelled(error: DatabaseError) {
                    Log.e("ChatInterfaceVM", "Online status listener failed", error.toException())
                }
            })

        // In Call listener
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
        val combined = latestFriends.map { user ->
            user.lastSeenTimestamp = lastSeenMap[user.uid]
            Triple(
                user,
                onlineMap[user.uid] == true,
                inCallMap[user.uid] == true
            )
        }
        _friendsWithFullStatus.value = combined
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

    fun acceptFriendRequest(currentUserUid: String, friendUid: String) {
        viewModelScope.launch {
            repository.acceptFriendRequest(currentUserUid, friendUid) { success ->
                if (success) {
                    _operationStatus.value = "Friend request accepted."
                    repository.removeFriendRequest(currentUserUid, friendUid)
                    repository.removeFriendRequest(friendUid, currentUserUid)
                } else {
                    _operationStatus.value = "Failed to accept friend request."
                }
            }
        }
    }

    fun removeFriendRequest(userUid: String, friendUid: String) {
        viewModelScope.launch {
            repository.removeFriendRequest(userUid, friendUid)
            _friendRequests.value = _friendRequests.value.filter { it.uid != friendUid }
        }
    }

    fun removeFriend(currentUserId: String, friendId: String) {
        viewModelScope.launch {
            val result = repository.removeFriend(currentUserId, friendId)
            _operationStatus.value = if (result) {
                "Friend removed successfully"
            } else {
                "Failed to remove friend"
            }
        }
    }

    fun clearOperationStatus() {
        _operationStatus.value = null
    }
}
