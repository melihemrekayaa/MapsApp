package com.example.mapsapp.webrtc.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mapsapp.databinding.ActivityMainWebrtcBinding
import com.example.mapsapp.view.ui.ChatFragment
import com.example.mapsapp.webrtc.adapters.MainRecyclerViewAdapter
import com.example.mapsapp.webrtc.repository.MainRepository
import com.example.mapsapp.webrtc.service.MainService
import com.example.mapsapp.webrtc.service.MainServiceRepository
import com.example.mapsapp.webrtc.utils.DataModel
import com.example.mapsapp.webrtc.utils.DataModelType
import com.example.mapsapp.webrtc.utils.getCameraAndMicPermission
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class WebRTCMainActivity : AppCompatActivity(), MainService.Listener {
    private val TAG = "WebRTCMainActivity"

    private lateinit var views: ActivityMainWebrtcBinding
    private var username: String? = null

    @Inject
    lateinit var mainRepository: MainRepository
    @Inject
    lateinit var mainServiceRepository: MainServiceRepository
    private var mainAdapter: MainRecyclerViewAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        views = ActivityMainWebrtcBinding.inflate(layoutInflater)
        setContentView(views.root)
        init()
    }

    private fun init() {
        username = intent.getStringExtra("username")
        if (username == null) finish()
        //1. observe other users status
        subscribeObservers()
        //2. start foreground service to listen negotiations and calls.
        startMyService()
    }

    private fun subscribeObservers() {
        MainService.listener = this
        mainRepository.observeUsersStatus {
            Log.d(TAG, "subscribeObservers: $it")
            mainAdapter?.updateList(it)
        }
    }



    private fun startMyService() {
        mainServiceRepository.startService(username!!)
    }



    override fun onBackPressed() {
        super.onBackPressed()
        mainServiceRepository.stopService()
    }

    override fun onCallReceived(model: DataModel) {
        runOnUiThread {
            views.apply {
                val isVideoCall = model.type == DataModelType.StartVideoCall
                val isVideoCallText = if (isVideoCall) "Video" else "Audio"
                incomingCallTitleTv.text = "${model.sender} is $isVideoCallText Calling you"
                incomingCallLayout.isVisible = true
                acceptButton.setOnClickListener {
                    getCameraAndMicPermission {
                        incomingCallLayout.isVisible = false
                        //create an intent to go to video call activity
                        startActivity(Intent(this@WebRTCMainActivity,CallActivity::class.java).apply {
                            putExtra("target",model.sender)
                            putExtra("isVideoCall",isVideoCall)
                            putExtra("isCaller",false)
                        })
                    }
                }
                declineButton.setOnClickListener {
                    incomingCallLayout.isVisible = false
                }

            }
        }
    }


}