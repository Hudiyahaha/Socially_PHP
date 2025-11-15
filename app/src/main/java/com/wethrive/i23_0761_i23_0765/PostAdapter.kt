package com.wethrive.i23_0761_i23_0765

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.VideoView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.io.FileOutputStream

class PostAdapter(
    private val posts: List<String>,      // Base64 media strings
    private val postIds: List<String>,    // Post IDs
    private val mediaTypes: List<String>, // "image" or "video"
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
        val base64String = posts[position]
        val mediaType = mediaTypes.getOrNull(position) ?: "image"

        try {
            val bytes = Base64.decode(base64String, Base64.DEFAULT)

            if (mediaType == "video") {
                // Hide image view
                holder.image.visibility = View.GONE
                holder.videoView.visibility = View.VISIBLE

                try {
                    // Write decoded bytes to temporary MP4 file
                    val tempFile = File.createTempFile("video_${postIds[position]}", ".mp4", holder.itemView.context.cacheDir)
                    val fos = FileOutputStream(tempFile)
                    fos.write(bytes)
                    fos.close()

                    holder.videoView.setVideoURI(Uri.fromFile(tempFile))
                    holder.videoView.seekTo(100) // Load first frame as thumbnail
                    holder.videoView.setOnPreparedListener { it.isLooping = false }

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(holder.itemView.context, "Error loading video", Toast.LENGTH_SHORT).show()
                }

            } else {
                // Hide video view
                holder.videoView.visibility = View.GONE
                holder.image.visibility = View.VISIBLE

                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                holder.image.setImageBitmap(bitmap)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Click listener
        holder.itemView.setOnClickListener {
            onPostClick(postIds[position])
        }
    }

    override fun getItemCount() = posts.size
}
