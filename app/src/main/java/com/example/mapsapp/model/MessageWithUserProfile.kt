package com.example.mapsapp.model

data class MessageWithUserProfile(
    val message: Message,
    val userName: String,
    val userPhotoBase64: String
)
