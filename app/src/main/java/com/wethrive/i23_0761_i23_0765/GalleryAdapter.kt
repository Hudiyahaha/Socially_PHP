package com.wethrive.i23_0761_i23_0765

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class GalleryAdapter(
    private val uris: List<Uri>,
    private val context: Context,
    private val onSelectionChanged: (Uri, Boolean) -> Unit // ✅ now sends uri + selected state
) : RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {

    private val selectedItems = mutableSetOf<Uri>()

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val thumbnail: ImageView = view.findViewById(R.id.mediaThumbnail)
        val overlay: View = view.findViewById(R.id.selectionOverlay)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_media, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val uri = uris[position]
        val mimeType = context.contentResolver.getType(uri)

        // Load thumbnail (video or image)
        if (mimeType?.startsWith("video") == true) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val bitmap = retriever.getFrameAtTime(0)
                holder.thumbnail.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.thumbnail.setImageResource(android.R.color.black)
            } finally {
                retriever.release()
            }
        } else {
            holder.thumbnail.setImageURI(uri)
        }

        // ✅ Update visual selection state
        if (selectedItems.contains(uri)) {
            holder.overlay.visibility = View.VISIBLE
        } else {
            holder.overlay.visibility = View.GONE
        }

        // ✅ Toggle selection on click
        holder.itemView.setOnClickListener {
            val isSelected = if (selectedItems.contains(uri)) {
                selectedItems.remove(uri)
                holder.overlay.visibility = View.GONE
                false
            } else {
                selectedItems.add(uri)
                holder.overlay.visibility = View.VISIBLE
                true
            }

            onSelectionChanged(uri, isSelected)
        }
    }

    override fun getItemCount() = uris.size
}
