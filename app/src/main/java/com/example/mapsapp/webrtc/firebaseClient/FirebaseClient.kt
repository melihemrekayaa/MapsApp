package com.example.mapsapp.webrtc.firebaseClient

import android.content.Intent
import android.util.Log
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat.startActivity
import com.example.mapsapp.webrtc.ui.CallActivity
import com.example.mapsapp.webrtc.utils.DataModel
import com.example.mapsapp.webrtc.utils.DataModelType
import com.example.mapsapp.webrtc.utils.FirebaseFieldNames.LATEST_EVENT
import com.example.mapsapp.webrtc.utils.FirebaseFieldNames.STATUS
import com.example.mapsapp.webrtc.utils.UserStatus
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseClient @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val gson: Gson
) {

    private var currentUserId: String? = null

    private fun setCurrentUserId(userId: String) {
        this.currentUserId = userId
    }

    fun updateUserStatus(status: UserStatus) {
        currentUserId?.let { userId ->
            firestore.collection("users")
                .document(userId)
                .update(STATUS, status.name)
                .addOnSuccessListener { Log.d("FirebaseClient", "User status updated: $status") }
                .addOnFailureListener { e -> Log.e("FirebaseClient", "Failed to update status", e) }
        }
    }

    fun listenForLatestEvent(listener: Listener) {
        currentUserId?.let { userId ->
            firestore.collection("users")
                .document(userId)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("FirebaseClient", "Error listening for latest event", e)
                        return@addSnapshotListener
                    }
                    snapshot?.let {
                        val latestEventData = it.getString(LATEST_EVENT)
                        latestEventData?.let { json ->
                            val event = gson.fromJson(json, DataModel::class.java)
                            listener.onLatestEventReceived(event)
                        }
                    }
                }
        }
    }

    fun sendMessageToOtherClient(message: DataModel, success: (Boolean) -> Unit) {
        // currentUserId, login işleminden sonra set ediliyor
        val messageWithSender = message.copy(sender = currentUserId)
        // JSON string’e çevirip, ardından Map’e dönüştürüyoruz
        val messageMap: Map<String, Any> = gson.fromJson(gson.toJson(messageWithSender), Map::class.java) as Map<String, Any>
        message.target?.let { targetId ->
            firestore.collection("users")
                .document(targetId)
                .update(LATEST_EVENT, messageMap)
                .addOnSuccessListener { success(true) }
                .addOnFailureListener { success(false) }
        }
    }


    fun clearLatestEvent() {
        currentUserId?.let { userId ->
            firestore.collection("users")
                .document(userId)
                .update(LATEST_EVENT, null)
        }
    }

    fun sendCallRequest(targetId: String, isVideoCall: Boolean, success: (Boolean) -> Unit) {
        val callData = hashMapOf(
            "sender" to currentUserId,
            "target" to targetId,
            "type" to if (isVideoCall) "StartVideoCall" else "StartAudioCall",
            "timeStamp" to System.currentTimeMillis(),
            "status" to "pending" // Çağrının kabul edilip edilmediğini takip edeceğiz
        )

        firestore.collection("calls").document(targetId)
            .set(callData)
            .addOnSuccessListener { success(true) }
            .addOnFailureListener { success(false) }
    }



    fun acceptCall(targetId: String) {
        FirebaseFirestore.getInstance().collection("calls").document(targetId).update("status", "accepted")
    }


    fun rejectCall(targetId: String) {
        FirebaseFirestore.getInstance().collection("calls").document(targetId).delete()
    }



    fun logOff(function: () -> Unit) {
        currentUserId?.let { userId ->
            firestore.collection("users")
                .document(userId)
                .update(STATUS, UserStatus.OFFLINE.name)
                .addOnCompleteListener { function() }
        }
    }

    interface Listener {
        fun onLatestEventReceived(event: DataModel)
    }
}
