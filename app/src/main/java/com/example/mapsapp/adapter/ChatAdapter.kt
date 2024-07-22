package com.example.mapsapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mapsapp.R
import com.example.mapsapp.model.Message

class ChatAdapter(private val messages: List<Message>, private val currentUserId : String): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_USER_MESSAGE = 1
        const val VIEW_TYPE_OTHER_MESSAGE = 2
    }

    class UserMessageViewHolder(itemview: View) : RecyclerView.ViewHolder(itemview){
        val messageTextView: TextView = itemview.findViewById(R.id.messageTextView)
    }
    class OtherMessageViewHolder(itemview: View) : RecyclerView.ViewHolder(itemview){
        val messageTextView: TextView = itemview.findViewById(R.id.messageTextView)
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderId == currentUserId) {
            VIEW_TYPE_USER_MESSAGE
        }
        else{
            VIEW_TYPE_OTHER_MESSAGE
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_USER_MESSAGE){
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user_message,parent,false)
            UserMessageViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_other_message,parent,false)
            OtherMessageViewHolder(view)
        }
    }


    override fun getItemCount(): Int {
        return messages.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        if (holder is UserMessageViewHolder){
            holder.messageTextView.text = message.message
        }
        else if(holder is OtherMessageViewHolder){
            holder.messageTextView.text = message.message
        }
    }
}