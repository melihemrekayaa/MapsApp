package com.example.mapsapp.view.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mapsapp.R
import com.example.mapsapp.adapter.FriendsAdapter
import com.example.mapsapp.adapter.FriendRequestsAdapter
import com.example.mapsapp.databinding.FragmentChatInterfaceBinding
import com.example.mapsapp.databinding.DialogFriendRequestsBinding
import com.example.mapsapp.model.User
import com.example.mapsapp.viewmodel.ChatInterfaceViewModel
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class ChatInterfaceFragment : Fragment() {

    private var _binding: FragmentChatInterfaceBinding? = null
    private val binding get() = _binding!!
    private val chatInterfaceViewModel: ChatInterfaceViewModel by viewModels()
    private lateinit var adapter: FriendsAdapter
    private lateinit var requestsAdapter: FriendRequestsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatInterfaceBinding.inflate(inflater, container, false)

        setupRecyclerView()
        setupFabButton()
        observeFriends()
        observeFriendRequests()

        chatInterfaceViewModel.loadFriends()
        chatInterfaceViewModel.loadFriendRequests()

        return binding.root
    }

    // RecyclerView'i ayarla
    private fun setupRecyclerView() {
        adapter = FriendsAdapter(mutableListOf()) { user ->
            val bundle = Bundle().apply {
                putString("receiverId", user.uid)
            }
            findNavController().navigate(R.id.chatFragment, bundle)
        }
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    // FAB butonunu ayarla
    private fun setupFabButton() {
        binding.fabAddFriend.setOnClickListener {
            findNavController().navigate(R.id.action_chatInterfaceFragment_to_addFriendsFragment)
        }
        adjustFabMargin()
    }

    // BottomNavigationView'e göre FAB marjını ayarla
    private fun adjustFabMargin() {
        val bottomNavView = requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavBar)
        bottomNavView?.let {
            val fabParams = binding.fabAddFriend.layoutParams as ConstraintLayout.LayoutParams
            fabParams.bottomMargin = it.height + 16
            binding.fabAddFriend.layoutParams = fabParams
        }
    }

    // Arkadaşları gözlemle
    private fun observeFriends() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            chatInterfaceViewModel.friends.collectLatest { friends ->
                adapter.updateFriends(friends)
            }
        }
    }

    // Arkadaşlık isteklerini gözlemle ve badge ekle
    private fun observeFriendRequests() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            chatInterfaceViewModel.friendRequests.collectLatest { requests ->
                setupBadge(requests.size)
                binding.showRequestsButton.setOnClickListener {
                    showFriendRequestsDialog(requests)
                }
            }
        }
    }

    // BottomNavigationView'de "Chat" sekmesine badge ekle
    private fun setupBadge(requestCount: Int) {
        val bottomNavView = requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavBar)
        val badge = bottomNavView.getOrCreateBadge(R.id.chatFragment)
        badge.isVisible = requestCount > 0
        badge.number = requestCount
        badge.badgeGravity = BadgeDrawable.TOP_END
    }

    // Arkadaşlık isteklerini BottomSheet ile göster
    private fun showFriendRequestsDialog(requests: List<User>) {
        val dialog = BottomSheetDialog(requireContext())
        val bindingSheet = DialogFriendRequestsBinding.inflate(layoutInflater)

        requestsAdapter = FriendRequestsAdapter(requests) { user ->
            chatInterfaceViewModel.acceptFriendRequest(user.uid)
            chatInterfaceViewModel.loadFriendRequests()
            chatInterfaceViewModel.loadFriends()
        }

        bindingSheet.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        bindingSheet.recyclerView.adapter = requestsAdapter
        dialog.setContentView(bindingSheet.root)
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
