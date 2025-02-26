package com.example.mapsapp.webrtc.webrtc

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log
import org.webrtc.ThreadUtils
import java.util.Collections
import java.util.HashSet

@SuppressLint("MissingPermission")
class RTCAudioManager private constructor(private val context: Context) {

    private val TAG = "RTCAudioManager"
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioManagerEvents: AudioManagerEvents? = null

    private var savedAudioMode = AudioManager.MODE_NORMAL
    private var savedIsSpeakerPhoneOn = false
    private var savedIsMicrophoneMute = false
    private var hasWiredHeadset = false

    private var defaultAudioDevice = AudioDevice.SPEAKER_PHONE
    private var selectedAudioDevice = AudioDevice.NONE
    private var userSelectedAudioDevice = AudioDevice.NONE
    private var audioDevices = HashSet<AudioDevice>()

    private val wiredHeadsetReceiver = WiredHeadsetReceiver()

    // 🔴 Eksik olan: Audio Focus Management (Şimdi eklendi)
    private var audioFocusRequest: AudioFocusRequest? = null

    companion object {
        fun create(context: Context): RTCAudioManager {
            return RTCAudioManager(context)
        }
    }

    fun start(audioManagerEvents: AudioManagerEvents) {
        ThreadUtils.checkIsOnMainThread()
        this.audioManagerEvents = audioManagerEvents

        savedAudioMode = audioManager.mode
        savedIsSpeakerPhoneOn = audioManager.isSpeakerphoneOn
        savedIsMicrophoneMute = audioManager.isMicrophoneMute

        hasWiredHeadset = isWiredHeadsetConnected()

        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isMicrophoneMute = false

        // 🔴 Eksik olan: Audio Focus Yönetimi (Şimdi eklendi)
        requestAudioFocus()

        userSelectedAudioDevice = AudioDevice.NONE
        selectedAudioDevice = AudioDevice.NONE
        audioDevices.clear()

        updateAudioDeviceState()
        registerReceiver(wiredHeadsetReceiver, IntentFilter(Intent.ACTION_HEADSET_PLUG))
    }

    fun stop() {
        ThreadUtils.checkIsOnMainThread()
        unregisterReceiver(wiredHeadsetReceiver)
        audioManager.isSpeakerphoneOn = savedIsSpeakerPhoneOn
        audioManager.isMicrophoneMute = savedIsMicrophoneMute
        audioManager.mode = savedAudioMode
        abandonAudioFocus()
        audioManagerEvents = null
    }

    private fun setAudioDeviceInternal(device: AudioDevice) {
        selectedAudioDevice = device
        when (device) {
            AudioDevice.SPEAKER_PHONE -> audioManager.isSpeakerphoneOn = true
            AudioDevice.EARPIECE, AudioDevice.WIRED_HEADSET -> audioManager.isSpeakerphoneOn = false
            else -> {}
        }
    }

    fun setDefaultAudioDevice(defaultDevice: AudioDevice) {
        ThreadUtils.checkIsOnMainThread()
        defaultAudioDevice = if (defaultDevice == AudioDevice.EARPIECE && hasEarpiece()) {
            defaultDevice
        } else {
            AudioDevice.SPEAKER_PHONE
        }
        updateAudioDeviceState()
    }

    fun selectAudioDevice(device: AudioDevice) {
        ThreadUtils.checkIsOnMainThread()
        if (audioDevices.contains(device)) {
            userSelectedAudioDevice = device
            updateAudioDeviceState()
        }
    }

    fun getAudioDevices(): Set<AudioDevice> {
        return Collections.unmodifiableSet(HashSet(audioDevices))
    }

    fun getSelectedAudioDevice(): AudioDevice {
        return selectedAudioDevice
    }

    private fun registerReceiver(receiver: BroadcastReceiver, filter: IntentFilter) {
        context.registerReceiver(receiver, filter)
    }

    private fun unregisterReceiver(receiver: BroadcastReceiver) {
        try {
            context.unregisterReceiver(receiver)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Receiver was not registered: ${e.message}")
        }
    }

    private fun updateAudioDeviceState() {
        ThreadUtils.checkIsOnMainThread()
        val newAudioDevices = HashSet<AudioDevice>()

        if (hasWiredHeadset) {
            newAudioDevices.add(AudioDevice.WIRED_HEADSET)
        } else {
            newAudioDevices.add(AudioDevice.SPEAKER_PHONE)
            if (hasEarpiece()) {
                newAudioDevices.add(AudioDevice.EARPIECE)
            }
        }

        if (audioDevices != newAudioDevices) {
            audioDevices = newAudioDevices
            val newAudioDevice = if (hasWiredHeadset) AudioDevice.WIRED_HEADSET else defaultAudioDevice
            setAudioDeviceInternal(newAudioDevice)
            audioManagerEvents?.onAudioDeviceChanged(selectedAudioDevice, audioDevices)
        }
    }

    private fun hasEarpiece(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
    }

    private fun isWiredHeadsetConnected(): Boolean {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
        return devices.any { it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET || it.type == AudioDeviceInfo.TYPE_USB_DEVICE }
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setOnAudioFocusChangeListener { }
                .build()
            val result = audioManager.requestAudioFocus(focusRequest)
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                Log.d(TAG, "Audio focus granted")
            } else {
                Log.e(TAG, "Audio focus request failed")
            }
            audioFocusRequest = focusRequest
        } else {
            @Suppress("DEPRECATION")
            val result = audioManager.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                Log.d(TAG, "Audio focus granted (Legacy)")
            } else {
                Log.e(TAG, "Audio focus request failed (Legacy)")
            }
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let {
                audioManager.abandonAudioFocusRequest(it)
                Log.d(TAG, "Audio focus abandoned")
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
            Log.d(TAG, "Audio focus abandoned (Legacy)")
        }
    }

    interface AudioManagerEvents {
        fun onAudioDeviceChanged(selectedAudioDevice: AudioDevice, availableAudioDevices: Set<AudioDevice>)
    }

    enum class AudioDevice {
        SPEAKER_PHONE, WIRED_HEADSET, EARPIECE, NONE
    }

    private inner class WiredHeadsetReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            hasWiredHeadset = intent.getIntExtra("state", 0) == 1
            updateAudioDeviceState()
        }
    }
}
