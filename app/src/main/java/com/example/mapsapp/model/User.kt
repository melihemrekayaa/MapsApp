package com.example.mapsapp.model

data class User(
    val uid: String = "",
    val name: String = "",
    val profileImageUrl: String = "",
    var isRequestSent: Boolean = false // Dinamik olarak arkadaş isteği durumu
)
