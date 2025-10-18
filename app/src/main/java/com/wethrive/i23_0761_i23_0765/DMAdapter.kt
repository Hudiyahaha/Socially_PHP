package com.wethrive.i23_0761_i23_0765

import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.hdodenhof.circleimageview.CircleImageView

// Simple data holder for a DM chat row; include `dp` for avatar base64 if available
data class DMItem(val id: String, val name: String, val lastMessage: String, val time: String, val dp: String? = null)

class DMAdapter(private val items: MutableList<DMItem>) : RecyclerView.Adapter<DMAdapter.DMViewHolder>() {

    class DMViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val avatar: CircleImageView = itemView.findViewById(R.id.imgAvatar)
        val name: TextView = itemView.findViewById(R.id.txtName)
        val message: TextView = itemView.findViewById(R.id.txtMessage)
        val time: TextView = itemView.findViewById(R.id.txtTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DMViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_dm, parent, false)
        return DMViewHolder(view)
    }

    override fun onBindViewHolder(holder: DMViewHolder, position: Int) {
        val item = items[position]
        holder.name.text = item.name
        holder.message.text = item.lastMessage
        holder.time.text = item.time

        // If profile dp (base64) provided, decode it and set as avatar; otherwise keep default in layout
        if (!item.dp.isNullOrBlank()) {
            try {
                val bytes = Base64.decode(item.dp, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bmp != null) holder.avatar.setImageBitmap(bmp)
            } catch (e: IllegalArgumentException) {
                // ignore and keep default
            }
        }

        // Accessibility
        holder.avatar.contentDescription = item.name

        holder.itemView.setOnClickListener {
            val ctx = holder.itemView.context
            val i = Intent(ctx, MainActivity9::class.java)
            i.putExtra("receiverId", item.id)
            i.putExtra("chatName", item.name)
            ctx.startActivity(i)
        }
    }

    override fun getItemCount(): Int = items.size

    fun submitList(newItems: List<DMItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun addItem(item: DMItem) {
        items.add(item)
        notifyItemInserted(items.lastIndex)
    }
}
