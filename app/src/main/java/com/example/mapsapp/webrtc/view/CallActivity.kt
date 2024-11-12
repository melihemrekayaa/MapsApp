package com.example.mapsapp.webrtc.view

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.mapsapp.R
import com.example.mapsapp.databinding.ActivityCallBinding
import com.example.mapsapp.webrtc.viewmodel.CallViewModel
import com.example.mapsapp.webrtc.utils.DataModel
import com.example.mapsapp.webrtc.utils.convertToHumanTime
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*

@AndroidEntryPoint
class CallActivity : AppCompatActivity(), CallViewModel.CallListener {

    private var target: String? = null
    private var isVideoCall: Boolean = true
    private var isCaller: Boolean = true

    private var isMicrophoneMuted = false
    private var isCameraMuted = false

    private val callViewModel: CallViewModel by viewModels()

    private lateinit var views: ActivityCallBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        views = ActivityCallBinding.inflate(layoutInflater)
        setContentView(views.root)
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

            callViewModel.setCallListener(this@CallActivity)
            callViewModel.setupViews(isVideoCall, isCaller, target!!)

            endCallButton.setOnClickListener {
                callViewModel.endCall()
            }

            switchCameraButton.setOnClickListener {
                callViewModel.switchCamera()
            }
        }
        setupMicToggleClicked()
        setupCameraToggleClicked()
    }

    private fun setupMicToggleClicked() {
        views.apply {
            toggleMicrophoneButton.setOnClickListener {
                if (!isMicrophoneMuted) {
                    callViewModel.toggleAudio(true)
                    toggleMicrophoneButton.setImageResource(R.drawable.ic_mic_on)
                } else {
                    callViewModel.toggleAudio(false)
                    toggleMicrophoneButton.setImageResource(R.drawable.ic_mic_off)
                }
                isMicrophoneMuted = !isMicrophoneMuted
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        callViewModel.endCall()
    }

    private fun setupCameraToggleClicked() {
        views.apply {
            toggleCameraButton.setOnClickListener {
                if (!isCameraMuted) {
                    callViewModel.toggleVideo(true)
                    toggleCameraButton.setImageResource(R.drawable.ic_camera_on)
                } else {
                    callViewModel.toggleVideo(false)
                    toggleCameraButton.setImageResource(R.drawable.ic_camera_off)
                }
                isCameraMuted = !isCameraMuted
            }
        }
    }

    override fun onCallReceived(model: DataModel) {
        // Handle incoming call
    }

    override fun onCallEnded() {
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        CallViewModel.remoteSurfaceView?.release()
        CallViewModel.remoteSurfaceView = null

        CallViewModel.localSurfaceView?.release()
        CallViewModel.localSurfaceView = null
    }
}

