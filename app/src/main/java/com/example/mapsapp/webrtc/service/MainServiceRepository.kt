package com.example.mapsapp.webrtc.service

import android.content.Context
import android.util.Log
import com.example.mapsapp.webrtc.model.DataModel

import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MainServiceRepository @Inject constructor(
    private val context: Context
) {

    private val firestore = FirebaseFirestore.getInstance()

    fun startService(currentUserId: String) {
        firestore.collection("calls")
            .document(currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

                val call = snapshot.toObject(DataModel::class.java) ?: return@addSnapshotListener
                if (!call.isValid() || call.target != currentUserId) return@addSnapshotListener

                Log.d("MainServiceRepository", "Gelen çağrı var: $call")

                // Buraya sistem genelindeki gelen çağrı eventi eklenebilir
                // Örn: NotificationManager.showCallNotification(...)
            }
    }
}
