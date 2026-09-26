package com.example.chatnova.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val lastMessage: String,
    val timestamp: String,
    val unreadCount: Int = 0,
    val isGroup: Boolean = false,
    val onlineStatus: String = "ONLINE",
    val deliveryStatus: String = "SENT",
    val otherUserId: String? = null,
    val otherUserPhoto: String? = null,
    val lastActivityMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["conversationId"]),
        Index(value = ["syncStatus"])
    ]
)
data class MessageEntity(
    @PrimaryKey val messageId: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val type: String = "TEXT",
    val timestamp: String,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val syncStatus: String = "SENT" // "PENDING", "SENDING", "SENT", "FAILED"
)

@Entity(tableName = "drafts")
data class DraftEntity(
    @PrimaryKey val conversationId: String,
    val draftText: String,
    val updatedAtMillis: Long = System.currentTimeMillis()
)
