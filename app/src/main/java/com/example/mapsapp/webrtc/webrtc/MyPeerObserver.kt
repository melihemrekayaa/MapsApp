package com.example.mapsapp.webrtc.webrtc

import android.util.Log
import org.webrtc.*

open class MyPeerObserver : PeerConnection.Observer {

    private val TAG = "MyPeerObserver"

    override fun onSignalingChange(state: PeerConnection.SignalingState?) {
        Log.d(TAG, "Signaling state changed: $state")
    }

    override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
        Log.d(TAG, "ICE connection state changed: $state")
    }

    override fun onIceConnectionReceivingChange(receiving: Boolean) {
        Log.d(TAG, "ICE connection receiving change: $receiving")
    }

    override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {
        Log.d(TAG, "ICE gathering state changed: $state")
    }

    override fun onIceCandidate(candidate: IceCandidate?) {
        candidate?.let {
            Log.d(TAG, "New ICE candidate: ${it.sdp}")
            // Burada ICE adaylarını sinyalleme sunucusuna gönderebilirsin
        }
    }

    override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {
        candidates?.let {
            Log.d(TAG, "ICE candidates removed")
        }
    }

    override fun onAddStream(mediaStream: MediaStream?) {
        mediaStream?.let {
            Log.d(TAG, "New media stream added: ${it.videoTracks.size} video tracks, ${it.audioTracks.size} audio tracks")
        }
    }

    override fun onRemoveStream(mediaStream: MediaStream?) {
        Log.d(TAG, "Media stream removed")
    }

    override fun onDataChannel(dataChannel: DataChannel?) {
        dataChannel?.let {
            Log.d(TAG, "Data channel opened: ${it.label()}")
        }
    }

    override fun onRenegotiationNeeded() {
        Log.d(TAG, "Renegotiation needed")
        // Bağlantı değişiklikleri olduğunda yeniden görüşme başlatabilirsin
    }

    override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
        Log.d(TAG, "New track added to media stream")
    }
}
