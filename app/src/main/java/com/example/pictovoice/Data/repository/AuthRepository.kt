package com.example.pictovoice.Data.repository

import com.example.pictovoice.Data.Model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.lang.Exception

class AuthRepository {
    private val auth: FirebaseAuth = Firebase.auth
    private val db = Firebase.firestore

    suspend fun login(username: String, password: String): Result<User> {
        return try {
            val email = "$username@pictovoice.com"
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val userDoc = db.collection("users").document(authResult.user?.uid ?: "").get().await()
            val user = User.fromSnapshot(userDoc) ?: throw Exception("User data not found")

            // Actualizar lastLogin
            db.collection("users").document(user.userId)
                .update("lastLogin", com.google.firebase.Timestamp.now()).await()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(
        fullName: String,
        username: String,
        password: String,
        role: String = "student"
    ): Result<User> {
        return try {
            // 1. Crear usuario en Firebase Auth
            val email = "$username@pictovoice.com"
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid ?: throw Exception("User creation failed")

            // 2. Crear documento en Firestore
            val newUser = User(
                userId = userId,
                username = username,
                fullName = fullName,
                email = email,
                role = role
            )

            db.collection("users").document(userId)
                .set(newUser.toMap())
                .await()

            Result.success(newUser)
        } catch (e: Exception) {
            // Si falla Firestore, eliminar el usuario de Auth para mantener consistencia
            auth.currentUser?.delete()?.await()
            Result.failure(e)
        }
    }

    fun getCurrentUser() = auth.currentUser
}