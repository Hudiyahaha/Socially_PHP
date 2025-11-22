package com.wethrive.i23_0761_i23_0765

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

class StoryAdapter(
    val stories: MutableList<Story>,
    private val onStoryClick: (Story) -> Unit
) : RecyclerView.Adapter<StoryAdapter.StoryViewHolder>() {

    class StoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val img: ImageView = view.findViewById(R.id.storyImage)
        val username: TextView = view.findViewById(R.id.story_username)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_story, parent, false)
        return StoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: StoryViewHolder, position: Int) {
        val story = stories[position]

        holder.username.text = story.username


        val dpBase64 = story.dpUrl  // this is base64, not URL

        val bmp = decodeBase64(dpBase64)
        if (bmp != null) {
            holder.img.setImageBitmap(bmp)
        } else {
            holder.img.setImageResource(R.drawable.dummy)
        }

        holder.itemView.setOnClickListener {
            onStoryClick(story)
        }
    }

    override fun getItemCount(): Int = stories.size
    fun decodeBase64(base64Str: String): Bitmap? {
        return try {
            val decodedBytes = android.util.Base64.decode(base64Str, android.util.Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            null
        }
    }

}
