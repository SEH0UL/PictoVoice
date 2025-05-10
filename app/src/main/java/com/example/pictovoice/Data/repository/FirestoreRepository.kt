package com.example.pictovoice.Data.repository // Asegúrate que el package es correcto

import android.util.Log
import com.example.pictovoice.Data.Model.Category // Importa tu modelo Category
import com.example.pictovoice.Data.Model.Classroom
import com.example.pictovoice.Data.Model.Pictogram
import com.example.pictovoice.Data.Model.User
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class FirestoreRepository {
    private val db = FirebaseFirestore.getInstance()

    // Referencias a las colecciones
    private val usersCollection = db.collection("users")
    private val pictogramsCollection = db.collection("pictograms")
    private val classesCollection = db.collection("classes")
    private val categoriesCollection = db.collection("categories")

    // ---------- Operaciones con Usuarios ----------

    suspend fun saveUser(user: User): kotlin.Result<Unit> = try {
        usersCollection.document(user.userId).set(user.toMap()).await()
        kotlin.Result.success(Unit)
    } catch (e: Exception) {
        Log.e("FirestoreRepo", "Error saving user ${user.userId}", e)
        kotlin.Result.failure(e)
    }

    suspend fun getUser(userId: String): kotlin.Result<User?> = try {
        val snapshot = usersCollection.document(userId).get().await()
        if (snapshot.exists()) {
            kotlin.Result.success(User.fromSnapshot(snapshot))
        } else {
            kotlin.Result.success(null) // Usuario no encontrado
        }
    } catch (e: Exception) {
        Log.e("FirestoreRepo", "Error getting user $userId", e)
        kotlin.Result.failure(e)
    }

    // CORRECCIÓN AQUÍ: Cambiado a cuerpo de bloque
    suspend fun getStudentsByTeacher(teacherId: String): kotlin.Result<List<User>> {
        return try { // El return ahora es de todo el bloque try-catch
            val teacherClassesResult = getClassesByTeacher(teacherId)
            if (teacherClassesResult.isFailure) {
                // Este return ahora es válido porque está dentro de un cuerpo de bloque
                // y es la expresión final de esta rama del if.
                kotlin.Result.failure(teacherClassesResult.exceptionOrNull() ?: Exception("Failed to get teacher classes"))
            } else {
                val studentIds = teacherClassesResult.getOrNull()?.flatMap { it.studentIds }?.distinct() ?: emptyList()

                if (studentIds.isEmpty()) {
                    kotlin.Result.success(emptyList())
                } else {
                    val querySnapshot = usersCollection
                        .whereIn("userId", studentIds)
                        .get().await()
                    val students = querySnapshot.documents.mapNotNull { User.fromSnapshot(it) }
                    kotlin.Result.success(students)
                }
            }
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error getting students for teacher $teacherId", e)
            kotlin.Result.failure(e)
        }
    }

    // ---------- Operaciones con Pictogramas ----------

    suspend fun savePictogram(pictogram: Pictogram): kotlin.Result<Unit> = try {
        val docRef = if (pictogram.pictogramId.isBlank()) {
            pictogramsCollection.document()
        } else {
            pictogramsCollection.document(pictogram.pictogramId)
        }
        val pictogramToSave = if (pictogram.pictogramId.isBlank()) pictogram.copy(pictogramId = docRef.id) else pictogram
        docRef.set(pictogramToSave.toMap()).await()
        kotlin.Result.success(Unit)
    } catch (e: Exception) {
        Log.e("FirestoreRepo", "Error saving pictogram ${pictogram.name}", e)
        kotlin.Result.failure(e)
    }

    suspend fun getPictogramsByCategoryAndLevel(categoryName: String, maxLevel: Int): kotlin.Result<List<Pictogram>> {
        return try {
            Log.d("FirestoreRepo", "Fetching pictos for category: $categoryName, maxLevel: $maxLevel")
            val querySnapshot = pictogramsCollection
                .whereEqualTo("category", categoryName)
                .whereLessThanOrEqualTo("levelRequired", maxLevel)
                .orderBy("name", Query.Direction.ASCENDING)
                .get()
                .await()

            val pictograms = querySnapshot.documents.mapNotNull { Pictogram.fromSnapshot(it) }
            Log.d("FirestoreRepo", "Found ${pictograms.size} pictos for $categoryName")
            kotlin.Result.success(pictograms)
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error fetching pictograms for category $categoryName and level $maxLevel", e)
            kotlin.Result.failure(e)
        }
    }

    suspend fun incrementPictogramUsage(pictogramId: String): kotlin.Result<Unit> = try {
        pictogramsCollection.document(pictogramId)
            .update("timesUsed", FieldValue.increment(1)).await()
        kotlin.Result.success(Unit)
    } catch (e: Exception) {
        Log.e("FirestoreRepo", "Error incrementing usage for pictogram $pictogramId", e)
        kotlin.Result.failure(e)
    }

    // ---------- Operaciones con Categorías (para las carpetas) ----------

    suspend fun getStudentCategories(): kotlin.Result<List<Category>> {
        return try {
            val querySnapshot = categoriesCollection
                .orderBy("displayOrder", Query.Direction.ASCENDING)
                .get().await()
            val categories = querySnapshot.documents.mapNotNull { Category.fromSnapshot(it) } // Asegúrate que Category.fromSnapshot existe y funciona
            kotlin.Result.success(categories)
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error fetching student categories", e)
            kotlin.Result.failure(e)
        }
    }

    // ---------- Operaciones con Clases ----------

    suspend fun saveClass(classData: Classroom): kotlin.Result<Unit> = try {
        val docRef = if (classData.classId.isBlank()) {
            classesCollection.document()
        } else {
            classesCollection.document(classData.classId)
        }
        val classToSave = if (classData.classId.isBlank()) classData.copy(classId = docRef.id) else classData
        docRef.set(classToSave.toMap()).await()
        kotlin.Result.success(Unit)
    } catch (e: Exception) {
        Log.e("FirestoreRepo", "Error saving class ${classData.className}", e)
        kotlin.Result.failure(e)
    }

    suspend fun getClassesByTeacher(teacherId: String): kotlin.Result<List<Classroom>> = try {
        val querySnapshot = classesCollection
            .whereEqualTo("teacherId", teacherId)
            .get().await()

        val classes = querySnapshot.documents.mapNotNull { Classroom.fromSnapshot(it) }
        kotlin.Result.success(classes)
    } catch (e: Exception) {
        Log.e("FirestoreRepo", "Error getting classes for teacher $teacherId", e)
        kotlin.Result.failure(e)
    }

    suspend fun addStudentToClass(classId: String, studentId: String): kotlin.Result<Unit> = try {
        classesCollection.document(classId)
            .update("studentIds", FieldValue.arrayUnion(studentId)).await()
        kotlin.Result.success(Unit)
    } catch (e: Exception) {
        Log.e("FirestoreRepo", "Error adding student $studentId to class $classId", e)
        kotlin.Result.failure(e)
    }

    // ---------- Sistema de Experiencia (ya lo tenías) ----------

    suspend fun addExperienceToStudent(
        studentId: String,
        expToAdd: Int
    ): kotlin.Result<Pair<Int, Int>> = try {
        val userDocRef = usersCollection.document(studentId)

        val resultPair: Pair<Int, Int> = db.runTransaction { transaction ->
            val snapshot = transaction.get(userDocRef)
            val currentExp = snapshot.getLong("currentExp")?.toInt() ?: 0
            val totalExp = snapshot.getLong("totalExp")?.toInt() ?: 0
            val currentLevel = snapshot.getLong("currentLevel")?.toInt() ?: 1

            val newTotalExp = totalExp + expToAdd
            var newCurrentExp = currentExp + expToAdd
            var newLevel = currentLevel

            var expNeededForNextLevel = newLevel * 1000

            while (newCurrentExp >= expNeededForNextLevel && newLevel < 100) {
                newCurrentExp -= expNeededForNextLevel
                newLevel++
                expNeededForNextLevel = newLevel * 1000
            }

            transaction.update(userDocRef, "currentExp", newCurrentExp)
            transaction.update(userDocRef, "totalExp", newTotalExp)
            transaction.update(userDocRef, "currentLevel", newLevel)

            Pair(newCurrentExp, newLevel)
        }.await()

        kotlin.Result.success(resultPair)
    } catch (e: Exception) {
        Log.e("FirestoreRepo", "Error adding experience to student $studentId", e)
        kotlin.Result.failure(e)
    }
}