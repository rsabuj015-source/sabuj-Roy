package com.example.chatnova.data.repository

import android.content.Context
import com.example.chatnova.core.network.NetworkMonitor
import com.example.chatnova.data.local.UserSessionManager
import com.example.chatnova.data.local.database.AppDatabase
import com.example.chatnova.data.local.entity.ConversationEntity
import com.example.chatnova.data.local.entity.DraftEntity
import com.example.chatnova.data.local.entity.MessageEntity
import com.example.chatnova.domain.model.ChatMessage
import com.example.chatnova.domain.model.ConversationItem
import com.example.chatnova.domain.model.MessageDeliveryStatus
import com.example.chatnova.domain.model.OnlineStatus
import com.example.chatnova.domain.model.UserProfile
import com.example.chatnova.domain.repository.ChatRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class FirebaseChatRepository(private val context: Context) : ChatRepository {
    private val database = AppDatabase.getInstance(context)
    private val chatDao = database.chatDao()
    private val sessionManager = UserSessionManager(context)
    private val networkMonitor = NetworkMonitor(context)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var activeConversationListener: ListenerRegistration? = null
    private var conversationsListListener: ListenerRegistration? = null

    init {
        // Seed default initial conversations and messages if Room cache is fresh
        scope.launch {
            val existing = chatDao.getConversations().first()
            if (existing.isEmpty()) {
                seedInitialRoomData()
            }
        }

        // Auto-sync pending messages when device reconnects to internet
        scope.launch {
            networkMonitor.isOnline.collect { online ->
                if (online) {
                    syncPendingMessages()
                }
            }
        }
    }

    private suspend fun seedInitialRoomData() {
        val initialConversations = listOf(
            ConversationEntity(
                id = "c1",
                title = "Luna Starlight",
                lastMessage = "Ready for the Tic Tac Toe rematch?",
                timestamp = "10:42 AM",
                unreadCount = 2,
                isGroup = false,
                onlineStatus = "ONLINE",
                deliveryStatus = "SENT",
                otherUserId = "u_luna",
                lastActivityMillis = System.currentTimeMillis() - 1000 * 60 * 10
            ),
            ConversationEntity(
                id = "c2",
                title = "Nova Gaming Squad",
                lastMessage = "Marcus: Who's hosting the tournament tonight?",
                timestamp = "09:15 AM",
                unreadCount = 5,
                isGroup = true,
                onlineStatus = "ONLINE",
                deliveryStatus = "SENT",
                lastActivityMillis = System.currentTimeMillis() - 1000 * 60 * 60
            ),
            ConversationEntity(
                id = "c3",
                title = "David Kim",
                lastMessage = "Check out the new game strategy I posted.",
                timestamp = "Yesterday",
                unreadCount = 0,
                isGroup = false,
                onlineStatus = "PLAYING",
                deliveryStatus = "SENT",
                otherUserId = "u_david",
                lastActivityMillis = System.currentTimeMillis() - 1000 * 60 * 60 * 24
            ),
            ConversationEntity(
                id = "c4",
                title = "Amina Rahman",
                lastMessage = "কেমন আছো? চ্যাটে কথা বলি চলো!",
                timestamp = "Yesterday",
                unreadCount = 1,
                isGroup = false,
                onlineStatus = "ONLINE",
                deliveryStatus = "SENT",
                otherUserId = "u_amina",
                lastActivityMillis = System.currentTimeMillis() - 1000 * 60 * 60 * 26
            )
        )
        chatDao.insertConversations(initialConversations)

        val initialMessages = listOf(
            MessageEntity("m1_1", "c1", "u_luna", "Luna Starlight", "Hey Alex! Excited for the ChatNova launch 🚀", "TEXT", "10:35 AM", System.currentTimeMillis() - 1000 * 60 * 20, "SENT"),
            MessageEntity("m1_2", "c1", "u_nova_1", "Alex Vance", "Hey Luna! Yes, offline messaging and games are working great.", "TEXT", "10:38 AM", System.currentTimeMillis() - 1000 * 60 * 15, "SENT"),
            MessageEntity("m1_3", "c1", "u_luna", "Luna Starlight", "Awesome! Ready for the Tic Tac Toe rematch?", "TEXT", "10:42 AM", System.currentTimeMillis() - 1000 * 60 * 10, "SENT"),
            MessageEntity("m2_1", "c2", "u_elena", "Elena", "The new cosmic arena map is live!", "TEXT", "09:05 AM", System.currentTimeMillis() - 1000 * 60 * 70, "SENT"),
            MessageEntity("m2_2", "c2", "u_marcus", "Marcus", "Marcus: Who's hosting the tournament tonight?", "TEXT", "09:15 AM", System.currentTimeMillis() - 1000 * 60 * 60, "SENT"),
            MessageEntity("m3_1", "c3", "u_david", "David Kim", "Check out the new game strategy I posted.", "TEXT", "Yesterday", System.currentTimeMillis() - 1000 * 60 * 60 * 24, "SENT"),
            MessageEntity("m4_1", "c4", "u_amina", "Amina Rahman", "কেমন আছো? চ্যাটে কথা বলি চলো!", "TEXT", "Yesterday", System.currentTimeMillis() - 1000 * 60 * 60 * 26, "SENT")
        )
        chatDao.insertMessages(initialMessages)
    }

    private fun isFirebaseConfigured(): Boolean {
        return try {
            FirebaseApp.getApps(context).isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }

    override fun getConversationsFlow(currentUserId: String): Flow<List<ConversationItem>> {
        // Trigger background cloud sync if online & Firebase configured
        if (isFirebaseConfigured() && networkMonitor.isCurrentlyOnline()) {
            startConversationsCloudSync(currentUserId)
        }

        // Return Room flow as Single Source of Truth (offline cached & instant)
        return chatDao.getConversations().map { entities ->
            entities.map { entity ->
                ConversationItem(
                    id = entity.id,
                    title = entity.title,
                    lastMessage = entity.lastMessage,
                    timestamp = entity.timestamp,
                    unreadCount = entity.unreadCount,
                    isGroup = entity.isGroup,
                    onlineStatus = parseOnlineStatus(entity.onlineStatus),
                    deliveryStatus = parseDeliveryStatus(entity.deliveryStatus),
                    otherUserId = entity.otherUserId,
                    otherUserPhoto = entity.otherUserPhoto
                )
            }
        }
    }

    private fun startConversationsCloudSync(currentUserId: String) {
        conversationsListListener?.remove()
        try {
            val firestore = FirebaseFirestore.getInstance()
            conversationsListListener = firestore.collection("conversations")
                .whereArrayContains("memberIds", currentUserId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener

                    scope.launch {
                        val entities = snapshot.documents.mapNotNull { doc ->
                            val id = doc.id
                            val memberIds = (doc.get("memberIds") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                            val otherUid = memberIds.firstOrNull { it != currentUserId } ?: "unknown"

                            val membersMap = doc.get("members") as? Map<*, *>
                            val otherMemberMap = membersMap?.get(otherUid) as? Map<*, *>
                            val otherDisplayName = otherMemberMap?.get("displayName")?.toString()
                                ?: otherMemberMap?.get("username")?.toString()
                                ?: "Nova Member"
                            val otherPhoto = otherMemberMap?.get("photoUrl")?.toString() ?: "p1"

                            val lastMsg = doc.getString("lastMessage") ?: "No messages"
                            val lastTime = doc.getString("lastMessageTimeFormatted") ?: "Recent"
                            val updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time ?: System.currentTimeMillis()

                            ConversationEntity(
                                id = id,
                                title = otherDisplayName,
                                lastMessage = lastMsg,
                                timestamp = lastTime,
                                unreadCount = 0,
                                isGroup = doc.getString("type") == "GROUP",
                                onlineStatus = "ONLINE",
                                deliveryStatus = "SENT",
                                otherUserId = otherUid,
                                otherUserPhoto = otherPhoto,
                                lastActivityMillis = updatedAt
                            )
                        }
                        if (entities.isNotEmpty()) {
                            chatDao.insertConversations(entities)
                        }
                    }
                }
        } catch (_: Exception) {}
    }

    override fun getMessagesFlow(
        conversationId: String,
        currentUserId: String,
        limit: Int
    ): Flow<List<ChatMessage>> {
        // Trigger background cloud sync for this conversation if online
        if (isFirebaseConfigured() && networkMonitor.isCurrentlyOnline()) {
            startActiveConversationCloudSync(conversationId, limit)
        }

        // Return Room flow as Single Source of Truth (includes pending outbox messages)
        return chatDao.getMessages(conversationId, limit).map { entities ->
            entities.map { entity ->
                ChatMessage(
                    id = entity.messageId,
                    conversationId = entity.conversationId,
                    senderId = entity.senderId,
                    senderName = entity.senderName,
                    content = entity.text,
                    timestamp = entity.timestamp,
                    status = parseDeliveryStatus(entity.syncStatus),
                    isFromMe = entity.senderId == currentUserId,
                    createdAtMillis = entity.createdAtMillis
                )
            }
        }
    }

    private fun startActiveConversationCloudSync(conversationId: String, limit: Int) {
        activeConversationListener?.remove()
        try {
            val firestore = FirebaseFirestore.getInstance()
            val timeFormatter = SimpleDateFormat("hh:mm a", Locale.US)

            activeConversationListener = firestore.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .limitToLast(limit.toLong())
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener

                    scope.launch {
                        val messagesToInsert = snapshot.documents.mapNotNull { doc ->
                            val msgId = doc.id
                            val senderId = doc.getString("senderId") ?: ""
                            val senderName = doc.getString("senderName") ?: "User"
                            val text = doc.getString("text") ?: ""
                            val timestampObj = doc.getTimestamp("createdAt")
                            val formattedTime = timestampObj?.toDate()?.let { timeFormatter.format(it) } ?: "Just now"
                            val createdAtMillis = timestampObj?.toDate()?.time ?: System.currentTimeMillis()

                            MessageEntity(
                                messageId = msgId,
                                conversationId = conversationId,
                                senderId = senderId,
                                senderName = senderName,
                                text = text,
                                type = "TEXT",
                                timestamp = formattedTime,
                                createdAtMillis = createdAtMillis,
                                syncStatus = "SENT"
                            )
                        }
                        if (messagesToInsert.isNotEmpty()) {
                            chatDao.insertMessages(messagesToInsert)
                        }
                    }
                }
        } catch (_: Exception) {}
    }

    override suspend fun sendMessage(
        conversationId: String,
        senderId: String,
        senderName: String,
        text: String
    ): Result<ChatMessage> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Message cannot be empty."))
        }
        if (trimmed.length > 2000) {
            return Result.failure(IllegalArgumentException("Message length exceeds 2000 characters."))
        }

        // 1. Generate unique client message ID (duplicate prevention)
        val messageId = "msg_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        val timeFormatter = SimpleDateFormat("hh:mm a", Locale.US)
        val formattedTime = timeFormatter.format(Date())
        val createdAtMillis = System.currentTimeMillis()

        val isOnline = networkMonitor.isCurrentlyOnline()
        val initialStatus = if (isOnline && isFirebaseConfigured()) "SENDING" else "PENDING"

        // 2. Persist locally in Room immediately (Offline-First)
        val entity = MessageEntity(
            messageId = messageId,
            conversationId = conversationId,
            senderId = senderId,
            senderName = senderName,
            text = trimmed,
            type = "TEXT",
            timestamp = formattedTime,
            createdAtMillis = createdAtMillis,
            syncStatus = initialStatus
        )
        chatDao.insertMessage(entity)

        // Clear draft for this conversation upon sending
        chatDao.deleteDraft(conversationId)

        // Update local conversation snippet
        val currentConv = chatDao.getConversations().first().find { it.id == conversationId }
        val updatedConv = currentConv?.copy(
            lastMessage = trimmed,
            timestamp = formattedTime,
            lastActivityMillis = createdAtMillis
        ) ?: ConversationEntity(
            id = conversationId,
            title = "Conversation",
            lastMessage = trimmed,
            timestamp = formattedTime,
            lastActivityMillis = createdAtMillis
        )
        chatDao.upsertConversation(updatedConv)

        // 3. If online, trigger sync immediately in background
        if (isOnline && isFirebaseConfigured()) {
            scope.launch {
                pushMessageToCloud(entity)
            }
        }

        val chatMessage = ChatMessage(
            id = messageId,
            conversationId = conversationId,
            senderId = senderId,
            senderName = senderName,
            content = trimmed,
            timestamp = formattedTime,
            status = parseDeliveryStatus(initialStatus),
            isFromMe = true,
            createdAtMillis = createdAtMillis
        )

        return Result.success(chatMessage)
    }

    override suspend fun retryMessage(messageId: String): Result<Unit> {
        val pendingList = chatDao.getPendingOrFailedMessages()
        val target = pendingList.find { it.messageId == messageId }
            ?: return Result.failure(IllegalArgumentException("Message not found."))

        chatDao.updateMessageSyncStatus(messageId, "PENDING")

        if (networkMonitor.isCurrentlyOnline() && isFirebaseConfigured()) {
            scope.launch {
                pushMessageToCloud(target)
            }
        }
        return Result.success(Unit)
    }

    override suspend fun syncPendingMessages() {
        if (!isFirebaseConfigured() || !networkMonitor.isCurrentlyOnline()) return

        val pending = chatDao.getPendingOrFailedMessages()
        for (msg in pending) {
            pushMessageToCloud(msg)
        }
    }

    private suspend fun pushMessageToCloud(msg: MessageEntity) {
        try {
            chatDao.updateMessageSyncStatus(msg.messageId, "SENDING")

            val firestore = FirebaseFirestore.getInstance()
            val convRef = firestore.collection("conversations").document(msg.conversationId)
            val msgRef = convRef.collection("messages").document(msg.messageId)

            val msgData = hashMapOf(
                "id" to msg.messageId,
                "conversationId" to msg.conversationId,
                "senderId" to msg.senderId,
                "senderName" to msg.senderName,
                "text" to msg.text,
                "type" to "TEXT",
                "status" to "sent",
                "createdAt" to FieldValue.serverTimestamp()
            )

            val batch = firestore.batch()
            // Set message using client messageId for strict duplicate prevention
            batch.set(msgRef, msgData, SetOptions.merge())
            batch.update(
                convRef,
                mapOf(
                    "lastMessage" to msg.text,
                    "lastMessageAt" to FieldValue.serverTimestamp(),
                    "lastMessageTimeFormatted" to msg.timestamp,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()

            chatDao.updateMessageSyncStatus(msg.messageId, "SENT")
        } catch (_: Exception) {
            chatDao.updateMessageSyncStatus(msg.messageId, "FAILED")
        }
    }

    override suspend fun saveDraft(conversationId: String, text: String) {
        if (text.isBlank()) {
            chatDao.deleteDraft(conversationId)
        } else {
            chatDao.saveDraft(DraftEntity(conversationId, text.trim()))
        }
    }

    override fun getDraftFlow(conversationId: String): Flow<String?> {
        return chatDao.getDraft(conversationId).map { it?.draftText }.distinctUntilChanged()
    }

    override suspend fun clearDraft(conversationId: String) {
        chatDao.deleteDraft(conversationId)
    }

    override fun getPendingCountFlow(): Flow<Int> {
        return chatDao.getPendingCountFlow()
    }

    override fun getNetworkStatusFlow(): Flow<Boolean> {
        return networkMonitor.isOnline
    }

    override suspend fun getOrCreateDirectConversation(
        currentUserId: String,
        currentUserName: String,
        currentUserPhoto: String?,
        otherUser: UserProfile
    ): Result<ConversationItem> {
        val convId = if (currentUserId < otherUser.userId) {
            "direct_${currentUserId}_${otherUser.userId}"
        } else {
            "direct_${otherUser.userId}_${currentUserId}"
        }

        val timeFormatter = SimpleDateFormat("hh:mm a", Locale.US)
        val formattedTime = timeFormatter.format(Date())

        val existing = chatDao.getConversations().first().find { it.id == convId }
        if (existing != null) {
            return Result.success(
                ConversationItem(
                    id = convId,
                    title = existing.title,
                    lastMessage = existing.lastMessage,
                    timestamp = existing.timestamp,
                    onlineStatus = parseOnlineStatus(existing.onlineStatus),
                    otherUserId = otherUser.userId,
                    otherUserPhoto = otherUser.profilePhoto
                )
            )
        }

        val entity = ConversationEntity(
            id = convId,
            title = otherUser.displayName,
            lastMessage = "Say hello to ${otherUser.displayName} 👋",
            timestamp = formattedTime,
            onlineStatus = otherUser.onlineStatus.name,
            deliveryStatus = "SENT",
            otherUserId = otherUser.userId,
            otherUserPhoto = otherUser.profilePhoto,
            lastActivityMillis = System.currentTimeMillis()
        )
        chatDao.upsertConversation(entity)

        // If online & Firebase active, register cloud document
        if (isFirebaseConfigured() && networkMonitor.isCurrentlyOnline()) {
            scope.launch {
                try {
                    val firestore = FirebaseFirestore.getInstance()
                    val convRef = firestore.collection("conversations").document(convId)
                    val convData = hashMapOf(
                        "id" to convId,
                        "type" to "DIRECT",
                        "memberIds" to listOf(currentUserId, otherUser.userId),
                        "members" to hashMapOf(
                            currentUserId to hashMapOf(
                                "displayName" to currentUserName,
                                "photoUrl" to (currentUserPhoto ?: "p1")
                            ),
                            otherUser.userId to hashMapOf(
                                "displayName" to otherUser.displayName,
                                "username" to otherUser.username,
                                "photoUrl" to (otherUser.profilePhoto ?: "p1")
                            )
                        ),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                        "lastMessage" to "Say hello to ${otherUser.displayName} 👋",
                        "lastMessageTimeFormatted" to formattedTime
                    )
                    convRef.set(convData, SetOptions.merge()).await()
                } catch (_: Exception) {}
            }
        }

        return Result.success(
            ConversationItem(
                id = convId,
                title = otherUser.displayName,
                lastMessage = entity.lastMessage,
                timestamp = formattedTime,
                onlineStatus = otherUser.onlineStatus,
                otherUserId = otherUser.userId,
                otherUserPhoto = otherUser.profilePhoto
            )
        )
    }

    override suspend fun searchUsers(query: String, currentUserId: String): Result<List<UserProfile>> {
        val q = query.trim().lowercase()
        val results = mutableListOf<UserProfile>()

        if (isFirebaseConfigured() && networkMonitor.isCurrentlyOnline()) {
            try {
                val firestore = FirebaseFirestore.getInstance()
                val snapshot = if (q.isBlank()) {
                    firestore.collection("users").limit(20).get().await()
                } else {
                    firestore.collection("users")
                        .orderBy("username")
                        .startAt(q)
                        .endAt(q + "\uf8ff")
                        .limit(20)
                        .get()
                        .await()
                }

                snapshot.documents.forEach { doc ->
                    val uid = doc.id
                    if (uid != currentUserId) {
                        results.add(
                            UserProfile(
                                userId = uid,
                                username = doc.getString("username") ?: "user",
                                displayName = doc.getString("displayName") ?: "Nova User",
                                email = doc.getString("email") ?: "",
                                bio = doc.getString("bio") ?: "",
                                profilePhoto = doc.getString("photoUrl") ?: "p1",
                                onlineStatus = OnlineStatus.ONLINE,
                                friendsCount = doc.getLong("friendsCount")?.toInt() ?: 0,
                                gamesPlayed = doc.getLong("gamesPlayed")?.toInt() ?: 0,
                                winsCount = doc.getLong("winsCount")?.toInt() ?: 0,
                                achievementsCount = doc.getLong("achievementsCount")?.toInt() ?: 1,
                                joinedDate = doc.getString("createdAtFormatted") ?: "September 2026"
                            )
                        )
                    }
                }
                return Result.success(results)
            } catch (_: Exception) {}
        }

        // Local accounts fallback
        val localFound = sessionManager.searchLocalUsers(q, currentUserId)
        results.addAll(localFound)

        val seedFriends = listOf(
            UserProfile("u_luna", "starlight_luna", "Luna Starlight", "luna@chatnova.io", "p1", "Competitive gamer 🌟"),
            UserProfile("u_david", "dkim_player", "David Kim", "david@chatnova.io", "p2", "Master tactician & board game lover ♟️"),
            UserProfile("u_amina", "amina_r", "Amina Rahman", "amina@chatnova.io", "p3", "ঢাকা থেকে চ্যাটনোভার সাথে যুক্ত আছি!"),
            UserProfile("u_marcus", "brody_speed", "Marcus Brody", "marcus@chatnova.io", "p4", "Level 99 Nova Challenger 🚀"),
            UserProfile("u_elena", "elena_cosmo", "Elena Rostova", "elena@chatnova.io", "p5", "Designing games for the future 🪐")
        )

        seedFriends.forEach { sf ->
            if (sf.userId != currentUserId && results.none { it.userId == sf.userId }) {
                if (q.isBlank() || sf.username.contains(q) || sf.displayName.lowercase().contains(q)) {
                    results.add(sf)
                }
            }
        }

        return Result.success(results)
    }

    override suspend fun loadMoreMessages(
        conversationId: String,
        currentUserId: String,
        currentCount: Int,
        additionalCount: Int
    ): Result<List<ChatMessage>> {
        return Result.success(emptyList())
    }

    private fun parseDeliveryStatus(status: String): MessageDeliveryStatus {
        return when (status.uppercase(Locale.ROOT)) {
            "PENDING" -> MessageDeliveryStatus.PENDING
            "SENDING" -> MessageDeliveryStatus.SENDING
            "SENT" -> MessageDeliveryStatus.SENT
            "DELIVERED" -> MessageDeliveryStatus.DELIVERED
            "READ" -> MessageDeliveryStatus.READ
            "FAILED" -> MessageDeliveryStatus.FAILED
            else -> MessageDeliveryStatus.SENT
        }
    }

    private fun parseOnlineStatus(status: String): OnlineStatus {
        return when (status.uppercase(Locale.ROOT)) {
            "ONLINE" -> OnlineStatus.ONLINE
            "PLAYING" -> OnlineStatus.PLAYING
            else -> OnlineStatus.OFFLINE
        }
    }
}
