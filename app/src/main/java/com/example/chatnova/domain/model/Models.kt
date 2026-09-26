package com.example.chatnova.domain.model

enum class OnlineStatus {
    ONLINE, OFFLINE, AWAY, PLAYING
}

enum class MessageDeliveryStatus {
    PENDING, SENDING, SENT, DELIVERED, READ, FAILED
}

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

enum class AppLanguage {
    ENGLISH, BANGLA
}

data class UserProfile(
    val userId: String,
    val username: String,
    val displayName: String,
    val email: String,
    val profilePhoto: String? = null,
    val bio: String,
    val onlineStatus: OnlineStatus = OnlineStatus.ONLINE,
    val lastSeen: String = "Just now",
    val friendsCount: Int = 48,
    val gamesPlayed: Int = 112,
    val winsCount: Int = 74,
    val achievementsCount: Int = 8,
    val joinedDate: String = "September 2026"
)

data class ConversationItem(
    val id: String,
    val title: String,
    val lastMessage: String,
    val timestamp: String,
    val unreadCount: Int = 0,
    val isGroup: Boolean = false,
    val onlineStatus: OnlineStatus = OnlineStatus.OFFLINE,
    val deliveryStatus: MessageDeliveryStatus = MessageDeliveryStatus.SENT,
    val avatarInitial: String = title.take(2).uppercase(),
    val otherUserId: String? = null,
    val otherUserPhoto: String? = null
)

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val timestamp: String,
    val status: MessageDeliveryStatus = MessageDeliveryStatus.SENT,
    val isFromMe: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
)

data class FriendItem(
    val id: String,
    val name: String,
    val username: String,
    val status: OnlineStatus,
    val bio: String,
    val mutualFriends: Int,
    val avatarInitial: String = name.take(2).uppercase()
)

data class FriendRequestItem(
    val id: String,
    val name: String,
    val username: String,
    val timeAgo: String,
    val mutualFriends: Int,
    val avatarInitial: String = name.take(2).uppercase()
)

data class GameItem(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val playersCount: String,
    val isAvailable: Boolean = true,
    val badge: String? = null
)

data class AchievementItem(
    val id: String,
    val title: String,
    val description: String,
    val iconEmoji: String,
    val isUnlocked: Boolean,
    val progress: Int,
    val maxProgress: Int
)
