package com.example.mapsapp.webrtc.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.example.mapsapp.webrtc.service.MainService
import com.example.mapsapp.R
import com.example.mapsapp.databinding.FragmentCallBinding
import com.example.mapsapp.viewmodel.ChatViewModel
import com.example.mapsapp.webrtc.service.MainServiceRepository
import com.example.mapsapp.webrtc.utils.convertToHumanTime
import com.example.mapsapp.webrtc.webrtc.RTCAudioManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class CallFragment : Fragment(), MainService.EndCallListener {

    private var target: String? = null
    private var isVideoCall: Boolean = true
    private var isCaller: Boolean = true

    private var isMicrophoneMuted = false
    private var isCameraMuted = false
    private var isSpeakerMode = true
    private var isScreenCasting = false
    private val chatViewModel: ChatViewModel by viewModels()

    @Inject
    lateinit var serviceRepository: MainServiceRepository
    private lateinit var requestScreenCaptureLauncher: ActivityResultLauncher<Intent>

    private var _binding: FragmentCallBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestScreenCaptureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val intent = result.data
                MainService.screenPermissionIntent = intent
                isScreenCasting = true
                serviceRepository.toggleScreenShare(true)
            }
        }

        chatViewModel.messages.observe(viewLifecycleOwner, Observer { messages ->
            println()

        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCallBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    private fun init() {
        arguments?.getString("target")?.let {
            this.target = it
        } ?: run {
            activity?.onBackPressed()
        }

        isVideoCall = arguments?.getBoolean("isVideoCall", true) ?: true
        isCaller = arguments?.getBoolean("isCaller", true) ?: true

        binding.apply {
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
        setupMicToggleClicked()
        setupCameraToggleClicked()
        setupToggleAudioDevice()
        MainService.endCallListener = this
    }

    private fun setupMicToggleClicked() {
        binding.apply {
            toggleMicrophoneButton.setOnClickListener {
                if (!isMicrophoneMuted) {
                    serviceRepository.toggleAudio(true)
                    toggleMicrophoneButton.setImageResource(R.drawable.ic_mic_on)
                } else {
                    serviceRepository.toggleAudio(false)
                    toggleMicrophoneButton.setImageResource(R.drawable.ic_mic_off)
                }
                isMicrophoneMuted = !isMicrophoneMuted
            }
        }
    }

    private fun setupToggleAudioDevice() {
        binding.apply {
            toggleAudioDevice.setOnClickListener {
                if (isSpeakerMode) {
                    toggleAudioDevice.setImageResource(R.drawable.ic_speaker)
                    serviceRepository.toggleAudioDevice(RTCAudioManager.AudioDevice.EARPIECE.name)
                } else {
                    toggleAudioDevice.setImageResource(R.drawable.ic_ear)
                    serviceRepository.toggleAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE.name)
                }
                isSpeakerMode = !isSpeakerMode
            }
        }
    }

    private fun setupCameraToggleClicked() {
        binding.apply {
            toggleCameraButton.setOnClickListener {
                if (!isCameraMuted) {
                    serviceRepository.toggleVideo(true)
                    toggleCameraButton.setImageResource(R.drawable.ic_camera_on)
                } else {
                    serviceRepository.toggleVideo(false)
                    toggleCameraButton.setImageResource(R.drawable.ic_camera_off)
                }
                isCameraMuted = !isCameraMuted
            }
        }
    }

    override fun onCallEnded() {
        activity?.onBackPressed()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        MainService.remoteSurfaceView?.release()
        MainService.remoteSurfaceView = null

        MainService.localSurfaceView?.release()
        MainService.localSurfaceView = null

        _binding = null
    }
}
