package com.wethrive.i23_0761_i23_0765

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class MessagesDbHelper private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "messages.db"
        private const val DATABASE_VERSION = 1
        const val TABLE_NAME = "messages"
        const val COL_MESSAGE_ID = "message_id"
        const val COL_CHAT_ID = "chat_id"
        const val COL_SENDER_ID = "sender_id"
        const val COL_RECEIVER_ID = "receiver_id"
        const val COL_TEXT = "text"
        const val COL_IMAGE_BASE64 = "image_base64"
        const val COL_IMAGE_URL = "image_url"
        const val COL_POST_ID = "post_id"
        const val COL_TIMESTAMP = "timestamp"
        const val COL_EDITED = "edited"
        const val COL_DELETED = "deleted"
        const val COL_VANISH_MODE = "vanish_mode"
        const val COL_SEEN = "seen"
        const val COL_DELIVERY_STATE = "delivery_state"

        @Volatile
        private var INSTANCE: MessagesDbHelper? = null

        fun getInstance(context: Context): MessagesDbHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MessagesDbHelper(context).also { INSTANCE = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        val sql = """
            CREATE TABLE IF NOT EXISTS $TABLE_NAME (
               $COL_MESSAGE_ID TEXT PRIMARY KEY,
               $COL_CHAT_ID TEXT,
               $COL_SENDER_ID TEXT,
               $COL_RECEIVER_ID TEXT,
               $COL_TEXT TEXT,
               $COL_IMAGE_BASE64 TEXT,
               $COL_IMAGE_URL TEXT,
               $COL_POST_ID TEXT,
               $COL_TIMESTAMP INTEGER,
               $COL_EDITED INTEGER DEFAULT 0,
               $COL_DELETED INTEGER DEFAULT 0,
               $COL_VANISH_MODE INTEGER DEFAULT 0,
               $COL_SEEN INTEGER DEFAULT 0,
               $COL_DELIVERY_STATE INTEGER DEFAULT 0
            )
        """.trimIndent()
        db.execSQL(sql)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insertOrUpdateMessage(msg: Message) {
        val db = writableDatabase
        try {
            val cv = ContentValues().apply {
                put(COL_MESSAGE_ID, msg.messageId)
                put(COL_CHAT_ID, msg.chatId)
                put(COL_SENDER_ID, msg.senderId)
                put(COL_RECEIVER_ID, msg.receiverId)
                put(COL_TEXT, msg.text)
                put(COL_IMAGE_BASE64, msg.imageBase64)
                put(COL_IMAGE_URL, msg.imageUrl)
                put(COL_POST_ID, msg.postId)
                put(COL_TIMESTAMP, msg.timestamp)
                put(COL_EDITED, if (msg.edited) 1 else 0)
                put(COL_DELETED, if (msg.deleted) 1 else 0)
                put(COL_VANISH_MODE, if (msg.vanishMode) 1 else 0)
                put(COL_SEEN, if (msg.seen) 1 else 0)
                put(COL_DELIVERY_STATE, msg.deliveryState)
            }
            db.insertWithOnConflict(TABLE_NAME, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        } finally {
            db.close()
        }
    }

    fun getMessagesForChat(chatId: String): List<Message> {
        val list = mutableListOf<Message>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NAME,
            null,
            "$COL_CHAT_ID = ?",
            arrayOf(chatId),
            null,
            null,
            "$COL_TIMESTAMP ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                val m = Message(
                    messageId = it.getString(it.getColumnIndexOrThrow(COL_MESSAGE_ID)),
                    chatId = it.getString(it.getColumnIndexOrThrow(COL_CHAT_ID)),
                    senderId = it.getString(it.getColumnIndexOrThrow(COL_SENDER_ID)),
                    receiverId = it.getString(it.getColumnIndexOrThrow(COL_RECEIVER_ID)),
                    text = it.getString(it.getColumnIndexOrThrow(COL_TEXT)),
                    imageBase64 = it.getString(it.getColumnIndexOrThrow(COL_IMAGE_BASE64)),
                    imageUrl = it.getString(it.getColumnIndexOrThrow(COL_IMAGE_URL)),
                    postId = it.getString(it.getColumnIndexOrThrow(COL_POST_ID)),
                    timestamp = it.getLong(it.getColumnIndexOrThrow(COL_TIMESTAMP)),
                    edited = it.getInt(it.getColumnIndexOrThrow(COL_EDITED)) == 1,
                    deleted = it.getInt(it.getColumnIndexOrThrow(COL_DELETED)) == 1,
                    vanishMode = it.getInt(it.getColumnIndexOrThrow(COL_VANISH_MODE)) == 1,
                    seen = it.getInt(it.getColumnIndexOrThrow(COL_SEEN)) == 1,
                    deliveryState = it.getInt(it.getColumnIndexOrThrow(COL_DELIVERY_STATE)),
                    isPending = false
                )
                list.add(m)
            }
        }
        db.close()
        return list
    }

    fun deleteMessage(messageId: String) {
        val db = writableDatabase
        try {
            db.delete(TABLE_NAME, "$COL_MESSAGE_ID = ?", arrayOf(messageId))
        } finally {
            db.close()
        }
    }

    fun clearChatMessages(chatId: String) {
        val db = writableDatabase
        try {
            db.delete(TABLE_NAME, "$COL_CHAT_ID = ?", arrayOf(chatId))
        } finally {
            db.close()
        }
    }

    fun deleteVanishModeMessages(chatId: String, receiverId: String): Int {
        val db = writableDatabase
        // Delete messages that are in vanish mode, marked as seen, and were received by the current user
        // This matches the PHP endpoint logic
        return db.delete(
            TABLE_NAME,
            "$COL_CHAT_ID = ? AND $COL_VANISH_MODE = 1 AND $COL_SEEN = 1 AND $COL_RECEIVER_ID = ?",
            arrayOf(chatId, receiverId)
        )
    }
}

