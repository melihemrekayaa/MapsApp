package com.example.mapsapp.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val friends: List<String> = listOf(), // UID listesi olmalı
    val friendRequests: List<String> = listOf(),
    val isOnline: Boolean = false,
    val from : String = ""

)

