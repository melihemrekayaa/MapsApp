package com.example.mapsapp.model

data class FriendLocation(
    val uid: String,
    val name: String,
    val photoBase64: String?, // null olabilir
    val lat: Double,
    val lng: Double
)
