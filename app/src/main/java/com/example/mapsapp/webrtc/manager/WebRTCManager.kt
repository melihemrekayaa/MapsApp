package com.example.mapsapp.webrtc.manager

import android.content.Context
import android.util.Log
import com.example.mapsapp.webrtc.adapter.SdpObserverAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import org.webrtc.*

class WebRTCManager(
    private val context: Context,
    private val localRenderer: SurfaceViewRenderer,
    private val remoteRenderer: SurfaceViewRenderer,
    private val isCaller: Boolean,
    private val isVideoCall: Boolean,
    private val targetId: String
) {
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val roomId = if (isCaller) "$currentUserId-$targetId" else "$targetId-$currentUserId"



    private lateinit var peerConnectionFactory: PeerConnectionFactory
    private var peerConnection: PeerConnection? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    private lateinit var eglBase: EglBase

    init {
        setup()
    }

    private fun setup() {
        initializePeerConnection()
        startLocalMedia()

        if (isCaller) {
            createOffer { offer ->
                sendOfferToFirebase(offer)
                listenForAnswer()
                listenForIceCandidates()
            }
        } else {
            listenForOffer()
            listenForIceCandidates()
        }
    }

    private fun initializePeerConnection() {
        eglBase = EglBase.create()

        localRenderer.init(eglBase.eglBaseContext, null)
        remoteRenderer.init(eglBase.eglBaseContext, null)
        localRenderer.setMirror(true)

        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context).createInitializationOptions()
        )

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .createPeerConnectionFactory()

        val config = PeerConnection.RTCConfiguration(listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        ))

        peerConnection = peerConnectionFactory.createPeerConnection(config, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate?.let {
                    val map = mapOf(
                        "sdp" to it.sdp,
                        "sdpMid" to it.sdpMid,
                        "sdpMLineIndex" to it.sdpMLineIndex
                    )
                    database.child("calls").child(roomId).child("candidates").push().setValue(map)
                }
            }

            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                streams?.firstOrNull()?.videoTracks?.firstOrNull()?.addSink(remoteRenderer)
            }

            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {}
            override fun onIceConnectionReceivingChange(p0: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(channel: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddStream(stream: MediaStream?) {}
        })
    }

    fun startLocalMedia() {
        val videoCapturer = createVideoCapturer() ?: return
        val surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)

        val videoSource = peerConnectionFactory.createVideoSource(false)
        videoCapturer.initialize(surfaceTextureHelper, context, videoSource.capturerObserver)
        videoCapturer.startCapture(640, 480, 30)

        val audioSource = peerConnectionFactory.createAudioSource(MediaConstraints())

        localVideoTrack = peerConnectionFactory.createVideoTrack("video", videoSource)
        localAudioTrack = peerConnectionFactory.createAudioTrack("audio", audioSource)

        localVideoTrack?.addSink(localRenderer)

        peerConnection?.addTrack(localVideoTrack, listOf("stream"))
        peerConnection?.addTrack(localAudioTrack, listOf("stream"))
    }

    fun startCall() {
        createOffer { sessionDescription ->
            val offer = mapOf(
                "sdp" to sessionDescription.description,
                "type" to sessionDescription.type.canonicalForm()
            )

            database.child("calls").child(roomId).child("offer").setValue(offer)

            database.child("calls").child(targetId).setValue(
                mapOf(
                    "type" to if (isVideoCall) "StartVideoCall" else "StartVoiceCall",
                    "sender" to currentUserId,
                    "roomId" to roomId,
                    "target" to targetId
                )
            )

            listenForAnswer() // ❌ roomId geçme
            listenForIceCandidates() // ❌ roomId geçme
        }
    }

    fun receiveCall() {
        listenForOffer()
        listenForIceCandidates()
    }




    private fun createVideoCapturer(): VideoCapturer? {
        val enumerator = if (Camera2Enumerator.isSupported(context))
            Camera2Enumerator(context) else Camera1Enumerator(false)

        enumerator.deviceNames.forEach {
            if (enumerator.isFrontFacing(it)) {
                return enumerator.createCapturer(it, null)
            }
        }

        enumerator.deviceNames.forEach {
            return enumerator.createCapturer(it, null)
        }

        return null
    }

    private fun createOffer(callback: (SessionDescription) -> Unit) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }

        peerConnection?.createOffer(object : SdpObserverAdapter() {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                sdp?.let {
                    peerConnection?.setLocalDescription(SdpObserverAdapter(), it)
                    callback(it)
                }
            }
        }, constraints)
    }

    private fun sendOfferToFirebase(offer: SessionDescription) {
        val offerMap = mapOf(
            "sdp" to offer.description,
            "type" to offer.type.canonicalForm()
        )

        database.child("calls").child(roomId).child("offer").setValue(offerMap)

        // Diğer kullanıcıya bildir
        val signalData = mapOf(
            "sender" to currentUserId,
            "target" to targetId,
            "roomId" to roomId,
            "type" to if (isVideoCall) "StartVideoCall" else "StartVoiceCall"
        )

        database.child("calls").child(targetId).setValue(signalData)
    }

    private fun listenForOffer() {
        database.child("calls").child(roomId).child("offer")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val sdp = snapshot.child("sdp").getValue(String::class.java)
                    val type = snapshot.child("type").getValue(String::class.java)

                    if (sdp != null && type == "offer") {
                        val sessionDescription = SessionDescription(SessionDescription.Type.OFFER, sdp)
                        peerConnection?.setRemoteDescription(SdpObserverAdapter(), sessionDescription)
                        createAnswer()
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }


    private fun createAnswer() {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }

        peerConnection?.createAnswer(object : SdpObserverAdapter() {
            override fun onCreateSuccess(answer: SessionDescription?) {
                answer?.let {
                    peerConnection?.setLocalDescription(SdpObserverAdapter(), it)
                    val answerMap = mapOf(
                        "sdp" to it.description,
                        "type" to it.type.canonicalForm()
                    )
                    database.child("calls").child(roomId).child("answer").setValue(answerMap)
                }
            }
        }, constraints)
    }

    private fun listenForAnswer() {
        database.child("calls").child(roomId).child("answer")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val sdp = snapshot.child("sdp").getValue(String::class.java)
                    val type = snapshot.child("type").getValue(String::class.java)
                    if (sdp != null && type != null) {
                        val answer = SessionDescription(SessionDescription.Type.ANSWER, sdp)
                        peerConnection?.setRemoteDescription(SdpObserverAdapter(), answer)
                        Log.d("WebRTCManager", "🎯 SDP Answer yüklendi.")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("WebRTCManager", "❌ SDP Answer dinlenemedi: ${error.message}")
                }
            })
    }



    private fun listenForIceCandidates() {
        database.child("calls").child(roomId).child("candidates")
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val sdpMid = snapshot.child("sdpMid").getValue(String::class.java)
                    val sdpMLineIndex = snapshot.child("sdpMLineIndex").getValue(Int::class.java)
                    val sdp = snapshot.child("sdp").getValue(String::class.java)

                    if (sdpMid != null && sdpMLineIndex != null && sdp != null) {
                        val candidate = IceCandidate(sdpMid, sdpMLineIndex, sdp)
                        peerConnection?.addIceCandidate(candidate)
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            })
    }


    fun close() {
        peerConnection?.close()
        peerConnection = null
        localVideoTrack = null
        localAudioTrack = null
        localRenderer?.release()
        remoteRenderer?.release()
        PeerConnectionFactory.shutdownInternalTracer()
    }


}
