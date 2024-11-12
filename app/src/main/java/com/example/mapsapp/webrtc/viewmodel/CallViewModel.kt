package com.example.mapsapp.webrtc.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.mapsapp.webrtc.repository.MainRepository
import com.example.mapsapp.webrtc.utils.DataModel
import com.example.mapsapp.webrtc.utils.isValid
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import org.webrtc.SurfaceViewRenderer
import javax.inject.Inject

@HiltViewModel
class CallViewModel @Inject constructor(
    private val mainRepository: MainRepository
) : ViewModel(), MainRepository.Listener {

    private val _incomingCall = MutableLiveData<DataModel>()
    val incomingCall: LiveData<DataModel> get() = _incomingCall

    private var listener: CallListener? = null

    init {
        mainRepository.listener = this
        mainRepository.initFirebase()
    }

    fun setCallListener(listener: CallListener) {
        this.listener = listener
    }

    fun observeIncomingCalls(onCallReceived: (String, String) -> Unit) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        mainRepository.observeIncomingCalls(currentUserId, onCallReceived)
    }

    fun observeUsersStatus(currentUserId: String, status: (List<Pair<String, String>>) -> Unit) {
        mainRepository.observeUsersStatus(currentUserId, status)
    }

    fun sendConnectionRequest(target: String, isVideoCall: Boolean, success: (Boolean) -> Unit) {
        mainRepository.sendConnectionRequest(target, isVideoCall, success)
    }

    fun startService(username: String) {
        mainRepository.initWebrtcClient(username)
    }

    fun setupViews(isVideoCall: Boolean, isCaller: Boolean, target: String) {
        mainRepository.setTarget(target)
        mainRepository.initLocalSurfaceView(localSurfaceView!!, isVideoCall)
        mainRepository.initRemoteSurfaceView(remoteSurfaceView!!)
        if (!isCaller) {
            mainRepository.startCall()
        }
    }

    fun answerCall(callId: String) {
        mainRepository.answerCall(callId)
    }


    fun switchCamera() {
        mainRepository.switchCamera()
    }

    fun toggleAudio(shouldBeMuted: Boolean) {
        mainRepository.toggleAudio(shouldBeMuted)
    }

    fun toggleVideo(shouldBeMuted: Boolean) {
        mainRepository.toggleVideo(shouldBeMuted)
    }


    override fun onLatestEventReceived(data: DataModel) {
        if (data.isValid()) {
            _incomingCall.postValue(data)
            listener?.onCallReceived(data)
        }
    }

    override fun endCall() {
        listener?.onCallEnded()
    }

    interface CallListener {
        fun onCallReceived(model: DataModel)
        fun onCallEnded()
    }

    companion object {
        var localSurfaceView: SurfaceViewRenderer? = null
        var remoteSurfaceView: SurfaceViewRenderer? = null
    }
}
