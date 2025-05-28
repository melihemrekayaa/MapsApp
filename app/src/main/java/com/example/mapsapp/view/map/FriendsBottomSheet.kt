package com.example.mapsapp.view.map

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mapsapp.adapter.FriendSheetAdapter
import com.example.mapsapp.databinding.BottomSheetFriendsBinding
import com.example.mapsapp.model.FriendLocation
import com.example.mapsapp.model.User
import com.example.mapsapp.viewmodel.FriendsViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FriendsBottomSheet(
    private val onLatestLocationClick: (FriendLocation) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetFriendsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FriendsViewModel by viewModels({ requireParentFragment() })
    private lateinit var adapter: FriendSheetAdapter

    private val currentUserId: String
        get() = FirebaseAuth.getInstance().currentUser?.uid
            ?: error("User not signed in")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetFriendsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.friendsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = FriendSheetAdapter(
            onMapClick = { friendLoc ->
                onLatestLocationClick(friendLoc)
                dismiss()
            },
            onChatClick = { user ->
                val navController = requireParentFragment().findNavController()
                val action = MapFragmentDirections.actionMapFragmentToChatFragment(user.uid, user.name)
                navController.navigate(action)
                dismiss()
            }
        )
        binding.friendsRecyclerView.adapter = adapter

        viewModel.observeFriendsWithStatus()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.friendsWithStatus.collectLatest { list ->
                    adapter.submitList(list)
                }
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
