package com.example.mapsapp.view.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mapsapp.R
import com.example.mapsapp.adapter.ChatAdapter
import com.example.mapsapp.databinding.FragmentChatBinding
import com.example.mapsapp.model.User
import com.example.mapsapp.util.BaseFragment
import com.example.mapsapp.viewmodel.ChatViewModel
import com.example.mapsapp.webrtc.CallActivity
import com.example.mapsapp.webrtc.FirebaseClient
import com.example.mapsapp.webrtc.IncomingCallActivity.Companion.isActive
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
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


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()

        receiverId = arguments?.getString("receiverId")
        receiverName = arguments?.getString("receiverName")

        Log.d("ChatFragment", "receiverId: $receiverId")

        setupToolbar()
        observeMessages()
        setupSendButton()

        return binding.root
    }

    private fun setupToolbar() {
        toolbar = binding.chatToolbar
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        toolbar.navigationIcon?.setTint(resources.getColor(R.color.white))
        toolbar.title = receiverName ?: "User"
        toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_voice_call -> {
                    startCall(false)
                    true
                }
                R.id.action_video_call -> {
                    startCall(true)
                    true
                }
                else -> false
            }
        }
    }
    private fun observeMessages() {

        adapter = ChatAdapter(currentUserId = auth.currentUser?.uid ?: "")
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(context)

        chatViewModel.messagesWithProfiles.observe(viewLifecycleOwner) { messages ->

            if (messages.isNullOrEmpty()) {
                binding.recyclerView.visibility = View.GONE
            } else {
                adapter.submitList(messages)
                binding.recyclerView.visibility = View.VISIBLE
                binding.recyclerView.scrollToPosition(messages.size - 1)
            }
        }

        receiverId?.let { chatViewModel.listenForMessages(it) }
    }






    private fun setupSendButton() {
        binding.sendButton.setOnClickListener {
            val messageText = binding.messageEditText.text.toString()
            if (messageText.isNotBlank() && receiverId != null) {
                chatViewModel.sendMessage(receiverId!!, messageText)
                binding.messageEditText.text?.clear()
            }
        }
    }

    private fun startCall(isVideoCall: Boolean) {
        getCameraAndMicPermission {
            receiverId?.let { receiverId ->
                val roomId = "room_${System.currentTimeMillis()}"
                val senderUid = FirebaseAuth.getInstance().currentUser?.uid ?: return@let

                lifecycleScope.launch {
                    val inCall = firebaseClient.isUserInCall(receiverId)

                    if (inCall) {
                        Toast.makeText(requireContext(), "Kullanıcı şu anda başka bir görüşmede.", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    firebaseClient.setUserInCall(receiverId, true)

                    chatViewModel.sendCallRequest(receiverId, isVideoCall, roomId) { success ->
                        if (success) {
                            lifecycleScope.launch {
                                val status = firebaseClient.listenForCallStatus(roomId)
                                if (status == "rejected") {
                                    Toast.makeText(requireContext(), "Çağrı reddedildi", Toast.LENGTH_SHORT).show()
                                }
                            }

                            val intent = Intent(requireContext(), CallActivity::class.java).apply {
                                putExtra("roomId", roomId)
                                putExtra("callerUid", senderUid)
                                putExtra("isCaller", true)
                                putExtra("isVideoCall", isVideoCall)
                            }
                            startActivity(intent)
                        } else {
                            Toast.makeText(requireContext(), "Çağrı gönderilemedi", Toast.LENGTH_SHORT).show()
                            lifecycleScope.launch {
                                firebaseClient.setUserInCall(receiverId, false)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getCameraAndMicPermission(onGranted: () -> Unit) {
        val permissions = arrayOf(android.Manifest.permission.CAMERA, android.Manifest.permission.RECORD_AUDIO)
        val deniedPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }

        if (deniedPermissions.isEmpty()) {
            onGranted()
        } else {
            onPermissionGranted = onGranted
            requestPermissions(deniedPermissions.toTypedArray(), REQUEST_CODE_PERMISSIONS)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                onPermissionGranted?.invoke()
            } else {
                Toast.makeText(requireContext(), "Çağrı için izinler gerekli.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        isActive = false

        lifecycleScope.launch {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                try {
                    firebaseClient.setUserInCall(uid, false)
                    Log.d("CallActivity", "✅ inCall FALSE yazıldı: $uid")
                } catch (e: Exception) {
                    Log.e("CallActivity", "❌ inCall sıfırlanamadı: ${e.message}")
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private var onPermissionGranted: (() -> Unit)? = null
    }
}
