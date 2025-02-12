package com.example.mapsapp.view.ui

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Guideline
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupFabButton()
        setupFriendRequestButton()
        observeData()

        // Load friend requests and friends list
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId != null) {
            chatInterfaceViewModel.loadFriendRequests(currentUserId)
            chatInterfaceViewModel.fetchFriendsList(currentUserId)
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())

        friendsAdapter = FriendsAdapter { friend ->
            navigateToChat(friend)
        }
        binding.recyclerView.adapter = friendsAdapter
    }

    private fun setupFabButton() {
        binding.fabAddFriend.setOnClickListener {
            findNavController().navigate(R.id.action_chatInterfaceFragment_to_addFriendsFragment)
        }
    }

    private fun setupFriendRequestButton() {
        binding.requestButton.setOnClickListener {
            showFriendRequestsDialog()
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    chatInterfaceViewModel.friendsList.collect { friends ->
                        Log.d("ChatInterfaceFragment", "Friends list received: ${friends.size} friends")

                        if (friends.isEmpty()) {
                            Log.w("ChatInterfaceFragment", "Friends list is empty. RecyclerView may not update.")
                        } else {
                            Log.d("ChatInterfaceFragment", "Updating RecyclerView with friends: $friends")
                        }

                        friendsAdapter.submitList(friends)
                        friendsAdapter.notifyDataSetChanged() // Force RecyclerView update
                    }
                }
            }
        }
    }


    private fun navigateToChat(friend: User) {
        val action = ChatInterfaceFragmentDirections.actionChatInterfaceFragmentToChatFragment(friend.uid)
        findNavController().navigate(action)
    }

    private fun showFriendRequestsDialog() {
        val dialog = Dialog(requireContext(), R.style.CustomDialogTheme)
        val bindingSheet = DialogFriendRequestsBinding.inflate(layoutInflater)
        dialog.setContentView(bindingSheet.root)

        friendRequestsAdapter = FriendRequestsAdapter(chatInterfaceViewModel.friendRequests.value) { user ->
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            if (currentUserId != null) {
                chatInterfaceViewModel.acceptFriendRequest(currentUserId, user.uid)
            }
        }

        bindingSheet.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = friendRequestsAdapter
        }

        dialog.show()
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}