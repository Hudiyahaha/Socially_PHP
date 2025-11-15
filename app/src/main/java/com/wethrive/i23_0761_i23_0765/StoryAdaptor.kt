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
import com.google.firebase.Firebase
import com.google.firebase.database.FirebaseDatabase


class StoryAdapter(private val stories: MutableList<Story>,
                   private val onStoryClick: (Story) -> Unit
) :
    RecyclerView.Adapter<StoryAdapter.StoryViewHolder>() {

    class StoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val img: ImageView = itemView.findViewById(R.id.storyImage)
        val username: TextView = itemView.findViewById(R.id.story_username)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_story, parent, false)
        return StoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: StoryViewHolder, position: Int) {
        val story = stories[position]

        // Show the user's profile picture (dp) as the cover
        if (!story.userId.isNullOrEmpty()) {
            val userRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(story.userId!!)

            userRef.get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val username = snapshot.child("uname").getValue(String::class.java)
                    val dpBase64 = snapshot.child("dp").getValue(String::class.java)

                    holder.username.text = username ?: "Unknown"

                    if (!dpBase64.isNullOrEmpty()) {
                        val bytes = Base64.decode(dpBase64, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        holder.img.setImageBitmap(bitmap)
                    } else {
                        holder.img.setImageResource(R.drawable.me)
                    }
                }
            }.addOnFailureListener {
                holder.username.text = "Unknown"
                holder.img.setImageResource(R.drawable.me)
            }
        }

        // Click to open the story
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, ViewOtherStory::class.java)
            intent.putExtra("storyUserId", story.userId) // pass correct user ID
            context.startActivity(intent)
        }
    }
    override fun getItemCount(): Int = stories.size
}
