package com.wethrive.i23_0761_i23_0765

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.VideoView
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.io.FileOutputStream

class PostMediaAdapter(
    private val mediaBase64List: List<String>,
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
        val base64 = mediaBase64List[position]
        val type = mediaTypeList.getOrNull(position) ?: "image"

        if (type == "video") {
            holder.imageItem.visibility = View.GONE
            holder.videoItem.visibility = View.VISIBLE

            try {
                val bytes = Base64.decode(base64, Base64.DEFAULT)
                val tempFile = File.createTempFile("feed_video_$position", ".mp4", holder.itemView.context.cacheDir)
                FileOutputStream(tempFile).use { it.write(bytes) }

                holder.videoItem.setVideoURI(Uri.fromFile(tempFile))
                holder.videoItem.seekTo(100)
                holder.videoItem.setOnPreparedListener { it.isLooping = false }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            holder.videoItem.visibility = View.GONE
            holder.imageItem.visibility = View.VISIBLE

            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            holder.imageItem.setImageBitmap(bitmap)
        }
    }

    override fun getItemCount(): Int = mediaBase64List.size
}
