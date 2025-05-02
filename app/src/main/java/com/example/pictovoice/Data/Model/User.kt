package com.example.pictovoice.Data.Model

import com.google.firebase.firestore.DocumentSnapshot
import java.util.Date

data class User(
    val userId: String = "",
    val username: String = "",
    val fullName: String = "",
    val email: String = "",
    val role: String = "student", // "student" o "teacher"
    val createdAt: Date = Date(),
    val lastLogin: Date = Date(),
    val profileImageUrl: String = "",
    // Campos específicos de estudiantes
    val currentLevel: Int = 1,
    val currentExp: Int = 0,
    val totalExp: Int = 0,
    val unlockedCategories: List<String> = listOf("basico")
) {
    // Función para convertir a Map (útil para Firestore)
    fun toMap(): Map<String, Any?> {
        return mapOf(
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
            "unlockedCategories" to unlockedCategories
        )
    }

    companion object {
        // Función para crear User desde DocumentSnapshot
        fun fromSnapshot(snapshot: DocumentSnapshot): User? {
            return snapshot.toObject(User::class.java)?.copy(userId = snapshot.id)
        }
    }
}