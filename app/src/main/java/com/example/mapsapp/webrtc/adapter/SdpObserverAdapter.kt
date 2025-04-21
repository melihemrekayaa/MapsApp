package com.example.mapsapp.webrtc.adapter

import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

// 🔧 Artık abstract değil, override ettik direkt
open class SdpObserverAdapter : SdpObserver {
    override fun onCreateSuccess(sdp: SessionDescription?) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(p0: String?) {}
    override fun onSetFailure(p0: String?) {}
}
