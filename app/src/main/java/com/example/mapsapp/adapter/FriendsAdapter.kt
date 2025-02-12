package com.example.mapsapp.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mapsapp.R
import com.example.mapsapp.databinding.ItemFriendBinding
import com.example.mapsapp.model.User

class FriendsAdapter(
    private val onFriendClick: (User) -> Unit
) : ListAdapter<User, FriendsAdapter.FriendViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend, parent, false)
        return FriendViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val friend = getItem(position)
        Log.d("FriendsAdapter", "Binding friend: ${friend.name}")
        holder.bind(friend)
    }


    inner class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val friendName: TextView = itemView.findViewById(R.id.friendName)
        private val friendProfilePic: ImageView = itemView.findViewById(R.id.friendProfilePic)
        private val friendStatusIcon: ImageView = itemView.findViewById(R.id.friendStatusIcon)

        fun bind(friend: User) {
            Log.d("FriendsAdapter", "Binding friend: ${friend.name}")

            friendName.text = friend.name

            if (friend.photoUrl != null) {
                Glide.with(itemView.context)
                    .load(friend.photoUrl)
                    .into(friendProfilePic)
            } else {
                friendProfilePic.setImageResource(R.drawable.baseline_account_circle_24)
            }

            // Update the online/offline status
            updateStatusIndicator(friend.isOnline)
        }

        private fun updateStatusIndicator(isOnline: Boolean) {
            Log.d("FriendsAdapter", "Updating status icon for ${friendName.text}: $isOnline")

            friendStatusIcon.setImageResource(
                if (isOnline) R.drawable.circle_green else R.drawable.circle_red
            )
        }



    }



    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<User>() {
            override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
                return oldItem.uid == newItem.uid
            }

            override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
                return oldItem == newItem
            }
        }
    }
}