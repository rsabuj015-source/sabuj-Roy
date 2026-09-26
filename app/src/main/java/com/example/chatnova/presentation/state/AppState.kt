package com.example.chatnova.presentation.state

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatnova.data.repository.FirebaseAuthRepository
import com.example.chatnova.data.repository.FirebaseChatRepository
import com.example.chatnova.domain.model.AchievementItem
import com.example.chatnova.domain.model.AppLanguage
import com.example.chatnova.domain.model.ChatMessage
import com.example.chatnova.domain.model.ConversationItem
import com.example.chatnova.domain.model.FriendItem
import com.example.chatnova.domain.model.FriendRequestItem
import com.example.chatnova.domain.model.GameItem
import com.example.chatnova.domain.model.MessageDeliveryStatus
import com.example.chatnova.domain.model.OnlineStatus
import com.example.chatnova.domain.model.ThemeMode
import com.example.chatnova.domain.model.UserProfile
import com.example.chatnova.domain.repository.AuthRepository
import com.example.chatnova.domain.repository.ChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ScreenRoute {
    SPLASH,
    ONBOARDING,
    LOGIN,
    REGISTER,
    MAIN_APP,
    SETTINGS
}

enum class MainTab {
    HOME,
    CHATS,
    GAMES,
    FRIENDS,
    PROFILE
}

