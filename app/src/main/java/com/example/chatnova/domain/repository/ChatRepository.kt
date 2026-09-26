package com.example.chatnova.domain.repository

import com.example.chatnova.domain.model.ChatMessage
import com.example.chatnova.domain.model.ConversationItem
import com.example.chatnova.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getConversationsFlow(currentUserId: String): Flow<List<ConversationItem>>
    fun getMessagesFlow(conversationId: String, currentUserId: String, limit: Int = 50): Flow<List<ChatMessage>>
    suspend fun sendMessage(
        conversationId: String,
        senderId: String,
        senderName: String,
        text: String
    ): Result<ChatMessage>
    suspend fun retryMessage(messageId: String): Result<Unit>
    suspend fun getOrCreateDirectConversation(
        currentUserId: String,
        currentUserName: String,
        currentUserPhoto: String?,
        otherUser: UserProfile
    ): Result<ConversationItem>
    suspend fun searchUsers(query: String, currentUserId: String): Result<List<UserProfile>>
    suspend fun loadMoreMessages(
        conversationId: String,
        currentUserId: String,
        currentCount: Int,
        additionalCount: Int = 30
    ): Result<List<ChatMessage>>

    // Offline-First Outbox and Draft Management
    suspend fun saveDraft(conversationId: String, text: String)
    fun getDraftFlow(conversationId: String): Flow<String?>
    suspend fun clearDraft(conversationId: String)
    fun getPendingCountFlow(): Flow<Int>
    fun getNetworkStatusFlow(): Flow<Boolean>
    suspend fun syncPendingMessages()
}
