package com.example.mapsapp.model

data class Message(
    val id: String = "", // YENİ EKLENDİ
    val senderId: String = "",
    val receiverId: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val senderPhotoBase64: String? = null
)

