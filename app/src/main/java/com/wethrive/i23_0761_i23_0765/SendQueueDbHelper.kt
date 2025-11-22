package com.wethrive.i23_0761_i23_0765

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class SendQueueDbHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "send_queue.db"
        private const val DATABASE_VERSION = 1
        const val TABLE_NAME = "send_queue"
        const val COL_ID = "id"
        const val COL_MESSAGE_ID = "message_id"
        const val COL_CHAT_ID = "chat_id"
        const val COL_SENDER_ID = "sender_id"
        const val COL_RECEIVER_ID = "receiver_id"
        const val COL_TEXT = "text"
        const val COL_IMAGE = "image"
        const val COL_POST_ID = "post_id"
        const val COL_VANISH_MODE = "vanish_mode"
        const val COL_TIMESTAMP = "timestamp"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val sql = """
            CREATE TABLE IF NOT EXISTS $TABLE_NAME (
               $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
               $COL_MESSAGE_ID TEXT UNIQUE,
               $COL_CHAT_ID TEXT,
               $COL_SENDER_ID TEXT,
               $COL_RECEIVER_ID TEXT,
               $COL_TEXT TEXT,
               $COL_IMAGE TEXT,
               $COL_POST_ID TEXT,
               $COL_VANISH_MODE INTEGER DEFAULT 0,
               $COL_TIMESTAMP INTEGER
            )
        """.trimIndent()
        db.execSQL(sql)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insertMessage(msg: Message) {
        val db = writableDatabase
        try {
            val cv = ContentValues().apply {
                put(COL_MESSAGE_ID, msg.messageId)
                put(COL_CHAT_ID, msg.chatId)
                put(COL_SENDER_ID, msg.senderId)
                put(COL_RECEIVER_ID, msg.receiverId)
                put(COL_TEXT, msg.text)
                put(COL_IMAGE, msg.imageBase64)
                put(COL_POST_ID, msg.postId)
                put(COL_VANISH_MODE, if (msg.vanishMode) 1 else 0)
                put(COL_TIMESTAMP, msg.timestamp)
            }
            db.insertWithOnConflict(TABLE_NAME, null, cv, SQLiteDatabase.CONFLICT_IGNORE)
        } finally {
            db.close()
        }
    }

    fun getAllPendingMessages(): List<Message> {
        val list = mutableListOf<Message>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NAME,
            null, null, null, null, null,
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
                    imageBase64 = it.getString(it.getColumnIndexOrThrow(COL_IMAGE)),
                    postId = it.getString(it.getColumnIndexOrThrow(COL_POST_ID)),
                    vanishMode = it.getInt(it.getColumnIndexOrThrow(COL_VANISH_MODE)) == 1,
                    timestamp = it.getLong(it.getColumnIndexOrThrow(COL_TIMESTAMP)),
                    isPending = true,
                    deliveryState = 0
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
}
