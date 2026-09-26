package com.example.chatnova.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.chatnova.data.local.entity.ConversationEntity
import com.example.chatnova.data.local.entity.DraftEntity
import com.example.chatnova.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM conversations ORDER BY lastActivityMillis DESC")
    fun getConversations(): Flow<List<ConversationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversations(conversations: List<ConversationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConversation(conversation: ConversationEntity)

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAtMillis ASC LIMIT :limit")
    fun getMessages(conversationId: String, limit: Int): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("UPDATE messages SET syncStatus = :status WHERE messageId = :messageId")
    suspend fun updateMessageSyncStatus(messageId: String, status: String)

    @Query("SELECT * FROM messages WHERE syncStatus IN ('PENDING', 'FAILED') ORDER BY createdAtMillis ASC")
    suspend fun getPendingOrFailedMessages(): List<MessageEntity>

    @Query("SELECT COUNT(*) FROM messages WHERE syncStatus IN ('PENDING', 'SENDING')")
    fun getPendingCountFlow(): Flow<Int>

    @Query("SELECT * FROM drafts WHERE conversationId = :conversationId")
    fun getDraft(conversationId: String): Flow<DraftEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDraft(draft: DraftEntity)

    @Query("DELETE FROM drafts WHERE conversationId = :conversationId")
    suspend fun deleteDraft(conversationId: String)
}
