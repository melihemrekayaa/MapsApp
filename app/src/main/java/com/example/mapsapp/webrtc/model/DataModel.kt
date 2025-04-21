package com.example.mapsapp.webrtc.model

import com.example.mapsapp.webrtc.utils.DataModelType

data class DataModel(
    val sender: String? = null,
    val target: String? = null,
    val type: DataModelType? = null,
    val timestamp: Long? = null // 🔥 zaman eklendi
) {
    fun isValid(): Boolean {
        val now = System.currentTimeMillis()
        return !sender.isNullOrBlank() && !target.isNullOrBlank() && type != null &&
                timestamp != null && now - timestamp < 30_000 // ⏱ 30 saniyeden eskiyse geçersiz
    }
}
