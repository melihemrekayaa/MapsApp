package com.example.mapsapp.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val friends: List<String> = emptyList(),
    val friendRequests: List<String> = emptyList(),
    val isOnline: Boolean = false,  // Kullanıcının çevrimiçi olup olmadığını takip etmek için
    val latestEvent: Event? = null  // Son etkinliği saklamak için
) {
    constructor() : this("", "", "", null, emptyList(), emptyList(), false, null)
}

data class Event(
    val eventType: String = "",
    val sender: String = "",
    val target: String = "",
    val timeStamp: Long = 0,
    val type: String = ""
)
