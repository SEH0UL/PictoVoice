package com.example.pictovoice.Data.repository

import android.util.Log
import com.example.pictovoice.Data.Model.Category
import com.example.pictovoice.Data.Model.Classroom
import com.example.pictovoice.Data.Model.Pictogram
import com.example.pictovoice.Data.Model.User
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

// Constantes de Category ID (idealmente, desde un archivo común, aquí duplicadas/definidas para el repositorio)
private const val CATEGORY_ID_COMIDA = "local_comida"
private const val CATEGORY_ID_ANIMALES = "local_animales"
private const val CATEGORY_ID_SENTIMIENTOS = "local_sentimientos"
private const val CATEGORY_ID_NUMEROS = "local_numeros"
private const val CATEGORY_ID_AFICIONES = "local_aficiones"
private const val CATEGORY_ID_LUGARES = "local_lugares"
private const val CATEGORY_ID_VERBOS_2 = "local_verbos_2"
private const val CATEGORY_ID_CHARLA_RAPIDA = "local_charla_rapida"
private const val CATEGORY_ID_FRASES_HECHAS = "local_frases_hechas"
private const val CATEGORY_ID_OBJETOS = "local_objetos"
private const val CATEGORY_ID_ACCIONES = "local_acciones"


class FirestoreRepository {
    private val db = FirebaseFirestore.getInstance()

    private val usersCollection = db.collection("users")
    private val pictogramsCollection = db.collection("pictograms")
    private val classesCollection = db.collection("classes")
    private val categoriesCollection = db.collection("categories")


    private val levelUnlockMap: Map<Int, List<String>> = mapOf(
        // Nivel 1 ya tiene CATEGORY_ID_COMIDA por defecto en User.kt
        2 to listOf(CATEGORY_ID_ANIMALES),
        3 to listOf(CATEGORY_ID_SENTIMIENTOS),
        4 to listOf(CATEGORY_ID_NUMEROS),
        5 to listOf(CATEGORY_ID_AFICIONES),
        6 to listOf(CATEGORY_ID_LUGARES),
        7 to listOf(CATEGORY_ID_VERBOS_2),
        8 to listOf(CATEGORY_ID_CHARLA_RAPIDA),
        9 to listOf(CATEGORY_ID_FRASES_HECHAS),
        10 to listOf(CATEGORY_ID_OBJETOS),
        11 to listOf(CATEGORY_ID_ACCIONES)
    )

    suspend fun saveUser(user: User): kotlin.Result<Unit> = withContext(Dispatchers.IO) {
        try {
            usersCollection.document(user.userId).set(user.toMap()).await()
            kotlin.Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error guardando usuario ${user.userId}", e)
            kotlin.Result.failure(e)
        }
    }

    suspend fun getUser(userId: String): kotlin.Result<User?> = withContext(Dispatchers.IO) {
        try {
            val snapshot = usersCollection.document(userId).get().await()
            if (snapshot.exists()) {
                kotlin.Result.success(User.fromSnapshot(snapshot))
            } else {
                Log.w("FirestoreRepo", "Usuario no encontrado con ID: $userId")
                kotlin.Result.success(null)
            }
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error obteniendo usuario $userId", e)
            kotlin.Result.failure(e)
        }
    }

    suspend fun updateUserWordRequestStatus(userId: String, hasRequested: Boolean): kotlin.Result<Unit> = try {
        usersCollection.document(userId)
            .update("hasPendingWordRequest", hasRequested)
            .await()
        kotlin.Result.success(Unit)
    } catch (e: Exception) {
        Log.e("FirestoreRepo", "Error updating word request status for user $userId", e)
        kotlin.Result.failure(e)
    }

