package com.example.mapsapp.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.GeoPoint

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val location: GeoPoint? = null,
    val photoUrl: String? = null,
    val friends: List<String> = emptyList(),
    var lastSeenTimestamp: Long? = null,
    var isInCall: Boolean = false,
    var photoBase64: String? = null,
    var isOnline: Boolean = false,

    @get:Exclude
    var isRequestSent: Boolean = false
)
