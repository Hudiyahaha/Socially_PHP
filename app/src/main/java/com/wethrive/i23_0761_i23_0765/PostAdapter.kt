package com.wethrive.i23_0761_i23_0765

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.VideoView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

class PostAdapter(
    private val mediaUrls: List<String>,      // URLs via i.php
    private val postIds: List<String>,        // Post IDs
    private val mediaTypes: List<String>,     // "image" or "video"
    private val onPostClick: (String) -> Unit
) : RecyclerView.Adapter<PostAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.mediaThumbnail)
        val videoView: VideoView = itemView.findViewById(R.id.videoView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_media, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val mediaUrl = mediaUrls[position]
        val mediaType = mediaTypes.getOrNull(position) ?: "image"

        if (mediaType == "video") {
            holder.image.visibility = View.GONE
            holder.videoView.visibility = View.VISIBLE

            try {
                holder.videoView.setVideoURI(Uri.parse(mediaUrl))
                holder.videoView.seekTo(100) // Optional: show first frame as thumbnail
                holder.videoView.setOnPreparedListener { it.isLooping = false }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        } else {
            holder.videoView.visibility = View.GONE
            holder.image.visibility = View.VISIBLE

            // Use Picasso to load image from URL
            Picasso.get()
                .load(mediaUrl)
                .placeholder(R.drawable.dummy) // optional placeholder
                .error(R.drawable.me)
                .into(holder.image)
        }

        holder.itemView.setOnClickListener {
            onPostClick(postIds[position])
        }
    }

    override fun getItemCount() = mediaUrls.size
}
