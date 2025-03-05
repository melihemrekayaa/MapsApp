package com.example.mapsapp.webrtc.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.mapsapp.R
import com.example.mapsapp.databinding.ActivityCallBinding
import com.example.mapsapp.webrtc.service.MainService
import com.example.mapsapp.webrtc.service.MainServiceRepository
import com.example.mapsapp.webrtc.utils.convertToHumanTime
import com.example.mapsapp.webrtc.webrtc.RTCAudioManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import org.webrtc.*
import java.util.concurrent.Executors
import javax.inject.Inject

@AndroidEntryPoint
class CallActivity : AppCompatActivity(), MainService.EndCallListener {

    private var target: String? = null
    private var isVideoCall: Boolean = true
    private var isCaller: Boolean = true

    private var isMicrophoneMuted = false
    private var isCameraMuted = false
    private var isSpeakerMode = true
    private var isScreenCasting = false

    private var peerConnection: PeerConnection? = null
    private var cameraCapturer: CameraVideoCapturer? = null
    private lateinit var rootEglBase: EglBase


    @Inject
    lateinit var serviceRepository: MainServiceRepository
    private lateinit var requestScreenCaptureLauncher: ActivityResultLauncher<Intent>

    private lateinit var views: ActivityCallBinding

    override fun onStart() {
        super.onStart()
        requestScreenCaptureLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val intent = result.data
                MainService.screenPermissionIntent = intent
                isScreenCasting = true
                updateUiToScreenCaptureIsOn()
                serviceRepository.toggleScreenShare(true)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        views = ActivityCallBinding.inflate(layoutInflater)
        setContentView(views.root)

        rootEglBase = EglBase.create()

        views.endCallButton.setOnClickListener {
            closeCall()
        }

        init()
    }

    private fun init() {
        intent.getStringExtra("target")?.let {
            this.target = it
        } ?: kotlin.run {
            finish()
        }

        isVideoCall = intent.getBooleanExtra("isVideoCall", true)
        isCaller = intent.getBooleanExtra("isCaller", true)

        views.apply {
            callTitleTv.text = "In call with $target"
            CoroutineScope(Dispatchers.IO).launch {
                for (i in 0..3600) {
                    delay(1000)
                    withContext(Dispatchers.Main) {
                        callTimerTv.text = i.convertToHumanTime()
                    }
                }
            }

            if (!isVideoCall) {
                toggleCameraButton.isVisible = false
                screenShareButton.isVisible = false
                switchCameraButton.isVisible = false
            }

            MainService.remoteSurfaceView = remoteView
            MainService.localSurfaceView = localView
            serviceRepository.setupViews(isVideoCall, isCaller, target!!)

            endCallButton.setOnClickListener {
                serviceRepository.sendEndCall()
            }

            switchCameraButton.setOnClickListener {
                serviceRepository.switchCamera()
            }
        }

        setupPeerConnection()
        setupCameraCapturer()

        setupMicToggleClicked()
        setupCameraToggleClicked()
        setupToggleAudioDevice()
        setupScreenCasting()

        MainService.endCallListener = this
    }

    private fun setupPeerConnection() {
        val factory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(rootEglBase.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(rootEglBase.eglBaseContext))
            .createPeerConnectionFactory()

        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
        peerConnection = factory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onSignalingChange(signalingState: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(iceConnectionState: PeerConnection.IceConnectionState?) {}
            override fun onIceConnectionReceivingChange(p0: Boolean) {}
            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidate(iceCandidate: IceCandidate?) {}
            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
            override fun onAddStream(p0: MediaStream?) {}
            override fun onRemoveStream(p0: MediaStream?) {}
            override fun onDataChannel(p0: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {}
        })
    }

