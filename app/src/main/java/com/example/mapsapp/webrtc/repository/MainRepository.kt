package com.example.mapsapp.webrtc.repository

import com.example.mapsapp.webrtc.firebaseClient.FirebaseClient
import com.example.mapsapp.webrtc.utils.DataModel
import com.example.mapsapp.webrtc.utils.UserStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MainRepository @Inject constructor(
    private val firebaseClient: FirebaseClient
) {

    fun observeUsersStatus(currentUserId: String, status: (List<Pair<String, String>>) -> Unit) {
        firebaseClient.observeUsersStatus(currentUserId, status)
    }

    fun observeIncomingCalls(username: String, listener: (String, String) -> Unit) {
        firebaseClient.observeIncomingCalls(username, listener)
    }

    fun subscribeForLatestEvent(listener: FirebaseClient.Listener) {
        firebaseClient.subscribeForLatestEvent(listener)
    }

    fun placeCall(caller: String, callee: String, callId: String, callback: (Boolean) -> Unit) {
        firebaseClient.placeCall(caller, callee, callId, callback)
    }

    fun answerCall(callId: String) {
        firebaseClient.answerCall(callId)
    }

    fun endCall(callId: String) {
        firebaseClient.endCall(callId)
    }

    fun sendMessageToOtherClient(message: DataModel, success: (Boolean) -> Unit) {
        firebaseClient.sendMessageToOtherClient(message, success)
    }

    fun logOff(completion: () -> Unit) {
        firebaseClient.logOff(completion)
    }

    fun clearLatestEvent() {
        firebaseClient.clearLatestEvent()
    }
}
