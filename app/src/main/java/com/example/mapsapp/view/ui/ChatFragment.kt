package com.example.mapsapp.view.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mapsapp.R
import com.example.mapsapp.adapter.ChatAdapter
import com.example.mapsapp.databinding.FragmentChatBinding


import com.example.mapsapp.model.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject

class ChatFragment : Fragment() {
    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var adapter: ChatAdapter
    private val messages = mutableListOf<Message>()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentChatBinding.inflate(inflater,container,false)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        adapter = ChatAdapter(messages,auth.currentUser?.uid ?: "")
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(context)

        binding.sendButton.setOnClickListener {
            sendMessage()
        }

        listenForMessages()
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);


        return binding.root
    }

    fun sendMessage(){
        val messageText = binding.messageEditText.text.toString()
        if (messageText.isNotEmpty()){
            val message = Message(senderId = auth.currentUser?.uid ?: "", message = messageText)
            firestore.collection("messages").add(message)
            binding.messageEditText.text?.clear()
        }
    }

    fun listenForMessages(){
        firestore.collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener{ value,error ->
                if (error != null){
                    Toast.makeText(requireContext(), "Error while loading messages.", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                messages.clear()
                for(doc in value!!){
                    val message = doc.toObject(Message::class.java)
                    messages.add(message)
                }

                adapter.notifyDataSetChanged()
                binding.recyclerView.scrollToPosition(messages.size)

            }
    }

}