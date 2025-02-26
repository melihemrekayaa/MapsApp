package com.example.mapsapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mapsapp.R
import com.example.mapsapp.model.User

class FriendsAdapter(
    private val onFriendClick: (User) -> Unit,
    private val onRemoveClick: (User) -> Unit
) : ListAdapter<User, FriendsAdapter.FriendViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend, parent, false)
        return FriendViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val friend = getItem(position)
        holder.bind(friend)
    }

    inner class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val friendName: TextView = itemView.findViewById(R.id.friendName)
        private val friendStatusIcon: ImageView = itemView.findViewById(R.id.friendStatusIcon)
        private val removeButton: ImageButton = itemView.findViewById(R.id.removeFriendButton)

        fun bind(friend: User) {
            friendName.text = friend.name

            friendStatusIcon.setImageResource(
                if (friend.isOnline) R.drawable.circle_green else R.drawable.circle_red
            )

            itemView.setOnClickListener { onFriendClick(friend) }
            removeButton.setOnClickListener { onRemoveClick(friend) }
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

