/*
package com.example.mapsapp.webrtc.webrtc;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioDeviceInfo;
import android.preference.PreferenceManager;
import android.util.Log;

import org.webrtc.ThreadUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@SuppressLint("MissingPermission")
public class RTCAudioManager {
    private static final String TAG = RTCAudioManager.class.getSimpleName();
    private static final String SPEAKERPHONE_AUTO = "auto";
    private static final String SPEAKERPHONE_TRUE = "true";
    private static final String SPEAKERPHONE_FALSE = "false";
    private final Context apprtcContext;
    private final String useSpeakerphone;
    private final android.media.AudioManager audioManager;
    private AudioManagerEvents audioManagerEvents;
    private AudioManagerState amState;
    private int savedAudioMode = android.media.AudioManager.MODE_INVALID;
    private boolean savedIsSpeakerPhoneOn = false;
    private boolean savedIsMicrophoneMute = false;
    private boolean hasWiredHeadset = false;
    private AudioDevice defaultAudioDevice;
    private AudioDevice selectedAudioDevice;
    private AudioDevice userSelectedAudioDevice;
    private Set<AudioDevice> audioDevices = new HashSet<>();
    private final BroadcastReceiver wiredHeadsetReceiver;

    private RTCAudioManager(Context context) {
        ThreadUtils.checkIsOnMainThread();
        apprtcContext = context;
        audioManager = ((android.media.AudioManager) context.getSystemService(Context.AUDIO_SERVICE));
        amState = AudioManagerState.UNINITIALIZED;

        useSpeakerphone = PreferenceManager.getDefaultSharedPreferences(context).getString("speakerphone_preference", "auto");
        if (useSpeakerphone.equals(SPEAKERPHONE_FALSE)) {
            defaultAudioDevice = AudioDevice.EARPIECE;
        } else {
            defaultAudioDevice = AudioDevice.SPEAKER_PHONE;
        }

        wiredHeadsetReceiver = new WiredHeadsetReceiver();
    }

    public static RTCAudioManager create(Context context) {
        return new RTCAudioManager(context);
    }

    public void start(AudioManagerEvents audioManagerEvents) {
        ThreadUtils.checkIsOnMainThread();
        if (amState == AudioManagerState.RUNNING) {
            return;
        }

        this.audioManagerEvents = audioManagerEvents;
        amState = AudioManagerState.RUNNING;

        savedAudioMode = audioManager.getMode();
        savedIsSpeakerPhoneOn = audioManager.isSpeakerphoneOn();
        savedIsMicrophoneMute = audioManager.isMicrophoneMute();
        hasWiredHeadset = hasWiredHeadset();

        audioManager.requestAudioFocus(null, android.media.AudioManager.STREAM_VOICE_CALL, android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        audioManager.setMode(android.media.AudioManager.MODE_IN_COMMUNICATION);
        setMicrophoneMute(false);

        userSelectedAudioDevice = AudioDevice.NONE;
        selectedAudioDevice = AudioDevice.NONE;
        audioDevices.clear();

        updateAudioDeviceState();

        registerReceiver(wiredHeadsetReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
    }

    @SuppressLint("WrongConstant")
    public void stop() {
        ThreadUtils.checkIsOnMainThread();
        if (amState != AudioManagerState.RUNNING) {
            return;
        }
        amState = AudioManagerState.UNINITIALIZED;

        unregisterReceiver(wiredHeadsetReceiver);

        setSpeakerphoneOn(savedIsSpeakerPhoneOn);
        setMicrophoneMute(savedIsMicrophoneMute);
        audioManager.setMode(savedAudioMode);

        audioManager.abandonAudioFocus(null);

        audioManagerEvents = null;
    }

    private void setAudioDeviceInternal(AudioDevice device) {
        switch (device) {
            case SPEAKER_PHONE:
                setSpeakerphoneOn(true);
                break;
            case EARPIECE:
            case WIRED_HEADSET:
                setSpeakerphoneOn(false);
                break;
            default:
                break;
        }
        selectedAudioDevice = device;
    }

    public void setDefaultAudioDevice(AudioDevice defaultDevice) {
        ThreadUtils.checkIsOnMainThread();
        switch (defaultDevice) {
            case SPEAKER_PHONE:
                defaultAudioDevice = defaultDevice;
                break;
            case EARPIECE:
                if (hasEarpiece()) {
                    defaultAudioDevice = defaultDevice;
                } else {
                    defaultAudioDevice = AudioDevice.SPEAKER_PHONE;
                }
                break;
            default:
                break;
        }
        updateAudioDeviceState();
    }

    public void selectAudioDevice(AudioDevice device) {
        ThreadUtils.checkIsOnMainThread();
        if (!audioDevices.contains(device)) {
            return;
        }
        userSelectedAudioDevice = device;
        updateAudioDeviceState();
    }

    public Set<AudioDevice> getAudioDevices() {
        ThreadUtils.checkIsOnMainThread();
        return Collections.unmodifiableSet(new HashSet<>(audioDevices));
    }

    public AudioDevice getSelectedAudioDevice() {
        ThreadUtils.checkIsOnMainThread();
        return selectedAudioDevice;
    }

    private void registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        apprtcContext.registerReceiver(receiver, filter);
    }

    private void unregisterReceiver(BroadcastReceiver receiver) {
        apprtcContext.unregisterReceiver(receiver);
    }

    private void setSpeakerphoneOn(boolean on) {
        boolean wasOn = audioManager.isSpeakerphoneOn();
        if (wasOn == on) {
            return;
        }
        audioManager.setSpeakerphoneOn(on);
    }

    private void setMicrophoneMute(boolean on) {
        boolean wasMuted = audioManager.isMicrophoneMute();
        if (wasMuted == on) {
            return;
        }
        audioManager.setMicrophoneMute(on);
    }

    private boolean hasEarpiece() {
        return apprtcContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
    }

    @Deprecated
    private boolean hasWiredHeadset() {
        @SuppressLint("WrongConstant") final AudioDeviceInfo[] devices = audioManager.getDevices(android.media.AudioManager.GET_DEVICES_ALL);
        for (AudioDeviceInfo device : devices) {
            final int type = device.getType();
            if (type == AudioDeviceInfo.TYPE_WIRED_HEADSET || type == AudioDeviceInfo.TYPE_USB_DEVICE) {
                return true;
            }
        }
        return false;
    }

    public void updateAudioDeviceState() {
        ThreadUtils.checkIsOnMainThread();

        Set<AudioDevice> newAudioDevices = new HashSet<>();

        if (hasWiredHeadset) {
            newAudioDevices.add(AudioDevice.WIRED_HEADSET);
        } else {
            newAudioDevices.add(AudioDevice.SPEAKER_PHONE);
            if (hasEarpiece()) {
                newAudioDevices.add(AudioDevice.EARPIECE);
            }
        }

        boolean audioDeviceSetUpdated = !audioDevices.equals(newAudioDevices);
        audioDevices = newAudioDevices;

        if (hasWiredHeadset && userSelectedAudioDevice == AudioDevice.SPEAKER_PHONE) {
            userSelectedAudioDevice = AudioDevice.WIRED_HEADSET;
        }
        if (!hasWiredHeadset && userSelectedAudioDevice == AudioDevice.WIRED_HEADSET) {
            userSelectedAudioDevice = AudioDevice.SPEAKER_PHONE;
        }

        AudioDevice newAudioDevice = hasWiredHeadset ? AudioDevice.WIRED_HEADSET : defaultAudioDevice;

        if (newAudioDevice != selectedAudioDevice || audioDeviceSetUpdated) {
            setAudioDeviceInternal(newAudioDevice);

            if (audioManagerEvents != null) {
                audioManagerEvents.onAudioDeviceChanged(selectedAudioDevice, audioDevices);
            }
        }
    }

    public enum AudioDevice {
        SPEAKER_PHONE, WIRED_HEADSET, EARPIECE, NONE
    }

    public enum AudioManagerState {
        UNINITIALIZED, PREINITIALIZED, RUNNING,
    }

    public interface AudioManagerEvents {
        void onAudioDeviceChanged(AudioDevice selectedAudioDevice, Set<AudioDevice> availableAudioDevices);
    }

    private class WiredHeadsetReceiver extends BroadcastReceiver {
        private static final int STATE_UNPLUGGED = 0;
        private static final int STATE_PLUGGED = 1;

        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra("state", STATE_UNPLUGGED);
            hasWiredHeadset = (state == STATE_PLUGGED);
            updateAudioDeviceState();
        }
    }
}
*/
