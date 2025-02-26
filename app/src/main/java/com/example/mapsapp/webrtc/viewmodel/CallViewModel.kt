package com.example.mapsapp.webrtc.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.mapsapp.webrtc.firebaseClient.FirebaseClient
import com.example.mapsapp.webrtc.repository.MainRepository
import com.example.mapsapp.webrtc.utils.DataModel
import com.example.mapsapp.webrtc.utils.isValid
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CallViewModel @Inject constructor(
    private val mainRepository: MainRepository
) : ViewModel(), FirebaseClient.Listener {

    private val _incomingCall = MutableLiveData<DataModel?>()
    val incomingCall: LiveData<DataModel?> get() = _incomingCall

    private val _usersStatus = MutableLiveData<List<Pair<String, String>>>()
    val usersStatus: LiveData<List<Pair<String, String>>> get() = _usersStatus

    private var listener: CallListener? = null

    init {
        mainRepository.subscribeForLatestEvent(this)
        observeUsersStatus()
    }

    fun setCallListener(listener: CallListener) {
        this.listener = listener
    }

    fun observeUsersStatus() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        mainRepository.observeUsersStatus(currentUserId) { statusList ->
            _usersStatus.postValue(statusList)
        }
    }

    fun observeIncomingCalls(onCallReceived: (String, String) -> Unit) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        mainRepository.observeIncomingCalls(currentUserId, onCallReceived)
    }

    fun placeCall(caller: String, callee: String, callId: String, callback: (Boolean) -> Unit) {
        mainRepository.placeCall(caller, callee, callId, callback)
    }

    fun answerCall(callId: String) {
        mainRepository.answerCall(callId)
    }

    fun endCall(callId: String) {
        mainRepository.endCall(callId)
        _incomingCall.postValue(null)
        listener?.onCallEnded()
    }

    override fun onLatestEventReceived(data: DataModel) {
        if (data.isValid()) {
            _incomingCall.postValue(data)
            listener?.onCallReceived(data)
        }
    }

    interface CallListener {
        fun onCallReceived(model: DataModel)
        fun onCallEnded()
    }
}
