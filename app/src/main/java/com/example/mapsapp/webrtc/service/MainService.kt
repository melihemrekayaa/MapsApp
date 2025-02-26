package com.example.mapsapp.webrtc.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.mapsapp.R
import com.example.mapsapp.webrtc.repository.MainRepository
import com.example.mapsapp.webrtc.ui.CallActivity
import com.example.mapsapp.webrtc.utils.DataModel
import com.example.mapsapp.webrtc.utils.DataModelType
import com.example.mapsapp.webrtc.utils.isValid
import com.example.mapsapp.webrtc.webrtc.RTCAudioManager
import dagger.hilt.android.AndroidEntryPoint
import org.webrtc.SurfaceViewRenderer
import javax.inject.Inject

@AndroidEntryPoint
class MainService : Service(), MainRepository.Listener {

    private val TAG = "MainService"

    private var isServiceRunning = false
    private var username: String? = null

    @Inject
    lateinit var mainRepository: MainRepository

    private lateinit var notificationManager: NotificationManager
    private lateinit var rtcAudioManager: RTCAudioManager
    private var isPreviousCallStateVideo = true

    companion object {
        var listener: Listener? = null
        var endCallListener: EndCallListener? = null
        var localSurfaceView: SurfaceViewRenderer? = null
        var remoteSurfaceView: SurfaceViewRenderer? = null
        var screenPermissionIntent: Intent? = null
    }

    override fun onCreate() {
        super.onCreate()
        rtcAudioManager = RTCAudioManager.create(this)
        rtcAudioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE)
        notificationManager = getSystemService(NotificationManager::class.java)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let { incomingIntent ->
            when (incomingIntent.action) {
                "START_SERVICE" -> handleStartService(incomingIntent)
                "SETUP_VIEWS" -> handleSetupViews(incomingIntent)
                "END_CALL" -> handleEndCall()
                "SWITCH_CAMERA" -> handleSwitchCamera()
                "TOGGLE_AUDIO" -> handleToggleAudio(incomingIntent)
                "TOGGLE_VIDEO" -> handleToggleVideo(incomingIntent)
                "STOP_SERVICE" -> handleStopService()
                else -> Unit
            }
        }
        return START_STICKY
    }

    private fun handleStopService() {
        mainRepository.endCall()
        mainRepository.logOff {
            isServiceRunning = false
            stopSelf()
        }
    }

    private fun handleToggleVideo(incomingIntent: Intent) {
        val shouldBeMuted = incomingIntent.getBooleanExtra("shouldBeMuted", true)
        this.isPreviousCallStateVideo = !shouldBeMuted
        mainRepository.toggleVideo(shouldBeMuted)
    }

    private fun handleToggleAudio(incomingIntent: Intent) {
        val shouldBeMuted = incomingIntent.getBooleanExtra("shouldBeMuted", true)
        mainRepository.toggleAudio(shouldBeMuted)
    }

    private fun handleSwitchCamera() {
        mainRepository.switchCamera()
    }

    private fun handleEndCall() {
        mainRepository.sendEndCall()
        endCallAndRestartRepository()
    }

    private fun endCallAndRestartRepository() {
        if (username != null) {
            mainRepository.endCall()
            endCallListener?.onCallEnded()
            mainRepository.initWebrtcClient(username!!)
        } else {
            Log.e(TAG, "Username is null, cannot restart repository")
        }
    }

    private fun handleSetupViews(incomingIntent: Intent) {
        val isCaller = incomingIntent.getBooleanExtra("isCaller", false)
        val isVideoCall = incomingIntent.getBooleanExtra("isVideoCall", true)
        val target = incomingIntent.getStringExtra("target")
        this.isPreviousCallStateVideo = isVideoCall
        mainRepository.setTarget(target!!)
        mainRepository.initLocalSurfaceView(localSurfaceView!!, isVideoCall)
        mainRepository.initRemoteSurfaceView(remoteSurfaceView!!)
        if (!isCaller) {
            mainRepository.startCall()
        }
    }

    private fun handleStartService(incomingIntent: Intent) {
        if (!isServiceRunning) {
            isServiceRunning = true
            username = incomingIntent.getStringExtra("username")
            if (username == null) {
                Log.e(TAG, "Username is null, cannot start service")
                stopSelf()
                return
            }
            startServiceWithNotification()
            mainRepository.listener = this
            mainRepository.initFirebase()
            mainRepository.initWebrtcClient(username!!)
        }
    }

    private fun startServiceWithNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                "channel1", "foreground", NotificationManager.IMPORTANCE_HIGH
            )
            val intent = Intent(this, MainServiceReceiver::class.java).apply {
                action = "ACTION_EXIT"
            }
            val pendingIntent: PendingIntent =
                PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            notificationManager.createNotificationChannel(notificationChannel)
            val notification = NotificationCompat.Builder(
                this, "channel1"
            ).setSmallIcon(R.mipmap.ic_launcher)
                .addAction(R.drawable.ic_end_call, "Exit", pendingIntent)
            startForeground(1, notification.build())
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onLatestEventReceived(data: DataModel) {
        if (data.isValid()) {
            when (data.type) {
                DataModelType.StartVideoCall,
                DataModelType.StartAudioCall -> {
                    listener?.onCallReceived(data)

                    // Gelen çağrıyı bildirim olarak göster
                    showIncomingCallNotification(data)
                }
                else -> Unit
            }
        }
    }

    private fun showIncomingCallNotification(data: DataModel) {
        val callType = if (data.type == DataModelType.StartVideoCall) "Video" else "Audio"

        // Çağrıya yanıt vermek için "Kabul Et" Intent'i
        val acceptIntent = Intent(this, CallActivity::class.java).apply {
            putExtra("callerId", data.sender)
            putExtra("callType", data.type.name)
            action = "ACCEPT_CALL"
        }
        val acceptPendingIntent = PendingIntent.getActivity(this, 0, acceptIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Çağrıyı reddetmek için Broadcast Receiver
        val rejectIntent = Intent(this, MainServiceReceiver::class.java).apply {
            action = "REJECT_CALL"
            putExtra("callerId", data.sender)
        }
        val rejectPendingIntent = PendingIntent.getBroadcast(this, 1, rejectIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notificationManager = getSystemService(NotificationManager::class.java)

        // Android 8.0+ için bildirim kanalı oluştur
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "incoming_call", "Incoming Calls", NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, "incoming_call")
            .setSmallIcon(R.drawable.ic_call)
            .setContentTitle("Incoming $callType Call")
            .setContentText("Call from ${data.sender}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .addAction(R.drawable.accept_call, "Accept", acceptPendingIntent)
            .addAction(R.drawable.reject_call, "Reject", rejectPendingIntent)
            .build()

        notificationManager.notify(1001, notification)
    }



    override fun endCall() {
        endCallAndRestartRepository()
    }

    interface Listener {
        fun onCallReceived(model: DataModel)
    }

    interface EndCallListener {
        fun onCallEnded()
    }
}