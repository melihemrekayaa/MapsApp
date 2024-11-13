import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mapsapp.R
import com.example.mapsapp.databinding.ItemFriendBinding
import com.example.mapsapp.model.User

class FriendsAdapter(
    private var friends: MutableList<User>,
    private val onItemClick: (User) -> Unit // Kullanıcıyı seçmek için lambda
) : RecyclerView.Adapter<FriendsAdapter.FriendsViewHolder>() {

    inner class FriendsViewHolder(private val binding: ItemFriendBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: User) {
            binding.friendName.text = user.name

            Glide.with(binding.root.context)
                .load(user.profileImageUrl)
                .placeholder(R.drawable.friend_status_indicator)
                .error(R.drawable.friend_status_indicator)
                .into(binding.friendProfilePic)

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
        holder.bind(friends[position])
    }

    override fun getItemCount(): Int = friends.size

    fun updateFriends(newFriends: List<User>) {
        friends.clear()
        friends.addAll(newFriends)
        notifyDataSetChanged()
    }
}
