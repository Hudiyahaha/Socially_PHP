package com.wethrive.i23_0761_i23_0765

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase
import de.hdodenhof.circleimageview.CircleImageView

class FeedPostAdapter(private val postList: List<Post>) :
    RecyclerView.Adapter<FeedPostAdapter.PostViewHolder>() {

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val postImagesRecycler: RecyclerView = itemView.findViewById(R.id.postImagesRecycler)
        val username: TextView = itemView.findViewById(R.id.username)
        val profileImage: CircleImageView = itemView.findViewById(R.id.profile)
        val captionText: TextView = itemView.findViewById(R.id.captionText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post_feed, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = postList[position]

        //  Setup inner horizontal RecyclerView for post images
        holder.postImagesRecycler.layoutManager =
            LinearLayoutManager(holder.itemView.context, LinearLayoutManager.HORIZONTAL, false)
        holder.postImagesRecycler.adapter =
            PostMediaAdapter(post.mediaBase64List, post.mediaTypeList)




        // Load username and profile pic from Firebase
        val userRef = FirebaseDatabase.getInstance().getReference("Users").child(post.userId)
        userRef.child("uname").get().addOnSuccessListener {
            holder.username.text = it.getValue(String::class.java) ?: "Unknown"
        }
        userRef.child("dp").get().addOnSuccessListener { snapshot ->
            val base64 = snapshot.getValue(String::class.java)
            if (!base64.isNullOrEmpty()) {
                val bytes = Base64.decode(base64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                holder.profileImage.setImageBitmap(bitmap)
            }
        }

        //  Caption
        holder.captionText.text = post.caption ?: ""
    }

    override fun getItemCount(): Int = postList.size
}
