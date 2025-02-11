package com.example.mapsapp.view.ui

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Guideline
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mapsapp.R
import com.example.mapsapp.adapter.FriendsAdapter
import com.example.mapsapp.adapter.FriendRequestsAdapter
import com.example.mapsapp.databinding.FragmentChatInterfaceBinding
import com.example.mapsapp.databinding.DialogFriendRequestsBinding
import com.example.mapsapp.model.User
import com.example.mapsapp.util.BaseFragment
import com.example.mapsapp.viewmodel.ChatInterfaceViewModel
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChatInterfaceFragment : BaseFragment() {

    private var _binding: FragmentChatInterfaceBinding? = null
    private val binding get() = _binding!!
    private val chatInterfaceViewModel: ChatInterfaceViewModel by viewModels()
    private lateinit var friendsAdapter: FriendsAdapter
    private lateinit var friendRequestsAdapter: FriendRequestsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatInterfaceBinding.inflate(inflater, container, false)

        setupRecyclerView()
        setupFabButton()
        observeData()

        // Verileri yükle
        chatInterfaceViewModel.loadFriends()
        chatInterfaceViewModel.loadFriendRequests(
            FirebaseAuth.getInstance().currentUser?.uid ?: ""
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyBottomInsetToView(binding.requestButton)
        adjustFabMargin()
    }

    // RecyclerView'i ayarla
    private fun setupRecyclerView() {
        friendsAdapter = FriendsAdapter(mutableListOf()) { user ->
            val bundle = Bundle().apply {
                putString("receiverId", user.uid)
            }
            findNavController().navigate(R.id.chatFragment, bundle)
        }
        binding.recyclerView.apply {
            adapter = friendsAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    // FAB butonunu ayarla
    private fun setupFabButton() {
        binding.fabAddFriend.setOnClickListener {
            findNavController().navigate(R.id.action_chatInterfaceFragment_to_addFriendsFragment)
        }
    }

    // Tüm gözlemleri bir yerde toplamak
    private fun observeData() {

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    chatInterfaceViewModel.friends.collectLatest { friends ->
                        friendsAdapter.updateFriends(friends)
                    }
                }

                launch {
                    chatInterfaceViewModel.friendRequests.collectLatest { requests ->
                        setupFriendRequestButton(requests)
                    }
                }
            }
        }

    }



    // Arkadaşlık istekleri butonunu ayarla
    private fun setupFriendRequestButton(requests: List<User>) {
        binding.requestButton.setOnClickListener {
            showFriendRequestsDialog(requests)
        }
    }

    // Arkadaşlık isteklerini gösteren dialog
    private fun showFriendRequestsDialog(requests: List<User>) {
        val dialog = Dialog(requireContext(), R.style.CustomDialogTheme)
        val bindingSheet = DialogFriendRequestsBinding.inflate(layoutInflater)
        dialog.setContentView(bindingSheet.root)

        friendRequestsAdapter = FriendRequestsAdapter(requests) { user ->
            chatInterfaceViewModel.acceptFriendRequest(
                currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                friendUid = user.uid
            )
        }

        bindingSheet.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = friendRequestsAdapter
        }

        dialog.show()
    }

    private fun adjustFabMargin() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.fabAddFriend) { _, insets ->
            val systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val params = binding.fabAddFriend.layoutParams as ViewGroup.MarginLayoutParams
            params.bottomMargin = systemBarInsets.bottom + 16 // Navigation bar yüksekliğini ekle
            params.marginEnd = 16 // Sağ marj
            binding.fabAddFriend.layoutParams = params
            insets
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

