package com.example.mapsapp.view.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mapsapp.R
import com.example.mapsapp.adapter.ChatAdapter
import com.example.mapsapp.databinding.FragmentChatBinding
import com.example.mapsapp.viewmodel.ChatViewModel
import com.example.mapsapp.webrtc.repository.MainRepository
import com.example.mapsapp.webrtc.service.MainService
import com.example.mapsapp.webrtc.ui.CallActivity
import com.example.mapsapp.webrtc.ui.WebRTCMainActivity
import com.example.mapsapp.webrtc.utils.DataModel
import com.example.mapsapp.webrtc.utils.DataModelType
import com.example.mapsapp.webrtc.utils.getCameraAndMicPermission
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ChatFragment : Fragment(), MainService.Listener {
    lateinit var _binding: FragmentChatBinding
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private val chatViewModel: ChatViewModel by viewModels()
    private lateinit var adapter: ChatAdapter
    private var receiverId: String? = null

    @Inject
    lateinit var mainRepository: MainRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()

        arguments?.let {
            receiverId = it.getString("receiverId")
        }

        init()

        setupToolbar()

        adapter = ChatAdapter(chatViewModel.messages.value ?: emptyList(), auth.currentUser?.uid ?: "")
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(context)

        binding.sendButton.setOnClickListener {
            val messageText = binding.messageEditText.text.toString()
            receiverId?.let { receiverId ->
                chatViewModel.sendMessage(receiverId, messageText)
                binding.messageEditText.text?.clear()
                binding.recyclerView.scrollToPosition(adapter.itemCount - 1)
            }
        }

        chatViewModel.messages.observe(viewLifecycleOwner, Observer { messages ->
            adapter.updateMessages(messages)
            binding.recyclerView.scrollToPosition(messages.size - 1)
        })

        receiverId?.let { chatViewModel.listenForMessages(it) }

        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        return binding.root
    }

    private fun init() {
        // 1. Observe other users status
        subscribeObservers()
        // 2. Start foreground service to listen negotiations and calls.
        // startMyService()
        // 3. Observe incoming calls
        /*mainRepository.observeIncomingCalls(username!!) { callId, caller ->
            showIncomingCallDialog(callId, caller)
        }*/
    }

    private fun subscribeObservers() {
       // setupRecyclerView()
        MainService.listener = this
        /*val currentUserId = username ?: return
        mainRepository.observeUsersStatus(currentUserId) {
            Log.d(TAG, "subscribeObservers: $it")
            mainAdapter?.updateList(it)
        }*/
    }


    private fun setupToolbar() {
        val toolbar = binding.chatToolbar
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        receiverId?.let { id ->
            chatViewModel.fetchUserName(id).observe(viewLifecycleOwner, Observer { userName ->
                toolbar.title = userName ?: "User"
            })
        }
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_voice_call -> {
                    startVoiceCall()
                    true
                }
                R.id.action_video_call -> {
                    startVideoCall()
                    true
                }
                else -> false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
       /* _binding = null*/
    }

    private fun startVoiceCall() {
        getCameraAndMicPermission {
            receiverId?.let { receiverId ->
                val senderUsername = getUsernameFromEmail(auth.currentUser?.email ?: "")
                val receiverUsername = receiverId
                mainRepository.sendConnectionRequest(receiverUsername, false) { success ->
                    if (success) {
                        startActivity(Intent(context, WebRTCMainActivity::class.java).apply {
                            putExtra("target", receiverUsername)
                            putExtra("isVideoCall", false)
                            putExtra("isCaller", true)
                            putExtra("username", senderUsername)  // Burada username'i geçiriyoruz
                        })
                    } else {
                        Toast.makeText(context, "Voice call failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun startVideoCall() {
        getCameraAndMicPermission {
            receiverId?.let { receiverId ->
                val senderUsername = getUsernameFromEmail(auth.currentUser?.email ?: "")
                val receiverUsername = receiverId
                mainRepository.sendConnectionRequest(receiverUsername, true) { success ->
                    if (success) {
                        startActivity(Intent(context, CallActivity::class.java).apply {
                            putExtra("target", receiverUsername)
                            putExtra("isVideoCall", true)
                            putExtra("isCaller", true)
                            putExtra("username", senderUsername)  // Burada username'i geçiriyoruz
                        })
                    } else {
                        Toast.makeText(context, "Video call failed", Toast.LENGTH_SHORT).show()
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
            onPermissionGranted()
        } else {
            Companion.onPermissionGranted = onPermissionGranted
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
                Companion.onPermissionGranted?.invoke()
            } else {
                Toast.makeText(requireContext(), "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
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

    override fun onCallReceived(model: DataModel) {
        CoroutineScope(Dispatchers.Main).launch {
            _binding.apply {
                val isVideoCall = model.type == DataModelType.StartVideoCall
                val isVideoCallText = if (isVideoCall) "Video" else "Audio"
                incomingCallTitleTv.text = "${model.sender} is $isVideoCallText Calling you"
                incomingCallLayout.isVisible = true
                acceptButton.setOnClickListener {
                    getCameraAndMicPermission {
                        incomingCallLayout.isVisible = false
                        // Create an intent to go to video call activity
                        startActivity(Intent(requireContext(), CallActivity::class.java).apply {
                            putExtra("target", model.sender)
                            putExtra("isVideoCall", isVideoCall)
                            putExtra("isCaller", false)
                            putExtra("username", "username")  // username'i geçiriyoruz
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
}
