package com.example.mapsapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mapsapp.R
import com.example.mapsapp.databinding.ItemAddFriendBinding
import com.example.mapsapp.model.User

class AddFriendsAdapter(
    private var users: List<User>,
    private val onAddFriendClick: (User) -> Unit,
    private val onCancelRequestClick: (User) -> Unit
) : RecyclerView.Adapter<AddFriendsAdapter.AddFriendViewHolder>() {

    inner class AddFriendViewHolder(private val binding: ItemAddFriendBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: User) {
            binding.userName.text = user.name
            Glide.with(binding.root.context)
                .load(user.profileImageUrl)
                .placeholder(R.drawable.friend_status_indicator)
                .error(R.drawable.friend_status_indicator)
                .into(binding.userProfilePic)

            if (user.isRequestSent) {
                binding.addFriendButton.visibility = View.GONE
                binding.cancelButton.visibility = View.VISIBLE
            } else {
                binding.addFriendButton.visibility = View.VISIBLE
                binding.cancelButton.visibility = View.GONE
            }

            binding.addFriendButton.setOnClickListener {
                onAddFriendClick(user)
                updateUserRequestStatus(user, true)
            }

            binding.cancelButton.setOnClickListener {
                onCancelRequestClick(user)
                updateUserRequestStatus(user, false)
            }
        }

        private fun updateUserRequestStatus(user: User, isRequestSent: Boolean) {
            user.isRequestSent = isRequestSent
            notifyItemChanged(bindingAdapterPosition)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddFriendViewHolder {
        val binding = ItemAddFriendBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AddFriendViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AddFriendViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount(): Int = users.size

    fun updateUsers(newUsers: List<User>) {
        this.users = newUsers
        notifyDataSetChanged()
    }

    fun updateFriendStatus(user: User, isRequestSent: Boolean) {
        val index = users.indexOfFirst { it.uid == user.uid }
        if (index != -1) {
            users[index].isRequestSent = isRequestSent
            notifyItemChanged(index)
        }
    }
}
