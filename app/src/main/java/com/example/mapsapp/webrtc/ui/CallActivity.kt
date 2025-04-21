package com.example.mapsapp.webrtc.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.mapsapp.databinding.ActivityCallBinding
import com.example.mapsapp.webrtc.manager.WebRTCManager
import com.google.firebase.database.*
import dagger.hilt.android.AndroidEntryPoint
import org.webrtc.SurfaceViewRenderer

@AndroidEntryPoint
class CallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCallBinding
    private lateinit var webRtcManager: WebRTCManager

    private lateinit var localView: SurfaceViewRenderer
    private lateinit var remoteView: SurfaceViewRenderer

    private var isCaller = false
    private var isVideoCall = true
    private lateinit var targetId: String
    private lateinit var roomId: String

    companion object {
        private val PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        private const val PERMISSION_REQUEST_CODE = 123
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Intent verilerini al
        isCaller = intent.getBooleanExtra("isCaller", false)
        isVideoCall = intent.getBooleanExtra("isVideoCall", true)
        targetId = intent.getStringExtra("target") ?: run {
            finish()
            return
        }

        roomId = if (isCaller) {
            "${getCurrentUid()}-$targetId"
        } else {
            "$targetId-${getCurrentUid()}"
        }

        localView = binding.localView
        remoteView = binding.remoteView

        if (hasPermissions()) {
            startCallSetup()
        } else {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_REQUEST_CODE)
        }

        binding.endCallButton.setOnClickListener {
            endCall()
        }

        listenForCallEnd()
    }

    private fun hasPermissions(): Boolean {
        return PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun getCurrentUid(): String {
        return com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
    }

    private fun startCallSetup() {
        webRtcManager = WebRTCManager(
            context = applicationContext,
            localRenderer = localView,
            remoteRenderer = remoteView,
            isCaller = isCaller,
            isVideoCall = isVideoCall,
            targetId = targetId
        )

        webRtcManager.startLocalMedia()

        if (isCaller) {
            webRtcManager.startCall()
        } else {
            webRtcManager.receiveCall()
        }

    }

    private fun endCall() {
        webRtcManager.close()
        FirebaseDatabase.getInstance().reference
            .child("calls")
            .child(roomId)
            .child("ended")
            .setValue(true) // Karşı taraf da algılasın
        FirebaseDatabase.getInstance().reference
            .child("calls")
            .child(roomId)
            .removeValue()
        finish()
    }

    private fun listenForCallEnd() {
        FirebaseDatabase.getInstance().reference
            .child("calls").child(roomId).child("ended")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.getValue(Boolean::class.java) == true) {
                        finish()
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            startCallSetup()
        } else {
            Toast.makeText(this, "Gerekli izinler verilmedi", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
