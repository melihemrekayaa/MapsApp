package com.example.mapsapp.webrtc

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.firebase.database.*
import org.webrtc.*

class WebRTCManager(
    private val context: Context,
    private val roomId: String,
    private val isCaller: Boolean,
    private val callerUid: String,
    private val isVideoCall: Boolean,
    private val localView: SurfaceViewRenderer,
    private val remoteView: SurfaceViewRenderer
) {
    private val TAG = "WebRTCManager"

    private val firebaseDb = FirebaseDatabase.getInstance().reference
    private lateinit var eglBase: EglBase
    private lateinit var peerConnectionFactory: PeerConnectionFactory
    private var peerConnection: PeerConnection? = null

    private var localAudioTrack: AudioTrack? = null
    private var localVideoTrack: VideoTrack? = null
    private var videoCapturer: VideoCapturer? = null
    private var videoSource: VideoSource? = null

    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:fr-turn1.xirsys.com").createIceServer(),
        PeerConnection.IceServer.builder("turn:fr-turn1.xirsys.com:80?transport=udp")
            .setUsername("jxHBkY12hPwlU9EfDkVi-4x0d1O1XqQ2ilEXQb5EqzvhVJsk902und-VtMYuLrjpAAAAAGgaMhRkYXJ4")
            .setPassword("35fc3b74-2a93-11f0-9646-0242ac120004")
            .createIceServer(),
        PeerConnection.IceServer.builder("turn:fr-turn1.xirsys.com:3478?transport=udp")
            .setUsername("jxHBkY12hPwlU9EfDkVi-4x0d1O1XqQ2ilEXQb5EqzvhVJsk902und-VtMYuLrjpAAAAAGgaMhRkYXJ4")
            .setPassword("35fc3b74-2a93-11f0-9646-0242ac120004")
            .createIceServer(),
        PeerConnection.IceServer.builder("turn:fr-turn1.xirsys.com:80?transport=tcp")
            .setUsername("jxHBkY12hPwlU9EfDkVi-4x0d1O1XqQ2ilEXQb5EqzvhVJsk902und-VtMYuLrjpAAAAAGgaMhRkYXJ4")
            .setPassword("35fc3b74-2a93-11f0-9646-0242ac120004")
            .createIceServer(),
        PeerConnection.IceServer.builder("turn:fr-turn1.xirsys.com:3478?transport=tcp")
            .setUsername("jxHBkY12hPwlU9EfDkVi-4x0d1O1XqQ2ilEXQb5EqzvhVJsk902und-VtMYuLrjpAAAAAGgaMhRkYXJ4")
            .setPassword("35fc3b74-2a93-11f0-9646-0242ac120004")
            .createIceServer(),
        PeerConnection.IceServer.builder("turns:fr-turn1.xirsys.com:443?transport=tcp")
            .setUsername("jxHBkY12hPwlU9EfDkVi-4x0d1O1XqQ2ilEXQb5EqzvhVJsk902und-VtMYuLrjpAAAAAGgaMhRkYXJ4")
            .setPassword("35fc3b74-2a93-11f0-9646-0242ac120004")
            .createIceServer(),
        PeerConnection.IceServer.builder("turns:fr-turn1.xirsys.com:5349?transport=tcp")
            .setUsername("jxHBkY12hPwlU9EfDkVi-4x0d1O1XqQ2ilEXQb5EqzvhVJsk902und-VtMYuLrjpAAAAAGgaMhRkYXJ4")
            .setPassword("35fc3b74-2a93-11f0-9646-0242ac120004")
            .createIceServer()


    )
    private val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
        sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
    }

    fun startCall() {
        eglBase = EglBase.create()

        localView.init(eglBase.eglBaseContext, null)
        remoteView.init(eglBase.eglBaseContext, null)

        localView.setMirror(true)
        remoteView.setMirror(true)

        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context).createInitializationOptions()
        )

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(PeerConnectionFactory.Options())
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .createPeerConnectionFactory()

        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, pcObserver)

        startLocalMedia()

        if (isCaller) {
            createOffer()
        } else {
            listenForOffer()
        }

        listenForCallEnd()
    }

    private fun startLocalMedia() {
        val audioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
        localAudioTrack = peerConnectionFactory.createAudioTrack("AUDIO", audioSource)
        peerConnection?.addTrack(localAudioTrack, listOf("ARDAMS"))

        if (isVideoCall) {
            val surfaceHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
            videoCapturer = createVideoCapturer()
            videoSource = peerConnectionFactory.createVideoSource(videoCapturer!!.isScreencast)
            videoCapturer!!.initialize(surfaceHelper, context, videoSource!!.capturerObserver)
            videoCapturer!!.startCapture(1280, 720, 30)

            localVideoTrack = peerConnectionFactory.createVideoTrack("VIDEO", videoSource)
            localVideoTrack?.addSink(localView)
            peerConnection?.addTrack(localVideoTrack) // stream id olmadan dene
        }
    }

    private fun createVideoCapturer(): VideoCapturer {
        val enumerator = Camera2Enumerator(context)
        return enumerator.deviceNames
            .mapNotNull { name -> enumerator.createCapturer(name, null) }
            .firstOrNull() ?: throw IllegalStateException("No camera found")
    }

    private val pcObserver = object : PeerConnection.Observer {

        override fun onIceCandidate(candidate: IceCandidate?) {
            candidate?.let {
                Log.d(TAG, "📤 ICE Candidate Gönderiliyor: ${it.sdp}")
                firebaseDb.child("calls").child(roomId).child("candidates").push().setValue(
                    mapOf(
                        "sdp" to it.sdp,
                        "sdpMid" to it.sdpMid,
                        "sdpMLineIndex" to it.sdpMLineIndex
                    )
                )
            }
        }

        override fun onTrack(transceiver: RtpTransceiver?) {
            Log.d(TAG, "🎥 onTrack çağrıldı!")
            val mediaTrack = transceiver?.receiver?.track()

            if (mediaTrack is VideoTrack) {
                Log.d(TAG, "✅ Remote video track remoteView'a eklendi.")
                mediaTrack.addSink(remoteView)
            } else {
                Log.w(TAG, "⚠️ Alınan track video değil.")
            }
        }

        override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
            // Abstract fonksiyon olduğundan boş bırakıldı
        }

        override fun onSignalingChange(newState: PeerConnection.SignalingState?) {}
        override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {}
        override fun onIceConnectionReceivingChange(p0: Boolean) {}
        override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
        override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
        override fun onRemoveStream(p0: MediaStream?) {}
        override fun onDataChannel(p0: DataChannel?) {}
        override fun onRenegotiationNeeded() {}
        override fun onAddStream(p0: MediaStream?) {}
    }

    private fun createOffer() {
        val constraints = MediaConstraints()
        peerConnection?.createOffer(object : SdpObserverAdapter() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                peerConnection?.setLocalDescription(SdpObserverAdapter(), desc)
                firebaseDb.child("calls").child(roomId).child("offer")
                    .setValue(mapOf("sdp" to desc?.description, "type" to desc?.type?.canonicalForm()))
                listenForAnswer()
            }
        }, constraints)
    }

    private fun createAnswer() {
        val constraints = MediaConstraints()
        peerConnection?.createAnswer(object : SdpObserverAdapter() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                peerConnection?.setLocalDescription(SdpObserverAdapter(), desc)
                firebaseDb.child("calls").child(roomId).child("answer")
                    .setValue(mapOf("sdp" to desc?.description, "type" to desc?.type?.canonicalForm()))
                listenForIceCandidates()
            }
        }, constraints)
    }

    private fun listenForOffer() {
        Log.d("WebRTCManager", "👂 listenForOffer çağrıldı")
        firebaseDb.child("calls").child(roomId).child("offer")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val sdp = snapshot.child("sdp").getValue(String::class.java)
                    if (sdp != null) {
                        val desc = SessionDescription(SessionDescription.Type.OFFER, sdp)
                        peerConnection?.setRemoteDescription(SdpObserverAdapter(), desc)
                        createAnswer()
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun listenForAnswer() {
        firebaseDb.child("calls").child(roomId).child("answer")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val sdp = snapshot.child("sdp").getValue(String::class.java)
                    if (sdp != null) {
                        val desc = SessionDescription(SessionDescription.Type.ANSWER, sdp)
                        peerConnection?.setRemoteDescription(SdpObserverAdapter(), desc)
                        listenForIceCandidates()
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun listenForIceCandidates() {
        firebaseDb.child("calls").child(roomId).child("candidates")
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {

                    val sdp = snapshot.child("sdp").getValue(String::class.java)
                    Log.d("WebRTC", "📥 ICE candidate received: $sdp")
                    val mid = snapshot.child("sdpMid").getValue(String::class.java)
                    val index = snapshot.child("sdpMLineIndex").getValue(Int::class.java) ?: return
                    val candidate = IceCandidate(mid, index, sdp)
                    peerConnection?.addIceCandidate(candidate)
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun listenForCallEnd() {
        firebaseDb.child("calls").child(roomId).child("callEnded")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.getValue(Boolean::class.java) == true) {
                        endCall()
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    fun toggleMicrophone(): Boolean {
        localAudioTrack?.setEnabled(!(localAudioTrack?.enabled() ?: true))
        return localAudioTrack?.enabled() ?: true
    }

    fun toggleCamera(): Boolean {
        localVideoTrack?.setEnabled(!(localVideoTrack?.enabled() ?: true))
        return localVideoTrack?.enabled() ?: true
    }

    fun switchCamera() {
        if (videoCapturer is CameraVideoCapturer) {
            (videoCapturer as CameraVideoCapturer).switchCamera(null)
        }
    }

    fun toggleAudioDevice() {
        // İsteğe göre hoparlör, kulaklık, Bluetooth gibi audio switch yapılabilir
    }

    fun toggleScreenSharing(activity: Activity) {
        // Ekran paylaşımı eklenebilir (ayrıca MediaProjection API gerekiyor)
    }

    fun endCall() {
        firebaseDb.child("calls").child(roomId).child("callEnded").setValue(true)
        firebaseDb.child("callRequests").child(callerUid).removeValue() // 🔄 sadece caller UID
        peerConnection?.close()
        try {
            videoCapturer?.stopCapture()
        } catch (_: Exception) {}
    }


    fun release() {
        try {
            videoCapturer?.stopCapture()
        } catch (_: Exception) {}
        peerConnection?.dispose()
        videoCapturer?.dispose()
        videoSource?.dispose()
        localAudioTrack?.dispose()
        localVideoTrack?.dispose()
    }


    open class SdpObserverAdapter : SdpObserver {
        override fun onCreateSuccess(p0: SessionDescription?) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(p0: String?) {}
        override fun onSetFailure(p0: String?) {}
    }
}
