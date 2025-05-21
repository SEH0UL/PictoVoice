package com.example.pictovoice.Data.repository // o com.example.pictovoice.Data.repository

import User
import android.util.Log
import com.example.pictovoice.Data.datasource.PictogramDataSource
import com.google.firebase.Timestamp

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser // Import explícito para claridad
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.min

/**
 * Repositorio encargado de todas las operaciones relacionadas con la autenticación
 * de usuarios (Firebase Authentication) y la creación/gestión inicial de
 * sus perfiles en Firestore durante el registro.
 */
class AuthRepository {
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    /**
     * Verifica si hay un usuario actualmente autenticado en Firebase.
     * @return `true` si hay un usuario logueado, `false` en caso contrario.
     */
    fun isUserLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    /**
     * Genera un nombre de usuario único basado en el nombre completo proporcionado.
     * El formato base es (Primeras 3 letras del Nombre)(Primeras 3 letras del Primer Apellido) + LongitudTotalDelNombreCompletoSinEspacios.
     * Añade un contador numérico si el username base ya existe para asegurar la unicidad.
     * Todos los usernames se almacenan y comparan en mayúsculas.
     *
     * @param fullName El nombre completo del usuario.
     * @return Un [String] con el nombre de usuario único generado, en mayúsculas.
     */
    suspend fun generateUniqueUsername(fullName: String): String = withContext(Dispatchers.IO) {
        val normalizedFullName = fullName.trim().replace("\\s+".toRegex(), " ")
        val nameParts = normalizedFullName.split(" ").filter { it.isNotBlank() }

        if (nameParts.isEmpty()) {
            var fallbackUsername = "USER${System.currentTimeMillis()}"
            // Asegurar unicidad también para el fallback
            while (usersCollection.whereEqualTo("username", fallbackUsername.toUpperCase(Locale.ROOT)).get().await().isEmpty.not()) {
                fallbackUsername = "USER${System.currentTimeMillis()}${(0..9).random()}"
            }
            return@withContext fallbackUsername.toUpperCase(Locale.ROOT)
        }

        val firstNamePart = nameParts.first().toUpperCase(Locale.ROOT)
        val extractedFirstName = firstNamePart.substring(0, min(3, firstNamePart.length))

        val firstLastNamePart = if (nameParts.size > 1) {
            nameParts[1].toUpperCase(Locale.ROOT) // Tomamos la segunda parte como el primer apellido
        } else {
            "" // Si solo hay una palabra en el nombre, no hay "primer apellido" separado
        }
        val extractedFirstLastName = firstLastNamePart.substring(0, min(3, firstLastNamePart.length))

        val nameCode = extractedFirstName + extractedFirstLastName
        val fullNameNoSpaces = normalizedFullName.replace(" ", "")
        val lengthSuffix = fullNameNoSpaces.length.toString()
        val baseUsernameAttempt = nameCode + lengthSuffix

        var finalUsername = baseUsernameAttempt
        var counter = 0
        while (true) {
            val usernameToCheck = if (counter == 0) finalUsername else "$baseUsernameAttempt$counter"
            // Las comparaciones y el almacenamiento de usernames son en mayúsculas
            val querySnapshot = usersCollection.whereEqualTo("username", usernameToCheck.toUpperCase(Locale.ROOT)).get().await()
            if (querySnapshot.isEmpty) {
                finalUsername = usernameToCheck
                break
            }
            counter++
            if (counter > 999) { // Límite para prevenir bucles infinitos en casos extremos
                Log.e("AuthRepository", "No se pudo generar un username único para '$fullName' después de $counter intentos.")
                // Fallback de emergencia aún más robusto
                var emergencyUsername = "U${System.currentTimeMillis()}"
                while (usersCollection.whereEqualTo("username", emergencyUsername.toUpperCase(Locale.ROOT)).get().await().isEmpty.not()) {
                    emergencyUsername = "U${System.currentTimeMillis()}${(0..9).random()}"
                }
                return@withContext emergencyUsername.toUpperCase(Locale.ROOT)
            }
        }
        return@withContext finalUsername.toUpperCase(Locale.ROOT)
    }

