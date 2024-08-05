package com.example.mapsapp.webrtc.firebaseClient

import com.example.mapsapp.webrtc.utils.DataModel
import com.example.mapsapp.webrtc.utils.UserStatus
import com.google.firebase.database.*
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseClient @Inject constructor(
    private val database: FirebaseDatabase,
    private val gson: Gson
) {

    var currentUsername: String? = null
        private set

    private fun setUsername(username: String) {
        this.currentUsername = username
    }

    fun addUserToWebRTC(username: String, password: String, done: (Boolean) -> Unit) {
        val userRef = database.getReference("users/$username")
        userRef.child("password").setValue(password).addOnCompleteListener {
            if (it.isSuccessful) {
                userRef.child("status").setValue(UserStatus.ONLINE.name).addOnCompleteListener { statusTask ->
                    done(statusTask.isSuccessful)
                }
            } else {
                done(false)
            }
        }
    }

    fun login(username: String, password: String, done: (Boolean, String?) -> Unit) {
        val userRef = database.getReference("users/$username")
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val dbPassword = snapshot.child("password").getValue(String::class.java)
                    if (password == dbPassword) {
                        userRef.child("status").setValue(UserStatus.ONLINE.name).addOnCompleteListener {
                            setUsername(username)
                            done(true, null)
                        }.addOnFailureListener {
                            done(false, it.message)
                        }
                    } else {
                        done(false, "Password is wrong")
                    }
                } else {
                    val userMap = mapOf(
                        "password" to password,
                        "status" to UserStatus.ONLINE.name
                    )
                    userRef.setValue(userMap).addOnCompleteListener {
                        if (it.isSuccessful) {
                            setUsername(username)
                            done(true, null)
                        } else {
                            done(false, it.exception?.message)
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                done(false, error.message)
            }
        })
    }

    fun observeUsersStatus(status: (List<Pair<String, String>>) -> Unit) {
        val usersRef = database.getReference("users")
        usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.filter { it.key != currentUsername }.map {
                    it.key!! to it.child("status").getValue(String::class.java).orEmpty()
                }
                status(list)
            }

            override fun onCancelled(error: DatabaseError) {
                status(emptyList())
            }
        })
    }

    fun placeCall(caller: String, callee: String, callId: String, callback: (Boolean) -> Unit) {
        val callRef = database.getReference("calls/$callId")
        val callData = mapOf(
            "caller" to caller,
            "callee" to callee,
            "status" to "calling"
        )
        callRef.setValue(callData).addOnCompleteListener {
            callback(it.isSuccessful)
        }
    }

    fun answerCall(callId: String) {
        val callRef = database.getReference("calls/$callId")
        callRef.child("status").setValue("accepted")
    }

    fun endCall(callId: String) {
        val callRef = database.getReference("calls/$callId")
        callRef.child("status").setValue("ended")
    }

    fun observeCallStatus(callId: String, listener: (String) -> Unit) {
        val callRef = database.getReference("calls/$callId")
        callRef.child("status").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val status = snapshot.getValue(String::class.java)
                listener(status.orEmpty())
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun observeIncomingCalls(username: String, listener: (String, String) -> Unit) {
        val callsRef = database.getReference("calls")
        callsRef.orderByChild("callee").equalTo(username).addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val callId = snapshot.key!!
                val caller = snapshot.child("caller").getValue(String::class.java)!!
                val status = snapshot.child("status").getValue(String::class.java)
                if (status == "calling") {
                    listener(callId, caller)
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun sendMessageToOtherClient(message: DataModel, success: (Boolean) -> Unit) {
        val convertedMessage = gson.toJson(message)
        database.getReference("users/${message.target}/latestEvent").setValue(convertedMessage)
            .addOnCompleteListener {
                success(it.isSuccessful)
            }.addOnFailureListener {
                success(false)
            }
    }

    fun subscribeForLatestEvent(listener: Listener) {
        currentUsername?.let { username ->
            database.getReference("users/$username/latestEvent").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val event = snapshot.getValue(String::class.java)?.let {
                        gson.fromJson(it, DataModel::class.java)
                    }
                    event?.let {
                        listener.onLatestEventReceived(it)
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

    fun changeMyStatus(status: UserStatus) {
        currentUsername?.let {
            database.getReference("users/$it/status").setValue(status.name)
        }
    }

    fun clearLatestEvent() {
        currentUsername?.let {
            database.getReference("users/$it/latestEvent").setValue(null)
        }
    }

    fun logOff(function: () -> Unit) {
        currentUsername?.let {
            database.getReference("users/$it/status").setValue(UserStatus.OFFLINE.name)
                .addOnCompleteListener { function() }
        }
    }

    interface Listener {
        fun onLatestEventReceived(event: DataModel)
    }
}
