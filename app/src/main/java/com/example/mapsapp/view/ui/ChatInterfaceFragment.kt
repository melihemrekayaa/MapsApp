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
import com.google.firebase.database.FirebaseDatabase
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
    private lateinit var dialog: Dialog

    private var currentFriendList: List<Triple<User, Boolean, Boolean>> = emptyList()

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
        setupSearchView()
        observeViewModel()

        chatInterfaceViewModel.resetInCallState()

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        chatInterfaceViewModel.loadFriendRequests(currentUserId)
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        friendsAdapter = FriendsAdapter(
            onFriendClick = { friend -> navigateToChat(friend) },
            onRemoveClick = { friend -> showRemoveFriendDialog(friend) }
        )
        binding.recyclerView.adapter = friendsAdapter
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = true
            override fun onQueryTextChange(newText: String?): Boolean {
                filterAndSubmitList(newText.orEmpty().trim())
                return true
            }
        })
    }

    private fun filterAndSubmitList(query: String) {
        val filtered = if (query.isBlank()) currentFriendList
        else currentFriendList.filter { (user, _, _) ->
            user.name.contains(query, ignoreCase = true)
        }
        friendsAdapter.submitList(filtered)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    chatInterfaceViewModel.friendsWithFullStatus.collectLatest { list ->
                        currentFriendList = list
                        filterAndSubmitList(binding.searchView.query?.toString()?.trim().orEmpty())
                    }
                }
                launch {
                    chatInterfaceViewModel.friendRequests.collectLatest { requests ->
                        if (::friendRequestsAdapter.isInitialized) {
                            friendRequestsAdapter.updateRequests(requests)
                            if (requests.isEmpty()) dialog.dismiss()
                        }
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

    private fun navigateToChat(friend: User) {
        findNavController().navigate(
            R.id.action_chatInterfaceFragment_to_chatFragment,
            Bundle().apply {
                putString("receiverId", friend.uid)
                putString("receiverName", friend.name)
            }
        )
    }

    private fun showRemoveFriendDialog(friend: User) {
        AlertDialog.Builder(requireContext())
            .setTitle("Remove Friend")
            .setMessage("Are you sure you want to remove ${friend.name}?")
            .setPositiveButton("Yes") { dialog, _ ->
                FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
                    chatInterfaceViewModel.removeFriend(uid, friend.uid)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
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

