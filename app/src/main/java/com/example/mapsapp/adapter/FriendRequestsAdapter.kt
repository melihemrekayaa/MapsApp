package com.example.mapsapp.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mapsapp.R
import com.example.mapsapp.model.User

class FriendRequestsAdapter(
    private var requests: List<User>,
    private val onAcceptClick: (User) -> Unit
) : RecyclerView.Adapter<FriendRequestsAdapter.FriendRequestViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendRequestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend_request, parent, false)
        return FriendRequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendRequestViewHolder, position: Int) {
        val friendRequest = requests[position]
        holder.bind(friendRequest)
    }

    override fun getItemCount(): Int = requests.size

    inner class FriendRequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val friendName: TextView = itemView.findViewById(R.id.friendRequestName)
        private val acceptButton: ImageButton = itemView.findViewById(R.id.acceptFriendRequestButton)
        private val profilePic: ImageView = itemView.findViewById(R.id.friendRequestProfilePic)

        fun bind(friend: User) {
            friendName.text = friend.name.ifEmpty { "Unknown" } // İsmi boşsa "Unknown" yaz
            Log.d("FriendRequestsAdapter", "Displaying: ${friend.name}")



            acceptButton.setOnClickListener {
                Log.d("FriendRequestsAdapter", "Accepting request from: ${friend.uid}")
                onAcceptClick(friend)
            }
        }
    }

    fun updateRequests(newRequests: List<User>) {
        requests = newRequests
        notifyDataSetChanged()
    }
}

