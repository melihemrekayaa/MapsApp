// app/src/main/java/com/example/mapsapp/viewmodel/FriendsViewModel.kt
package com.example.mapsapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mapsapp.model.User
import com.example.mapsapp.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// app/src/main/java/com/example/mapsapp/viewmodel/FriendsViewModel.kt
@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _friendsList = MutableStateFlow<List<User>>(emptyList())
    val friendsList: StateFlow<List<User>> = _friendsList.asStateFlow()

    fun observeFriendsList(userId: String) {
        Log.d("FriendsViewModel", "observeFriendsList() called for userId=$userId")
        viewModelScope.launch {
            authRepository.getFriendsList(userId)
                .onEach { list ->
                    Log.d("FriendsViewModel", "Repository emitted ${list.size} friends: $list")
                }
                .catch { e ->
                    Log.e("FriendsViewModel", "Error fetching friends", e)
                }
                .collect { list ->
                    _friendsList.value = list
                    Log.d("FriendsViewModel", "StateFlow updated: ${_friendsList.value.size} friends")
                }
        }
    }
}

