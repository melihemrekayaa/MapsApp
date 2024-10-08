package com.example.mapsapp.webrtc.repository

import android.util.Log
import com.example.mapsapp.webrtc.firebaseClient.FirebaseClient
import com.example.mapsapp.webrtc.service.MainService
import com.example.mapsapp.webrtc.utils.DataModel
import com.example.mapsapp.webrtc.utils.DataModelType
import com.example.mapsapp.webrtc.utils.UserStatus
import com.example.mapsapp.webrtc.webrtc.MyPeerObserver
import com.example.mapsapp.webrtc.webrtc.WebRTCClient
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import org.webrtc.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MainRepository @Inject constructor(
    private val firebaseClient: FirebaseClient,
    private val webRTCClient: WebRTCClient,
    private val gson: Gson
) : WebRTCClient.Listener {

    private var target: String? = null
    var listener: Listener? = null
    private var remoteView: SurfaceViewRenderer? = null
    private val auth = FirebaseAuth.getInstance()

    fun login(username: String, password: String, isDone: (Boolean, String?) -> Unit) {
        firebaseClient.login(username, password, isDone)
    }

    fun observeUsersStatus(currentUserId: String, status: (List<Pair<String, String>>) -> Unit) {
        firebaseClient.observeUsersStatus(currentUserId) { users ->
            val filteredUsers = users.filterNot { it.first == currentUserId }
            status(filteredUsers)
        }
    }

    fun initFirebase() {
        firebaseClient.subscribeForLatestEvent(object : FirebaseClient.Listener {
            override fun onLatestEventReceived(event: DataModel) {
                Log.e("TAG", "----------------------  onLatestEventReceived: $event")
                when (event.type) {
                    DataModelType.StartVideoCall,
                    DataModelType.StartAudioCall -> {
                        MainService.listener?.onCallReceived(event)
                    }
                    DataModelType.Offer -> handleRemoteOffer(event)
                    DataModelType.Answer -> handleRemoteAnswer(event)
                    DataModelType.IceCandidates -> handleIceCandidate(event)
                    DataModelType.EndCall -> listener?.endCall()
                    else -> Unit
                }
            }
        })
    }

    private fun handleRemoteOffer(event: DataModel) {
        webRTCClient.onRemoteSessionReceived(
            SessionDescription(
                SessionDescription.Type.OFFER,
                event.data.toString()
            )
        )
        webRTCClient.answer(target!!)
    }

    private fun handleRemoteAnswer(event: DataModel) {
        webRTCClient.onRemoteSessionReceived(
            SessionDescription(
                SessionDescription.Type.ANSWER,
                event.data.toString()
            )
        )
    }

    private fun handleIceCandidate(event: DataModel) {
        val candidate: IceCandidate? = try {
            gson.fromJson(event.data.toString(), IceCandidate::class.java)
        } catch (e: Exception) {
            null
        }
        candidate?.let {
            webRTCClient.addIceCandidateToPeer(it)
        }
    }

    fun sendConnectionRequest(target: String, isVideoCall: Boolean, success: (Boolean) -> Unit) {
        firebaseClient.sendMessageToOtherClient(
            DataModel(
                sender = auth.currentUser?.uid,
                type = if (isVideoCall) DataModelType.StartVideoCall else DataModelType.StartAudioCall,
                target = target
            ), success
        )
    }

    fun setTarget(target: String) {
        this.target = target
    }

    interface Listener {
        fun onLatestEventReceived(data: DataModel)
        fun endCall()
    }

    fun initWebrtcClient(username: String) {
        webRTCClient.listener = this
        webRTCClient.initializeWebrtcClient(username, object : MyPeerObserver() {
            override fun onAddStream(p0: MediaStream?) {
                p0?.videoTracks?.get(0)?.addSink(remoteView)
            }

            override fun onIceCandidate(p0: IceCandidate?) {
                p0?.let {
                    webRTCClient.sendIceCandidate(target!!, it)
                }
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                if (newState == PeerConnection.PeerConnectionState.CONNECTED) {
                    changeMyStatus(UserStatus.IN_CALL)
                    firebaseClient.clearLatestEvent()
                }
            }
        })
    }

    fun initLocalSurfaceView(view: SurfaceViewRenderer, isVideoCall: Boolean) {
        webRTCClient.initLocalSurfaceView(view, isVideoCall)
    }

    fun initRemoteSurfaceView(view: SurfaceViewRenderer) {
        webRTCClient.initRemoteSurfaceView(view)
        this.remoteView = view
    }

    fun startCall() {
        webRTCClient.call(target!!)
    }

    fun endCall() {
        webRTCClient.closeConnection()
        changeMyStatus(UserStatus.ONLINE)
    }

    fun sendEndCall() {
        onTransferEventToSocket(
            DataModel(
                type = DataModelType.EndCall,
                target = target!!
            )
        )
    }

    private fun changeMyStatus(status: UserStatus) {
        firebaseClient.changeMyStatus(status)
    }

    fun toggleAudio(shouldBeMuted: Boolean) {
        webRTCClient.toggleAudio(shouldBeMuted)
    }

    fun toggleVideo(shouldBeMuted: Boolean) {
        webRTCClient.toggleVideo(shouldBeMuted)
    }

    fun switchCamera() {
        webRTCClient.switchCamera()
    }

    override fun onTransferEventToSocket(data: DataModel) {
        firebaseClient.sendMessageToOtherClient(data) {}
    }

    fun logOff(function: () -> Unit) = firebaseClient.logOff(function)

    fun observeIncomingCalls(username: String, onCallReceived: (String, String) -> Unit) {
        firebaseClient.observeIncomingCalls(username, onCallReceived)
    }

    fun answerCall(callId: String) {
        firebaseClient.answerCall(callId)
    }
}
