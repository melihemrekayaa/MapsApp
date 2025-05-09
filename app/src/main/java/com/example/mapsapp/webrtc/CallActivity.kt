package com.example.mapsapp.webrtc

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.mapsapp.R
import com.example.mapsapp.databinding.ActivityCallBinding
import org.webrtc.SurfaceViewRenderer

class CallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCallBinding
    private lateinit var webRTCManager: WebRTCManager

    private var roomId: String = ""
    private var isCaller: Boolean = false
    private var callerUid: String = ""
    private var isVideoCall: Boolean = true

    companion object {
        var isActive = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Intent verilerini al
        roomId = intent.getStringExtra("roomId") ?: ""
        isCaller = intent.getBooleanExtra("isCaller", false)
        callerUid = intent.getStringExtra("callerUid") ?: ""
        isVideoCall = intent.getBooleanExtra("isVideoCall", true)

        // WebRTC'yi başlat
        webRTCManager = WebRTCManager(
            context = this,
            roomId = roomId,
            isCaller = isCaller,
            callerUid = callerUid,
            isVideoCall = isVideoCall,
            localView = binding.localView,
            remoteView = binding.remoteView,
        )

        // Görüntülü aramayı başlat
        webRTCManager.startCall()

        // Butonlar
        binding.endCallButton.setOnClickListener {
            webRTCManager.endCall()
            finish()
        }

        binding.toggleMicrophoneButton.setOnClickListener {
            val micOn = webRTCManager.toggleMicrophone()
            binding.toggleMicrophoneButton.setImageResource(
                if (micOn) R.drawable.ic_mic_on else R.drawable.ic_mic_off
            )
        }

        binding.toggleCameraButton.setOnClickListener {
            val camOn = webRTCManager.toggleCamera()
            binding.toggleCameraButton.setImageResource(
                if (camOn) R.drawable.ic_camera_on else R.drawable.ic_camera_off
            )
        }

        binding.switchCameraButton.setOnClickListener {
            webRTCManager.switchCamera()
        }

        binding.toggleAudioDevice.setOnClickListener {
            webRTCManager.toggleAudioDevice()
        }

        binding.screenShareButton.setOnClickListener {
            webRTCManager.toggleScreenSharing(this)
        }
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
        webRTCManager.release()
    }
}
