package com.example.mapsapp.webrtc.repository

import com.example.mapsapp.webrtc.firebaseClient.FirebaseClient
import com.example.mapsapp.webrtc.utils.DataModel
import com.example.mapsapp.webrtc.utils.DataModelType
import com.example.mapsapp.webrtc.utils.UserStatus
import com.example.mapsapp.webrtc.webrtc.MyPeerObserver
import com.example.mapsapp.webrtc.webrtc.WebRTCClient
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
    private var callId: String? = null
    var listener: Listener? = null
    private var remoteView: SurfaceViewRenderer? = null

    fun login(username: String, password: String, isDone: (Boolean, String?) -> Unit) {
        firebaseClient.login(username, password, isDone)
    }

    fun observeUsersStatus(status: (List<Pair<String, String>>) -> Unit) {
        firebaseClient.observeUsersStatus(status)
    }

    fun placeCall(caller: String, callee: String, callback: (Boolean) -> Unit) {
        val callId = "$caller-$callee-${System.currentTimeMillis()}"
        this.callId = callId
        firebaseClient.placeCall(caller, callee, callId) { success ->
            if (success) {
                observeCallStatus(callId)
            }
            callback(success)
        }
    }

    fun answerCall(callId: String) {
        this.callId = callId
        firebaseClient.answerCall(callId)
        startCall()
    }

    fun endCall() {
        callId?.let {
            firebaseClient.endCall(it)
            webRTCClient.closeConnection()
            changeMyStatus(UserStatus.ONLINE)
        }
    }

    private fun observeCallStatus(callId: String) {
        firebaseClient.observeCallStatus(callId) { status ->
            when (status) {
                "accepted" -> startCall()
                "ended" -> listener?.endCall()
            }
        }
    }

    fun observeIncomingCalls(username: String, listener: (String, String) -> Unit) {
        firebaseClient.observeIncomingCalls(username) { callId, caller ->
            listener(callId, caller)
        }
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
                super.onAddStream(p0)
                try {
                    p0?.videoTracks?.get(0)?.addSink(remoteView)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onIceCandidate(p0: IceCandidate?) {
                super.onIceCandidate(p0)
                p0?.let {
                    webRTCClient.sendIceCandidate(target!!, it)
                }
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                super.onConnectionChange(newState)
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

    fun sendEndCall() {
        callId?.let {
            firebaseClient.endCall(it)
        }
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

    fun initFirebase() {
        firebaseClient.subscribeForLatestEvent(object : FirebaseClient.Listener {
            override fun onLatestEventReceived(event: DataModel) {
                listener?.onLatestEventReceived(event)
                when (event.eventType) {
                    DataModelType.Offer.name -> {
                        webRTCClient.onRemoteSessionReceived(
                            SessionDescription(
                                SessionDescription.Type.OFFER,
                                event.data
                            )
                        )
                        webRTCClient.answer(target!!)
                    }
                    DataModelType.Answer.name -> {
                        webRTCClient.onRemoteSessionReceived(
                            SessionDescription(
                                SessionDescription.Type.ANSWER,
                                event.data
                            )
                        )
                    }
                    DataModelType.IceCandidates.name -> {
                        val candidate: IceCandidate? = try {
                            gson.fromJson(event.data, IceCandidate::class.java)
                        } catch (e: Exception) {
                            null
                        }
                        candidate?.let {
                            webRTCClient.addIceCandidateToPeer(it)
                        }
                    }
                    DataModelType.EndCall.name -> {
                        listener?.endCall()
                    }
                    else -> Unit
                }
            }
        })
    }

    override fun onTransferEventToSocket(data: DataModel) {
        firebaseClient.sendMessageToOtherClient(data) {}
    }

    fun logOff(function: () -> Unit) = firebaseClient.logOff(function)
}
