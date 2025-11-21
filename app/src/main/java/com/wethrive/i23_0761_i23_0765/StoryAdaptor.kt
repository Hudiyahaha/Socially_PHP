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

        if (!story.dp.isNullOrEmpty()) {
            val bytes = Base64.decode(story.dp, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            holder.img.setImageBitmap(bitmap)
        } else {
            holder.img.setImageResource(R.drawable.me)
        }

        holder.itemView.setOnClickListener {
            onStoryClick(story)
        }
    }

    override fun getItemCount(): Int = stories.size
}
