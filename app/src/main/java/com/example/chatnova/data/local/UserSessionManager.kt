package com.example.chatnova.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.chatnova.domain.model.OnlineStatus
import com.example.chatnova.domain.model.UserProfile
import org.json.JSONObject

class UserSessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("chatnova_user_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_EMAIL = "email"
        private const val KEY_BIO = "bio"
        private const val KEY_PHOTO_URL = "photo_url"
        private const val KEY_JOINED_DATE = "joined_date"
        private const val KEY_FRIENDS_COUNT = "friends_count"
        private const val KEY_GAMES_PLAYED = "games_played"
        private const val KEY_WINS_COUNT = "wins_count"
        private const val KEY_ACHIEVEMENTS_COUNT = "achievements_count"
        private const val KEY_ACCOUNTS_MAP = "accounts_map_json"
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun saveUserSession(user: UserProfile) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_ID, user.userId)
            putString(KEY_USERNAME, user.username)
            putString(KEY_DISPLAY_NAME, user.displayName)
            putString(KEY_EMAIL, user.email)
            putString(KEY_BIO, user.bio)
            putString(KEY_PHOTO_URL, user.profilePhoto)
            putString(KEY_JOINED_DATE, user.joinedDate)
            putInt(KEY_FRIENDS_COUNT, user.friendsCount)
            putInt(KEY_GAMES_PLAYED, user.gamesPlayed)
            putInt(KEY_WINS_COUNT, user.winsCount)
            putInt(KEY_ACHIEVEMENTS_COUNT, user.achievementsCount)
            apply()
        }
    }

    fun getStoredUser(): UserProfile? {
        if (!isLoggedIn()) return null
        val userId = prefs.getString(KEY_USER_ID, null) ?: return null
        return UserProfile(
            userId = userId,
            username = prefs.getString(KEY_USERNAME, "novamember") ?: "novamember",
            displayName = prefs.getString(KEY_DISPLAY_NAME, "Nova Member") ?: "Nova Member",
            email = prefs.getString(KEY_EMAIL, "user@chatnova.io") ?: "user@chatnova.io",
            bio = prefs.getString(KEY_BIO, "Exploring the ChatNova universe 🚀") ?: "Exploring the ChatNova universe 🚀",
            profilePhoto = prefs.getString(KEY_PHOTO_URL, "p1"),
            onlineStatus = OnlineStatus.ONLINE,
            lastSeen = "Just now",
            friendsCount = prefs.getInt(KEY_FRIENDS_COUNT, 0),
            gamesPlayed = prefs.getInt(KEY_GAMES_PLAYED, 0),
            winsCount = prefs.getInt(KEY_WINS_COUNT, 0),
            achievementsCount = prefs.getInt(KEY_ACHIEVEMENTS_COUNT, 1),
            joinedDate = prefs.getString(KEY_JOINED_DATE, "September 2026") ?: "September 2026"
        )
    }

    fun clearSession() {
        prefs.edit().apply {
            remove(KEY_IS_LOGGED_IN)
            remove(KEY_USER_ID)
            remove(KEY_USERNAME)
            remove(KEY_DISPLAY_NAME)
            remove(KEY_EMAIL)
            remove(KEY_BIO)
            remove(KEY_PHOTO_URL)
            remove(KEY_JOINED_DATE)
            remove(KEY_FRIENDS_COUNT)
            remove(KEY_GAMES_PLAYED)
            remove(KEY_WINS_COUNT)
            remove(KEY_ACHIEVEMENTS_COUNT)
            apply()
        }
    }

    // Local Accounts Directory for offline persistence and fallback
    fun saveAccountCredentials(email: String, passwordHash: String, profile: UserProfile) {
        val jsonStr = prefs.getString(KEY_ACCOUNTS_MAP, "{}") ?: "{}"
        val json = JSONObject(jsonStr)
        val userJson = JSONObject().apply {
            put("password", passwordHash)
            put("userId", profile.userId)
            put("username", profile.username)
            put("displayName", profile.displayName)
            put("email", profile.email)
            put("bio", profile.bio)
            put("photoUrl", profile.profilePhoto ?: "p1")
            put("joinedDate", profile.joinedDate)
            put("friendsCount", profile.friendsCount)
            put("gamesPlayed", profile.gamesPlayed)
            put("winsCount", profile.winsCount)
            put("achievementsCount", profile.achievementsCount)
        }
        json.put(email.lowercase().trim(), userJson)
        prefs.edit().putString(KEY_ACCOUNTS_MAP, json.toString()).apply()
    }

    fun findAccount(email: String): Pair<String, UserProfile>? {
        val jsonStr = prefs.getString(KEY_ACCOUNTS_MAP, "{}") ?: "{}"
        val json = JSONObject(jsonStr)
        val key = email.lowercase().trim()
        if (!json.has(key)) return null
        val userJson = json.getJSONObject(key)
        val password = userJson.getString("password")
        val profile = UserProfile(
            userId = userJson.optString("userId", "u_${System.currentTimeMillis()}"),
            username = userJson.optString("username", "novamember"),
            displayName = userJson.optString("displayName", "Nova Member"),
            email = userJson.optString("email", email),
            bio = userJson.optString("bio", "Exploring the ChatNova universe 🚀"),
            profilePhoto = userJson.optString("photoUrl", "p1"),
            onlineStatus = OnlineStatus.ONLINE,
            lastSeen = "Just now",
            friendsCount = userJson.optInt("friendsCount", 0),
            gamesPlayed = userJson.optInt("gamesPlayed", 0),
            winsCount = userJson.optInt("winsCount", 0),
            achievementsCount = userJson.optInt("achievementsCount", 1),
            joinedDate = userJson.optString("joinedDate", "September 2026")
        )
        return password to profile
    }

    fun isUsernameTaken(username: String, excludeUserId: String? = null): Boolean {
        val jsonStr = prefs.getString(KEY_ACCOUNTS_MAP, "{}") ?: "{}"
        val json = JSONObject(jsonStr)
        val target = username.lowercase().trim()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val userJson = json.getJSONObject(key)
            val u = userJson.optString("username", "").lowercase().trim()
            val uid = userJson.optString("userId", "")
            if (u == target && uid != excludeUserId) {
                return true
            }
        }
        return false
    }

    fun searchLocalUsers(query: String, currentUserId: String): List<UserProfile> {
        val jsonStr = prefs.getString(KEY_ACCOUNTS_MAP, "{}") ?: "{}"
        val json = JSONObject(jsonStr)
        val q = query.lowercase().trim()
        val result = mutableListOf<UserProfile>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val userJson = json.getJSONObject(key)
            val uid = userJson.optString("userId", "")
            if (uid == currentUserId) continue

            val username = userJson.optString("username", "")
            val displayName = userJson.optString("displayName", "")
            if (q.isBlank() || username.lowercase().contains(q) || displayName.lowercase().contains(q)) {
                result.add(
                    UserProfile(
                        userId = uid,
                        username = username,
                        displayName = displayName,
                        email = userJson.optString("email", key),
                        bio = userJson.optString("bio", ""),
                        profilePhoto = userJson.optString("photoUrl", "p1"),
                        onlineStatus = OnlineStatus.ONLINE,
                        friendsCount = userJson.optInt("friendsCount", 0),
                        gamesPlayed = userJson.optInt("gamesPlayed", 0),
                        winsCount = userJson.optInt("winsCount", 0),
                        achievementsCount = userJson.optInt("achievementsCount", 1),
                        joinedDate = userJson.optString("joinedDate", "September 2026")
                    )
                )
            }
        }
        return result
    }

    fun saveLocalMessagesJson(jsonString: String) {
        prefs.edit().putString("local_cached_messages", jsonString).apply()
    }

    fun getLocalMessagesJson(): String? {
        return prefs.getString("local_cached_messages", null)
    }

    fun saveLocalConversationsJson(jsonString: String) {
        prefs.edit().putString("local_cached_conversations", jsonString).apply()
    }

    fun getLocalConversationsJson(): String? {
        return prefs.getString("local_cached_conversations", null)
    }
}
