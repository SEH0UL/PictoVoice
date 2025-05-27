package com.example.pictovoice.Data.repository // o com.example.pictovoice.Data.repository

import User
import android.util.Log
import com.example.pictovoice.Data.datasource.PictogramDataSource // Importar para las constantes de ID
import com.example.pictovoice.Data.model.Classroom
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Repositorio para interactuar con la base de datos Cloud Firestore.
 * Maneja operaciones CRUD para usuarios, clases y otras entidades de la aplicación.
 */
class FirestoreRepository {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")
    private val classesCollection = db.collection("classes")
    // La colección 'pictograms' y 'categories' ya no se gestionan directamente aquí
    // para el flujo principal del alumno, ya que se usa PictogramDataSource.

    // Mapeo de niveles a los IDs de las categorías que se desbloquean.
    // Las constantes de ID de categoría se obtienen de PictogramDataSource para consistencia.
    private val levelUnlockMap: Map<Int, List<String>> = mapOf(
        2 to listOf(PictogramDataSource.CATEGORY_ID_ANIMALES),
        3 to listOf(PictogramDataSource.CATEGORY_ID_SENTIMIENTOS),
        4 to listOf(PictogramDataSource.CATEGORY_ID_NUMEROS),
        5 to listOf(PictogramDataSource.CATEGORY_ID_AFICIONES),
        6 to listOf(PictogramDataSource.CATEGORY_ID_LUGARES),
        7 to listOf(PictogramDataSource.CATEGORY_ID_VERBOS_2),
        8 to listOf(PictogramDataSource.CATEGORY_ID_CHARLA_RAPIDA),
        9 to listOf(PictogramDataSource.CATEGORY_ID_FRASES_HECHAS),
        10 to listOf(PictogramDataSource.CATEGORY_ID_OBJETOS),
    )

    /**
     * Obtiene los datos de un usuario específico desde Firestore.
     * @param userId El ID del usuario a obtener.
     * @return Un [kotlin.Result] que contiene el objeto [User] o null si no se encuentra,
     * o una excepción en caso de error.
     */
    suspend fun getUser(userId: String): kotlin.Result<User?> = withContext(Dispatchers.IO) {
        try {
            val snapshot = usersCollection.document(userId).get().await()
            if (!snapshot.exists()) {
                Log.w("FirestoreRepo", "No se encontró el documento del usuario con ID: $userId")
                return@withContext kotlin.Result.success(null)
            }
            kotlin.Result.success(User.fromSnapshot(snapshot))
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error obteniendo usuario $userId: ${e.message}", e)
            kotlin.Result.failure(e)
        }
    }

