package com.example.mapsapp.webrtc


import com.example.mapsapp.webrtc.utils.awaitRemoveValue
import com.example.mapsapp.webrtc.utils.awaitSetValue
import com.example.mapsapp.webrtc.utils.awaitSingle
import com.google.firebase.database.*
import kotlinx.coroutines.tasks.await

class FirebaseClient {

    private val dbRef = FirebaseDatabase.getInstance().reference

    suspend fun sendCallRequest(
        receiverId: String,
        roomId: String,
        isVideoCall: Boolean,
        senderUid: String
    ): Boolean {
        return try {
            val callRequest = mapOf(
                "roomId" to roomId,
                "callerUid" to senderUid,
                "isVideoCall" to isVideoCall
            )
            dbRef.child("callRequests")
                .child(receiverId)
                .setValue(callRequest)
                .await()

            // (İsteğe bağlı) Arayan kişi meşgule geçsin
            setUserInCall(senderUid, true)

            true
        } catch (e: Exception) {
            false
        }
    }


    suspend fun acceptCall(roomId: String) {
        dbRef.child("calls")
            .child(roomId)
            .child("status")
            .awaitSetValue("accepted")
    }

    suspend fun rejectCall(roomId: String) {
        dbRef.child("calls").child(roomId).child("status").awaitSetValue("rejected")

        // Güvence için callEnded da yaz
        dbRef.child("calls").child(roomId).child("callEnded").awaitSetValue(true)
    }

    suspend fun cancelCall(roomId: String) {
        dbRef.child("calls").child(roomId).awaitRemoveValue()
    }


    suspend fun listenForCallStatus(roomId: String): String? {
        return try {
            val snapshot = dbRef.child("calls")
                .child(roomId)
                .child("status")
                .awaitSingle()
            snapshot.getValue(String::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun removeCallRequest(receiverId: String, roomId: String) {
        try {
            val ref = dbRef.child("callRequests").child(receiverId)

            val snapshot = ref.awaitSingle()

            // callRequests tek bir obje tutuyorsa
            val currentRoomId = snapshot.child("roomId").getValue(String::class.java)
            if (currentRoomId == roomId) {
                ref.awaitRemoveValue()
                return
            }

            // callRequests altında birden fazla obje varsa (push ile eklenmişse)
            snapshot.children.forEach { child ->
                val rid = child.child("roomId").getValue(String::class.java)
                if (rid == roomId) {
                    child.ref.awaitRemoveValue()
                }
            }

        } catch (_: Exception) { }
    }


    suspend fun isUserInCall(userId: String): Boolean {
        return try {
            val snapshot = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("inCall")
                .awaitSingle()
            snapshot.getValue(Boolean::class.java) ?: false
        } catch (e: Exception) {
            false
        }
    }

    suspend fun setUserInCall(userId: String, inCall: Boolean) {
        FirebaseDatabase.getInstance()
            .getReference("users")
            .child(userId)
            .child("inCall")
            .awaitSetValue(inCall)
    }

}
