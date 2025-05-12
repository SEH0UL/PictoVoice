package com.example.pictovoice.Data.repository

import android.util.Log
import com.example.pictovoice.Data.Model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

class AuthRepository {
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    // Función para verificar si el usuario ya está logueado
    fun isUserLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    suspend fun generateUniqueUsername(fullName: String): String {
        val nameParts = fullName.split(" ").filter { it.isNotBlank() }
        if (nameParts.isEmpty()) return "USER${System.currentTimeMillis()}"

        // Para el nombre completo, usamos toUpperCase(Locale.ROOT) para asegurar consistencia
        // independientemente del Locale del dispositivo, especialmente para identificadores.
        val firstName = nameParts.first().toUpperCase(Locale.ROOT)
        val baseUsername = if (nameParts.size > 1) {
            // CORRECCIÓN: Convertir el Char a String ANTES de llamar toUpperCase(Locale)
            val lastNameInitial = nameParts.last().first().toString().toUpperCase(Locale.ROOT)
            "$firstName$lastNameInitial"
        } else {
            firstName
        }

        var finalUsername = baseUsername
        var counter = 0
        // Bucle para asegurar unicidad
        while (true) {
            // Comparamos con el username en Firestore que también debería estar en mayúsculas (según esta lógica)
            val querySnapshot = usersCollection.whereEqualTo("username", finalUsername).get().await()
            if (querySnapshot.isEmpty) {
                break // Username es único
            }
            counter++
            finalUsername = "$baseUsername$counter"
        }
        return finalUsername
    }


    // LOGIN: Ahora devuelve kotlin.Result<User>
    suspend fun login(identifier: String, password: String): kotlin.Result<User> = withContext(Dispatchers.IO) {
        try {
            var userId: String? = null
            var userEmail: String? = null

            // Paso 1: Determinar si el identifier es un email o un username
            if (identifier.contains("@")) { // Asumimos que es un email
                userEmail = identifier.toLowerCase(Locale.ROOT) // Normalizar email a minúsculas
            } else { // Asumimos que es un username
                // Buscamos el username en mayúsculas, ya que así se genera y se guarda
                val userQuery = usersCollection.whereEqualTo("username", identifier.toUpperCase(Locale.ROOT)).limit(1).get().await()
                if (userQuery.isEmpty) {
                    return@withContext kotlin.Result.failure(Exception("Usuario no encontrado."))
                }
                val foundUser = User.fromSnapshot(userQuery.documents.first())
                if (foundUser == null || foundUser.email.isBlank()) {
                    return@withContext kotlin.Result.failure(Exception("No se pudo obtener el email del usuario."))
                }
                userEmail = foundUser.email // El email ya debería estar normalizado en Firestore
            }

            if (userEmail.isNullOrBlank()) {
                return@withContext kotlin.Result.failure(Exception("Identificador de usuario no válido."))
            }

            // Paso 2: Autenticar con Firebase Auth usando el email
            val authResult = firebaseAuth.signInWithEmailAndPassword(userEmail, password).await()
            val firebaseUser = authResult.user
            if (firebaseUser == null) {
                return@withContext kotlin.Result.failure(Exception("Error de autenticación de Firebase."))
            }
            userId = firebaseUser.uid // UID de Firebase Auth

            // Paso 3: Obtener el documento del usuario de Firestore
            val userDocument = usersCollection.document(userId).get().await()
            if (!userDocument.exists()) {
                firebaseAuth.signOut()
                return@withContext kotlin.Result.failure(Exception("Datos del usuario no encontrados en la base de datos."))
            }

            val user = User.fromSnapshot(userDocument)
            if (user == null) {
                firebaseAuth.signOut()
                return@withContext kotlin.Result.failure(Exception("No se pudieron procesar los datos del usuario."))
            }

            // Opcional: Actualizar lastLogin
            usersCollection.document(userId).update("lastLogin", com.google.firebase.Timestamp.now()).await()

            kotlin.Result.success(user)

        } catch (e: FirebaseAuthInvalidUserException) {
            Log.w("AuthRepository", "Login fallido: Usuario no existe o deshabilitado.", e)
            kotlin.Result.failure(Exception("Usuario o contraseña incorrectos."))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Log.w("AuthRepository", "Login fallido: Credenciales incorrectas.", e)
            kotlin.Result.failure(Exception("Usuario o contraseña incorrectos."))
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error durante el login: ${e.message}", e)
            kotlin.Result.failure(Exception("Error durante el login: ${e.localizedMessage}"))
        }
    }

    suspend fun register(fullName: String, email: String, password: String, role: String): kotlin.Result<String> = withContext(Dispatchers.IO) {
        try {
            // Paso 1: Crear usuario en Firebase Auth (Firebase Auth maneja la normalización del email para la autenticación)
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
            if (firebaseUser == null) {
                return@withContext kotlin.Result.failure(Exception("No se pudo crear el usuario en Firebase Auth."))
            }

            // Paso 2: Generar username único (la función generateUniqueUsername ya lo devuelve en mayúsculas)
            val username = generateUniqueUsername(fullName)

            // Paso 3: Crear objeto User
            val newUser = User(
                userId = firebaseUser.uid,
                username = username, // Ya está en mayúsculas
                fullName = fullName,
                email = email.toLowerCase(Locale.ROOT), // Guardar email normalizado en Firestore
                role = role,
                createdAt = com.google.firebase.Timestamp.now(),
                lastLogin = com.google.firebase.Timestamp.now(),
                unlockedCategories = if (role == "student") listOf("basico") else emptyList()
            )

            // Paso 4: Guardar usuario en Firestore
            usersCollection.document(firebaseUser.uid).set(newUser.toMap()).await()

            kotlin.Result.success(username)

        } catch (e: Exception) {
            Log.e("AuthRepository", "Error durante el registro: ${e.message}", e)
            // Considerar la limpieza si el usuario se creó en Auth pero falló en Firestore
            // firebaseUser?.delete()?.await() // Esto requiere reautenticación reciente y es complejo de manejar aquí.
            kotlin.Result.failure(Exception("Error durante el registro: ${e.localizedMessage}"))
        }
    }

    fun logout() {
        firebaseAuth.signOut()
    }
}