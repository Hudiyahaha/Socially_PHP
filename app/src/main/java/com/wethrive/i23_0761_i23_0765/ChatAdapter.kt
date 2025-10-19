package com.wethrive.i23_0761_i23_0765

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter(
    private val messages: MutableList<Message>,
    private val currentUserId: String,
    private val onMessageLongPress: (Message) -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private companion object {
        const val VIEW_TYPE_SENT = 1
        const val VIEW_TYPE_RECEIVED = 2
    }

    override fun getItemViewType(position: Int): Int {
        val m = messages[position]
        return if (m.senderId == currentUserId) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_SENT) {
            val v = inflater.inflate(R.layout.item_message_sent, parent, false)
            MessageViewHolder(v)
        } else {
            val v = inflater.inflate(R.layout.item_message_received, parent, false)
            MessageViewHolder(v)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val m = messages[position]
        (holder as MessageViewHolder).bind(m)
    }

    override fun getItemCount(): Int = messages.size

    fun submitList(newItems: List<Message>) {
        messages.clear()
        messages.addAll(newItems)
        notifyDataSetChanged()
    }

    fun addMessage(message: Message) {
        messages.add(message)
        notifyItemInserted(messages.lastIndex)
    }

    private fun formatTime(ts: Long): String {
        if (ts <= 0L) return ""
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date(ts))
    }

    private inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        private val imageMessage: ImageView = itemView.findViewById(R.id.imageMessage)

        fun bind(m: Message) {
            // Long-press support
            val longPressListener = View.OnLongClickListener {
                onMessageLongPress(m)
                true
            }
            itemView.setOnLongClickListener(longPressListener)
            tvMessage.setOnLongClickListener(longPressListener)
            imageMessage.setOnLongClickListener(longPressListener)

            // Deleted takes precedence over any content
            if (m.deleted) {
                tvMessage.visibility = View.VISIBLE
                tvMessage.text = "Message deleted"
                tvMessage.isClickable = false
                tvMessage.setOnClickListener(null)
                imageMessage.visibility = View.GONE
                imageMessage.setImageDrawable(null)
                tvTime.text = formatTime(m.timestamp)
                return
            }

            // Shared post rendering
            val sharedPost = m.postId?.takeIf { it.isNotBlank() }
            if (sharedPost != null) {
                tvMessage.visibility = View.VISIBLE
                tvMessage.text = "View shared post"
                tvMessage.isClickable = true
                tvMessage.setOnClickListener {
                    val parts = sharedPost.split(":", limit = 2)
                    if (parts.size == 2) {
                        val i = Intent(itemView.context, ViewPost::class.java)
                        i.putExtra("uid", parts[0])
                        i.putExtra("postId", parts[1])
                        itemView.context.startActivity(i)
                    }
                }
                imageMessage.visibility = View.GONE
                imageMessage.setImageDrawable(null)
            } else {
                // Text message
                val baseText = (m.text ?: "").trim()
                if (baseText.isNotEmpty()) {
                    val editedMark = if (m.edited) " (edited)" else ""
                    tvMessage.visibility = View.VISIBLE
                    tvMessage.text = baseText + editedMark
                    tvMessage.setOnClickListener(null)
                    tvMessage.isClickable = false
                } else {
                    tvMessage.visibility = View.GONE
                    tvMessage.text = ""
                    tvMessage.setOnClickListener(null)
                    tvMessage.isClickable = false
                }

                // Image message
                val url = m.imageUrl
                val b64 = m.imageBase64
                when {
                    !url.isNullOrBlank() -> {
                        imageMessage.visibility = View.VISIBLE
                        imageMessage.contentDescription = "Image message"
                        SimpleImageLoader.load(url, imageMessage)
                    }
                    !b64.isNullOrBlank() -> {
                        imageMessage.visibility = View.VISIBLE
                        imageMessage.contentDescription = "Image message"
                        SimpleImageLoader.loadBase64(b64, imageMessage)
                    }
                    else -> {
                        imageMessage.visibility = View.GONE
                        imageMessage.setImageDrawable(null)
                    }
                }
            }

            // Time
            tvTime.text = formatTime(m.timestamp)
        }
    }
}

// Minimal, no-dependency image loader with in-memory cache.
private object SimpleImageLoader {
    // Use ~1/8th of available memory for cache
    private val cacheSize: Int = (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()
    private val cache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount / 1024
        }
    }

    fun load(url: String, imageView: ImageView) {
        // Serve from cache if available
        cache.get(url)?.let { bmp ->
            imageView.setImageBitmap(bmp)
            return
        }

        // Tag to avoid race when views are reused
        imageView.tag = url
        imageView.setImageDrawable(null)

        Thread {
            try {
                val bmp = downloadBitmap(url)
                if (bmp != null) {
                    cache.put(url, bmp)
                    if (imageView.tag == url) {
                        imageView.post { imageView.setImageBitmap(bmp) }
                    }
                }
            } catch (_: Exception) {
                // ignore failures silently
            }
        }.start()
    }

    fun loadBase64(b64: String, imageView: ImageView) {
        // Build a stable cache key without storing the entire string key
        val key = "b64_" + b64.hashCode()
        cache.get(key)?.let { bmp ->
            imageView.setImageBitmap(bmp)
            return
        }

        imageView.tag = key
        imageView.setImageDrawable(null)

        Thread {
            try {
                val bytes = Base64.decode(b64, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bmp != null) {
                    cache.put(key, bmp)
                    if (imageView.tag == key) {
                        imageView.post { imageView.setImageBitmap(bmp) }
                    }
                }
            } catch (_: Exception) {
                // ignore failures silently
            }
        }.start()
    }

    private fun downloadBitmap(urlStr: String): Bitmap? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlStr)
            conn = (url.openConnection() as HttpURLConnection).apply {
                doInput = true
                connectTimeout = 8000
                readTimeout = 8000
            }
            conn.connect()
            conn.inputStream.use { input -> BitmapFactory.decodeStream(input) }
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }
}
