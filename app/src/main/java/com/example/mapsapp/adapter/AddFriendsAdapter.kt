package com.example.mapsapp.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mapsapp.databinding.ItemAddFriendBinding
import com.example.mapsapp.model.User

class AddFriendsAdapter(
    private var users: MutableList<User>,
    private val onAddFriendClick: (User) -> Unit,
    private val onCancelRequestClick: (User) -> Unit
) : RecyclerView.Adapter<AddFriendsAdapter.AddFriendViewHolder>() {

    // ViewHolder tanımı
    inner class AddFriendViewHolder(private val binding: ItemAddFriendBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            binding.userName.text = user.name

            // Arkadaşlık isteği durumu kontrolü
            val currentUserId = user.uid
            val isRequestSent = user.friendRequests.contains(currentUserId)

            // Eğer arkadaşlık isteği gönderildiyse, butonları değiştir
            if (isRequestSent) {
                binding.addFriendButton.visibility = View.GONE
                binding.cancelButton.visibility = View.VISIBLE
            } else {
                binding.addFriendButton.visibility = View.VISIBLE
                binding.cancelButton.visibility = View.GONE
            }

            // Add Friend Butonu
            binding.addFriendButton.setOnClickListener {
                onAddFriendClick(user)
                updateUserRequestStatus(user, true)
            }

            // Cancel Request Butonu
            binding.cancelButton.setOnClickListener {
                onCancelRequestClick(user)
                updateUserRequestStatus(user, false)
            }
        }

        // Arkadaşlık isteği durumunu güncelle
        private fun updateUserRequestStatus(user: User, isRequestSent: Boolean) {
            val updatedFriendRequests = if (isRequestSent) {
                user.friendRequests + user.uid // Arkadaşlık isteği göndermek
            } else {
                user.friendRequests - user.uid // Arkadaşlık isteğini iptal etmek
            }

            // Kullanıcının arkadaşlık isteklerini güncelle
            users[bindingAdapterPosition] = user.copy(friendRequests = updatedFriendRequests)
            notifyItemChanged(bindingAdapterPosition)
        }
    }

    // ViewHolder oluşturma
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddFriendViewHolder {
        val binding = ItemAddFriendBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AddFriendViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AddFriendViewHolder, position: Int) {
        val user = users[position]
        Log.d("AddFriendsAdapter", "Binding user: ${user.name}, position: $position")
        holder.bind(user)
    }

    override fun getItemCount(): Int {
        Log.d("AddFriendsAdapter", "Adapter itemCount: ${users.size}")
        return users.size
    }

    // Kullanıcı listesini güncelle
    fun updateUsers(newUsers: List<User>) {
        this.users = newUsers.toMutableList()
        notifyDataSetChanged()
    }

    // Belirli bir kullanıcının arkadaşlık isteği durumunu güncelle
    fun updateFriendStatus(user: User, isRequestSent: Boolean) {
        val index = users.indexOfFirst { it.uid == user.uid }
        if (index != -1) {
            val updatedFriendRequests = if (isRequestSent) {
                users[index].friendRequests + user.uid
            } else {
                users[index].friendRequests - user.uid
            }
            users[index] = users[index].copy(friendRequests = updatedFriendRequests)
            notifyItemChanged(index)
        }
    }
}
