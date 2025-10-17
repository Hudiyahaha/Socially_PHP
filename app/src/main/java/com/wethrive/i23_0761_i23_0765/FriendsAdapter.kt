package com.wethrive.i23_0761_i23_0765

import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Adapter for the friends RecyclerView shown in activity_friends.xml
 * Binds a list of UserData objects (name, email, avatar(base64), bio)
 */
class FriendsAdapter(
    private val items: MutableList<UserData>
) : RecyclerView.Adapter<FriendsAdapter.FriendViewHolder>() {

    class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val avatar: ImageView = itemView.findViewById(R.id.imgAvatar)
        val name: TextView = itemView.findViewById(R.id.txtName)
        //val email: TextView = itemView.findViewById(R.id.txtEmail)
        val bio: TextView = itemView.findViewById(R.id.txtBio)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_friend, parent, false)
        return FriendViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val user = items[position]
        holder.name.text = user.uname
//        holder.email.text = user.email
        holder.bio.text = user.bio

        // Attempt to decode base64 avatar if present
        if (!user.dp.isNullOrBlank()) {
            try {
                val bytes = Base64.decode(user.dp, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bmp != null) {
                    holder.avatar.setImageBitmap(bmp)
                } else {
                    holder.avatar.setImageResource(android.R.drawable.sym_def_app_icon)
                }
            } catch (e: IllegalArgumentException) {
                holder.avatar.setImageResource(android.R.drawable.sym_def_app_icon)
            }
        } else {
            holder.avatar.setImageResource(android.R.drawable.sym_def_app_icon)
        }

        holder.itemView.setOnClickListener {
            val i = Intent(holder.itemView.context, profile::class.java)
            i.putExtra("id", user.id)
            holder.itemView.context.startActivity(i)
        }
    }

    override fun getItemCount(): Int = items.size

    /** Replace entire data set */
    fun submitList(newItems: List<UserData>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    /** Add a single friend */
    fun addItem(user: UserData) {
        items.add(user)
        notifyItemInserted(items.lastIndex)
    }
}

