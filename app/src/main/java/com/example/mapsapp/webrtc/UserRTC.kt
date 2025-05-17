package com.example.mapsapp.webrtc

data class UserRTC(
    val uid: String = "",
    val name: String = "",
    val photoUrl: String? = null,
    val isInCall: Boolean = true
)