    /**
     * Configura un listener en tiempo real para el documento de un usuario específico.
     * Notifica a través de los callbacks cualquier actualización del documento o error.
     *
     * @param userId El ID del usuario a escuchar.
     * @param onUpdate Callback que se invoca con el objeto [User] actualizado (o null si se elimina/no existe).
     * @param onError Callback que se invoca si ocurre un error durante la escucha.
     * @return Una [ListenerRegistration] que puede usarse para detener el listener.
     */
    fun addUserDocumentListener(
        userId: String,
        onUpdate: (User?) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        val userDocRef = usersCollection.document(userId)
        return userDocRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("FirestoreRepo", "Escucha fallida para usuario $userId.", e)
                onError(e)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                Log.d("FirestoreRepo", "Actualización de documento de usuario recibida para $userId")
                onUpdate(User.fromSnapshot(snapshot))
            } else {
                Log.d("FirestoreRepo", "Datos actuales: nulos para usuario $userId (documento podría haber sido eliminado)")
                onUpdate(null)
            }
        }
    }

    /**
     * Registra la solicitud de palabras de un alumno en Firestore.
     * Establece `hasPendingWordRequest` a `true` y `levelWordsRequestedFor` al nivel actual del alumno.
     *
     * @param userId El ID del alumno que realiza la solicitud.
     * @param requestedLevel El nivel actual del alumno para el cual se solicitan las palabras.
     * @return Un [kotlin.Result] indicando éxito o fallo.
     */
    suspend fun recordStudentWordRequest(userId: String, requestedLevel: Int): kotlin.Result<Unit> = withContext(Dispatchers.IO) {
        try {
            usersCollection.document(userId)
                .update(mapOf(
                    "hasPendingWordRequest" to true,
                    "levelWordsRequestedFor" to requestedLevel.toLong() // Firestore espera Long para números si no se especifica
                ))
                .await()
            Log.d("FirestoreRepo", "Solicitud de palabras registrada para usuario $userId en nivel $requestedLevel")
            kotlin.Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error registrando solicitud de palabras para usuario $userId", e)
            kotlin.Result.failure(e)
        }
    }

    /**
     * Aprueba la solicitud de palabras de un alumno y actualiza el nivel máximo de contenido aprobado.
     * Establece `hasPendingWordRequest` a `false` y `maxContentLevelApproved` al nivel aprobado.
     *
     * @param userId El ID del alumno cuya solicitud se aprueba.
     * @param levelApproved El nivel de contenido que se está aprobando.
     * @return Un [kotlin.Result] indicando éxito o fallo.
     */
    suspend fun approveWordRequestAndSetContentLevel(userId: String, levelApproved: Int): kotlin.Result<Unit> = withContext(Dispatchers.IO) {
        try {
            usersCollection.document(userId)
                .update(mapOf(
                    "hasPendingWordRequest" to false,
                    "maxContentLevelApproved" to levelApproved.toLong() // Firestore espera Long
                ))
                .await()
            Log.d("FirestoreRepo", "Solicitud de palabras aprobada para usuario $userId y maxContentLevelApproved establecido a $levelApproved")
            kotlin.Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error aprobando solicitud / estableciendo nivel de contenido para $userId", e)
            kotlin.Result.failure(e)
        }
    }

    /**
     * Guarda o actualiza una clase en Firestore.
     * Si `classData.classId` está vacío, se crea un nuevo documento; si no, se actualiza el existente.
     *
     * @param classData El objeto [Classroom] a guardar o actualizar.
     * @return Un [kotlin.Result] indicando éxito o fallo.
     */
    suspend fun saveClass(classData: Classroom): kotlin.Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val docRef = if (classData.classId.isBlank()) {
                classesCollection.document()
            } else {
                classesCollection.document(classData.classId)
            }
            // Asegurar que el objeto a guardar tiene el ID correcto si es uno nuevo
            val classToSave = if (classData.classId.isBlank()) classData.copy(classId = docRef.id) else classData
            docRef.set(classToSave.toMap()).await()
            Log.d("FirestoreRepo", "Clase guardada/actualizada: ${classToSave.classId}")
            kotlin.Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error guardando clase ${classData.className}", e)
            kotlin.Result.failure(e)
        }
    }

    /**
     * Obtiene todas las clases creadas por un profesor específico.
     * @param teacherId El ID del profesor.
     * @return Un [kotlin.Result] que contiene una lista de [Classroom] o una excepción.
     */
    suspend fun getClassesByTeacher(teacherId: String): kotlin.Result<List<Classroom>> = withContext(Dispatchers.IO) {
        try {
            val querySnapshot = classesCollection
                .whereEqualTo("teacherId", teacherId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await()
            val classes = querySnapshot.documents.mapNotNull { Classroom.fromSnapshot(it) }
            kotlin.Result.success(classes)
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error obteniendo clases para profesor $teacherId", e)
            kotlin.Result.failure(e)
        }
    }

    /**
     * Elimina una clase de Firestore.
     * @param classId El ID de la clase a eliminar.
     * @return Un [kotlin.Result] indicando éxito o fallo.
     */
    suspend fun deleteClass(classId: String): kotlin.Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (classId.isBlank()) {
                Log.e("FirestoreRepo", "Intento de eliminar clase con ID vacío.")
                return@withContext kotlin.Result.failure(IllegalArgumentException("El ID de la clase no puede estar vacío."))
            }
            classesCollection.document(classId).delete().await()
            Log.d("FirestoreRepo", "Clase eliminada con ID: $classId")
            kotlin.Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error eliminando clase $classId", e)
            kotlin.Result.failure(e)
        }
    }

    /**
     * Obtiene los detalles de una clase específica por su ID.
     * @param classId El ID de la clase.
     * @return Un [kotlin.Result] que contiene el [Classroom] o null si no se encuentra, o una excepción.
     */
    suspend fun getClassDetails(classId: String): kotlin.Result<Classroom?> = withContext(Dispatchers.IO) {
        try {
            if (classId.isBlank()) {
                return@withContext kotlin.Result.failure(IllegalArgumentException("El ID de la clase no puede estar vacío."))
            }
            val snapshot = classesCollection.document(classId).get().await()
            kotlin.Result.success(Classroom.fromSnapshot(snapshot))
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error obteniendo detalles de clase $classId", e)
            kotlin.Result.failure(e)
        }
    }

    /**
     * Obtiene una lista de usuarios basada en una lista de IDs de usuario.
     * Útil para obtener los perfiles de los alumnos de una clase.
     * @param userIds Lista de IDs de los usuarios a obtener.
     * @return Un [kotlin.Result] que contiene una lista de [User] o una excepción.
     */
    suspend fun getUsersByIds(userIds: List<String>): kotlin.Result<List<User>> = withContext(Dispatchers.IO) {
        if (userIds.isEmpty()) {
            return@withContext kotlin.Result.success(emptyList())
        }
        try {
            // Firestore limita las consultas 'in' a 30 elementos por defecto (anteriormente 10).
            // Si se esperan más de 30 IDs, se necesitaría dividir la consulta en lotes.
            if (userIds.size > 30) {
                Log.w("FirestoreRepo", "La lista de userIds (${userIds.size}) para getUsersByIds excede el límite de 30. Considerar paginación o múltiples consultas.")
                // Implementar lógica de chunking (dividir en lotes de 30) si es un caso común y necesario.
            }
            val querySnapshot = usersCollection.whereIn(FieldPath.documentId(), userIds).get().await()
            val users = querySnapshot.documents.mapNotNull { User.fromSnapshot(it) }
            kotlin.Result.success(users)
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error obteniendo usuarios por IDs", e)
            kotlin.Result.failure(e)
        }
    }

    /**
     * Elimina un alumno de una clase específica actualizando la lista `studentIds` de la clase.
     * @param classId El ID de la clase.
     * @param studentId El ID del alumno a eliminar.
     * @return Un [kotlin.Result] indicando éxito o fallo.
     */
    suspend fun removeStudentFromClassroom(classId: String, studentId: String): kotlin.Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (classId.isBlank() || studentId.isBlank()) {
                return@withContext kotlin.Result.failure(IllegalArgumentException("IDs de clase y alumno no pueden estar vacíos."))
            }
            classesCollection.document(classId)
                .update("studentIds", FieldValue.arrayRemove(studentId))
                .await()
            Log.d("FirestoreRepo", "Alumno $studentId eliminado de la clase $classId")
            kotlin.Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error eliminando alumno $studentId de clase $classId", e)
            kotlin.Result.failure(e)
        }
    }

    /**
     * Añade un alumno a una clase específica actualizando la lista `studentIds` de la clase.
     * @param classId El ID de la clase.
     * @param studentId El ID del alumno a añadir.
     * @return Un [kotlin.Result] indicando éxito o fallo.
     */
    suspend fun addStudentToClass(classId: String, studentId: String): kotlin.Result<Unit> = withContext(Dispatchers.IO) {
        try {
            classesCollection.document(classId)
                .update("studentIds", FieldValue.arrayUnion(studentId)).await()
            Log.d("FirestoreRepo", "Alumno $studentId añadido a la clase $classId")
            kotlin.Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error añadiendo alumno $studentId a clase $classId", e)
            kotlin.Result.failure(e)
        }
    }

    /**
     * Busca alumnos por nombre (búsqueda de prefijo insensible a mayúsculas/minúsculas)
     * para la funcionalidad de añadir alumnos a una clase.
     * Requiere un índice compuesto en Firestore: `users` (colección), `role` (asc), `fullNameLowercase` (asc).
     *
     * @param nameQuery El término de búsqueda (prefijo del nombre).
     * @param limit Número máximo de resultados a devolver.
     * @return Un [kotlin.Result] que contiene una lista de [User] coincidentes o una excepción.
     */
    suspend fun searchStudentsByName(
        nameQuery: String,
        limit: Long = 20
    ): kotlin.Result<List<User>> = withContext(Dispatchers.IO) {
        if (nameQuery.isBlank()) {
            return@withContext kotlin.Result.success(emptyList())
        }
        try {
            val lowercaseQuery = nameQuery.trim().toLowerCase(Locale.ROOT)
            Log.d("FirestoreRepo", "Buscando alumnos con query: '$lowercaseQuery'")
            val querySnapshot = usersCollection
                .whereEqualTo("role", "student")
                .orderBy("fullNameLowercase")
                .startAt(lowercaseQuery)
                .endAt(lowercaseQuery + '\uf8ff') // Carácter Unicode alto para simular "endsWith" en prefijos
                .limit(limit)
                .get()
                .await()

            val students = querySnapshot.documents.mapNotNull { User.fromSnapshot(it) }
            Log.d("FirestoreRepo", "Búsqueda insensible de alumnos por '$lowercaseQuery' encontró ${students.size} resultados.")
            kotlin.Result.success(students)
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error buscando alumnos (insensible) por nombre '$nameQuery': ${e.message}", e)
            kotlin.Result.failure(e)
        }
    }

    /**
     * Incrementa el contador de frases creadas para un usuario.
     * @param userId El ID del usuario.
     * @return Un [kotlin.Result] indicando éxito o fallo.
     */
    suspend fun incrementPhrasesCreatedCount(userId: String): kotlin.Result<Unit> = withContext(Dispatchers.IO) {
        try {
            usersCollection.document(userId).update("phrasesCreatedCount", FieldValue.increment(1)).await()
            Log.d("FirestoreRepo", "Incrementado phrasesCreatedCount para usuario $userId")
            kotlin.Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error incrementando phrasesCreatedCount para usuario $userId", e)
            kotlin.Result.failure(e)
        }
    }

    /**
     * Incrementa el contador de palabras usadas para un usuario.
     * @param userId El ID del usuario.
     * @param count La cantidad a incrementar.
     * @return Un [kotlin.Result] indicando éxito o fallo.
     */
    suspend fun incrementWordsUsedCount(userId: String, count: Int): kotlin.Result<Unit> = withContext(Dispatchers.IO) {
        if (count <= 0) {
            Log.d("FirestoreRepo", "No se incrementa wordsUsedCount para usuario $userId porque count es $count.")
            return@withContext kotlin.Result.success(Unit)
        }
        try {
            usersCollection.document(userId).update("wordsUsedCount", FieldValue.increment(count.toLong())).await()
            Log.d("FirestoreRepo", "Incrementado wordsUsedCount por $count para usuario $userId")
            kotlin.Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error incrementando wordsUsedCount para usuario $userId", e)
            kotlin.Result.failure(e)
        }
    }

    /**
     * Añade experiencia a un alumno, actualiza su nivel y desbloquea categorías si corresponde.
     * Esta operación se realiza dentro de una transacción de Firestore.
     *
     * @param studentId El ID del alumno.
     * @param expToAdd La cantidad de experiencia a añadir.
     * @return Un [kotlin.Result] que contiene un par con la nueva EXP actual y el nuevo Nivel, o una excepción.
     */
    suspend fun addExperienceToStudent(studentId: String, expToAdd: Int): kotlin.Result<Pair<Int, Int>> = withContext(Dispatchers.IO) {
        try {
            val userDocRef = usersCollection.document(studentId)
            val resultPair: Pair<Int, Int> = db.runTransaction { transaction ->
                val snapshot = transaction.get(userDocRef)
                if (!snapshot.exists()) {
                    Log.e("FirestoreRepo", "Documento de usuario $studentId no encontrado en transacción addExperience.")
                    throw Exception("Documento de usuario $studentId no encontrado")
                }
                val currentExp = snapshot.getLong("currentExp")?.toInt() ?: 0
                val totalExp = snapshot.getLong("totalExp")?.toInt() ?: 0
                val currentLevel = snapshot.getLong("currentLevel")?.toInt() ?: 1

                val newTotalExp = totalExp + expToAdd
                var newCurrentExp = currentExp + expToAdd
                var newLevel = currentLevel
                val initialLevelBeforeLoop = currentLevel

                // Lógica de subida de nivel (ej. 1000 EXP por nivel)
                var expNeededForNextLevel = newLevel * 1000

                while (newCurrentExp >= expNeededForNextLevel && newLevel < 100) { // Límite de nivel 100
                    newCurrentExp -= expNeededForNextLevel
                    newLevel++
                    expNeededForNextLevel = newLevel * 1000
                }
                // Ajustar EXP si se alcanza el nivel máximo y se excede
                if (newLevel >= 100 && newCurrentExp >= expNeededForNextLevel) {
                    newCurrentExp = expNeededForNextLevel -1
                }
                transaction.update(userDocRef, "currentExp", newCurrentExp)
                transaction.update(userDocRef, "currentExp", newCurrentExp.toLong())
                transaction.update(userDocRef, "totalExp", newTotalExp.toLong())
                transaction.update(userDocRef, "currentLevel", newLevel.toLong())

                // Lógica de Desbloqueo de Categorías si el nivel ha aumentado
                if (newLevel > initialLevelBeforeLoop) {
                    Log.d("FirestoreRepo", "Usuario $studentId subió de nivel: $initialLevelBeforeLoop -> $newLevel. Comprobando desbloqueo de categorías.")
                    for (levelReached in (initialLevelBeforeLoop + 1)..newLevel) {
                        levelUnlockMap[levelReached]?.forEach { categoryIdToUnlock ->
                            Log.d("FirestoreRepo", "Desbloqueando carpeta de categoría '$categoryIdToUnlock' para usuario $studentId en nivel $levelReached")
                            transaction.update(userDocRef, "unlockedCategories", FieldValue.arrayUnion(categoryIdToUnlock))
                        }
                    }
                }
                Pair(newCurrentExp, newLevel)
            }.await()
            kotlin.Result.success(resultPair)
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error añadiendo experiencia al alumno $studentId: ${e.message}", e)
            kotlin.Result.failure(e)
        }
    }
}