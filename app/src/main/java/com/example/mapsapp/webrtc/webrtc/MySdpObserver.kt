package com.example.mapsapp.webrtc.webrtc

import android.util.Log
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

open class MySdpObserver : SdpObserver {

    private val TAG = "MySdpObserver"

    override fun onCreateSuccess(desc: SessionDescription?) {
        Log.d(TAG, "SDP Create Success: ${desc?.type}")
    }

    override fun onSetSuccess() {
        Log.d(TAG, "SDP Set Success")
    }

    override fun onCreateFailure(p0: String?) {
        Log.e(TAG, "SDP Create Failure: $p0")
    }

    override fun onSetFailure(p0: String?) {
        Log.e(TAG, "SDP Set Failure: $p0")
    }
}
