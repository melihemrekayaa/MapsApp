package com.example.mapsapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mapsapp.databinding.ItemFriendBinding
import com.example.mapsapp.model.User

class FriendsAdapter(
    friends: List<User>, // Immutable List kullanılıyor
    private val onItemClick: (User) -> Unit // Kullanıcıyı seçmek için lambda
) : RecyclerView.Adapter<FriendsAdapter.FriendsViewHolder>() {

    private val friendList = friends.toMutableList() // MutableList'e dönüştürme

    inner class FriendsViewHolder(private val binding: ItemFriendBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: User) {
            // Kullanıcı adını göster
            binding.friendName.text = user.name

            // Profil resmi kullanmayacağımız için Glide kısmını kaldırdık

            // Seçilen arkadaş için onClickListener
            binding.root.setOnClickListener {
                onItemClick(user) // Seçilen kullanıcıyı geri döndür
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendsViewHolder {
        val binding = ItemFriendBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FriendsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FriendsViewHolder, position: Int) {
        holder.bind(friendList[position])
    }

    override fun getItemCount(): Int = friendList.size

    // Arkadaş listesini güncelle
    fun updateFriends(newFriends: List<User>) {
        friendList.clear()
        friendList.addAll(newFriends)
        notifyDataSetChanged()
    }
}
