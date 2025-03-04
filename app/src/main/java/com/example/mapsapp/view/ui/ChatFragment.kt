package com.example.mapsapp.view.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import android.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mapsapp.R
import com.example.mapsapp.adapter.ChatAdapter
import com.example.mapsapp.databinding.FragmentChatBinding
import com.example.mapsapp.model.Event
import com.example.mapsapp.model.User
import com.example.mapsapp.util.BaseFragment
import com.example.mapsapp.viewmodel.ChatViewModel
import com.example.mapsapp.webrtc.repository.MainRepository
import com.example.mapsapp.webrtc.service.MainService
import com.example.mapsapp.webrtc.ui.CallActivity
import com.example.mapsapp.webrtc.utils.DataModel
import com.example.mapsapp.webrtc.utils.DataModelType
import com.example.mapsapp.webrtc.webrtc.WebRTCClient
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ChatFragment(
    private val listener : Listener
) : BaseFragment(), WebRTCClient.Listener {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private val chatViewModel: ChatViewModel by viewModels()
    private lateinit var adapter: ChatAdapter
    private var receiverId: String? = null
    private var receiverName: String? = null
    private var userListener: ListenerRegistration? = null


    @Inject
    lateinit var mainRepository: MainRepository

    private lateinit var webRTCClient: WebRTCClient
    private lateinit var toolbar: androidx.appcompat.widget.Toolbar


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()

        receiverId = arguments?.getString("receiverId")
        receiverName = arguments?.getString("receiverName")

        Log.d("ChatFragment", "receiverId: $receiverId") // 📌 **Gelen receiverId kontrolü**

        init()

        adapter = ChatAdapter(chatViewModel.messages.value ?: emptyList(), auth.currentUser?.uid ?: "")
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(context)

        binding.sendButton.setOnClickListener {
            val messageText = binding.messageEditText.text.toString()
            if (messageText.isNotBlank() && receiverId != null) {
                chatViewModel.sendMessage(receiverId!!, messageText)
                binding.messageEditText.text?.clear()
                binding.recyclerView.scrollToPosition(adapter.itemCount - 1)
            }
        }

        val progressBar = binding.loadingIndicator
        progressBar.visibility = View.VISIBLE

        toolbar = binding.chatToolbar

        chatViewModel.messages.observe(viewLifecycleOwner) { messages ->
            adapter.updateMessages(messages)
            binding.recyclerView.scrollToPosition(messages.size - 1)
            progressBar.visibility = View.GONE
            setupToolbar()
        }

        receiverId?.let { chatViewModel.listenForMessages(it) }
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            startUserListener(userId)
        }
    }

    private fun init() {
        webRTCClient = WebRTCClient(requireContext(), com.google.gson.Gson())
        webRTCClient.listener = this

        Log.d("ChatFragment", "✅ WebRTC Client initialized")
    }

    interface Listener {
        fun onVideoCallClicked(username:String)
        fun onAudioCallClicked(username:String)
    }

    fun bind(
        user:Pair<String,String>,
        videoCallClicked:(String) -> Unit,
        audioCallClicked:(String)-> Unit
    ){

        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_voice_call -> {
                    Log.d("ChatFragment", "🔊 Voice Call Clicked")
                    audioCallClicked.invoke(user.first)
                    true
                }
                R.id.action_video_call -> {
                    Log.d("ChatFragment", "📹 Video Call Clicked")
                    videoCallClicked.invoke(user.first)
                    true
                }
                else -> false
            }
        }

        /*binding.apply {
            when (user.second) {
                "ONLINE" -> {
                    videoCallBtn.isVisible = true
                    audioCallBtn.isVisible = true
                    videoCallBtn.setOnClickListener {
                        videoCallClicked.invoke(user.first)
                    }
                    audioCallBtn.setOnClickListener {
                        audioCallClicked.invoke(user.first)
                    }
                    statusTv.setTextColor(context.resources.getColor(R.color.light_green, null))
                    statusTv.text = "Online"
                }
                "OFFLINE" -> {
                    videoCallBtn.isVisible = false
                    audioCallBtn.isVisible = false
                    statusTv.setTextColor(context.resources.getColor(R.color.red, null))
                    statusTv.text = "Offline"
                }
                "IN_CALL" -> {
                    videoCallBtn.isVisible = false
                    audioCallBtn.isVisible = false
                    statusTv.setTextColor(context.resources.getColor(R.color.yellow, null))
                    statusTv.text = "In Call"
                }
            }

            usernameTv.text = user.first
        }*/
    }

    private fun setupToolbar() {
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        toolbar.navigationIcon?.setTint(resources.getColor(R.color.white))
        toolbar.title = receiverName ?: "User"
        toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }


    }

    private fun startCall(isVideoCall: Boolean) {
        getCameraAndMicPermission {
            receiverId?.let { receiverId ->
                val senderUsername = getUsernameFromEmail(auth.currentUser?.email ?: "")
                Log.d("ChatFragment", "📞 Starting call with $receiverId - isVideoCall: $isVideoCall")

                mainRepository.sendConnectionRequest(receiverId, isVideoCall) { success ->
                    if (success) {
                        Log.d("ChatFragment", "✅ Call request sent successfully")

                        startActivity(Intent(context, CallActivity::class.java).apply {
                            putExtra("target", receiverId)
                            putExtra("isVideoCall", isVideoCall)
                            putExtra("isCaller", true)
                            putExtra("username", senderUsername)
                        })
                    } else {
                        Log.e("ChatFragment", "🚨 Call request failed")
                        Toast.makeText(context, "Call failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun getCameraAndMicPermission(onPermissionGranted: () -> Unit) {
        val permissions = arrayOf(android.Manifest.permission.CAMERA, android.Manifest.permission.RECORD_AUDIO)
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isEmpty()) {
            Log.d("ChatFragment", "✅ Permissions already granted")
            onPermissionGranted()
        } else {
            Log.d("ChatFragment", "🚨 Requesting permissions: $permissionsToRequest")
            requestPermissions(permissionsToRequest.toTypedArray(), REQUEST_CODE_PERMISSIONS)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Log.d("ChatFragment", "✅ Permissions granted by user")
                onPermissionGranted?.invoke()
            } else {
                Log.e("ChatFragment", "🚨 Permissions denied by user")
                Toast.makeText(requireContext(), "Permissions not granted.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private var onPermissionGranted: (() -> Unit)? = null
    }

    private fun getUsernameFromEmail(email: String): String {
        return email.substringBefore("@")
    }

    override fun onTransferEventToSocket(data: DataModel) {
        Log.d("ChatFragment", "📩 WebRTC Event Sent: $data")
    }


    private fun startUserListener(userId: String) {

        val db = Firebase.firestore

        userListener = db.collection("users").document(userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("FirestoreListener", "Hata: Firestore dinleme başarısız!", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val userData = snapshot.data
                    if (userData != null) {
                        val latestEventData = userData["latestEvent"]
                        val latestEvent = if (latestEventData is Map<*, *>) {
                            Event(
                                eventType = latestEventData["eventType"] as? String ?: "",
                                sender = latestEventData["sender"] as? String ?: "",
                                target = latestEventData["target"] as? String ?: "",
                                timeStamp = (latestEventData["timeStamp"] as? Number)?.toLong() ?: 0L,
                                type = latestEventData["type"] as? String ?: ""
                            )
                        } else {
                            Event() // Default boş Event nesnesi
                        }

                        val user = User(
                            email = userData["email"] as? String ?: "",
                            friends = userData["friends"] as? List<String> ?: emptyList(),
                            isOnline = userData["isOnline"] as? Boolean ?: false,
                            latestEvent = latestEvent,
                            name = userData["name"] as? String ?: "",
                            photoUrl = userData["photoUrl"] as? String,
                            uid = userData["uid"] as? String ?: ""
                        )

                        Log.d("FirestoreListener", "Güncellenen Kullanıcı Verisi: $user")
                    }
                } else {
                    Log.d("FirestoreListener", "Kullanıcı verisi bulunamadı!")
                }
            }

    }

    private fun stopUserListener() {
        userListener?.remove()
        userListener = null
        Log.d("FirestoreListener", "❌ Firestore dinleyici durduruldu.")
    }


    override fun onDestroyView() {
        super.onDestroyView()
        stopUserListener()
    }

}
