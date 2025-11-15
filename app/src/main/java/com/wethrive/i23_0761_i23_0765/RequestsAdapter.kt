package com.wethrive.i23_0761_i23_0765

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Adapter for the follow requests RecyclerView shown in activity_requests.xml
 * Binds a list of UserData objects (name, email, avatar(base64), bio)
 * Provides Accept/Reject buttons for each request
 */
class RequestsAdapter(
    private val items: MutableList<UserData>,
    private val onAccept: (UserData, Int) -> Unit,
    private val onReject: (UserData, Int) -> Unit
) : RecyclerView.Adapter<RequestsAdapter.RequestViewHolder>() {

    class RequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val avatar: ImageView = itemView.findViewById(R.id.imgAvatar)
        val name: TextView = itemView.findViewById(R.id.txtName)
        val btnAccept: TextView = itemView.findViewById(R.id.Accept)
        val btnReject: TextView = itemView.findViewById(R.id.Reject)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_request, parent, false)
        return RequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val user = items[position]
        holder.name.text = user.uname

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

        holder.btnAccept.setOnClickListener {
            onAccept(user, holder.adapterPosition)
        }

        holder.btnReject.setOnClickListener {
            onReject(user, holder.adapterPosition)
        }
    }

    override fun getItemCount(): Int = items.size

    /** Replace entire data set */
    fun submitList(newItems: List<UserData>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    /** Remove item at position */
    fun removeItem(position: Int) {
        if (position >= 0 && position < items.size) {
            items.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}