    /**
     * Realiza el proceso de login del usuario.
     * Permite el login usando el correo electrónico o el nombre de usuario.
     *
     * @param identifier Correo electrónico o nombre de usuario. Si no contiene "@", se asume que es un nombre de usuario.
     * Los nombres de usuario se comparan en mayúsculas.
     * @param password Contraseña del usuario.
     * @return Un [kotlin.Result] que encapsula el objeto [User] si el login es exitoso,
     * o una excepción en caso de fallo.
     */
    suspend fun login(identifier: String, password: String): kotlin.Result<User> = withContext(Dispatchers.IO) {
        try {
            val userEmail: String? = if (identifier.contains("@")) {
                identifier.toLowerCase(Locale.ROOT)
            } else {
                // Búsqueda de usuario por username (case-insensitive implícito al guardar/buscar en mayúsculas)
                val userQuery = usersCollection.whereEqualTo("username", identifier.toUpperCase(Locale.ROOT))
                    .limit(1).get().await()
                if (userQuery.isEmpty) {
                    return@withContext kotlin.Result.failure(IllegalArgumentException("Usuario no encontrado con el identificador proporcionado."))
                }
                val foundUserDoc = userQuery.documents.first()
                User.fromSnapshot(foundUserDoc)?.email // Obtener email del usuario encontrado
            }

            if (userEmail.isNullOrBlank()) {
                return@withContext kotlin.Result.failure(IllegalArgumentException("Identificador de usuario no válido o email no encontrado para el username."))
            }

            val authResult = firebaseAuth.signInWithEmailAndPassword(userEmail, password).await()
            val firebaseUser: FirebaseUser = authResult.user
                ?: return@withContext kotlin.Result.failure(IllegalStateException("Error de autenticación de Firebase: usuario nulo."))

            val userDocument = usersCollection.document(firebaseUser.uid).get().await()
            if (!userDocument.exists()) {
                Log.w("AuthRepository", "Usuario autenticado con UID ${firebaseUser.uid} pero sin documento en Firestore. Deslogueando.")
                firebaseAuth.signOut()
                return@withContext kotlin.Result.failure(IllegalStateException("Datos del usuario no encontrados en la base de datos."))
            }

            val user = User.fromSnapshot(userDocument)
                ?: return@withContext kotlin.Result.failure(IllegalStateException("No se pudieron procesar los datos del usuario desde Firestore."))

            // Actualizar lastLogin
            usersCollection.document(firebaseUser.uid).update("lastLogin", Timestamp.now()).await()
            kotlin.Result.success(user)

        } catch (e: FirebaseAuthInvalidUserException) {
            Log.w("AuthRepository", "Login fallido: Usuario no existe en Firebase Auth o deshabilitado.", e)
            kotlin.Result.failure(Exception("Usuario o contraseña incorrectos."))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Log.w("AuthRepository", "Login fallido: Credenciales incorrectas.", e)
            kotlin.Result.failure(Exception("Usuario o contraseña incorrectos."))
        } catch (e: IllegalArgumentException) { // Capturar excepciones de validación
            Log.w("AuthRepository", "Login fallido: ${e.message}", e)
            kotlin.Result.failure(e)
        }
        catch (e: Exception) {
            Log.e("AuthRepository", "Error inesperado durante el login: ${e.message}", e)
            kotlin.Result.failure(Exception("Error durante el login: ${e.localizedMessage ?: "Error desconocido"}"))
        }
    }

    /**
     * Registra un nuevo usuario en Firebase Authentication y crea su perfil en Firestore.
     *
     * @param fullName Nombre completo del usuario.
     * @param email Correo electrónico del usuario.
     * @param password Contraseña para el nuevo usuario (debe cumplir los requisitos de Firebase Auth).
     * @param role Rol del nuevo usuario ("student" o "teacher").
     * @return Un [kotlin.Result] que encapsula el nombre de usuario [String] generado si el registro es exitoso,
     * o una excepción en caso de fallo.
     */
    suspend fun register(fullName: String, email: String, password: String, role: String): kotlin.Result<String> = withContext(Dispatchers.IO) {
        try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser: FirebaseUser = authResult.user
                ?: return@withContext kotlin.Result.failure(IllegalStateException("No se pudo crear el usuario en Firebase Auth: usuario nulo."))

            val username = generateUniqueUsername(fullName) // Ya devuelve en mayúsculas
            val normalizedFullName = fullName.trim().replace("\\s+".toRegex(), " ")
            val initialUserCategories = if (role == "student") listOf(PictogramDataSource.CATEGORY_ID_COMIDA) else emptyList()


            val newUser = User(
                userId = firebaseUser.uid,
                username = username, // Se guarda en mayúsculas
                fullName = normalizedFullName,
                fullNameLowercase = normalizedFullName.toLowerCase(Locale.ROOT),
                email = email.toLowerCase(Locale.ROOT),
                role = role,
                createdAt = Timestamp.now(),
                lastLogin = Timestamp.now(),
                unlockedCategories = initialUserCategories,
                maxContentLevelApproved = if (role == "student") 1 else 0 // Nivel 1 de contenido aprobado por defecto para alumnos
                // los demás campos numéricos y booleanos se inicializan con sus valores por defecto del Data class User
            )

            usersCollection.document(firebaseUser.uid).set(newUser.toMap()).await()
            kotlin.Result.success(username)

        } catch (e: Exception) {
            Log.e("AuthRepository", "Error durante el registro: ${e.message}", e)
            // Intentar eliminar el usuario de Firebase Auth si la creación del perfil en Firestore falla
            // para evitar usuarios huérfanos en Auth.
            firebaseAuth.currentUser?.delete()?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("AuthRepository", "Usuario de Auth eliminado después de fallo en registro de Firestore.")
                } else {
                    Log.w("AuthRepository", "Fallo al eliminar usuario de Auth después de error en Firestore.", task.exception)
                }
            }
            kotlin.Result.failure(Exception("Error durante el registro: ${e.localizedMessage ?: "Error desconocido"}"))
        }
    }

    /**
     * Cierra la sesión del usuario actualmente autenticado.
     */
    fun logout() {
        firebaseAuth.signOut()
        Log.d("AuthRepository", "Usuario ha cerrado sesión.")
    }
}