package com.example.mapsapp.view.map

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import com.example.mapsapp.adapter.FriendSheetAdapter
import com.example.mapsapp.databinding.BottomSheetFriendsBinding
import com.example.mapsapp.model.FriendLocation
import com.example.mapsapp.viewmodel.FriendsViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import com.google.firebase.auth.FirebaseAuth
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetFriendsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d("FriendsBottomSheet", "onViewCreated — setting up adapter & observing")

        binding.friendsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = FriendSheetAdapter { friendLoc ->
            Log.d("FriendsBottomSheet", "Clicked friend → $friendLoc")
            onLatestLocationClick(friendLoc)
            dismiss()
        }
        binding.friendsRecyclerView.adapter = adapter

        // Başlatıyoruz
        viewModel.observeFriendsList(currentUserId)

        // StateFlow’u gözlemle
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.friendsList.collectLatest { users ->
                    Log.d("FriendsBottomSheet", "collectLatest: received ${users.size} users")
                    adapter.submitList(users)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
