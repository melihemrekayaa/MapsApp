package com.example.mapsapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mapsapp.R
import com.example.mapsapp.model.User

class AddFriendsAdapter(
    private var users: List<User>,
    private val onAddFriendClick: (User) -> Unit,
    private val onCancelRequestClick: (User) -> Unit
) : RecyclerView.Adapter<AddFriendsAdapter.AddFriendViewHolder>() {

    inner class AddFriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userProfilePic: ImageView = itemView.findViewById(R.id.userProfilePic)
        private val userName: TextView = itemView.findViewById(R.id.userName)
        private val addFriendButton: ImageButton = itemView.findViewById(R.id.addFriendButton)
        private val cancelButton: ImageButton = itemView.findViewById(R.id.cancelButton)

        fun bind(user: User, isRequestSent: Boolean) {
            userName.text = user.name
            Glide.with(itemView.context)
                .load(user.profileImageUrl)
                .placeholder(R.drawable.placeholder_profile)
                .into(userProfilePic)

            if (isRequestSent) {
                addFriendButton.visibility = View.GONE
                cancelButton.visibility = View.VISIBLE
            } else {
                addFriendButton.visibility = View.VISIBLE
                cancelButton.visibility = View.GONE
            }

            addFriendButton.setOnClickListener {
                onAddFriendClick(user)
                addFriendButton.visibility = View.GONE
                cancelButton.visibility = View.VISIBLE
            }

            cancelButton.setOnClickListener {
                onCancelRequestClick(user)
                cancelButton.visibility = View.GONE
                addFriendButton.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddFriendViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_add_friend, parent, false)
        return AddFriendViewHolder(view)
    }

    override fun onBindViewHolder(holder: AddFriendViewHolder, position: Int) {
        holder.bind(users[position], false) // İlk başta istek gönderilmemiş olarak başlat
    }

    override fun getItemCount(): Int = users.size

    fun updateUsers(newUsers: List<User>) {
        this.users = newUsers
        notifyDataSetChanged()
    }
}