data class ChatNovaUiState(
    val currentRoute: ScreenRoute = ScreenRoute.SPLASH,
    val selectedTab: MainTab = MainTab.HOME,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val isAuthenticated: Boolean = false,
    val isAuthLoading: Boolean = false,
    val authErrorMessage: String? = null,
    val isFirebaseActive: Boolean = false,
    val isOnline: Boolean = true,
    val pendingSyncCount: Int = 0,
    val activeDraft: String = "",
    val currentUser: UserProfile = UserProfile(
        userId = "u_nova_1",
        username = "novamaster",
        displayName = "Alex Vance",
        email = "alex.vance@chatnova.io",
        bio = "Cosmic gamer, mobile developer & space explorer. Building the next-gen real-time web & mobile games.",
        onlineStatus = OnlineStatus.ONLINE,
        friendsCount = 42,
        gamesPlayed = 118,
        winsCount = 76,
        achievementsCount = 6
    ),
    val conversations: List<ConversationItem> = listOf(
        ConversationItem(
            id = "c1",
            title = "Luna Starlight",
            lastMessage = "Ready for the Tic Tac Toe rematch?",
            timestamp = "10:42 AM",
            unreadCount = 2,
            isGroup = false,
            onlineStatus = OnlineStatus.ONLINE,
            deliveryStatus = MessageDeliveryStatus.READ
        ),
        ConversationItem(
            id = "c2",
            title = "Nova Gaming Squad",
            lastMessage = "Marcus: Who's hosting the tournament tonight?",
            timestamp = "09:15 AM",
            unreadCount = 5,
            isGroup = true,
            onlineStatus = OnlineStatus.ONLINE,
            deliveryStatus = MessageDeliveryStatus.DELIVERED
        ),
        ConversationItem(
            id = "c3",
            title = "David Kim",
            lastMessage = "Check out the new game strategy I posted.",
            timestamp = "Yesterday",
            unreadCount = 0,
            isGroup = false,
            onlineStatus = OnlineStatus.PLAYING,
            deliveryStatus = MessageDeliveryStatus.READ
        ),
        ConversationItem(
            id = "c4",
            title = "Amina Rahman",
            lastMessage = "কেমন আছো? চ্যাটে কথা বলি চলো!",
            timestamp = "Yesterday",
            unreadCount = 1,
            isGroup = false,
            onlineStatus = OnlineStatus.ONLINE,
            deliveryStatus = MessageDeliveryStatus.SENT
        ),
        ConversationItem(
            id = "c5",
            title = "Orion Team",
            lastMessage = "Server sync architecture is verified.",
            timestamp = "Sep 24",
            unreadCount = 0,
            isGroup = true,
            onlineStatus = OnlineStatus.OFFLINE,
            deliveryStatus = MessageDeliveryStatus.READ
        )
    ),
    val activeConversationId: String? = null,
    val conversationMessages: Map<String, List<ChatMessage>> = mapOf(
        "c1" to listOf(
            ChatMessage("m1_1", "c1", "u_luna", "Luna Starlight", "Hey Alex! Excited for the ChatNova launch 🚀", "10:35 AM", MessageDeliveryStatus.READ, isFromMe = false),
            ChatMessage("m1_2", "c1", "u_nova_1", "Alex Vance", "Hey Luna! Yes, offline messaging and games are working great.", "10:38 AM", MessageDeliveryStatus.READ, isFromMe = true),
            ChatMessage("m1_3", "c1", "u_luna", "Luna Starlight", "Awesome! Ready for the Tic Tac Toe rematch?", "10:42 AM", MessageDeliveryStatus.READ, isFromMe = false)
        ),
        "c2" to listOf(
            ChatMessage("m2_1", "c2", "u_elena", "Elena", "The new cosmic arena map is live!", "09:05 AM", MessageDeliveryStatus.READ, isFromMe = false),
            ChatMessage("m2_2", "c2", "u_marcus", "Marcus", "Marcus: Who's hosting the tournament tonight?", "09:15 AM", MessageDeliveryStatus.DELIVERED, isFromMe = false)
        ),
        "c3" to listOf(
            ChatMessage("m3_1", "c3", "u_david", "David Kim", "Check out the new game strategy I posted.", "Yesterday", MessageDeliveryStatus.READ, isFromMe = false)
        ),
        "c4" to listOf(
            ChatMessage("m4_1", "c4", "u_amina", "Amina Rahman", "কেমন আছো? চ্যাটে কথা বলি চলো!", "Yesterday", MessageDeliveryStatus.SENT, isFromMe = false)
        ),
        "c5" to listOf(
            ChatMessage("m5_1", "c5", "u_orion", "Orion Team", "Server sync architecture is verified.", "Sep 24", MessageDeliveryStatus.READ, isFromMe = false)
        )
    ),
    val friends: List<FriendItem> = listOf(
        FriendItem(
            id = "f1",
            name = "Luna Starlight",
            username = "starlight_luna",
            status = OnlineStatus.ONLINE,
            bio = "Competitive Tic Tac Toe champion 🌟",
            mutualFriends = 14
        ),
        FriendItem(
            id = "f2",
            name = "David Kim",
            username = "dkim_player",
            status = OnlineStatus.PLAYING,
            bio = "Master tactician & board game lover ♟️",
            mutualFriends = 8
        ),
        FriendItem(
            id = "f3",
            name = "Amina Rahman",
            username = "amina_r",
            status = OnlineStatus.ONLINE,
            bio = "ঢাকা থেকে চ্যাটনোভার সাথে যুক্ত আছি!",
            mutualFriends = 5
        ),
        FriendItem(
            id = "f4",
            name = "Marcus Brody",
            username = "brody_speed",
            status = OnlineStatus.AWAY,
            bio = "Level 99 Nova Challenger 🚀",
            mutualFriends = 21
        ),
        FriendItem(
            id = "f5",
            name = "Elena Rostova",
            username = "elena_cosmo",
            status = OnlineStatus.OFFLINE,
            bio = "Designing games for the future 🪐",
            mutualFriends = 3
        )
    ),
    val friendRequests: List<FriendRequestItem> = listOf(
        FriendRequestItem(
            id = "fr1",
            name = "Kai Henderson",
            username = "kai_h",
            timeAgo = "15m ago",
            mutualFriends = 4
        ),
        FriendRequestItem(
            id = "fr2",
            name = "Tanvir Ahmed",
            username = "tanvir_nova",
            timeAgo = "2h ago",
            mutualFriends = 7
        )
    ),
    val games: List<GameItem> = listOf(
        GameItem(
            id = "g_ttt",
            title = "Tic Tac Toe",
            category = "Strategy / 2 Players",
            description = "Classic 3x3 turn-based board challenge. Play online with a friend or challenge the AI bot.",
            playersCount = "1v1 Online",
            isAvailable = true,
            badge = "Active"
        ),
        GameItem(
            id = "g_chess",
            title = "Nova Chess",
            category = "Grand Strategy",
            description = "Timed blitz and rapid multiplayer chess tournaments with automated move validation.",
            playersCount = "1v1 Online",
            isAvailable = false,
            badge = "Phase 10"
        ),
        GameItem(
            id = "g_connect4",
            title = "Connect Four",
            category = "Arcade Puzzle",
            description = "Drop discs to connect 4 in a row vertically, horizontally, or diagonally before your opponent.",
            playersCount = "1v1 Online",
            isAvailable = false,
            badge = "Upcoming"
        ),
        GameItem(
            id = "g_trivia",
            title = "Nova Arena Quiz",
            category = "Multiplayer Trivia",
            description = "Compete with up to 8 players in live trivia across gaming, pop culture, science, and tech.",
            playersCount = "2-8 Players",
            isAvailable = false,
            badge = "Upcoming"
        )
    ),
    val achievements: List<AchievementItem> = listOf(
        AchievementItem(
            id = "a1",
            title = "Nova Pioneer",
            description = "Joined the ChatNova universe during launch phase",
            iconEmoji = "🚀",
            isUnlocked = true,
            progress = 1,
            maxProgress = 1
        ),
        AchievementItem(
            id = "a2",
            title = "First Victory",
            description = "Won your first multiplayer match in Tic Tac Toe",
            iconEmoji = "🏆",
            isUnlocked = true,
            progress = 1,
            maxProgress = 1
        ),
        AchievementItem(
            id = "a3",
            title = "Social Butterfly",
            description = "Connected with 25 friends across ChatNova",
            iconEmoji = "🤝",
            isUnlocked = true,
            progress = 25,
            maxProgress = 25
        ),
        AchievementItem(
            id = "a4",
            title = "Centurion",
            description = "Play 100 multiplayer matches",
            iconEmoji = "⚔️",
            isUnlocked = true,
            progress = 100,
            maxProgress = 100
        ),
        AchievementItem(
            id = "a5",
            title = "Grandmaster",
            description = "Win 100 multiplayer matches",
            iconEmoji = "👑",
            isUnlocked = false,
            progress = 76,
            maxProgress = 100
        )
    )
)

class ChatNovaViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository: AuthRepository = FirebaseAuthRepository(application)
    private val chatRepository: ChatRepository = FirebaseChatRepository(application)
    private var conversationsJob: Job? = null
    private var activeMessagesJob: Job? = null

    private val _uiState = MutableStateFlow(ChatNovaUiState())
    val uiState: StateFlow<ChatNovaUiState> = _uiState.asStateFlow()

    init {
        val isConfigured = authRepository.isFirebaseConfigured()
        _uiState.update { it.copy(isFirebaseActive = isConfigured) }

        viewModelScope.launch {
            chatRepository.getNetworkStatusFlow().collect { online ->
                _uiState.update { it.copy(isOnline = online) }
            }
        }

        viewModelScope.launch {
            chatRepository.getPendingCountFlow().collect { count ->
                _uiState.update { it.copy(pendingSyncCount = count) }
            }
        }

        viewModelScope.launch {
            if (authRepository.isUserLoggedIn()) {
                val stored = authRepository.getCurrentUser()
                if (stored != null) {
                    _uiState.update {
                        it.copy(
                            isAuthenticated = true,
                            currentUser = stored,
                            currentRoute = ScreenRoute.MAIN_APP,
                            selectedTab = MainTab.HOME
                        )
                    }
                    subscribeToConversations(stored.userId)
                }
            } else {
                subscribeToConversations(_uiState.value.currentUser.userId)
            }
        }
    }

    private fun subscribeToConversations(userId: String) {
        conversationsJob?.cancel()
        conversationsJob = viewModelScope.launch {
            chatRepository.getConversationsFlow(userId).collect { convs ->
                _uiState.update { state ->
                    val list = if (convs.isNotEmpty()) convs else state.conversations
                    state.copy(conversations = list)
                }
            }
        }
    }

    fun navigateTo(route: ScreenRoute) {
        _uiState.update { it.copy(currentRoute = route) }
    }

    fun selectTab(tab: MainTab) {
        _uiState.update { it.copy(selectedTab = tab, currentRoute = ScreenRoute.MAIN_APP) }
    }

    fun setThemeMode(mode: ThemeMode) {
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun setLanguage(language: AppLanguage) {
        _uiState.update { it.copy(language = language) }
    }

    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        _uiState.update { it.copy(isAuthLoading = true, authErrorMessage = null) }
        viewModelScope.launch {
            val result = authRepository.login(email, password)
            _uiState.update { it.copy(isAuthLoading = false) }
            result.onSuccess { user ->
                _uiState.update {
                    it.copy(
                        isAuthenticated = true,
                        currentUser = user,
                        currentRoute = ScreenRoute.MAIN_APP,
                        selectedTab = MainTab.HOME,
                        authErrorMessage = null
                    )
                }
                subscribeToConversations(user.userId)
                onResult(true, null)
            }.onFailure { error ->
                val msg = error.message ?: "Authentication failed. Please verify credentials."
                _uiState.update { it.copy(authErrorMessage = msg) }
                onResult(false, msg)
            }
        }
    }

    fun register(
        username: String,
        displayName: String,
        email: String,
        password: String,
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        _uiState.update { it.copy(isAuthLoading = true, authErrorMessage = null) }
        viewModelScope.launch {
            val result = authRepository.register(username, displayName, email, password)
            _uiState.update { it.copy(isAuthLoading = false) }
            result.onSuccess { user ->
                _uiState.update {
                    it.copy(
                        isAuthenticated = true,
                        currentUser = user,
                        currentRoute = ScreenRoute.MAIN_APP,
                        selectedTab = MainTab.HOME,
                        authErrorMessage = null
                    )
                }
                subscribeToConversations(user.userId)
                onResult(true, null)
            }.onFailure { error ->
                val msg = error.message ?: "Registration failed. Please try again."
                _uiState.update { it.copy(authErrorMessage = msg) }
                onResult(false, msg)
            }
        }
    }

    fun loginAsGuest() {
        val guestUser = UserProfile(
            userId = "u_guest_${System.currentTimeMillis()}",
            username = "guest_nova",
            displayName = "Guest Commander",
            email = "guest@chatnova.io",
            bio = "Exploring ChatNova as Guest Pioneer 🚀",
            onlineStatus = OnlineStatus.ONLINE,
            friendsCount = 0,
            gamesPlayed = 0,
            winsCount = 0,
            achievementsCount = 1,
            joinedDate = "September 2026"
        )
        _uiState.update {
            it.copy(
                isAuthenticated = true,
                currentUser = guestUser,
                currentRoute = ScreenRoute.MAIN_APP,
                selectedTab = MainTab.HOME
            )
        }
        subscribeToConversations(guestUser.userId)
    }

    fun logout() {
        viewModelScope.launch {
            conversationsJob?.cancel()
            activeMessagesJob?.cancel()
            authRepository.logout()
            _uiState.update {
                it.copy(
                    isAuthenticated = false,
                    currentRoute = ScreenRoute.LOGIN,
                    activeConversationId = null
                )
            }
        }
    }

    fun updateProfile(displayName: String, username: String, bio: String, photoPreset: String? = null) {
        val uid = _uiState.value.currentUser.userId
        viewModelScope.launch {
            val result = authRepository.updateProfile(uid, displayName, username, bio, photoPreset)
            result.onSuccess { updated ->
                _uiState.update { it.copy(currentUser = updated) }
            }.onFailure {
                val fallback = _uiState.value.currentUser.copy(
                    displayName = displayName.ifBlank { _uiState.value.currentUser.displayName },
                    username = username.ifBlank { _uiState.value.currentUser.username },
                    bio = bio,
                    profilePhoto = photoPreset ?: _uiState.value.currentUser.profilePhoto
                )
                _uiState.update { it.copy(currentUser = fallback) }
            }
        }
    }

    fun acceptFriendRequest(requestId: String) {
        _uiState.update { state ->
            val req = state.friendRequests.find { it.id == requestId }
            val newFriends = if (req != null) {
                state.friends + FriendItem(
                    id = "f_${req.id}",
                    name = req.name,
                    username = req.username,
                    status = OnlineStatus.ONLINE,
                    bio = "Recently connected via ChatNova",
                    mutualFriends = req.mutualFriends
                )
            } else state.friends
            state.copy(
                friendRequests = state.friendRequests.filterNot { it.id == requestId },
                friends = newFriends
            )
        }
    }

    fun declineFriendRequest(requestId: String) {
        _uiState.update { state ->
            state.copy(friendRequests = state.friendRequests.filterNot { it.id == requestId })
        }
    }

    fun openConversation(id: String) {
        _uiState.update { state ->
            val updatedConversations = state.conversations.map {
                if (it.id == id) it.copy(unreadCount = 0) else it
            }
            state.copy(
                activeConversationId = id,
                conversations = updatedConversations
            )
        }

        activeMessagesJob?.cancel()
        activeMessagesJob = viewModelScope.launch {
            val currentUserId = _uiState.value.currentUser.userId
            chatRepository.getMessagesFlow(id, currentUserId, limit = 50).collect { messages ->
                _uiState.update { state ->
                    state.copy(
                        conversationMessages = state.conversationMessages + (id to messages)
                    )
                }
            }
        }
    }

    fun closeConversation() {
        activeMessagesJob?.cancel()
        activeMessagesJob = null
        _uiState.update { it.copy(activeConversationId = null) }
    }

    fun sendMessage(conversationId: String, text: String) {
        if (text.isBlank()) return
        val trimmed = text.trim()
        val user = _uiState.value.currentUser
        viewModelScope.launch {
            chatRepository.sendMessage(conversationId, user.userId, user.displayName, trimmed)
        }
    }

    fun startDirectChat(otherUser: UserProfile) {
        val current = _uiState.value.currentUser
        viewModelScope.launch {
            val result = chatRepository.getOrCreateDirectConversation(
                currentUserId = current.userId,
                currentUserName = current.displayName,
                currentUserPhoto = current.profilePhoto,
                otherUser = otherUser
            )
            result.onSuccess { conv ->
                openConversation(conv.id)
            }
        }
    }

    fun searchUsers(query: String, onResult: (List<UserProfile>) -> Unit) {
        val currentUserId = _uiState.value.currentUser.userId
        viewModelScope.launch {
            val res = chatRepository.searchUsers(query, currentUserId)
            onResult(res.getOrDefault(emptyList()))
        }
    }

    fun startNewConversation(friend: FriendItem) {
        val profile = UserProfile(
            userId = friend.id,
            username = friend.username,
            displayName = friend.name,
            email = "${friend.username}@chatnova.io",
            bio = friend.bio,
            onlineStatus = friend.status
        )
        startDirectChat(profile)
    }

    fun retryMessage(messageId: String) {
        viewModelScope.launch {
            chatRepository.retryMessage(messageId)
        }
    }

    fun saveDraft(conversationId: String, text: String) {
        viewModelScope.launch {
            chatRepository.saveDraft(conversationId, text)
        }
    }

    fun clearDraft(conversationId: String) {
        viewModelScope.launch {
            chatRepository.clearDraft(conversationId)
        }
    }
}
