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
import kotlin.math.min // Necesario para kotlin.math.min

class AuthRepository {
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    fun isUserLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    suspend fun generateUniqueUsername(fullName: String): String {
        val normalizedFullName = fullName.trim().replace("\\s+".toRegex(), " ")
        val nameParts = normalizedFullName.split(" ").filter { it.isNotBlank() }

        if (nameParts.isEmpty()) {
            // Fallback si el nombre está vacío o solo espacios
            var fallbackUsername = "USER${System.currentTimeMillis()}"
            while (usersCollection.whereEqualTo("username", fallbackUsername.toUpperCase(Locale.ROOT)).get().await().isEmpty.not()) {
                fallbackUsername = "USER${System.currentTimeMillis()}${ (0..9).random() }" // Añadir un poco más de aleatoriedad
            }
            return fallbackUsername.toUpperCase(Locale.ROOT)
        }

        val firstNamePart = nameParts.first().toUpperCase(Locale.ROOT)
        val extractedFirstName = firstNamePart.substring(0, min(3, firstNamePart.length))

        val firstLastNamePart = if (nameParts.size > 1) {
            nameParts[1].toUpperCase(Locale.ROOT) // Usamos la segunda parte como el primer apellido
        } else {
            "" // No hay segundo apellido si solo hay un nombre/palabra
        }
        val extractedFirstLastName = firstLastNamePart.substring(0, min(3, firstLastNamePart.length))

        val nameCode = extractedFirstName + extractedFirstLastName

        val fullNameNoSpaces = normalizedFullName.replace(" ", "")
        val lengthSuffix = fullNameNoSpaces.length.toString()

        val baseUsernameAttempt = nameCode + lengthSuffix // Ejemplo: DAVLUN13

        var finalUsername = baseUsernameAttempt
        var counter = 0
        // Bucle para asegurar unicidad, comparando en mayúsculas
        while (true) {
            val usernameToCheck = if (counter == 0) finalUsername else "$baseUsernameAttempt$counter"
            val querySnapshot = usersCollection.whereEqualTo("username", usernameToCheck.toUpperCase(Locale.ROOT)).get().await()
            if (querySnapshot.isEmpty) {
                finalUsername = usernameToCheck // Username es único
                break
            }
            counter++
            if (counter > 999) { // Límite para evitar bucles infinitos en casos extremos
                Log.e("AuthRepository", "Could not generate unique username for $fullName after 999 tries.")
                // Fallback aún más robusto si es necesario, o lanzar excepción
                var emergencyUsername = "U${System.currentTimeMillis()}"
                while (usersCollection.whereEqualTo("username", emergencyUsername.toUpperCase(Locale.ROOT)).get().await().isEmpty.not()) {
                    emergencyUsername = "U${System.currentTimeMillis()}${ (0..9).random() }"
                }
                return emergencyUsername.toUpperCase(Locale.ROOT)
            }
        }
        return finalUsername.toUpperCase(Locale.ROOT) // Guardar y devolver en mayúsculas
    }

    suspend fun login(identifier: String, password: String): kotlin.Result<User> = withContext(Dispatchers.IO) {
        try {
            var userEmail: String?

            if (identifier.contains("@")) {
                userEmail = identifier.toLowerCase(Locale.ROOT)
            } else {
                // Los usernames se guardan/comparan en MAYÚSCULAS
                val userQuery = usersCollection.whereEqualTo("username", identifier.toUpperCase(Locale.ROOT)).limit(1).get().await()
                if (userQuery.isEmpty) {
                    return@withContext kotlin.Result.failure(Exception("Usuario no encontrado."))
                }
                val foundUser = User.fromSnapshot(userQuery.documents.first())
                if (foundUser == null || foundUser.email.isBlank()) {
                    return@withContext kotlin.Result.failure(Exception("No se pudo obtener el email del usuario."))
                }
                userEmail = foundUser.email
            }

            if (userEmail.isNullOrBlank()) {
                return@withContext kotlin.Result.failure(Exception("Identificador de usuario no válido."))
            }

            val authResult = firebaseAuth.signInWithEmailAndPassword(userEmail, password).await()
            val firebaseUser = authResult.user
                ?: return@withContext kotlin.Result.failure(Exception("Error de autenticación de Firebase."))

            val userId = firebaseUser.uid

            val userDocument = usersCollection.document(userId).get().await()
            if (!userDocument.exists()) {
                firebaseAuth.signOut() // Importante: desloguear si no hay datos en Firestore
                return@withContext kotlin.Result.failure(Exception("Datos del usuario no encontrados en la base de datos."))
            }

            val user = User.fromSnapshot(userDocument)
            if (user == null) {
                firebaseAuth.signOut()
                return@withContext kotlin.Result.failure(Exception("No se pudieron procesar los datos del usuario."))
            }

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
            kotlin.Result.failure(Exception("Error durante el login: ${e.localizedMessage ?: "Error desconocido"}"))
        }
    }

    suspend fun register(fullName: String, email: String, password: String, role: String): kotlin.Result<String> = withContext(Dispatchers.IO) {
        try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
                ?: return@withContext kotlin.Result.failure(Exception("No se pudo crear el usuario en Firebase Auth."))

            // Generar username único con la NUEVA lógica
            val username = generateUniqueUsername(fullName) // Ya se devuelve en mayúsculas

            val newUser = User(
                userId = firebaseUser.uid,
                username = username, // Almacenar en mayúsculas
                fullName = fullName.trim().replace("\\s+".toRegex(), " "), // Guardar nombre normalizado
                email = email.toLowerCase(Locale.ROOT),
                role = role,
                createdAt = com.google.firebase.Timestamp.now(),
                lastLogin = com.google.firebase.Timestamp.now(),
                unlockedCategories = if (role == "student") listOf("local_comida") else emptyList() // Usar la constante si es posible
            )

            usersCollection.document(firebaseUser.uid).set(newUser.toMap()).await()
            kotlin.Result.success(username)

        } catch (e: Exception) {
            Log.e("AuthRepository", "Error durante el registro: ${e.message}", e)
            firebaseAuth.currentUser?.delete()?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("AuthRepository", "User deleted from Auth after Firestore registration failure.")
                } else {
                    Log.w("AuthRepository", "Failed to delete user from Auth after Firestore registration failure.", task.exception)
                }
            }
            kotlin.Result.failure(Exception("Error durante el registro: ${e.localizedMessage ?: "Error desconocido"}"))
        }
    }

    fun logout() {
        firebaseAuth.signOut()
    }
}