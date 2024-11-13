package com.example.mapsapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.mapsapp.model.User
import com.example.mapsapp.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AddFriendsViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _users = MutableLiveData<List<User>>()
    val users: LiveData<List<User>> get() = _users

    fun loadUsers() {
        repository.loadUsers { loadedUsers ->
            val filteredUsers = loadedUsers.filter { it.uid != repository.getCurrentUser()?.uid }
            _users.value = filteredUsers
        }
    }

    fun sendFriendRequest(receiverId: String) {
        repository.sendFriendRequest(receiverId) { success ->
            // Gerekirse işlem sonucunu UI'a yansıt
        }
    }

    fun cancelFriendRequest(receiverId: String) {
        repository.cancelFriendRequest(receiverId) { success ->
            // Gerekirse işlem sonucunu UI'a yansıt
        }
    }
}
