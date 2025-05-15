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
) : ListAdapter<Pair<User, Boolean>, FriendsAdapter.FriendViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend, parent, false)
        return FriendViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val friendName: TextView = itemView.findViewById(R.id.friendName)
        private val friendStatusIcon: ImageView = itemView.findViewById(R.id.friendStatusIcon)
        private val inCallLabel: TextView = itemView.findViewById(R.id.inCallLabel)
        private val removeButton: ImageButton = itemView.findViewById(R.id.removeFriendButton)

        fun bind(item: Pair<User, Boolean>) {
            val user = item.first
            val isInCall = item.second

            friendName.text = user.name
            friendStatusIcon.setImageResource(
                if (user.isOnline) R.drawable.circle_green else R.drawable.circle_red
            )

            inCallLabel.visibility = if (isInCall) View.VISIBLE else View.GONE

            itemView.setOnClickListener { onFriendClick(user) }
            removeButton.setOnClickListener { onRemoveClick(user) }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Pair<User, Boolean>>() {
            override fun areItemsTheSame(oldItem: Pair<User, Boolean>, newItem: Pair<User, Boolean>): Boolean {
                return oldItem.first.uid == newItem.first.uid
            }

            override fun areContentsTheSame(oldItem: Pair<User, Boolean>, newItem: Pair<User, Boolean>): Boolean {
                return oldItem == newItem
            }
        }
    }
}



