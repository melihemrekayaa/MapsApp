package com.example.mapsapp.view.ui

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mapsapp.R
import com.example.mapsapp.adapter.ChatAdapter
import com.example.mapsapp.databinding.FragmentChatBinding
import com.example.mapsapp.viewmodel.ChatViewModel
import com.example.mapsapp.webrtc.ui.CallActivity
import com.example.mapsapp.webrtc.webrtc.MyPeerObserver
import com.example.mapsapp.webrtc.webrtc.WebRTCClient
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import org.webrtc.IceCandidate
import javax.inject.Inject

@AndroidEntryPoint
class ChatFragment : Fragment() {
    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private val chatViewModel: ChatViewModel by viewModels()
    private lateinit var adapter: ChatAdapter
    private var receiverId: String? = null

    @Inject
    lateinit var webRTCClient: WebRTCClient

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()

        arguments?.let {
            receiverId = it.getString("receiverId")
        }

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
        _binding = null
    }

    private fun startVoiceCall() {
        receiverId?.let { receiverId ->
            val senderUsername = getUsernameFromEmail(auth.currentUser?.email ?: "")
            val receiverUsername = getUsernameFromEmail(receiverId)
            webRTCClient.initializeWebrtcClient(senderUsername, object : MyPeerObserver() {
                override fun onIceCandidate(candidate: IceCandidate?) {
                    super.onIceCandidate(candidate)
                    candidate?.let { webRTCClient.sendIceCandidate(receiverUsername, it) }
                }
            })
            webRTCClient.call(receiverUsername)
            startActivity(Intent(context, CallActivity::class.java).apply {
                putExtra("target", receiverUsername)
                putExtra("isVideoCall", false)
                putExtra("isCaller", true)
            })
        }
    }

    private fun startVideoCall() {
        receiverId?.let { receiverId ->
            val senderUsername = getUsernameFromEmail(auth.currentUser?.email ?: "")
            val receiverUsername = getUsernameFromEmail(receiverId)
            webRTCClient.initializeWebrtcClient(senderUsername, object : MyPeerObserver() {
                override fun onIceCandidate(candidate: IceCandidate?) {
                    super.onIceCandidate(candidate)
                    candidate?.let { webRTCClient.sendIceCandidate(receiverUsername, it) }
                }
            })
            webRTCClient.call(receiverUsername)
            startActivity(Intent(context, CallActivity::class.java).apply {
                putExtra("target", receiverUsername)
                putExtra("isVideoCall", true)
                putExtra("isCaller", true)
            })
        }
    }

    private fun getUsernameFromEmail(email: String): String {
        return email.substringBefore("@")
    }
}
