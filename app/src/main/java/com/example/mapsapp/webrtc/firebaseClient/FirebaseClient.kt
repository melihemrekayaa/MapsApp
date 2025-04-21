package com.example.mapsapp.webrtc.firebaseClient

import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseClient @Inject constructor() {

    private val db = FirebaseFirestore.getInstance()

    fun acceptCall(userId: String) {
        db.collection("calls").document(userId)
            .update("response", "accept")
    }

    fun rejectCall(userId: String) {
        db.collection("calls").document(userId)
            .update("response", "reject")
    }

    fun endCall(userId: String) {
        db.collection("calls").document(userId).delete()
    }
}
