package com.example.mapsapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mapsapp.R
import com.example.mapsapp.model.BotChatMessage
import java.text.SimpleDateFormat
import java.util.Locale

class BotChatAdapter(private val messages: List<BotChatMessage>): RecyclerView.Adapter<RecyclerView.ViewHolder>()  {

    companion object{
        const val VIEW_TYPE_USER_MESSAGE = 1
        const val VIEW_TYPE_BOT_MESSAGE = 2
    }
    class UserMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageTextView: TextView = itemView.findViewById(R.id.text_message_body)
        val timeTextView: TextView = itemView.findViewById(R.id.text_message_time)
    }

    class BotMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageTextView: TextView = itemView.findViewById(R.id.text_message_body)
        val timeTextView: TextView = itemView.findViewById(R.id.text_message_time)
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isUser) {
            VIEW_TYPE_USER_MESSAGE
        } else {
            VIEW_TYPE_BOT_MESSAGE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_USER_MESSAGE) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user_message, parent, false)
            UserMessageViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_other_message, parent, false)
            BotMessageViewHolder(view)
        }
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val time = sdf.format(message.timestamp)

        if (holder is UserMessageViewHolder) {
            holder.messageTextView.text = message.message
            holder.timeTextView.text = time
        } else if (holder is BotMessageViewHolder) {
            holder.messageTextView.text = message.message
            holder.timeTextView.text = time
        }
    }
}
