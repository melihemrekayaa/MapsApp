package com.example.mapsapp.view.ui

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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

    private lateinit var dialog : Dialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatInterfaceBinding.inflate(inflater, container, false)
        dialog = Dialog(requireContext(), R.style.CustomDialogTheme)
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

        friendsAdapter = FriendsAdapter(
            onFriendClick = { friend -> navigateToChat(friend)},
            onRemoveClick = { friend -> showRemoveFriendDialog(friend) }

        )
        binding.recyclerView.adapter = friendsAdapter
    }

    private fun navigateToChat(friend: User) {
        val bundle = Bundle()
        bundle.putString("receiverId", friend.uid)
        bundle.putString("receiverName", friend.name)
        findNavController().navigate(R.id.action_chatInterfaceFragment_to_chatFragment,bundle)
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
                    chatInterfaceViewModel.friendsList.collectLatest { friends ->
                        Log.d("ChatInterfaceFragment", "Updated Friends List: ${friends.map { it.name }}")
                        friendsAdapter.submitList(friends)
                    }
                }

                launch {
                    chatInterfaceViewModel.friendRequests.collectLatest { requests ->
                        if (::friendRequestsAdapter.isInitialized) {
                            friendRequestsAdapter.updateRequests(requests)

                            if (requests.isEmpty()) {
                                dialog.dismiss() // Liste boşsa dialogu kapat
                            }
                        }
                        Log.d("ChatInterfaceFragment", "Updated Friend Requests: ${requests.map { it.name }}")
                    }
                }

                launch {
                    chatInterfaceViewModel.operationStatus.collectLatest { status ->
                        status?.let {
                            showToast(it)
                            chatInterfaceViewModel.clearOperationStatus()
                        }
                    }
                }
            }
        }
    }

    private fun showFriendRequestsDialog() {
        val bindingSheet = DialogFriendRequestsBinding.inflate(layoutInflater)
        dialog.setContentView(bindingSheet.root)

        friendRequestsAdapter = FriendRequestsAdapter(chatInterfaceViewModel.friendRequests.value) { user ->
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            if (currentUserId != null) {
                chatInterfaceViewModel.acceptFriendRequest(currentUserId, user.uid)
                chatInterfaceViewModel.removeFriendRequest(currentUserId, user.uid) // UI'den de kaldır
                chatInterfaceViewModel.removeFriendRequest(user.uid, currentUserId) // Karşı taraftan da kaldır
                dialog.dismiss()
            }
        }

        bindingSheet.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = friendRequestsAdapter
        }

        dialog.show()
    }

    private fun showRemoveFriendDialog(friend: User) {
        AlertDialog.Builder(requireContext())
            .setTitle("Remove Friend")
            .setMessage("Are you sure you want to remove ${friend.name}?")
            .setPositiveButton("Yes") { dialog, _ ->
                chatInterfaceViewModel.removeFriend(FirebaseAuth.getInstance().currentUser?.uid ?: "", friend.uid)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}

