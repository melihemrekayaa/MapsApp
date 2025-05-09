package com.example.mapsapp.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.mapsapp.webrtc.CallActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    application: Application,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AndroidViewModel(application) {


    private val _user = MutableLiveData<FirebaseUser?>()
    val user: LiveData<FirebaseUser?> get() = _user

    init {
        _user.value = auth.currentUser
    }

    fun sendCallRequest(
        targetUid: String,
        isVideoCall: Boolean
    ) {
        val callerUid = auth.currentUser?.uid ?: return
        val roomId = UUID.randomUUID().toString()

        val callData = mapOf(
            "callerUid" to callerUid,
            "roomId" to roomId,
            "isVideoCall" to isVideoCall
        )

        FirebaseDatabase.getInstance()
            .getReference("callRequests")
            .child(targetUid)
            .push()
            .setValue(callData)

        val context = getApplication<Application>().applicationContext

        val intent = Intent(context, CallActivity::class.java).apply {
            putExtra("roomId", roomId)
            putExtra("callerUid", callerUid)
            putExtra("isCaller", true)
            putExtra("isVideoCall", isVideoCall)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)
    }


    fun logout() {
        auth.signOut()
        _user.value = null
    }
}
