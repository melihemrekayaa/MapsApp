package com.example.mapsapp.model

import com.google.firebase.firestore.GeoPoint

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val location: GeoPoint? = null,
    val photoUrl: String? = null,
    val friends: List<String> = emptyList(),
    val friendRequests: List<String> = emptyList(),
    val isOnline: Boolean = false,
    @Transient
    val isRequestSent: Boolean = false,
    var isInCall: Boolean = false
)
