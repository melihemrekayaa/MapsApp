package com.example.mapsapp.view.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mapsapp.R
import com.example.mapsapp.adapter.UsersAdapter
import com.example.mapsapp.databinding.FragmentChatInterfaceBinding
import com.example.mapsapp.model.User
import com.example.mapsapp.viewmodel.ChatInterfaceViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChatInterfaceFragment : Fragment() {

    private var _binding: FragmentChatInterfaceBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private val chatInterfaceViewModel: ChatInterfaceViewModel by viewModels()
    private lateinit var adapter: UsersAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentChatInterfaceBinding.inflate(inflater, container, false)
        val view = binding.root
        auth = FirebaseAuth.getInstance()

        adapter = UsersAdapter(emptyList()) { user ->
            val action = ChatInterfaceFragmentDirections.actionChatInterfaceFragmentToChatFragment(user.uid)
            findNavController().navigate(action)
        }

        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())

        chatInterfaceViewModel.users.observe(viewLifecycleOwner, Observer { users ->
            adapter.updateUsers(users)
            if (users.isEmpty()) {
                Toast.makeText(requireContext(), "Error loading users", Toast.LENGTH_SHORT).show()
            }
        })

        chatInterfaceViewModel.loadUsers()

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
