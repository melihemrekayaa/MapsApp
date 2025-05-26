package com.example.mapsapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mapsapp.model.User
import com.example.mapsapp.repository.AuthRepository
import com.google.firebase.database.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _friendsWithStatus = MutableStateFlow<List<Pair<User, Boolean>>>(emptyList())
    val friendsWithStatus: StateFlow<List<Pair<User, Boolean>>> = _friendsWithStatus.asStateFlow()

    private var currentFriends: List<User> = emptyList()
    private val onlineStatusMap = mutableMapOf<String, Boolean>()
    private val lastSeenMap = mutableMapOf<String, Long?>()

    fun observeFriendsWithStatus() {
        val currentUser = authRepository.getCurrentUser()?.uid ?: return

        viewModelScope.launch {
            authRepository.getFriendsListRealtime(currentUser)
                .distinctUntilChanged()
                .collect { list ->
                    currentFriends = list
                    combineUsersWithStatus()
                }
        }

        listenOnlineStatus()
    }

    private fun listenOnlineStatus() {
        val ref = FirebaseDatabase.getInstance().getReference("usersOnlineStatus")

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onlineStatusMap.clear()
                lastSeenMap.clear()
                for (child in snapshot.children) {
                    val uid = child.key ?: continue
                    val isOnline = child.child("isOnline").getValue(Boolean::class.java) ?: false
                    val lastSeen = child.child("lastSeen").getValue(Long::class.java)

                    onlineStatusMap[uid] = isOnline
                    lastSeenMap[uid] = lastSeen
                }
                combineUsersWithStatus()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FriendsViewModel", "Failed to listen to online statuses", error.toException())
            }
        })
    }

    private fun combineUsersWithStatus() {
        val combined = currentFriends.map { user ->
            user.lastSeenTimestamp = lastSeenMap[user.uid]
            val isOnline = onlineStatusMap[user.uid] ?: false
            user to isOnline
        }
        _friendsWithStatus.value = combined
    }
}
