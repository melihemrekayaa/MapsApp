package com.example.mapsapp.webrtc

import com.google.firebase.database.FirebaseDatabase

class FirebaseClient {

    private val dbRef = FirebaseDatabase.getInstance().reference

    fun sendCallRequest(
        receiverId: String,
        roomId: String,
        isVideoCall: Boolean,
        senderUid: String,
        onComplete: (Boolean) -> Unit
    ) {
        val callRequest = mapOf(
            "roomId" to roomId,
            "callerUid" to senderUid,
            "isVideoCall" to isVideoCall
        )

        dbRef.child("callRequests")
            .child(receiverId)
            .push()
            .setValue(callRequest)
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    fun acceptCall(roomId: String) {
        dbRef.child("calls")
            .child(roomId)
            .child("status")
            .setValue("accepted")
    }

    fun rejectCall(roomId: String) {
        dbRef.child("calls")
            .child(roomId)
            .child("status")
            .setValue("rejected")
    }

    fun cancelCall(roomId: String) {
        dbRef.child("calls").child(roomId).removeValue()
    }

    fun listenForCallStatus(roomId: String, onResult: (String?) -> Unit) {
        dbRef.child("calls").child(roomId).child("status")
            .get().addOnSuccessListener { snapshot ->
                onResult(snapshot.getValue(String::class.java))
            }
    }
}
