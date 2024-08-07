package com.example.mapsapp.webrtc.firebaseClient

import android.util.Log
import com.example.mapsapp.repository.AuthRepository
import com.example.mapsapp.webrtc.utils.DataModel
import com.example.mapsapp.webrtc.utils.UserStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseClient @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val gson: Gson
) {

    var currentUsername: String? = null
        private set

    private fun setUsername(username: String) {
        this.currentUsername = username
    }

    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid




    fun addUserToWebRTC(username: String, password: String, done: (Boolean) -> Unit) {
        val userRef = firestore.collection("users").document(username)
        val userData = mapOf(
            "password" to password,
            "status" to UserStatus.ONLINE.name
        )
        userRef.set(userData).addOnCompleteListener {
            done(it.isSuccessful)
        }
    }

    fun login(username: String, password: String, done: (Boolean, String?) -> Unit) {
        val userRef = firestore.collection("users").document(username)
        userRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val dbPassword = document.getString("password")
                if (password == dbPassword) {
                    userRef.update("status", UserStatus.ONLINE.name).addOnCompleteListener {
                        setUsername(username)
                        done(true, null)
                    }.addOnFailureListener {
                        done(false, it.message)
                    }
                } else {
                    done(false, "Password is wrong")
                }
            } else {
                val userData = mapOf(
                    "password" to password,
                    "status" to UserStatus.ONLINE.name
                )
                userRef.set(userData).addOnCompleteListener {
                    if (it.isSuccessful) {
                        setUsername(username)
                        done(true, null)
                    } else {
                        done(false, it.exception?.message)
                    }
                }
            }
        }.addOnFailureListener {
            done(false, it.message)
        }
    }

    fun observeUsersStatus(currentUserId: String, status: (List<Pair<String, String>>) -> Unit) {
        firestore.collection("users").addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null) {
                status(emptyList())
                return@addSnapshotListener
            }

            Log.e("FirebaseClient", "User ID: ${snapshot.documents[0]}")

            val list = snapshot.documents.filter { it.id != currentUserId }.mapNotNull {

                val userId = it.id
                val username = it.getString("username") ?: userId // Eğer kullanıcı adı yoksa, ID'yi kullan
                val userStatus = it.getString("status") ?: "Unknown"
                username to userStatus
            }
            status(list)
        }
    }

    fun placeCall(caller: String, callee: String, callId: String, callback: (Boolean) -> Unit) {
        val callRef = firestore.collection("calls").document(callId)
        val callData = mapOf(
            "caller" to caller,
            "callee" to callee,
            "status" to "calling"
        )
        callRef.set(callData).addOnCompleteListener {
            callback(it.isSuccessful)
        }
    }

    fun answerCall(callId: String) {
        val callRef = firestore.collection("calls").document(callId)
        callRef.update("status", "accepted")
    }

    fun endCall(callId: String) {
        val callRef = firestore.collection("calls").document(callId)
        callRef.update("status", "ended")
    }

    fun observeCallStatus(callId: String, listener: (String) -> Unit) {
        val callRef = firestore.collection("calls").document(callId)
        callRef.addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null) {
                listener("")
                return@addSnapshotListener
            }
            val status = snapshot.getString("status").orEmpty()
            listener(status)
        }
    }

    fun observeIncomingCalls(username: String, listener: (String, String) -> Unit) {
        firestore.collection("calls")
            .whereEqualTo("callee", username)
            .whereEqualTo("status", "calling")
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                for (document in snapshot.documents) {
                    val callId = document.id
                    val caller = document.getString("caller").orEmpty()
                    listener(callId, caller)
                }
            }
    }

    fun sendMessageToOtherClient(message: DataModel, success: (Boolean) -> Unit) {
        val convertedMessage = gson.toJson(message)
        firestore.collection("users").document(message.target)
            .update("latestEvent", convertedMessage)
            .addOnCompleteListener {
                success(it.isSuccessful)
            }
    }

    fun subscribeForLatestEvent(listener: Listener) {
        currentUserId?.let { username ->
            firestore.collection("users").document(username)
                .addSnapshotListener { snapshot, e ->
                    if (e != null || snapshot == null) return@addSnapshotListener
                    val eventJson = snapshot.getString("latestEvent") ?: return@addSnapshotListener
                    val event = gson.fromJson(eventJson, DataModel::class.java)
                    listener.onLatestEventReceived(event)
                }
        }
    }

    fun changeMyStatus(status: UserStatus) {
        currentUsername?.let {
            firestore.collection("users").document(it)
                .update("status", status.name)
        }
    }

    fun clearLatestEvent() {
        currentUsername?.let {
            firestore.collection("users").document(it)
                .update("latestEvent", null)
        }
    }

    fun logOff(function: () -> Unit) {
        currentUsername?.let {
            firestore.collection("users").document(it)
                .update("status", UserStatus.OFFLINE.name)
                .addOnCompleteListener { function() }
        }
    }

    interface Listener {
        fun onLatestEventReceived(event: DataModel)
    }
}
