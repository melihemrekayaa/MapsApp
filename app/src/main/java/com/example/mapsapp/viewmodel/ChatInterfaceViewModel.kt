package com.example.mapsapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.mapsapp.model.User
import com.example.mapsapp.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ChatInterfaceViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _friends = MutableLiveData<List<User>>()
    val friends: LiveData<List<User>> get() = _friends

    fun loadFriends() {
        repository.loadFriends { loadedFriends ->
            _friends.value = loadedFriends
        }
    }
}
