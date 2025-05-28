package com.example.mapsapp.view.ui

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mapsapp.R
import com.example.mapsapp.adapter.AddFriendsAdapter
import com.example.mapsapp.adapter.FriendsAdapter
import com.example.mapsapp.adapter.FriendRequestsAdapter
import com.example.mapsapp.databinding.DialogAddFriendBinding
import com.example.mapsapp.databinding.FragmentChatInterfaceBinding
import com.example.mapsapp.model.User
import com.example.mapsapp.util.BaseFragment
import com.example.mapsapp.viewmodel.AddFriendsViewModel
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
    private val addFriendsViewModel: AddFriendsViewModel by viewModels()

    private lateinit var friendsAdapter: FriendsAdapter
    private lateinit var friendRequestsAdapter: FriendRequestsAdapter
    private lateinit var friendRequestDialog: Dialog
    private lateinit var addFriendDialog: Dialog
    private lateinit var addFriendsAdapter: AddFriendsAdapter

    private var currentFriendList: List<User> = emptyList() // Triple yerine sadece User

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatInterfaceBinding.inflate(inflater, container, false)
        friendRequestDialog = Dialog(requireContext(), R.style.CustomDialogTheme)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearchView()
        observeViewModel()
        setupFriendButtons()

        chatInterfaceViewModel.resetInCallState()

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        chatInterfaceViewModel.loadFriendRequests(currentUserId)
    }


    private fun setupRecyclerView() {
        friendsAdapter = FriendsAdapter(
            onFriendClick = { friend -> navigateToChat(friend) },
            onRemoveClick = { friend -> showRemoveFriendDialog(friend) }
        )
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = friendsAdapter
        }

        setupSwipeToDelete()
    }


    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = true
            override fun onQueryTextChange(newText: String?): Boolean {
                filterAndSubmitList(newText.orEmpty().trim())
                return true
            }
        })
    }

    private fun setupFriendButtons() {
        binding.btnRequests.setOnClickListener {
            findNavController().navigate(R.id.friendRequestsFragment)
        }

        binding.btnAddFriend.setOnClickListener {
            showAddFriendDialog()
        }
    }

    private fun showAddFriendDialog() {
        val dialogBinding = DialogAddFriendBinding.inflate(layoutInflater)
        addFriendDialog = Dialog(requireContext(), R.style.CustomDialogTheme)
        addFriendDialog.setContentView(dialogBinding.root)
        addFriendDialog.setCancelable(true)

        addFriendsAdapter = AddFriendsAdapter(
            users = mutableListOf(),
            onAddFriendClick = { user ->
                addFriendsViewModel.sendFriendRequest(user.uid)
                addFriendDialog.dismiss()
            },
            onCancelRequestClick = { user ->
                addFriendsViewModel.cancelFriendRequest(user.uid)
                addFriendDialog.dismiss()
            }
        )

        dialogBinding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = addFriendsAdapter
        }

        dialogBinding.swipeRefreshLayout.setOnRefreshListener {
            addFriendsViewModel.loadUsers()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            addFriendsViewModel.users.collectLatest { users ->
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                val friendIds = currentFriendList.map { it.uid }

                val filtered = users.filter { user ->
                    user.uid != currentUserId && user.uid !in friendIds
                }

                addFriendsAdapter.updateUsers(filtered)
                dialogBinding.swipeRefreshLayout.isRefreshing = false
            }
        }

        addFriendsViewModel.loadUsers()
        addFriendDialog.show()
    }

    private fun filterAndSubmitList(query: String) {
        val filtered = if (query.isBlank()) currentFriendList
        else currentFriendList.filter { user ->
            user.name.contains(query, ignoreCase = true)
        }
        friendsAdapter.submitList(filtered)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    chatInterfaceViewModel.friendsWithFullStatus.collectLatest { list ->
                        // ViewModel artık Triple yerine User döndürüyor
                        currentFriendList = list
                        filterAndSubmitList(binding.searchView.query?.toString()?.trim().orEmpty())
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

    private fun setupSwipeToDelete() {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false // Taşıma yok
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val userToDelete = friendsAdapter.getItemAt(position) // bu metodu FriendsAdapter'da tanımlayacağız

                showDeleteConfirmation(userToDelete.uid, userToDelete.name, position)
            }
        })

        itemTouchHelper.attachToRecyclerView(binding.recyclerView)
    }

    private fun showDeleteConfirmation(friendUid: String, friendName: String, position: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle("Arkadaş Sil")
            .setMessage("$friendName adlı kişiyi silmek istediğinize emin misiniz?")
            .setPositiveButton("Evet") { _, _ ->
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                if (currentUserId != null) {
                    chatInterfaceViewModel.removeFriend(currentUserId, friendUid)
                }
            }
            .setNegativeButton("Hayır") { dialog, _ ->
                dialog.dismiss()
                friendsAdapter.notifyItemChanged(position) // Swipe animasyonunu geri al
            }
            .setCancelable(false)
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
