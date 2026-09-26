package com.example.chatnova.presentation.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chatnova.core.theme.ChatNovaTheme
import com.example.chatnova.domain.model.FriendItem
import com.example.chatnova.domain.model.ThemeMode
import com.example.chatnova.features.auth.LoginScreen
import com.example.chatnova.features.auth.OnboardingScreen
import com.example.chatnova.features.auth.RegisterScreen
import com.example.chatnova.features.auth.SplashScreen
import com.example.chatnova.features.chat.ChatConversationScreen
import com.example.chatnova.features.chat.ChatsScreen
import com.example.chatnova.features.friends.FriendsScreen
import com.example.chatnova.features.games.GamesScreen
import com.example.chatnova.features.home.HomeScreen
import com.example.chatnova.features.profile.ProfileScreen
import com.example.chatnova.features.settings.SettingsScreen
import com.example.chatnova.features.shared.NovaBottomNavigationBar
import com.example.chatnova.presentation.state.ChatNovaViewModel
import com.example.chatnova.presentation.state.MainTab
import com.example.chatnova.presentation.state.ScreenRoute

@Composable
fun ChatNovaApp(
    viewModel: ChatNovaViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val isDarkTheme = when (uiState.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    ChatNovaTheme(darkTheme = isDarkTheme) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Adaptive max-width container for tablets / foldables / desktop
            Box(modifier = Modifier.fillMaxSize().widthIn(max = 680.dp)) {
                AnimatedContent(
                    targetState = uiState.currentRoute,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "screen_route_transition"
                ) { route ->
                    when (route) {
                        ScreenRoute.SPLASH -> {
                            SplashScreen(
                                onSplashFinished = {
                                    if (uiState.isAuthenticated) {
                                        viewModel.navigateTo(ScreenRoute.MAIN_APP)
                                    } else {
                                        viewModel.navigateTo(ScreenRoute.ONBOARDING)
                                    }
                                },
                                language = uiState.language
                            )
                        }

                        ScreenRoute.ONBOARDING -> {
                            OnboardingScreen(
                                onFinish = { viewModel.navigateTo(ScreenRoute.LOGIN) },
                                language = uiState.language
                            )
                        }

                        ScreenRoute.LOGIN -> {
                            LoginScreen(
                                onLogin = { email, password, onError ->
                                    viewModel.login(email, password) { success, errorMsg ->
                                        if (!success && errorMsg != null) {
                                            onError(errorMsg)
                                        }
                                    }
                                },
                                onNavigateToRegister = { viewModel.navigateTo(ScreenRoute.REGISTER) },
                                onExploreAsGuest = { viewModel.loginAsGuest() },
                                language = uiState.language,
                                isLoading = uiState.isAuthLoading
                            )
                        }

                        ScreenRoute.REGISTER -> {
                            RegisterScreen(
                                onRegister = { username, displayName, email, password, onError ->
                                    viewModel.register(username, displayName, email, password) { success, errorMsg ->
                                        if (!success && errorMsg != null) {
                                            onError(errorMsg)
                                        }
                                    }
                                },
                                onNavigateToLogin = { viewModel.navigateTo(ScreenRoute.LOGIN) },
                                language = uiState.language,
                                isLoading = uiState.isAuthLoading
                            )
                        }

                        ScreenRoute.SETTINGS -> {
                            SettingsScreen(
                                uiState = uiState,
                                onBack = { viewModel.navigateTo(ScreenRoute.MAIN_APP) },
                                onSetThemeMode = { viewModel.setThemeMode(it) },
                                onSetLanguage = { viewModel.setLanguage(it) },
                                onLogout = { viewModel.logout() }
                            )
                        }

                        ScreenRoute.MAIN_APP -> {
                            val activeConv = uiState.conversations.find { it.id == uiState.activeConversationId }
                            if (activeConv != null) {
                                val messages = uiState.conversationMessages[activeConv.id] ?: emptyList()
                                ChatConversationScreen(
                                    conversation = activeConv,
                                    messages = messages,
                                    onSendMessage = { text -> viewModel.sendMessage(activeConv.id, text) },
                                    onBack = { viewModel.closeConversation() }
                                )
                            } else {
                                BackHandler(enabled = uiState.selectedTab != MainTab.HOME) {
                                    viewModel.selectTab(MainTab.HOME)
                                }

                                Scaffold(
                                    bottomBar = {
                                        NovaBottomNavigationBar(
                                            selectedTab = uiState.selectedTab,
                                            onTabSelected = { viewModel.selectTab(it) },
                                            language = uiState.language
                                        )
                                    }
                                ) { paddingValues ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(paddingValues)
                                    ) {
                                        when (uiState.selectedTab) {
                                            MainTab.HOME -> {
                                                HomeScreen(
                                                    uiState = uiState,
                                                    onNavigateToTab = { viewModel.selectTab(it) },
                                                    onOpenSettings = { viewModel.navigateTo(ScreenRoute.SETTINGS) },
                                                    onOpenConversation = { convId -> viewModel.openConversation(convId) }
                                                )
                                            }

                                            MainTab.CHATS -> {
                                                ChatsScreen(
                                                    uiState = uiState,
                                                    onOpenConversation = { convId -> viewModel.openConversation(convId) },
                                                    onStartNewChat = { friend -> viewModel.startNewConversation(friend) },
                                                    onStartDirectChat = { user -> viewModel.startDirectChat(user) },
                                                    onSearchUsers = { query, onResult -> viewModel.searchUsers(query, onResult) }
                                                )
                                            }

                                            MainTab.GAMES -> {
                                                GamesScreen(uiState = uiState)
                                            }

                                            MainTab.FRIENDS -> {
                                                FriendsScreen(
                                                    uiState = uiState,
                                                    onAcceptRequest = { viewModel.acceptFriendRequest(it) },
                                                    onDeclineRequest = { viewModel.declineFriendRequest(it) },
                                                    onStartChatWithFriend = { friend -> viewModel.startNewConversation(friend) }
                                                )
                                            }

                                            MainTab.PROFILE -> {
                                                ProfileScreen(
                                                    uiState = uiState,
                                                    onOpenSettings = { viewModel.navigateTo(ScreenRoute.SETTINGS) },
                                                    onLogout = { viewModel.logout() },
                                                    onUpdateProfile = { displayName, username, bio, photoPreset ->
                                                        viewModel.updateProfile(displayName, username, bio, photoPreset)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
