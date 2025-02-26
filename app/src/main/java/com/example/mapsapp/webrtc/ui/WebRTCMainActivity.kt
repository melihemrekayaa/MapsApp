package com.example.mapsapp.webrtc.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.mapsapp.databinding.ActivityMainWebrtcBinding
import com.example.mapsapp.webrtc.utils.DataModel
import com.example.mapsapp.webrtc.utils.DataModelType
import com.example.mapsapp.webrtc.viewmodel.CallViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WebRTCMainActivity : AppCompatActivity(), CallViewModel.CallListener {
    private lateinit var views: ActivityMainWebrtcBinding
    private val callViewModel: CallViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        views = ActivityMainWebrtcBinding.inflate(layoutInflater)
        setContentView(views.root)

        callViewModel.setCallListener(this)

        callViewModel.incomingCall.observe(this) { data ->
            data?.let { onCallReceived(it) }
        }

        callViewModel.usersStatus.observe(this) { statusList ->
            Log.d("WebRTCMainActivity", "User Status Updated: $statusList")
        }
    }

    override fun onCallReceived(model: DataModel) {
        runOnUiThread {
            views.apply {
                val isVideoCall = model.type == DataModelType.StartVideoCall
                incomingCallTitleTv.text = "${model.sender} is calling..."
                incomingCallLayout.isVisible = true
                acceptButton.setOnClickListener {
                    incomingCallLayout.isVisible = false
                    callViewModel.answerCall(model.target)
                }
                declineButton.setOnClickListener {
                    incomingCallLayout.isVisible = false
                    callViewModel.endCall(model.target)
                }
            }
        }
    }

    override fun onCallEnded() {
        views.incomingCallLayout.isVisible = false
    }
}
