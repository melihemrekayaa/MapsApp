package com.example.mapsapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mapsapp.R
import com.example.mapsapp.model.Message
import com.example.mapsapp.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class UsersAdapter(private val users: List<User>,private val onUserClick: (User) -> Unit) : RecyclerView.Adapter<UsersAdapter.UsersViewHolder>() {
    class UsersViewHolder(val itemview : View) : RecyclerView.ViewHolder(itemview){
        val userImageView: ImageView = itemView.findViewById(R.id.userImageView)
        val userNameTextView: TextView = itemView.findViewById(R.id.userNameTextView)
        val lastMessageTextView: TextView = itemView.findViewById(R.id.lastMessageTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsersViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.chat_interface_item,parent,false)
        return UsersViewHolder(view)
    }

    override fun getItemCount(): Int {
        return users.size
    }

    override fun onBindViewHolder(holder: UsersViewHolder, position: Int) {
        val user = users[position]
        holder.userNameTextView.text = user.email.split("@")[0]
        holder.userImageView.setImageResource(R.drawable.baseline_account_circle_24)

        FirebaseFirestore.getInstance().collection("messages")
            .whereEqualTo("receiverId",user.uid)
            .orderBy("timestamp",Query.Direction.ASCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener {document ->
                if (!document.isEmpty){
                    val lastMessage = document.documents[0].toObject(Message::class.java)
                    holder.lastMessageTextView.text = lastMessage?.message
                }

            }

        holder.itemview.setOnClickListener{
            onUserClick(user)
        }

    }
}