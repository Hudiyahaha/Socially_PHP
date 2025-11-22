package com.wethrive.i23_0761_i23_0765

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
import kotlin.concurrent.thread

class ChatAdapter(
    private val messages: MutableList<Message>,
    private val currentUserId: String,
    private val onLongPress: (Message) -> Unit = {},
    private val onRetryClick: (Message) -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_SENT = 1
        const val VIEW_RECEIVED = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderId == currentUserId) VIEW_SENT else VIEW_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val layout = if (viewType == VIEW_SENT) R.layout.item_message_sent else R.layout.item_message_received
        val v = inflater.inflate(layout, parent, false)
        return MsgVH(v)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as MsgVH).bind(messages[position])
    }

    override fun getItemCount(): Int = messages.size

    fun addOrUpdateMessage(newMsg: Message) {
        val idx = messages.indexOfFirst { it.messageId == newMsg.messageId }
        if (idx >= 0) {
            messages[idx] = newMsg
            notifyItemChanged(idx)
        } else {
            messages.add(newMsg)
            notifyItemInserted(messages.lastIndex)
        }
    }

    fun addMessage(msg: Message) {
        messages.add(msg)
        notifyItemInserted(messages.lastIndex)
    }

    fun updateDeliveryState(messageId: String, state: Int) {
        val idx = messages.indexOfFirst { it.messageId == messageId }
        if (idx >= 0) {
            messages[idx].deliveryState = state
            messages[idx].isPending = state != 1
            notifyItemChanged(idx)
        }
    }

    private fun fmt(ts: Long): String {
        if (ts <= 0L) return ""
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date(ts))
    }

    private inner class MsgVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView? = itemView.findViewById(R.id.tvMessage)
        private val tvTime: TextView? = itemView.findViewById(R.id.tvTime)
        private val ivImage: ImageView? = itemView.findViewById(R.id.imageMessage)
        private val ivStatus: ImageView? = itemView.findViewById(R.id.statusIcon)

        fun bind(m: Message) {
            itemView.setOnLongClickListener {
                onLongPress(m); true
            }

            if (m.deleted) {
                tvMessage?.visibility = View.VISIBLE
                tvMessage?.setText(R.string.message_deleted)
                ivImage?.visibility = View.GONE
                tvTime?.text = fmt(m.timestamp)
                ivStatus?.visibility = View.GONE
                return
            }

            // Text
            val txt = (m.text ?: "").trim()
            if (txt.isNotEmpty()) {
                tvMessage?.visibility = View.VISIBLE
                tvMessage?.text = if (m.edited) "$txt (edited)" else txt
            } else {
                tvMessage?.visibility = View.GONE
            }

            // Image Base64 or URL
            when {
                !m.imageUrl.isNullOrBlank() -> {
                    ivImage?.visibility = View.VISIBLE
                    m.imageUrl?.let { url -> ivImage?.let { SimpleImageLoader.load(url, it) } }
                }
                !m.imageBase64.isNullOrBlank() -> {
                    ivImage?.visibility = View.VISIBLE
                    m.imageBase64?.let { b64 -> ivImage?.let { SimpleImageLoader.loadBase64(b64, it) } }
                }
                else -> {
                    ivImage?.visibility = View.GONE
                }
            }

            // Status indicator: pending/sent/failed
            ivStatus?.visibility = View.VISIBLE
            when (m.deliveryState) {
                0 -> { // queued/sending
                    ivStatus?.setImageResource(R.drawable.ic_clock)
                    ivStatus?.setOnClickListener(null)
                }
                1 -> { // sent
                    ivStatus?.setImageResource(R.drawable.ic_tick)
                    ivStatus?.setOnClickListener(null)
                }
                2 -> { // failed
                    ivStatus?.setImageResource(R.drawable.ic_failed)
                    ivStatus?.setOnClickListener {
                        onRetryClick(m)
                    }
                }
                else -> ivStatus?.visibility = View.GONE
            }

            // Time or "Sending..."
            tvTime?.text = if (m.isPending) itemView.context.getString(R.string.sending_label) else fmt(m.timestamp)
        }
    }
}

/** minimal image loader with cache */
private object SimpleImageLoader {
    private val cacheSize = (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()
    private val cache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount / 1024
    }

    fun load(url: String, iv: ImageView) {
        cache.get(url)?.let {
            iv.setImageBitmap(it); return
        }
        iv.tag = url
        iv.setImageDrawable(null)
        thread {
            var conn: HttpURLConnection? = null
            try {
                val u = URL(url)
                conn = (u.openConnection() as HttpURLConnection).apply {
                    doInput = true; connectTimeout = 8000; readTimeout = 8000
                }
                conn.connect()
                val bmp = BitmapFactory.decodeStream(conn.inputStream)
                if (bmp != null) {
                    cache.put(url, bmp)
                    if (iv.tag == url) iv.post { iv.setImageBitmap(bmp) }
                }
            } catch (_: Exception) { }
            finally { conn?.disconnect() }
        }
    }

    fun loadBase64(b64: String, iv: ImageView) {
        val key = "b64_${b64.hashCode()}"
        cache.get(key)?.let { iv.setImageBitmap(it); return }
        iv.tag = key
        iv.setImageDrawable(null)
        thread {
            try {
                val bytes = Base64.decode(b64, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bmp != null) {
                    cache.put(key, bmp)
                    if (iv.tag == key) iv.post { iv.setImageBitmap(bmp) }
                }
            } catch (_: Exception) { }
        }
    }
}
