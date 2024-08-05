package com.example.mapsapp.webrtc.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.commit
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.codewithkael.firebasevideocall.service.MainService
import com.example.mapsapp.R
import com.example.mapsapp.webrtc.adapters.MainRecyclerViewAdapter
import com.example.mapsapp.webrtc.repository.MainRepository
import com.example.mapsapp.webrtc.service.MainServiceRepository
import com.example.mapsapp.webrtc.utils.DataModel
import com.example.mapsapp.webrtc.utils.DataModelType
import com.example.mapsapp.databinding.ActivityMainWebrtcBinding
import com.example.mapsapp.view.ui.ChatFragment
import com.example.mapsapp.webrtc.utils.getCameraAndMicPermission
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WebRTCMainActivity : AppCompatActivity(), MainRecyclerViewAdapter.Listener, MainService.Listener {
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

        // Intent ile gelen username değerini alalım ve loglayalım
        username = intent.getStringExtra("username")
        Log.d(TAG, "Username received: $username")

        if (username == null) {
            finish()
            return
        }

        init()
        val receiverId = intent.getStringExtra("receiverId")
        if (receiverId != null) {
            val chatFragment = ChatFragment().apply {
                arguments = Bundle().apply {
                    putString("receiverId", receiverId)
                }
            }
            supportFragmentManager.commit {
                replace(R.id.nav_host_fragment, chatFragment)
                addToBackStack(null)
            }
        }
    }

    private fun init() {
        // Loglama ekleyelim
        Log.d(TAG, "Initializing with username: $username")

        // 1. Observe other users status
        subscribeObservers()
        // 2. Start foreground service to listen negotiations and calls.
        startMyService()
        // 3. Observe incoming calls
        mainRepository.observeIncomingCalls(username!!) { callId, caller ->
            showIncomingCallDialog(callId, caller)
        }
    }

    private fun subscribeObservers() {
        setupRecyclerView()
        MainService.listener = this
        mainRepository.observeUsersStatus {
            Log.d(TAG, "subscribeObservers: $it")
            mainAdapter?.updateList(it)
        }
    }

    private fun setupRecyclerView() {
        mainAdapter = MainRecyclerViewAdapter(this)
        val layoutManager = LinearLayoutManager(this)
        views.mainRecyclerView.apply {
            this.layoutManager = layoutManager
            adapter = mainAdapter
        }
    }

    private fun startMyService() {
        username?.let {
            Log.d(TAG, "Starting service with username: $it")
            mainServiceRepository.startService(it)
        }
    }

    override fun onVideoCallClicked(username: String) {
        getCameraAndMicPermission {
            mainRepository.placeCall(this.username!!, username) {
                if (it) {
                    // Start video call
                    startActivity(Intent(this, WebRTCMainActivity::class.java).apply {
                        putExtra("target", username)
                        putExtra("isVideoCall", true)
                        putExtra("isCaller", true)
                        putExtra("username", this@WebRTCMainActivity.username)  // username'i geçiriyoruz
                    })
                }
            }
        }
    }

    override fun onAudioCallClicked(username: String) {
        getCameraAndMicPermission {
            mainRepository.placeCall(this.username!!, username) {
                if (it) {
                    // Start audio call
                    startActivity(Intent(this, WebRTCMainActivity::class.java).apply {
                        putExtra("target", username)
                        putExtra("isVideoCall", false)
                        putExtra("isCaller", true)
                        putExtra("username", this@WebRTCMainActivity.username)  // username'i geçiriyoruz
                    })
                }
            }
        }
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
                        // Create an intent to go to video call activity
                        startActivity(Intent(this@WebRTCMainActivity, WebRTCMainActivity::class.java).apply {
                            putExtra("target", model.sender)
                            putExtra("isVideoCall", isVideoCall)
                            putExtra("isCaller", false)
                            putExtra("username", username)  // username'i geçiriyoruz
                        })
                    }
                }
                declineButton.setOnClickListener {
                    incomingCallLayout.isVisible = false
                    mainRepository.endCall()
                }
            }
        }
    }

    private fun showIncomingCallDialog(callId: String, caller: String) {
        runOnUiThread {
            views.apply {
                val isVideoCallText = "Video"
                incomingCallTitleTv.text = "$caller is $isVideoCallText Calling you"
                incomingCallLayout.isVisible = true
                acceptButton.setOnClickListener {
                    getCameraAndMicPermission {
                        incomingCallLayout.isVisible = false
                        mainRepository.answerCall(callId)
                        // Start video call
                        startActivity(Intent(this@WebRTCMainActivity, WebRTCMainActivity::class.java).apply {
                            putExtra("target", caller)
                            putExtra("isVideoCall", true)
                            putExtra("isCaller", false)
                            putExtra("username", username)  // username'i geçiriyoruz
                        })
                    }
                }
                declineButton.setOnClickListener {
                    incomingCallLayout.isVisible = false
                    mainRepository.endCall()
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
