package com.example.mapsapp.view.ui

import FriendsAdapter
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mapsapp.R
import com.example.mapsapp.databinding.FragmentChatInterfaceBinding
import com.example.mapsapp.viewmodel.ChatInterfaceViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChatInterfaceFragment : Fragment() {

    private var _binding: FragmentChatInterfaceBinding? = null
    private val binding get() = _binding!!
    private val chatInterfaceViewModel: ChatInterfaceViewModel by viewModels()
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: FriendsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatInterfaceBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()

        adapter = FriendsAdapter(mutableListOf()) { user ->
            val bundle = Bundle().apply {
                putString("receiverId", user.uid)
            }
            findNavController().navigate(R.id.chatFragment, bundle)
        }

        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())

        binding.fabAddFriend.setOnClickListener {
            findNavController().navigate(R.id.action_chatInterfaceFragment_to_addFriendsFragment)
        }

        val bottomNavView = requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavBar)

        // BottomNavigationView yüksekliğini al ve FAB'nin alt marjını ayarla
        bottomNavView?.let {
            val fabParams = binding.fabAddFriend.layoutParams as ConstraintLayout.LayoutParams
            fabParams.bottomMargin = it.height + 16 // 16dp ek boşluk
            binding.fabAddFriend.layoutParams = fabParams
        }

        chatInterfaceViewModel.friends.observe(viewLifecycleOwner, Observer { friends ->
            adapter.updateFriends(friends) // Güncelleme işlemi
        })

        chatInterfaceViewModel.loadFriends() // Arkadaşları yükle

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

