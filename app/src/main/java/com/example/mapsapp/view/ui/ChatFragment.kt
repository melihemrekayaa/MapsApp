package com.example.mapsapp.view.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mapsapp.R
import com.example.mapsapp.adapter.ChatAdapter
import com.example.mapsapp.databinding.FragmentChatBinding
import com.example.mapsapp.model.Message
import com.example.mapsapp.viewmodel.ChatViewModel
import dagger.hilt.android.AndroidEntryPoint


class ChatFragment : Fragment() {
    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var adapter: ChatAdapter
    private val messages = mutableListOf<Message>()
    private var receiverId: String? = "receiverId"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentChatBinding.inflate(inflater, container, false)

        arguments?.let {
            receiverId = it.getString("receiverId")
        }

        setupToolbar()

        val currentUserId = viewModel.getCurrentUserId() ?: ""
        adapter = ChatAdapter(messages, currentUserId)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(context)

        binding.sendButton.setOnClickListener {
            receiverId?.let { id ->
                val messageText = binding.messageEditText.text.toString()
                viewModel.sendMessage(id, messageText)
                binding.messageEditText.text?.clear()
                binding.recyclerView.scrollToPosition(messages.size - 1)
            }
        }

        receiverId?.let { viewModel.listenForMessages(it) }
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        viewModel.messages.observe(viewLifecycleOwner, { newMessages ->
            updateRecyclerView(newMessages)
        })

        return binding.root
    }

    private fun updateRecyclerView(newMessages: List<Message>) {
        val diffResult = androidx.recyclerview.widget.DiffUtil.calculateDiff(object : androidx.recyclerview.widget.DiffUtil.Callback() {
            override fun getOldListSize(): Int {
                return messages.size
            }

            override fun getNewListSize(): Int {
                return newMessages.size
            }

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return messages[oldItemPosition].timestamp == newMessages[newItemPosition].timestamp
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return messages[oldItemPosition] == newMessages[newItemPosition]
            }
        })
        messages.clear()
        messages.addAll(newMessages)
        diffResult.dispatchUpdatesTo(adapter)
        binding.recyclerView.scrollToPosition(messages.size - 1)
    }

    private fun setupToolbar() {
        val toolbar = binding.chatToolbar
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        receiverId?.let { id ->
            viewModel.setupToolbarTitle(id, { userName ->
                toolbar.title = userName
            }, {
                Toast.makeText(requireContext(), "Failed to load user info", Toast.LENGTH_SHORT).show()
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
