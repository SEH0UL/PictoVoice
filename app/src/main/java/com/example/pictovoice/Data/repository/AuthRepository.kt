package com.example.pictovoice.Data.repository

import android.util.Log
import android.util.Patterns // Para chequear si el identifier es un email
import com.example.pictovoice.Data.Model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Locale // Para normalizar a mayúsculas/minúsculas

class AuthRepository {
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    fun isUserLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    // --- LOGIN DUAL (EMAIL O USERNAME) ---
    suspend fun login(identifier: String, password: String): kotlin.Result<String?> { // Devuelve UID o null
        return try {
            var emailToUse: String? = null

            // Estrategia 1: Comprobar si el identifier parece un email
            if (Patterns.EMAIL_ADDRESS.matcher(identifier).matches()) {
                emailToUse = identifier
                Log.d("AuthRepository", "Login attempt with identifier as email: $identifier")
            } else {
                // Estrategia 2: Si no es un email, buscar por username en Firestore
                Log.d("AuthRepository", "Identifier not an email, searching for username: $identifier")
                val querySnapshot = usersCollection.whereEqualTo("username", identifier.toUpperCase(Locale.ROOT)).limit(1).get().await()
                if (!querySnapshot.isEmpty) {
                    val userDoc = querySnapshot.documents.first()
                    emailToUse = userDoc.getString("email")
                    Log.d("AuthRepository", "Username found. Email to use for auth: $emailToUse")
                } else {
                    Log.d("AuthRepository", "Username '$identifier' not found in Firestore.")
                    // Si no es email y no se encuentra como username, podría ser un intento de login con un email mal formado
                    // o un username inexistente. Intentamos como email por si acaso era un email sin '@' etc.
                    // Opcionalmente, podrías fallar aquí directamente si exiges que los usernames no parezcan emails.
                    // Por ahora, si no se encontró username, no se hace nada más con Firestore y se dependerá de si emailToUse (que sería null)
                    // causa un fallo más adelante, o si el 'identifier' original era un email que Firebase pueda procesar.
                }
            }

            if (emailToUse.isNullOrEmpty() && !Patterns.EMAIL_ADDRESS.matcher(identifier).matches()) {
                // Si no pudimos determinar un email y el identificador original no era un email, fallamos.
                return kotlin.Result.failure(Exception("Usuario o correo no válido."))
            }

            val finalEmailForAuth = emailToUse ?: identifier // Usar el email encontrado o el identificador original si era un email

            // Autenticar con Firebase Auth
            val authResult = firebaseAuth.signInWithEmailAndPassword(finalEmailForAuth, password).await()
            val firebaseUser = authResult.user

            if (firebaseUser != null) {
                usersCollection.document(firebaseUser.uid).update("lastLogin", com.google.firebase.Timestamp.now()).await()
                kotlin.Result.success(firebaseUser.uid)
            } else {
                kotlin.Result.failure(Exception("Error de autenticación."))
            }

        } catch (e: Exception) {
            Log.e("AuthRepository", "Error en login: ${e.message}", e)
            kotlin.Result.failure(Exception("Usuario, correo o contraseña incorrectos."))
        }
    }

    // --- REGISTER CON GENERACIÓN DE USERNAME ---
    // Devuelve kotlin.Result<String> donde String es el username generado
    suspend fun register(fullName: String, email: String, password: String, role: String): kotlin.Result<String> {
        try {
            // 1. Verificar si el email ya está en uso en Auth (Firestore check es opcional aquí)
            val emailQueryAuth = firebaseAuth.fetchSignInMethodsForEmail(email).await()
            if (emailQueryAuth.signInMethods != null && emailQueryAuth.signInMethods!!.isNotEmpty()) {
                return kotlin.Result.failure(Exception("El correo electrónico ya está registrado."))
            }

            // 2. Generar el nombre de usuario y asegurar unicidad
            val generatedUsername = generateUniqueUsername(fullName)

            // 3. Crear usuario en Firebase Authentication
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
                ?: return kotlin.Result.failure(Exception("No se pudo crear el usuario en Firebase Auth."))

            // 4. Actualizar perfil de Firebase Auth (opcional)
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(generatedUsername) // Usar el username generado
                .build()
            firebaseUser.updateProfile(profileUpdates).await()

            // 5. Crear objeto User para Firestore
            val newUser = User(
                userId = firebaseUser.uid,
                username = generatedUsername, // Guardar el username generado y normalizado
                fullName = fullName,
                email = email,
                role = role,
                createdAt = com.google.firebase.Timestamp.now(),
                lastLogin = com.google.firebase.Timestamp.now()
            )

            // 6. Guardar usuario en Firestore
            usersCollection.document(firebaseUser.uid).set(newUser.toMap()).await()
            return kotlin.Result.success(generatedUsername) // Devolver el username generado

        } catch (e: FirebaseAuthUserCollisionException) {
            Log.w("AuthRepository", "Error de registro: Email ya en uso (debería haber sido capturado antes). ${e.message}")
            return kotlin.Result.failure(Exception("El correo electrónico ya está registrado."))
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error en registro: ${e.message}", e)
            return kotlin.Result.failure(Exception("Error durante el registro: ${e.message}"))
        }
    }

    // Función para generar username único
    private suspend fun generateUniqueUsername(fullName: String): String {
        val parts = fullName.trim().split(" ").filter { it.isNotBlank() }
        val namePart = if (parts.isNotEmpty()) parts[0].take(3) else "USR"
        val lastNamePart = if (parts.size > 1) parts[1].take(3) else namePart.takeLast(3) // Usa nombre si no hay apellido

        val nameLength = fullName.replace(" ", "").length

        var baseUsername = (namePart + lastNamePart + nameLength).toUpperCase(Locale.ROOT)
        var finalUsername = baseUsername
        var suffix = 0

        // Bucle para asegurar unicidad
        while (true) {
            val querySnapshot = usersCollection.whereEqualTo("username", finalUsername).limit(1).get().await()
            if (querySnapshot.isEmpty) {
                break // Username es único
            }
            suffix++
            finalUsername = "$baseUsername$suffix"
        }
        return finalUsername
    }
}