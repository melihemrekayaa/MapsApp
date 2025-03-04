package com.example.mapsapp.webrtc.firebaseClient

import android.util.Log
import com.example.mapsapp.webrtc.utils.DataModel
import com.example.mapsapp.webrtc.utils.FirebaseFieldNames.LATEST_EVENT
import com.example.mapsapp.webrtc.utils.FirebaseFieldNames.PASSWORD
import com.example.mapsapp.webrtc.utils.FirebaseFieldNames.STATUS
import com.example.mapsapp.webrtc.utils.MyEventListener
import com.example.mapsapp.webrtc.utils.UserStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseClient @Inject constructor(
    private val dbRef: DatabaseReference,
    private val gson:Gson
) {

    private var currentUsername:String?=null
    private fun setUsername(username: String){
        this.currentUsername = username
    }


    fun login(username: String, password: String, done: (Boolean, String?) -> Unit) {
        dbRef.addListenerForSingleValueEvent(object  : MyEventListener(){
            override fun onDataChange(snapshot: DataSnapshot) {
                //if the current user exists
                if (snapshot.hasChild(username)){
                    //user exists , its time to check the password
                    val dbPassword = snapshot.child(username).child(PASSWORD).value
                    if (password == dbPassword) {
                        //password is correct and sign in
                        dbRef.child(username).child(STATUS).setValue(UserStatus.ONLINE)
                            .addOnCompleteListener {
                                setUsername(username)
                                done(true,null)
                            }.addOnFailureListener {
                                done(false,"${it.message}")
                            }
                    }else{
                        //password is wrong, notify user
                        done(false,"Password is wrong")
                    }

                }else{
                    //user doesnt exist, register the user
                    dbRef.child(username).child(PASSWORD).setValue(password).addOnCompleteListener {
                        dbRef.child(username).child(STATUS).setValue(UserStatus.ONLINE)
                            .addOnCompleteListener {
                                setUsername(username)
                                done(true,null)
                            }.addOnFailureListener {
                                done(false,it.message)
                            }
                    }.addOnFailureListener {
                        done(false,it.message)
                    }

                }
            }
        })
    }

    fun observeUsersStatus(status: (List<Pair<String, String>>) -> Unit) {
        dbRef.addValueEventListener(object : MyEventListener() {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.filter { it.key !=currentUsername }.map {
                    it.key!! to it.child(STATUS).value.toString()
                }
                status(list)
            }
        })
    }

    fun subscribeForLatestEvent(listener:Listener){
        try {
            dbRef.child(currentUsername!!).child(LATEST_EVENT).addValueEventListener(
                object : MyEventListener() {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        super.onDataChange(snapshot)
                        val event = try {
                            gson.fromJson(snapshot.value.toString(),DataModel::class.java)
                        }catch (e:Exception){
                            e.printStackTrace()
                            null
                        }
                        event?.let {
                            listener.onLatestEventReceived(it)
                        }
                    }
                }
            )
        }catch (e:Exception){
            e.printStackTrace()
        }
    }

    fun sendMessageToOtherClient(message:DataModel, success:(Boolean) -> Unit){
        val convertedMessage = gson.toJson(message.copy(sender = currentUsername))
        dbRef.child(message.target).child(LATEST_EVENT).setValue(convertedMessage)
            .addOnCompleteListener {
                success(true)
            }.addOnFailureListener {
                success(false)
            }
    }

    fun changeMyStatus(status: UserStatus) {
        dbRef.child(currentUsername!!).child(STATUS).setValue(status.name)
    }

    fun clearLatestEvent() {
        dbRef.child(currentUsername!!).child(LATEST_EVENT).setValue(null)
    }

    fun logOff(function:()->Unit) {
        dbRef.child(currentUsername!!).child(STATUS).setValue(UserStatus.OFFLINE)
            .addOnCompleteListener { function() }
    }


    interface Listener {
        fun onLatestEventReceived(event:DataModel)
    }
}
