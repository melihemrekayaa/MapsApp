package com.example.mapsapp.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val friends: List<String> = emptyList(),
    val friendRequests: List<String> = emptyList(),
    val isOnline: Boolean = false,

    @Transient
    val isRequestSent: Boolean = false
)
