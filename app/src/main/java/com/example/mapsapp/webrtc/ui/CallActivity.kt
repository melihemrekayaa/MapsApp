package com.example.mapsapp.webrtc.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.mapsapp.R
import com.example.mapsapp.databinding.ActivityCallBinding
import com.example.mapsapp.webrtc.viewmodel.CallViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*

@AndroidEntryPoint
class CallActivity : AppCompatActivity(), CallViewModel.CallListener {

    private val callViewModel: CallViewModel by viewModels()
    private lateinit var binding: ActivityCallBinding

    private var target: String? = null
    private var isVideoCall: Boolean = true
    private var isCaller: Boolean = true
    private var isMicrophoneMuted = false
    private var isCameraMuted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        init()
        observeViewModel()
    }

    private fun init() {
        target = intent.getStringExtra("target")
        isVideoCall = intent.getBooleanExtra("isVideoCall", true)
        isCaller = intent.getBooleanExtra("isCaller", true)

        if (target.isNullOrEmpty()) {
            finish()
            return
        }

        binding.callTitleTv.text = "In call with $target"

        CoroutineScope(Dispatchers.IO).launch {
            for (i in 0..3600) {
                delay(1000)
                withContext(Dispatchers.Main) {
                    binding.callTimerTv.text = "$i s"
                }
            }
        }

        if (!isVideoCall) {
            binding.toggleCameraButton.isVisible = false
            binding.screenShareButton.isVisible = false
            binding.switchCameraButton.isVisible = false
        }

        callViewModel.setCallListener(this)
        callViewModel.setupViews(isVideoCall, isCaller, target!!)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.endCallButton.setOnClickListener {
            callViewModel.endCall()
        }

        binding.switchCameraButton.setOnClickListener {
            callViewModel.switchCamera()
        }

        binding.toggleMicrophoneButton.setOnClickListener {
            isMicrophoneMuted = !isMicrophoneMuted
            callViewModel.toggleAudio(isMicrophoneMuted)
            updateMicButtonUI()
        }

        binding.toggleCameraButton.setOnClickListener {
            isCameraMuted = !isCameraMuted
            callViewModel.toggleVideo(isCameraMuted)
            updateCameraButtonUI()
        }
    }

    private fun updateMicButtonUI() {
        binding.toggleMicrophoneButton.setImageResource(
            if (isMicrophoneMuted) R.drawable.ic_mic_off else R.drawable.ic_mic_on
        )
    }

    private fun updateCameraButtonUI() {
        binding.toggleCameraButton.setImageResource(
            if (isCameraMuted) R.drawable.ic_camera_off else R.drawable.ic_camera_on
        )
    }

    private fun observeViewModel() {
        callViewModel.callStatus.observe(this) { status ->
            if (status == "Call Ended") {
                finish()
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        callViewModel.endCall()
    }

    override fun onCallReceived(model: com.example.mapsapp.webrtc.utils.DataModel) {
        // Gelen çağrı yönetimi (Şu an için burada işleme gerek yok)
    }

    override fun onCallEnded() {
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        callViewModel.endCall()
    }
}