    suspend fun saveClass(classData: Classroom): kotlin.Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val docRef = if (classData.classId.isBlank()) {
                classesCollection.document()
            } else {
                classesCollection.document(classData.classId)
            }
            val classToSave = if (classData.classId.isBlank()) classData.copy(classId = docRef.id) else classData
            docRef.set(classToSave.toMap()).await()
            kotlin.Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error guardando clase ${classData.className}", e)
            kotlin.Result.failure(e)
        }
    }

    suspend fun getClassesByTeacher(teacherId: String): kotlin.Result<List<Classroom>> = withContext(Dispatchers.IO) {
        try {
            val querySnapshot = classesCollection
                .whereEqualTo("teacherId", teacherId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await()
            val classes = querySnapshot.documents.mapNotNull { Classroom.fromSnapshot(it) }
            kotlin.Result.success(classes)
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error obteniendo clases para el profesor $teacherId", e)
            kotlin.Result.failure(e)
        }
    }

    suspend fun deleteClass(classId: String): kotlin.Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (classId.isBlank()) {
                return@withContext kotlin.Result.failure(IllegalArgumentException("El ID de la clase no puede estar vacío."))
            }
            classesCollection.document(classId).delete().await()
            kotlin.Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error eliminando clase $classId", e)
            kotlin.Result.failure(e)
        }
    }

    suspend fun getClassDetails(classId: String): kotlin.Result<Classroom?> = withContext(Dispatchers.IO) {
        try {
            if (classId.isBlank()) {
                return@withContext kotlin.Result.failure(IllegalArgumentException("El ID de la clase no puede estar vacío."))
            }
            val snapshot = classesCollection.document(classId).get().await()
            if (snapshot.exists()) {
                kotlin.Result.success(Classroom.fromSnapshot(snapshot))
            } else {
                kotlin.Result.success(null)
            }
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error obteniendo detalles de la clase $classId", e)
            kotlin.Result.failure(e)
        }
    }

    suspend fun getUsersByIds(userIds: List<String>): kotlin.Result<List<User>> = withContext(Dispatchers.IO) {
        if (userIds.isEmpty()) {
            return@withContext kotlin.Result.success(emptyList())
        }
        try {
            if (userIds.size > 30) {
                Log.w("FirestoreRepo", "La lista de userIds (${userIds.size}) excede el límite de Firestore 'in' (30).")
                // Aquí podrías implementar chunking si es necesario. Por ahora, se procede.
            }
            val querySnapshot = usersCollection.whereIn("userId", userIds).get().await()
            val users = querySnapshot.documents.mapNotNull { User.fromSnapshot(it) }
            kotlin.Result.success(users)
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error obteniendo usuarios por IDs", e)
            kotlin.Result.failure(e)
        }
    }

    suspend fun removeStudentFromClassroom(classId: String, studentId: String): kotlin.Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (classId.isBlank() || studentId.isBlank()) {
                return@withContext kotlin.Result.failure(IllegalArgumentException("IDs no pueden estar vacíos."))
            }
            classesCollection.document(classId)
                .update("studentIds", FieldValue.arrayRemove(studentId))
                .await()
            kotlin.Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error eliminando alumno $studentId de clase $classId", e)
            kotlin.Result.failure(e)
        }
    }

    suspend fun addStudentToClass(classId: String, studentId: String): kotlin.Result<Unit> = withContext(Dispatchers.IO) {
        try {
            classesCollection.document(classId)
                .update("studentIds", FieldValue.arrayUnion(studentId)).await()
            kotlin.Result.success(Unit)
        } catch (e: Exception)
        {
            Log.e("FirestoreRepo", "Error añadiendo alumno $studentId a clase $classId", e)
            kotlin.Result.failure(e)
        }
    }

    suspend fun searchStudentsByName(
        nameQuery: String,
        limit: Long = 20
    ): kotlin.Result<List<User>> = withContext(Dispatchers.IO) {
        if (nameQuery.isBlank()) {
            return@withContext kotlin.Result.success(emptyList())
        }
        try {
            val querySnapshot = usersCollection
                .whereEqualTo("role", "student")
                .orderBy("fullName")
                .startAt(nameQuery.trim())
                .endAt(nameQuery.trim() + '\uf8ff')
                .limit(limit)
                .get()
                .await()
            val students = querySnapshot.documents.mapNotNull { User.fromSnapshot(it) }
            kotlin.Result.success(students)
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error buscando alumnos por nombre '$nameQuery'", e)
            kotlin.Result.failure(e)
        }
    }

    suspend fun savePictogram(pictogram: Pictogram): kotlin.Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val docRef = if (pictogram.pictogramId.isBlank()) {
                pictogramsCollection.document()
            } else {
                pictogramsCollection.document(pictogram.pictogramId)
            }
            val pictogramToSave = if (pictogram.pictogramId.isBlank()) pictogram.copy(pictogramId = docRef.id) else pictogram
            docRef.set(pictogramToSave.toMap()).await()
            kotlin.Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error guardando pictograma ${pictogram.name}", e)
            kotlin.Result.failure(e)
        }
    }

    suspend fun getPictogramsByCategoryAndLevel(categoryName: String, maxLevel: Int): kotlin.Result<List<Pictogram>> = withContext(Dispatchers.IO) {
        try {
            val querySnapshot = pictogramsCollection
                .whereEqualTo("category", categoryName)
                .whereLessThanOrEqualTo("levelRequired", maxLevel)
                .orderBy("name", Query.Direction.ASCENDING)
                .get()
                .await()
            val pictograms = querySnapshot.documents.mapNotNull { Pictogram.fromSnapshot(it) }
            kotlin.Result.success(pictograms)
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error buscando pictogramas para categoría $categoryName y nivel $maxLevel", e)
            kotlin.Result.failure(e)
        }
    }

    suspend fun getStudentCategories(): kotlin.Result<List<Category>> = withContext(Dispatchers.IO) {
        try {
            val querySnapshot = categoriesCollection
                .orderBy("displayOrder", Query.Direction.ASCENDING)
                .get().await()
            val categories = querySnapshot.documents.mapNotNull { Category.fromSnapshot(it) }
            kotlin.Result.success(categories)
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error obteniendo categorías de pictogramas", e)
            kotlin.Result.failure(e)
        }
    }

    suspend fun addExperienceToStudent(studentId: String, expToAdd: Int): kotlin.Result<Pair<Int, Int>> = withContext(Dispatchers.IO) {
        try {
            val userDocRef = usersCollection.document(studentId)
            val resultPair: Pair<Int, Int> = db.runTransaction { transaction ->
                val snapshot = transaction.get(userDocRef)
                if (!snapshot.exists()) {
                    throw Exception("User document $studentId not found")
                }
                val currentExp = snapshot.getLong("currentExp")?.toInt() ?: 0
                val totalExp = snapshot.getLong("totalExp")?.toInt() ?: 0
                val currentLevel = snapshot.getLong("currentLevel")?.toInt() ?: 1

                val newTotalExp = totalExp + expToAdd
                var newCurrentExp = currentExp + expToAdd
                var newLevel = currentLevel
                val initialLevelBeforeLoop = currentLevel

                var expNeededForNextLevel = newLevel * 1000

                while (newCurrentExp >= expNeededForNextLevel && newLevel < 100) {
                    newCurrentExp -= expNeededForNextLevel
                    newLevel++
                    expNeededForNextLevel = newLevel * 1000
                }
                if (newLevel >= 100 && newCurrentExp >= expNeededForNextLevel) {
                    newCurrentExp = expNeededForNextLevel -1
                }

                transaction.update(userDocRef, "currentExp", newCurrentExp)
                transaction.update(userDocRef, "totalExp", newTotalExp)
                transaction.update(userDocRef, "currentLevel", newLevel)

                if (newLevel > initialLevelBeforeLoop) {
                    Log.d("FirestoreRepo", "User $studentId leveled up from $initialLevelBeforeLoop to $newLevel")
                    for (levelReached in (initialLevelBeforeLoop + 1)..newLevel) {
                        levelUnlockMap[levelReached]?.forEach { categoryIdToUnlock ->
                            Log.d("FirestoreRepo", "Unlocking category $categoryIdToUnlock for user $studentId at level $levelReached")
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