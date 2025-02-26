package com.example.mapsapp.webrtc.webrtc

import android.util.Log
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

open class MySdpObserver(
    private val onSuccess: ((SessionDescription) -> Unit)? = null
) : SdpObserver {

    private val TAG = "MySdpObserver"

    override fun onCreateSuccess(desc: SessionDescription?) {
        Log.d(TAG, "SDP Create Success: ${desc?.type}")
        desc?.let { onSuccess?.invoke(it) } // Eğer onSuccess fonksiyonu varsa, çağır
    }

    override fun onSetSuccess() {
        Log.d(TAG, "SDP Set Success")
    }

    override fun onCreateFailure(error: String?) {
        Log.e(TAG, "SDP Create Failure: $error")
    }

    override fun onSetFailure(error: String?) {
        Log.e(TAG, "SDP Set Failure: $error")
    }
}
