package com.example.mapsapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mapsapp.R
import com.example.mapsapp.model.User

class FriendsAdapter(
    private var friends: List<User>,
    private val onFriendClick: (User) -> Unit
) : RecyclerView.Adapter<FriendsAdapter.FriendViewHolder>() {

    inner class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val friendName: TextView = itemView.findViewById(R.id.friendName)
        private val friendProfilePic: ImageView = itemView.findViewById(R.id.friendProfilePic)

        fun bind(friend: User) {
            friendName.text = friend.name
            Glide.with(itemView.context)
                .load(friend.profileImageUrl)
                .placeholder(R.drawable.placeholder_profile)
                .into(friendProfilePic)

            itemView.setOnClickListener {
                onFriendClick(friend)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_friend, parent, false)
        return FriendViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        holder.bind(friends[position])
    }

    override fun getItemCount(): Int = friends.size

    fun updateFriends(newFriends: List<User>) {
        this.friends = newFriends
        notifyDataSetChanged()
    }
}
