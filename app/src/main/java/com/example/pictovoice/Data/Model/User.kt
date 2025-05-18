package com.example.pictovoice.Data.Model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import android.util.Log
import java.util.Locale // Necesario para toLowerCase(Locale.ROOT)

private const val INITIAL_DEFAULT_CATEGORY_ID = "local_comida"

data class User(
    val userId: String = "",
    val username: String = "",
    val fullName: String = "",
    val email: String = "",
    val role: String = "student",
    val createdAt: Timestamp = Timestamp.now(),
    val lastLogin: Timestamp = Timestamp.now(),
    val profileImageUrl: String = "",
    val currentLevel: Int = 1,
    val currentExp: Int = 0,
    val totalExp: Int = 0,
    val unlockedCategories: List<String> = listOf(INITIAL_DEFAULT_CATEGORY_ID),
    val hasPendingWordRequest: Boolean = false,
    val levelWordsRequestedFor: Int = 0,
    val maxContentLevelApproved: Int = 1,
    val wordsUsedCount: Int = 0,
    val phrasesCreatedCount: Int = 0,

    // --- NUEVO CAMPO PARA BÚSQUEDA CASE-INSENSITIVE ---
    val fullNameLowercase: String = ""
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "username" to username,
        "fullName" to fullName,
        "email" to email,
        "role" to role,
        "createdAt" to createdAt,
        "lastLogin" to lastLogin,
        "profileImageUrl" to profileImageUrl,
        "currentLevel" to currentLevel,
        "currentExp" to currentExp,
        "totalExp" to totalExp,
        "unlockedCategories" to unlockedCategories,
        "hasPendingWordRequest" to hasPendingWordRequest,
        "levelWordsRequestedFor" to levelWordsRequestedFor,
        "maxContentLevelApproved" to maxContentLevelApproved,
        "wordsUsedCount" to wordsUsedCount,
        "phrasesCreatedCount" to phrasesCreatedCount,
        // --- AÑADIR AL MAPA ---
        "fullNameLowercase" to fullNameLowercase
    )

    companion object {
        fun fromSnapshot(snapshot: DocumentSnapshot): User? {
            return try {
                val data = snapshot.data ?: return null
                User(
                    userId = snapshot.id,
                    username = data["username"] as? String ?: "",
                    fullName = data["fullName"] as? String ?: "",
                    email = data["email"] as? String ?: "",
                    role = data["role"] as? String ?: "student",
                    createdAt = data["createdAt"] as? Timestamp ?: Timestamp.now(),
                    lastLogin = data["lastLogin"] as? Timestamp ?: Timestamp.now(),
                    profileImageUrl = data["profileImageUrl"] as? String ?: "",
                    currentLevel = (data["currentLevel"] as? Long)?.toInt() ?: 1,
                    currentExp = (data["currentExp"] as? Long)?.toInt() ?: 0,
                    totalExp = (data["totalExp"] as? Long)?.toInt() ?: 0,
                    unlockedCategories = data["unlockedCategories"] as? List<String> ?: listOf(INITIAL_DEFAULT_CATEGORY_ID),
                    hasPendingWordRequest = data["hasPendingWordRequest"] as? Boolean ?: false,
                    levelWordsRequestedFor = (data["levelWordsRequestedFor"] as? Long)?.toInt() ?: 0,
                    maxContentLevelApproved = (data["maxContentLevelApproved"] as? Long)?.toInt() ?: 1,
                    wordsUsedCount = (data["wordsUsedCount"] as? Long)?.toInt() ?: 0,
                    phrasesCreatedCount = (data["phrasesCreatedCount"] as? Long)?.toInt() ?: 0,
                    // --- LEER DEL SNAPSHOT ---
                    fullNameLowercase = data["fullNameLowercase"] as? String ?: (data["fullName"] as? String ?: "").toLowerCase(Locale.ROOT) // Fallback si no existe aún
                )
            } catch (e: Exception) {
                Log.e("User", "Error al convertir snapshot a User: ${e.message}", e)
                null
            }
        }
    }
}