    private fun setupCameraCapturer() {
        val cameraEnumerator = Camera2Enumerator(this)
        val deviceNames = cameraEnumerator.deviceNames

        for (deviceName in deviceNames) {
            if (cameraEnumerator.isFrontFacing(deviceName)) {
                cameraCapturer = cameraEnumerator.createCapturer(deviceName, null)
                break
            }
        }

        if (cameraCapturer == null) {
            for (deviceName in deviceNames) {
                if (cameraEnumerator.isBackFacing(deviceName)) {
                    cameraCapturer = cameraEnumerator.createCapturer(deviceName, null)
                    break
                }
            }
        }
    }

    override fun onCallEnded() {
        finish()
    }

    private fun setupMicToggleClicked() {
        views.apply {
            toggleMicrophoneButton.setOnClickListener {
                isMicrophoneMuted = !isMicrophoneMuted
                serviceRepository.toggleAudio(isMicrophoneMuted)
                toggleMicrophoneButton.setImageResource(if (isMicrophoneMuted) R.drawable.ic_mic_off else R.drawable.ic_mic_on)
            }
        }
    }

    private fun setupCameraToggleClicked() {
        views.apply {
            toggleCameraButton.setOnClickListener {
                isCameraMuted = !isCameraMuted
                serviceRepository.toggleVideo(isCameraMuted)
                toggleCameraButton.setImageResource(if (isCameraMuted) R.drawable.ic_camera_off else R.drawable.ic_camera_on)
            }
        }
    }

    private fun setupToggleAudioDevice() {
        views.apply {
            toggleAudioDevice.setOnClickListener {
                isSpeakerMode = !isSpeakerMode
                val device = if (isSpeakerMode) RTCAudioManager.AudioDevice.SPEAKER_PHONE.name else RTCAudioManager.AudioDevice.EARPIECE.name
                serviceRepository.toggleAudioDevice(device)
                toggleAudioDevice.setImageResource(if (isSpeakerMode) R.drawable.ic_speaker else R.drawable.ic_ear)
            }
        }
    }

    private fun startScreenCapture() {
        val mediaProjectionManager = application.getSystemService(
            Context.MEDIA_PROJECTION_SERVICE
        ) as MediaProjectionManager

        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        requestScreenCaptureLauncher.launch(captureIntent)
    }


    private fun setupScreenCasting() {
        views.apply {
            screenShareButton.setOnClickListener {
                if (!isScreenCasting) {
                    AlertDialog.Builder(this@CallActivity)
                        .setTitle("Screen Casting")
                        .setMessage("You sure to start casting?")
                        .setPositiveButton("Yes") { dialog, _ ->
                            startScreenCapture() // ✅ Eksik fonksiyon artık var!
                            dialog.dismiss()
                        }
                        .setNegativeButton("No") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .create().show()
                } else {
                    isScreenCasting = false
                    updateUiToScreenCaptureIsOff()
                    serviceRepository.toggleScreenShare(false)
                }
            }
        }
    }


    private fun updateUiToScreenCaptureIsOn() {
        views.apply {
            localView.isVisible = false
            switchCameraButton.isVisible = false
            toggleCameraButton.isVisible = false
            screenShareButton.setImageResource(R.drawable.ic_stop_screen_share)
        }
    }

    private fun updateUiToScreenCaptureIsOff() {
        views.apply {
            localView.isVisible = true
            switchCameraButton.isVisible = true
            toggleCameraButton.isVisible = true
            screenShareButton.setImageResource(R.drawable.ic_screen_share)
        }
    }



    private fun closeCall() {
        peerConnection?.close()
        peerConnection = null

        val cameraExecutor = Executors.newSingleThreadExecutor()
        cameraExecutor.execute {
            cameraCapturer?.stopCapture()
            cameraCapturer = null
        }

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            FirebaseFirestore.getInstance().collection("calls").document(userId).delete()
        }

        runOnUiThread {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        MainService.remoteSurfaceView?.release()
        MainService.remoteSurfaceView = null

        MainService.localSurfaceView?.release()
        MainService.localSurfaceView = null
    }
}
