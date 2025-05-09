package com.example.mapsapp.view.ui

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mapsapp.R
import com.example.mapsapp.adapter.ChatAdapter
import com.example.mapsapp.databinding.FragmentChatBinding
import com.example.mapsapp.util.BaseFragment
import com.example.mapsapp.viewmodel.ChatViewModel
import com.example.mapsapp.webrtc.CallActivity
import com.example.mapsapp.webrtc.FirebaseClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ChatFragment : BaseFragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private val chatViewModel: ChatViewModel by viewModels()
    private lateinit var adapter: ChatAdapter
    private var receiverId: String? = null
    private var receiverName: String? = null
    @Inject
    lateinit var firebaseClient: FirebaseClient

    private lateinit var toolbar: androidx.appcompat.widget.Toolbar
    private var callDialogShown = false


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()

        receiverId = arguments?.getString("receiverId")
        receiverName = arguments?.getString("receiverName")

        Log.d("ChatFragment", "receiverId: $receiverId") // 📌 **Gelen receiverId kontrolü**

        initChatUI()

        toolbar = binding.chatToolbar
        setupToolbar()

        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_voice_call -> {
                    Log.d("ChatFragment", "🔊 Sesli Arama Başlatılıyor...")
                    startCall(false)
                    true
                }
                R.id.action_video_call -> {
                    Log.d("ChatFragment", "📹 Görüntülü Arama Başlatılıyor...")
                    startCall(true)
                    true
                }
                else -> false
            }
        }

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        chatViewModel.listenForIncomingCalls(currentUserId) { roomId, callerUid, isVideoCall ->
            if (!callDialogShown) {
                callDialogShown = true
                showIncomingCallDialog(roomId, callerUid, isVideoCall)
            }
        }
    }

    private fun initChatUI() {
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

        binding.loadingIndicator.visibility = View.VISIBLE

        chatViewModel.messages.observe(viewLifecycleOwner) { messages ->
            adapter.updateMessages(messages)
            binding.recyclerView.scrollToPosition(messages.size - 1)
            binding.loadingIndicator.visibility = View.GONE
        }

        receiverId?.let { chatViewModel.listenForMessages(it) }
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
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
                val roomId = "room_${System.currentTimeMillis()}"
                val senderUid = FirebaseAuth.getInstance().currentUser?.uid ?: return@let

                chatViewModel.sendCallRequest(receiverId, isVideoCall, roomId, requireContext()) { success ->
                    if (success) {
                        firebaseClient.listenForCallStatus(roomId) { status ->
                            when (status) {
                                "rejected" -> {
                                    Toast.makeText(requireContext(), "Çağrı reddedildi", Toast.LENGTH_SHORT).show()
                                }
                                "accepted" -> {
                                    // opsiyonel: kabul ekranı gösterilebilir
                                }
                            }
                        }

                        if (!CallActivity.isActive) {
                            val intent = Intent(requireContext(), CallActivity::class.java).apply {
                                putExtra("roomId", roomId)
                                putExtra("isCaller", true)
                                putExtra("callerUid", senderUid)
                                putExtra("isVideoCall", true)
                            }
                            startActivity(intent)
                        }

                    } else {
                        Toast.makeText(requireContext(), "Çağrı gönderilemedi", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }




    private fun showIncomingCallDialog(roomId: String, callerUid: String, isVideoCall: Boolean) {
        AlertDialog.Builder(requireContext())
            .setTitle("Incoming ${if (isVideoCall) "Video" else "Voice"} Call")
            .setMessage("User $callerUid is calling you.")
            .setPositiveButton("Accept") { _, _ ->
                val intent = Intent(requireContext(), CallActivity::class.java).apply {
                    putExtra("roomId", roomId)
                    putExtra("callerUid", callerUid)
                    putExtra("isVideoCall", isVideoCall)
                    putExtra("isCaller", false)
                }
                startActivity(intent)
            }
            .setNegativeButton("Reject") { _, _ ->
                firebaseClient.rejectCall(roomId)
            }
            .setCancelable(false)
            .show()
    }




    private fun getCameraAndMicPermission(onPermissionGranted: () -> Unit) {
        val permissions = arrayOf(android.Manifest.permission.CAMERA, android.Manifest.permission.RECORD_AUDIO)
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isEmpty()) {
            Log.d("ChatFragment", "✅ Kamera ve Mikrofon izinleri zaten verilmiş.")
            onPermissionGranted()
        } else {
            Log.d("ChatFragment", "🚨 Kamera ve Mikrofon izinleri isteniyor: $permissionsToRequest")
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
                Log.d("ChatFragment", "✅ Kullanıcı izinleri onayladı.")
                onPermissionGranted?.invoke()
            } else {
                Log.e("ChatFragment", "🚨 Kullanıcı izinleri reddetti.")
                Toast.makeText(requireContext(), "Çağrı için izinler gerekli.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        callDialogShown = false
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private var onPermissionGranted: (() -> Unit)? = null
    }
}
