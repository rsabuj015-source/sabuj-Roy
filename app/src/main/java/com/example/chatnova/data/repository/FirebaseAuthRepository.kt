package com.example.chatnova.data.repository

import android.content.Context
import com.example.chatnova.data.local.UserSessionManager
import com.example.chatnova.domain.model.OnlineStatus
import com.example.chatnova.domain.model.UserProfile
import com.example.chatnova.domain.repository.AuthRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FirebaseAuthRepository(private val context: Context) : AuthRepository {
    private val sessionManager = UserSessionManager(context)

    override fun isFirebaseConfigured(): Boolean {
        return try {
            FirebaseApp.getApps(context).isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }

    override fun isUserLoggedIn(): Boolean {
        return if (isFirebaseConfigured()) {
            try {
                FirebaseAuth.getInstance().currentUser != null || sessionManager.isLoggedIn()
            } catch (_: Exception) {
                sessionManager.isLoggedIn()
            }
        } else {
            sessionManager.isLoggedIn()
        }
    }

    override suspend fun getCurrentUser(): UserProfile? {
        val stored = sessionManager.getStoredUser()
        if (stored != null) return stored

        if (isFirebaseConfigured()) {
            try {
                val fbUser = FirebaseAuth.getInstance().currentUser ?: return null
                val doc = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(fbUser.uid)
                    .get()
                    .await()

                if (doc.exists()) {
                    val profile = UserProfile(
                        userId = fbUser.uid,
                        username = doc.getString("username") ?: "novamember",
                        displayName = doc.getString("displayName") ?: fbUser.displayName ?: "Nova Member",
                        email = fbUser.email ?: doc.getString("email") ?: "",
                        bio = doc.getString("bio") ?: "Exploring the ChatNova universe 🚀",
                        profilePhoto = doc.getString("photoUrl") ?: "p1",
                        onlineStatus = OnlineStatus.ONLINE,
                        lastSeen = "Just now",
                        friendsCount = doc.getLong("friendsCount")?.toInt() ?: 0,
                        gamesPlayed = doc.getLong("gamesPlayed")?.toInt() ?: 0,
                        winsCount = doc.getLong("winsCount")?.toInt() ?: 0,
                        achievementsCount = doc.getLong("achievementsCount")?.toInt() ?: 1,
                        joinedDate = doc.getString("createdAtFormatted") ?: "September 2026"
                    )
                    sessionManager.saveUserSession(profile)
                    return profile
                }
            } catch (_: Exception) {
                // Fallback to local session
            }
        }
        return null
    }

    override suspend fun login(email: String, password: String): Result<UserProfile> {
        val cleanEmail = email.trim().lowercase()
        val cleanPassword = password.trim()

        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        if (cleanPassword.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        }

        if (isFirebaseConfigured()) {
            try {
                val authResult = FirebaseAuth.getInstance()
                    .signInWithEmailAndPassword(cleanEmail, cleanPassword)
                    .await()
                val uid = authResult.user?.uid ?: throw IllegalStateException("User ID not returned by Firebase.")

                val doc = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .get()
                    .await()

                val profile = if (doc.exists()) {
                    UserProfile(
                        userId = uid,
                        username = doc.getString("username") ?: "novamember",
                        displayName = doc.getString("displayName") ?: authResult.user?.displayName ?: "Nova Member",
                        email = cleanEmail,
                        bio = doc.getString("bio") ?: "Exploring the ChatNova universe 🚀",
                        profilePhoto = doc.getString("photoUrl") ?: "p1",
                        onlineStatus = OnlineStatus.ONLINE,
                        lastSeen = "Just now",
                        friendsCount = doc.getLong("friendsCount")?.toInt() ?: 0,
                        gamesPlayed = doc.getLong("gamesPlayed")?.toInt() ?: 0,
                        winsCount = doc.getLong("winsCount")?.toInt() ?: 0,
                        achievementsCount = doc.getLong("achievementsCount")?.toInt() ?: 1,
                        joinedDate = doc.getString("createdAtFormatted") ?: "September 2026"
                    )
                } else {
                    val defaultProfile = UserProfile(
                        userId = uid,
                        username = cleanEmail.substringBefore("@"),
                        displayName = authResult.user?.displayName ?: cleanEmail.substringBefore("@"),
                        email = cleanEmail,
                        bio = "Exploring the ChatNova universe 🚀",
                        profilePhoto = "p1",
                        onlineStatus = OnlineStatus.ONLINE,
                        lastSeen = "Just now",
                        friendsCount = 0,
                        gamesPlayed = 0,
                        winsCount = 0,
                        achievementsCount = 1,
                        joinedDate = "September 2026"
                    )
                    // Save initial document to Firestore
                    saveToFirestore(defaultProfile)
                    defaultProfile
                }

                sessionManager.saveUserSession(profile)
                return Result.success(profile)
            } catch (e: Exception) {
                val friendlyError = mapFirebaseException(e)
                return Result.failure(Exception(friendlyError))
            }
        } else {
            // Local Persistent Authentication Fallback
            val account = sessionManager.findAccount(cleanEmail)
            if (account == null) {
                // If demo account, allow login
                if (cleanEmail == "commander@chatnova.io" && cleanPassword == "nova1234") {
                    val demoUser = UserProfile(
                        userId = "u_nova_demo",
                        username = "novamaster",
                        displayName = "Alex Vance",
                        email = cleanEmail,
                        bio = "Cosmic gamer, mobile developer & space explorer. Building the next-gen real-time web & mobile games.",
                        profilePhoto = "p1",
                        onlineStatus = OnlineStatus.ONLINE,
                        lastSeen = "Just now",
                        friendsCount = 42,
                        gamesPlayed = 118,
                        winsCount = 76,
                        achievementsCount = 6,
                        joinedDate = "September 2026"
                    )
                    sessionManager.saveUserSession(demoUser)
                    return Result.success(demoUser)
                }
                return Result.failure(Exception("No account found with this email. Please sign up first."))
            }

            val (savedPassword, profile) = account
            if (savedPassword != cleanPassword) {
                return Result.failure(Exception("Incorrect password. Please try again."))
            }

            sessionManager.saveUserSession(profile)
            return Result.success(profile)
        }
    }

    override suspend fun register(
        username: String,
        displayName: String,
        email: String,
        password: String
    ): Result<UserProfile> {
        val cleanUsername = username.trim().lowercase().filter { it.isLetterOrDigit() || it == '_' }
        val cleanDisplayName = displayName.trim().ifBlank { cleanUsername }
        val cleanEmail = email.trim().lowercase()
        val cleanPassword = password.trim()

        if (cleanUsername.length < 3) {
            return Result.failure(IllegalArgumentException("Username must be at least 3 characters."))
        }
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        if (cleanPassword.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        }

        if (isFirebaseConfigured()) {
            try {
                val firestore = FirebaseFirestore.getInstance()

                // Check username uniqueness in Cloud Firestore
                val usernameDoc = firestore.collection("usernames").document(cleanUsername).get().await()
                if (usernameDoc.exists()) {
                    return Result.failure(Exception("The username '@$cleanUsername' is already taken. Please choose another."))
                }

                // Create Firebase Auth user
                val authResult = FirebaseAuth.getInstance()
                    .createUserWithEmailAndPassword(cleanEmail, cleanPassword)
                    .await()
                val uid = authResult.user?.uid ?: throw IllegalStateException("Firebase user creation failed.")

                val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.US)
                val joinedFormatted = dateFormat.format(Date())

                val newUser = UserProfile(
                    userId = uid,
                    username = cleanUsername,
                    displayName = cleanDisplayName,
                    email = cleanEmail,
                    bio = "Exploring the ChatNova universe 🚀",
                    profilePhoto = "p1",
                    onlineStatus = OnlineStatus.ONLINE,
                    lastSeen = "Just now",
                    friendsCount = 0,
                    gamesPlayed = 0,
                    winsCount = 0,
                    achievementsCount = 1,
                    joinedDate = joinedFormatted
                )

                // Save in Firestore batch
                val batch = firestore.batch()
                val userRef = firestore.collection("users").document(uid)
                val usernameRef = firestore.collection("usernames").document(cleanUsername)

                val userMap = hashMapOf(
                    "userId" to uid,
                    "username" to cleanUsername,
                    "displayName" to cleanDisplayName,
                    "email" to cleanEmail,
                    "bio" to newUser.bio,
                    "photoUrl" to "p1",
                    "onlineStatus" to "ONLINE",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "createdAtFormatted" to joinedFormatted,
                    "friendsCount" to 0,
                    "gamesPlayed" to 0,
                    "winsCount" to 0,
                    "achievementsCount" to 1
                )

                batch.set(userRef, userMap)
                batch.set(usernameRef, hashMapOf("uid" to uid, "reservedAt" to FieldValue.serverTimestamp()))
                batch.commit().await()

                sessionManager.saveUserSession(newUser)
                return Result.success(newUser)
            } catch (e: Exception) {
                val friendly = mapFirebaseException(e)
                return Result.failure(Exception(friendly))
            }
        } else {
            // Local Persistent Storage
            if (sessionManager.findAccount(cleanEmail) != null) {
                return Result.failure(Exception("An account with this email address already exists."))
            }
            if (sessionManager.isUsernameTaken(cleanUsername)) {
                return Result.failure(Exception("The username '@$cleanUsername' is already taken. Please pick another."))
            }

            val uid = "u_${System.currentTimeMillis()}"
            val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.US)
            val joinedFormatted = dateFormat.format(Date())

            val newUser = UserProfile(
                userId = uid,
                username = cleanUsername,
                displayName = cleanDisplayName,
                email = cleanEmail,
                bio = "Exploring the ChatNova universe 🚀",
                profilePhoto = "p1",
                onlineStatus = OnlineStatus.ONLINE,
                lastSeen = "Just now",
                friendsCount = 0,
                gamesPlayed = 0,
                winsCount = 0,
                achievementsCount = 1,
                joinedDate = joinedFormatted
            )

            sessionManager.saveAccountCredentials(cleanEmail, cleanPassword, newUser)
            sessionManager.saveUserSession(newUser)
            return Result.success(newUser)
        }
    }

    override suspend fun updateProfile(
        userId: String,
        displayName: String,
        username: String,
        bio: String,
        photoPreset: String?
    ): Result<UserProfile> {
        val cleanDisplay = displayName.trim().ifBlank { username }
        val cleanUsername = username.trim().lowercase().filter { it.isLetterOrDigit() || it == '_' }
        val current = sessionManager.getStoredUser() ?: return Result.failure(IllegalStateException("No active user session."))

        if (cleanUsername.length < 3) {
            return Result.failure(IllegalArgumentException("Username must be at least 3 characters."))
        }

        if (isFirebaseConfigured()) {
            try {
                val firestore = FirebaseFirestore.getInstance()

                // If username is changing, ensure new one is free
                if (cleanUsername != current.username.lowercase()) {
                    val checkDoc = firestore.collection("usernames").document(cleanUsername).get().await()
                    if (checkDoc.exists()) {
                        return Result.failure(Exception("The username '@$cleanUsername' is already taken."))
                    }

                    // Release old username and claim new
                    val batch = firestore.batch()
                    batch.delete(firestore.collection("usernames").document(current.username.lowercase()))
                    batch.set(firestore.collection("usernames").document(cleanUsername), hashMapOf("uid" to userId))
                    batch.commit().await()
                }

                val updateMap = hashMapOf<String, Any>(
                    "displayName" to cleanDisplay,
                    "username" to cleanUsername,
                    "bio" to bio.trim(),
                    "photoUrl" to (photoPreset ?: current.profilePhoto ?: "p1")
                )
                firestore.collection("users").document(userId).set(updateMap, SetOptions.merge()).await()
            } catch (e: Exception) {
                return Result.failure(Exception("Failed to update cloud profile: ${e.localizedMessage}"))
            }
        } else {
            if (sessionManager.isUsernameTaken(cleanUsername, excludeUserId = userId)) {
                return Result.failure(Exception("The username '@$cleanUsername' is already taken."))
            }
        }

        val updated = current.copy(
            displayName = cleanDisplay,
            username = cleanUsername,
            bio = bio.trim(),
            profilePhoto = photoPreset ?: current.profilePhoto
        )
        sessionManager.saveUserSession(updated)
        return Result.success(updated)
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            if (isFirebaseConfigured()) {
                FirebaseAuth.getInstance().signOut()
            }
            sessionManager.clearSession()
            Result.success(Unit)
        } catch (e: Exception) {
            sessionManager.clearSession()
            Result.success(Unit)
        }
    }

    private suspend fun saveToFirestore(profile: UserProfile) {
        try {
            val firestore = FirebaseFirestore.getInstance()
            val userMap = hashMapOf(
                "userId" to profile.userId,
                "username" to profile.username,
                "displayName" to profile.displayName,
                "email" to profile.email,
                "bio" to profile.bio,
                "photoUrl" to (profile.profilePhoto ?: "p1"),
                "onlineStatus" to "ONLINE",
                "createdAt" to FieldValue.serverTimestamp(),
                "createdAtFormatted" to profile.joinedDate,
                "friendsCount" to profile.friendsCount,
                "gamesPlayed" to profile.gamesPlayed,
                "winsCount" to profile.winsCount,
                "achievementsCount" to profile.achievementsCount
            )
            firestore.collection("users").document(profile.userId).set(userMap, SetOptions.merge()).await()
            firestore.collection("usernames").document(profile.username.lowercase())
                .set(hashMapOf("uid" to profile.userId), SetOptions.merge()).await()
        } catch (_: Exception) {}
    }

    private fun mapFirebaseException(e: Exception): String {
        return when (e) {
            is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password. Please try again."
            is FirebaseAuthUserCollisionException -> "An account with this email address already exists. Please log in."
            is FirebaseAuthWeakPasswordException -> "Password must be at least 6 characters."
            is FirebaseNetworkException -> "Network error. Please check your internet connection."
            else -> e.message ?: "Authentication failed. Please verify your details."
        }
    }
}
