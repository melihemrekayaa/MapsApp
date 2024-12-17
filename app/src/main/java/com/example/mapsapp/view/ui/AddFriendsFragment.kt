package com.example.mapsapp.view.ui

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mapsapp.adapter.AddFriendsAdapter
import com.example.mapsapp.databinding.FragmentAddFriendsBinding
import com.example.mapsapp.viewmodel.AddFriendsViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
@AndroidEntryPoint
class AddFriendsFragment : Fragment() {

    private var _binding: FragmentAddFriendsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AddFriendsViewModel by viewModels()
    private lateinit var adapter: AddFriendsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddFriendsBinding.inflate(inflater, container, false)
        Log.d("AddFriendsFragment", "Fragment View Created")

        setupRecyclerView()
        setupSwipeRefreshLayout()
        observeViewModel()

        // Kullanıcıları yükle
        viewModel.loadUsers()
        Log.d("AddFriendsFragment", "loadUsers() called")

        return binding.root
    }

    private fun setupRecyclerView() {
        adapter = AddFriendsAdapter(mutableListOf(), { user ->
            Log.d("AddFriendsFragment", "Add Friend clicked for user: ${user.name}")
            viewModel.sendFriendRequest(user.uid)
            Snackbar.make(binding.root, "Friend request sent", Snackbar.LENGTH_LONG)
                .setAction("Undo") {
                    Log.d("AddFriendsFragment", "Undo clicked for user: ${user.name}")
                    viewModel.cancelFriendRequest(user.uid)
                    adapter.updateFriendStatus(user, false)
                }.show()
        }, { user ->
            Log.d("AddFriendsFragment", "Cancel Request clicked for user: ${user.name}")
            viewModel.cancelFriendRequest(user.uid)
            adapter.updateFriendStatus(user, false)
        })

        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        Log.d("AddFriendsFragment", "RecyclerView setup complete")
    }

    private fun setupSwipeRefreshLayout() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            Log.d("AddFriendsFragment", "SwipeRefresh triggered")
            viewModel.loadUsers()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.users.collectLatest { users ->
                Log.d("AddFriendsFragment", "Users received from ViewModel: $users")

                val currentUserId = viewModel.repository.getCurrentUser()?.uid
                val filteredUsers = users.filter { it.uid != currentUserId }
                Log.d("AddFriendsFragment", "Filtered users: $filteredUsers")

                if (filteredUsers.isNotEmpty()) {
                    adapter.updateUsers(filteredUsers)
                    binding.emptyView.visibility = View.GONE
                    Log.d("AddFriendsFragment", "RecyclerView updated with users")
                } else {
                    binding.emptyView.visibility = View.VISIBLE
                    Log.d("AddFriendsFragment", "No users available, emptyView shown")
                }
                binding.swipeRefreshLayout.isRefreshing = false
                Log.d("AddFriendsFragment", "SwipeRefresh stopped")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("AddFriendsFragment", "Fragment View Destroyed")
        _binding = null
    }
}

