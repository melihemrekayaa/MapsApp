package com.example.mapsapp.webrtc

data class CallRequest(
    val callerUid: String = "",
    val calleeUid: String = "",
    val roomId: String = "",
    val isVideoCall: Boolean = true
)
