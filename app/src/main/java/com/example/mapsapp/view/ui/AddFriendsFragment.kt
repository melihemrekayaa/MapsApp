package com.example.mapsapp.view.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
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
        setupRecyclerView()
        observeViewModel()
        setupSwipeRefresh()
        viewModel.loadUsers()
        return binding.root
    }

    private fun setupRecyclerView() {
        adapter = AddFriendsAdapter(
            users = mutableListOf(),
            onAddFriendClick = { user ->
                viewModel.sendFriendRequest(user.uid)
                adapter.updateFriendStatus(user, true)
            },
            onCancelRequestClick = { user ->
                viewModel.cancelFriendRequest(user.uid)
                adapter.updateFriendStatus(user, false)
            }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadUsers()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.users.collectLatest { userList ->
                adapter.updateUsers(userList)
                binding.emptyView.visibility = if (userList.isEmpty()) View.VISIBLE else View.GONE
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.operationStatus.collectLatest { message ->
                message?.let {
                    Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                    viewModel.clearStatus()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
