package com.example.mapsapp.webrtc

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.mapsapp.MapsMainActivity
import com.example.mapsapp.R
import com.example.mapsapp.databinding.ActivityCallBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.webrtc.*

class CallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCallBinding

    private lateinit var roomId: String
    private var isCaller: Boolean = true
    private lateinit var callerUid: String
    private var isVideoCall: Boolean = true


    private val firebaseDatabase = FirebaseDatabase.getInstance().reference
    private val firebaseClient = FirebaseClient()

    private lateinit var peerConnectionFactory: PeerConnectionFactory
    private var peerConnection: PeerConnection? = null

    private lateinit var localVideoView: SurfaceViewRenderer
    private lateinit var remoteVideoView: SurfaceViewRenderer
    private lateinit var eglBase: EglBase

    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    private var videoCapturer: VideoCapturer? = null
    private var videoSource: VideoSource? = null

    private var isCallEnded = false
    private lateinit var callEndListener: ValueEventListener

    private var isMicMuted = false
    private var isCameraOn = true
    private var isSpeakerOn = true

    private var callStartTime = 0L
    private val timerHandler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            val elapsed = System.currentTimeMillis() - callStartTime
            val seconds = (elapsed / 1000) % 60
            val minutes = (elapsed / 1000) / 60
            binding.callTimerTv.text = String.format("%02d:%02d", minutes, seconds)
            timerHandler.postDelayed(this, 1000)
        }
    }

    companion object {
        var isActive: Boolean = false
    }


    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
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

    private val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    private val requestCodePermissions = 101

    private var timerStarted = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        roomId = intent.getStringExtra("roomId") ?: ""
        isCaller = intent.getBooleanExtra("isCaller", true)
        callerUid = intent.getStringExtra("callerUid") ?: ""
        isVideoCall = intent.getBooleanExtra("isVideoCall", true)

        listenForCallStatus()

        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(this, permissions, requestCodePermissions)
        } else {
            startCallSetup()
        }

        binding.endCallButton.setOnClickListener {
            endCallAndExit()
        }


        binding.callTitleTv.text =
            if (isCaller) "Arama başlatılıyor..." else "Arama yanıtlanıyor..."

        onBackPressedDispatcher.addCallback(this) {
            endCallAndExit()
        }

    }

    private fun observeCallAccepted() {
        val statusRef = firebaseDatabase.child("calls").child(roomId).child("status")

        statusRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val status = snapshot.getValue(String::class.java)
                if (status == "accepted" && !timerStarted) {
                    Log.d("CallActivity", "✅ Call accepted → timer başlatılıyor")
                    startCallTimer()
                    timerStarted = true
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun startCallTimer() {
        callStartTime = System.currentTimeMillis()
        timerHandler.post(timerRunnable)
    }



    private fun allPermissionsGranted(): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults) // 👈 EKLENDİ
        if (requestCode == requestCodePermissions && allPermissionsGranted()) {
            startCallSetup()
        } else {
            finish()
        }
    }

    private fun startCallSetup() {
        eglBase = EglBase.create()

        localVideoView = binding.localView
        remoteVideoView = binding.remoteView

        localVideoView.init(eglBase.eglBaseContext, null)
        remoteVideoView.init(eglBase.eglBaseContext, null)

        localVideoView.setMirror(true)
        remoteVideoView.setMirror(true)

        initWebRTC()
        startLocalMedia()
        observeCallAccepted()

        if (isCaller) {
            createOffer()
        } else {
            listenForOffer()
        }
        listenForCallEnd()

        callStartTime = System.currentTimeMillis()
        timerHandler.post(timerRunnable)

        setupControlButtons()
        observeRemoteTermination()

    }

    private fun setupControlButtons() {
        binding.toggleMicrophoneButton.setOnClickListener {
            isMicMuted = !isMicMuted
            localAudioTrack?.setEnabled(!isMicMuted)
            binding.toggleMicrophoneButton.setImageResource(
                if (isMicMuted) R.drawable.ic_mic_off else R.drawable.ic_mic_on
            )
        }

        binding.toggleCameraButton.setOnClickListener {
            isCameraOn = !isCameraOn
            localVideoTrack?.setEnabled(isCameraOn)
            binding.toggleCameraButton.setImageResource(
                if (isCameraOn) R.drawable.ic_camera_on else R.drawable.ic_camera_off
            )
        }

        binding.switchCameraButton.setOnClickListener {
            if (videoCapturer is CameraVideoCapturer) {
                (videoCapturer as CameraVideoCapturer).switchCamera(null)
            }
        }

        binding.toggleAudioDevice.setOnClickListener {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            isSpeakerOn = !isSpeakerOn
            audioManager.isSpeakerphoneOn = isSpeakerOn
            binding.toggleAudioDevice.setImageResource(
                if (isSpeakerOn) R.drawable.ic_ear else R.drawable.ic_speaker
            )
        }

        // (İsteğe bağlı) screen share için ileride kullanılacak
        binding.screenShareButton.setOnClickListener {
            Toast.makeText(this, "Screen sharing not yet implemented", Toast.LENGTH_SHORT).show()
        }
    }


    private fun initWebRTC() {
        val options = PeerConnectionFactory.InitializationOptions.builder(this).createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(PeerConnectionFactory.Options())
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .createPeerConnectionFactory()

        peerConnection = peerConnectionFactory.createPeerConnection(
            rtcConfig,
            object : PeerConnection.Observer {
                override fun onIceCandidate(candidate: IceCandidate?) {
                    candidate?.let { sendIceCandidate(it) }
                }

                override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                    if (isVideoCall) {
                        receiver?.track()?.let { track ->
                            if (track is VideoTrack) {
                                track.addSink(remoteVideoView)
                            }
                        }
                    }
                }

                override fun onAddStream(stream: MediaStream?) {
                    Log.d("CallActivity", "onAddStream called but ignored (using Unified Plan)")
                }

                override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {}
                override fun onIceConnectionReceivingChange(receiving: Boolean) {}
                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
                override fun onRemoveStream(stream: MediaStream?) {}
                override fun onDataChannel(dataChannel: DataChannel?) {}
                override fun onRenegotiationNeeded() {}
            }
        )


    }

    private fun startLocalMedia() {
        val audioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
        localAudioTrack = peerConnectionFactory.createAudioTrack("ARDAMSa0", audioSource)
        peerConnection?.addTrack(localAudioTrack, listOf("ARDAMS"))

        if (isVideoCall) {
            val surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
            videoCapturer = createVideoCapturer()
            videoSource = peerConnectionFactory.createVideoSource(videoCapturer!!.isScreencast)
            videoCapturer!!.initialize(surfaceTextureHelper, applicationContext, videoSource!!.capturerObserver)
            videoCapturer!!.startCapture(1280, 720, 30)

            localVideoTrack = peerConnectionFactory.createVideoTrack("ARDAMSv0", videoSource)
            localVideoTrack?.addSink(localVideoView)
            peerConnection?.addTrack(localVideoTrack, listOf("ARDAMS"))
        } else {
            localVideoView.release() // Görüntüsüzse boşuna gösterme
        }
    }


    private fun createVideoCapturer(): VideoCapturer {
        val enumerator = Camera2Enumerator(this)
        return enumerator.deviceNames
            .mapNotNull { enumerator.createCapturer(it, null) }
            .firstOrNull() ?: throw IllegalStateException("No camera available")
    }

    private fun createOffer() {
        val constraints = MediaConstraints()
        peerConnection?.createOffer(object : SdpObserverAdapter() {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                peerConnection?.setLocalDescription(SdpObserverAdapter(), sdp)
                firebaseDatabase.child("calls").child(roomId).child("offer")
                    .setValue(mapOf("sdp" to sdp?.description, "type" to sdp?.type?.canonicalForm()))
                listenForAnswer()
            }
        }, constraints)
    }




    private fun createAnswer() {
        val constraints = MediaConstraints()
        peerConnection?.createAnswer(object : SdpObserverAdapter() {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                peerConnection?.setLocalDescription(SdpObserverAdapter(), sdp)
                firebaseDatabase.child("calls").child(roomId).child("answer")
                    .setValue(mapOf("sdp" to sdp?.description, "type" to sdp?.type?.canonicalForm()))
                listenForIceCandidates()
            }
        }, constraints)
    }

    private fun listenForAnswer() {
        firebaseDatabase.child("calls").child(roomId).child("answer")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val answerSdp = snapshot.child("sdp").getValue(String::class.java)
                    if (answerSdp != null) {
                        val sessionDescription = SessionDescription(SessionDescription.Type.ANSWER, answerSdp)
                        peerConnection?.setRemoteDescription(object : SdpObserver {
                            override fun onSetSuccess() {
                                Log.d("CallActivity", "ANSWER alındı ve set edildi")
                                listenForIceCandidates()
                            }
                            override fun onCreateSuccess(p0: SessionDescription?) {}
                            override fun onCreateFailure(p0: String?) {}
                            override fun onSetFailure(error: String?) {
                                Log.e("CallActivity", "ANSWER setRemoteDescription HATA: $error")
                            }
                        }, sessionDescription)
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("CallActivity", "Answer dinleme iptal: ${error.message}")
                }
            })
    }

    private fun listenForOffer() {
        firebaseDatabase.child("calls").child(roomId).child("offer")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val offerSdp = snapshot.child("sdp").getValue(String::class.java)
                    if (offerSdp != null) {
                        val sessionDescription = SessionDescription(SessionDescription.Type.OFFER, offerSdp)
                        peerConnection?.setRemoteDescription(object : SdpObserver {
                            override fun onSetSuccess() {
                                Log.d("CallActivity", "OFFER alındı, şimdi answer oluşturulacak")
                                createAnswer()
                            }
                            override fun onCreateSuccess(p0: SessionDescription?) {}
                            override fun onCreateFailure(error: String?) {
                                Log.e("CallActivity", "OFFER setRemoteDescription HATA: $error")
                            }
                            override fun onSetFailure(error: String?) {
                                Log.e("CallActivity", "OFFER setRemoteDescription HATA: $error")
                            }
                        }, sessionDescription)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("CallActivity", "Offer dinleme iptal: ${error.message}")
                }
            })
    }


    private fun listenForCallEnd() {
        val callsRef = firebaseDatabase.child("calls").child(roomId)
        callEndListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val ended = snapshot.child("callEnded").getValue(Boolean::class.java) ?: false
                if (ended || !snapshot.exists()) {
                    endCallAndExit()
                }
            }
            override fun onCancelled(error: DatabaseError) {
                endCallAndExit()
            }
        }
        callsRef.addValueEventListener(callEndListener)
    }



    private fun listenForCallStatus() {
        firebaseDatabase.child("calls").child(roomId).child("status")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val status = snapshot.getValue(String::class.java)
                    if (status == "rejected") {
                        Log.d("CallActivity", "Arama reddedildi.")
                        finish()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("CallActivity", "Status dinleme hatası: ${error.message}")
                }
            })
    }



    private fun sendIceCandidate(candidate: IceCandidate) {
        val candidateData = mapOf(
            "sdp" to candidate.sdp,
            "sdpMid" to candidate.sdpMid,
            "sdpMLineIndex" to candidate.sdpMLineIndex
        )
        firebaseDatabase.child("calls").child(roomId).child("candidates").push().setValue(candidateData)
    }

    private fun listenForIceCandidates() {
        firebaseDatabase.child("calls").child(roomId).child("candidates")
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, prevChildName: String?) {
                    val sdp = snapshot.child("sdp").getValue(String::class.java)
                    val mid = snapshot.child("sdpMid").getValue(String::class.java)
                    val index = snapshot.child("sdpMLineIndex").getValue(Int::class.java) ?: return
                    if (sdp != null && mid != null) {
                        val candidate = IceCandidate(mid, index, sdp)
                        peerConnection?.addIceCandidate(candidate)
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, prevName: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, prevName: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun observeRemoteTermination() {
        val callsRef = firebaseDatabase.child("calls").child(roomId)
        callsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val ended = snapshot.child("callEnded").getValue(Boolean::class.java) ?: false
                val offer = snapshot.child("offer").getValue() // varsa, hala aktif olabilir

                if (ended || !snapshot.exists() || offer == null) {
                    Log.w("CallActivity", "Karşı taraf bağlantıyı sonlandırmış, çıkılıyor...")
                    endCallAndExit()
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }



    private fun endCallAndExit() {
        if (isCallEnded) return
        isCallEnded = true

        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Firebase: Çağrı durumu güncelle
        val callsRef = firebaseDatabase.child("calls").child(roomId)
        callsRef.child("callEnded").setValue(true)
        callsRef.removeValue()
        callsRef.removeEventListener(callEndListener)

        firebaseDatabase.child("callRequests").child(currentUid).removeValue()
        firebaseDatabase.child("callRequests").child(callerUid).removeValue()

        FirebaseDatabase.getInstance().getReference("users")
            .child(currentUid).child("inCall").setValue(false)
        FirebaseDatabase.getInstance().getReference("users")
            .child(callerUid).child("inCall").setValue(false)


        // === Medya bileşenlerini serbest bırak ===
        try {
            peerConnection?.close()
            peerConnection = null

            localAudioTrack?.dispose()
            localVideoTrack?.dispose()
            videoSource?.dispose()
            videoCapturer?.stopCapture()
            videoCapturer?.dispose()

            localVideoView.release()
            remoteVideoView.release()
        } catch (e: Exception) {
            Log.e("CallActivity", "Medya serbest bırakılırken hata: ${e.message}")
        }

        timerHandler.removeCallbacks(timerRunnable)
        finish()
    }



    override fun onStart() {
        super.onStart()
        isActive = true
    }

    override fun onStop() {
        super.onStop()
        isActive = false
    }



    override fun onDestroy() {
        super.onDestroy()
        isActive = false

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(uid).child("inCall")

        // Retry with timeout
        userRef.setValue(false).addOnFailureListener {
            Handler(Looper.getMainLooper()).postDelayed({
                userRef.setValue(false)
            }, 2000)
        }

        timerHandler.removeCallbacks(timerRunnable)
        Log.d("CallActivity", "onDestroy çağrıldı.")
    }






    open class SdpObserverAdapter : SdpObserver {
        override fun onCreateSuccess(p0: SessionDescription?) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(p0: String?) {}
        override fun onSetFailure(p0: String?) {}
    }
}
