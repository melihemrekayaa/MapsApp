package com.example.mapsapp.view.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mapsapp.R
import com.example.mapsapp.adapter.AddFriendsAdapter
import com.example.mapsapp.databinding.FragmentAddFriendsBinding
import com.example.mapsapp.viewmodel.AddFriendsViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

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

        adapter = AddFriendsAdapter(emptyList(), { user ->
            viewModel.sendFriendRequest(user.uid)
            Snackbar.make(binding.root, "Friend request sent", Snackbar.LENGTH_LONG)
                .setAction("Undo") {
                    viewModel.cancelFriendRequest(user.uid)
                }.show()
        }, { user ->
            viewModel.cancelFriendRequest(user.uid)
        })

        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())

        viewModel.users.observe(viewLifecycleOwner) { users ->
            adapter.updateUsers(users)
        }

        viewModel.loadUsers()

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
