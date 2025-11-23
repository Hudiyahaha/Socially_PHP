package com.wethrive.i23_0761_i23_0765

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.VideoView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

class PostMediaAdapter(
    private val mediaUrlList: List<String>,
    private val mediaTypeList: List<String>
) : RecyclerView.Adapter<PostMediaAdapter.MediaViewHolder>() {

    inner class MediaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageItem: ImageView = itemView.findViewById(R.id.imageItem)
        val videoItem: VideoView = itemView.findViewById(R.id.videoItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post_media, parent, false)
        return MediaViewHolder(view)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        val mediaUrl = mediaUrlList[position]
        val type = mediaTypeList.getOrNull(position) ?: "image"

        if (type == "video") {
            holder.imageItem.visibility = View.GONE
            holder.videoItem.visibility = View.VISIBLE

            // Load video via URI
            holder.videoItem.setVideoURI(Uri.parse(mediaUrl))
            holder.videoItem.seekTo(100)
            holder.videoItem.setOnPreparedListener { it.isLooping = false }

        } else {
            holder.videoItem.visibility = View.GONE
            holder.imageItem.visibility = View.VISIBLE

            // Load image via Picasso
            Picasso.get()
                .load(mediaUrl)
                .placeholder(R.drawable.dummy)   // fallback placeholder
                .error(R.drawable.me)         // fallback if URL fails
                .into(holder.imageItem)
        }
    }

    override fun getItemCount(): Int = mediaUrlList.size
}
