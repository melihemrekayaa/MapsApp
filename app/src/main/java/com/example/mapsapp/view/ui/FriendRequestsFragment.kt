package com.example.mapsapp.view.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mapsapp.adapter.FriendRequestsAdapter
import com.example.mapsapp.databinding.FragmentFriendRequestsBinding
import com.example.mapsapp.viewmodel.FriendRequestsViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class FriendRequestsFragment : Fragment() {

    private var _binding: FragmentFriendRequestsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FriendRequestsViewModel by viewModels()
    private lateinit var adapter: FriendRequestsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFriendRequestsBinding.inflate(inflater, container, false)
        setupRecyclerView()
        observeFriendRequests()
        viewModel.loadRequests()
        return binding.root
    }

    private fun setupRecyclerView() {
        adapter = FriendRequestsAdapter(
            onAcceptClick = { user ->
                viewModel.acceptRequest(user)
                Snackbar.make(binding.root, "Arkadaşlık isteği kabul edildi", Snackbar.LENGTH_SHORT).show()
                requireActivity().onBackPressedDispatcher.onBackPressed() // 🔙 geri dön
            },
            onRejectClick = { user ->
                viewModel.rejectRequest(user)
                Snackbar.make(binding.root, "Arkadaşlık isteği reddedildi", Snackbar.LENGTH_SHORT).show()
                requireActivity().onBackPressedDispatcher.onBackPressed() // 🔙 geri dön
            }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }


    private fun observeFriendRequests() {
        lifecycleScope.launchWhenStarted {
            viewModel.requests.collectLatest { list ->
                adapter.submitList(list)
                binding.emptyView.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
