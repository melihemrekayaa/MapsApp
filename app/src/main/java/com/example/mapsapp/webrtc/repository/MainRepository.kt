package com.example.mapsapp.webrtc.repository

import android.util.Log
import com.example.mapsapp.webrtc.model.DataModel
import com.example.mapsapp.webrtc.utils.DataModelType
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MainRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    fun sendConnectionRequest(
        targetUid: String,
        isVideoCall: Boolean,
        onResult: (Boolean) -> Unit
    ) {
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val senderUid = currentUser?.uid ?: run {
            Log.e("MainRepository", "Kullanıcı girişi yok!")
            onResult(false)
            return
        }

        val callType = if (isVideoCall) DataModelType.StartVideoCall else DataModelType.StartVoiceCall

        val callData = DataModel(
            type = callType,
            sender = senderUid,
            target = targetUid
        )

        firestore.collection("calls")
            .document(targetUid) // Karşı tarafın UID'si
            .set(callData)
            .addOnSuccessListener {
                Log.d("MainRepository", "📡 Çağrı gönderildi: $callData")
                onResult(true)
            }
            .addOnFailureListener { e ->
                Log.e("MainRepository", "❌ Çağrı gönderilemedi: ${e.message}")
                onResult(false)
            }
    }
}
