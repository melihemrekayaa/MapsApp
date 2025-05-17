// app/src/main/java/com/example/mapsapp/adapter/FriendSheetAdapter.kt
package com.example.mapsapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mapsapp.R
import com.example.mapsapp.model.User
import com.example.mapsapp.model.FriendLocation

class FriendSheetAdapter(
    private val onLatestLocationClick: (FriendLocation) -> Unit
) : ListAdapter<User, FriendSheetAdapter.VH>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend_sheet, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTv: TextView = itemView.findViewById(R.id.friendNameTv)
        private val button: Button  = itemView.findViewById(R.id.latestLocationBtn)

        fun bind(user: User) {
            nameTv.text = user.name
            button.setOnClickListener {
                // GeoPoint → FriendLocation
                val gp = user.location
                val loc = FriendLocation(
                    uid   = user.uid,
                    email = user.email,
                    lat   = gp?.latitude  ?: 0.0,
                    lng   = gp?.longitude ?: 0.0
                )
                onLatestLocationClick(loc)
            }
        }
    }

    companion object {
        private val Diff = object : DiffUtil.ItemCallback<User>() {
            override fun areItemsTheSame(old: User, new: User) = old.uid == new.uid
            override fun areContentsTheSame(old: User, new: User) = old == new
        }
    }
}
