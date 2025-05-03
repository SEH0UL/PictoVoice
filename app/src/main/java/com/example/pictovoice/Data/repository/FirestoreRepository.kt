package com.example.pictovoice.Data.repository

import com.example.pictovoice.Data.Model.Classroom
import com.example.pictovoice.Data.Model.Pictogram
import com.example.pictovoice.Data.Model.User
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.example.pictovoice.utils.Result


class FirestoreRepository {
    private val db = FirebaseFirestore.getInstance()

    // Referencias a las colecciones
    private val usersCollection = db.collection("users")
    private val pictogramsCollection = db.collection("pictograms")
    private val classesCollection = db.collection("classes")

    // ---------- Operaciones con Usuarios ----------

    // Crear o actualizar usuario
    suspend fun saveUser(user: User): com.example.pictovoice.utils.Result<Unit> = try {
        usersCollection.document(user.userId).set(user.toMap()).await()
        com.example.pictovoice.utils.Result.Success(Unit)
    } catch (e: Exception) {
        com.example.pictovoice.utils.Result.Failure(e)
    }


    // Obtener usuario por ID
    suspend fun getUser(userId: String): Result<User> = try {
        val snapshot = usersCollection.document(userId).get().await()
        User.fromSnapshot(snapshot)?.let { Result.Success(it) } ?: Result.Failure(Exception("User not found"))
    } catch (e: Exception) {
        Result.Failure(e)
    }

    // Obtener todos los estudiantes de un profesor
    suspend fun getStudentsByTeacher(teacherId: String): Result<List<User>> = try {
        val querySnapshot = usersCollection
            .whereEqualTo("role", "student")
            // Aquí deberías implementar la lógica para obtener estudiantes vinculados
            // a través de las clases
            .get().await()

        val students = querySnapshot.documents.mapNotNull { User.fromSnapshot(it) }
        Result.success(students)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ---------- Operaciones con Pictogramas ----------

    // Crear o actualizar pictograma
    suspend fun savePictogram(pictogram: Pictogram): Result<Unit> = try {
        if (pictogram.pictogramId.isEmpty()) {
            // Nuevo pictograma
            pictogramsCollection.add(pictogram.toMap()).await()
        } else {
            // Actualizar existente
            pictogramsCollection.document(pictogram.pictogramId)
                .set(pictogram.toMap()).await()
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // Obtener pictogramas por categoría y nivel requerido
    suspend fun getPictogramsByCategoryAndLevel(
        category: String,
        maxLevel: Int
    ): Result<List<Pictogram>> = try {
        val querySnapshot = pictogramsCollection
            .whereEqualTo("category", category)
            .whereLessThanOrEqualTo("levelRequired", maxLevel)
            .get().await()

        val pictograms = querySnapshot.documents.mapNotNull { Pictogram.fromSnapshot(it) }
        Result.success(pictograms)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // Incrementar contador de usos de un pictograma
    suspend fun incrementPictogramUsage(pictogramId: String): Result<Unit> = try {
        pictogramsCollection.document(pictogramId)
            .update("timesUsed", FieldValue.increment(1)).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ---------- Operaciones con Clases ----------

    // Crear o actualizar clase
    suspend fun saveClass(classData: Classroom): Result<Unit> = try {
        if (classData.classId.isEmpty()) {
            classesCollection.add(classData.toMap()).await()
        } else {
            classesCollection.document(classData.classId)
                .set(classData.toMap()).await()
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // Obtener clases de un profesor
    suspend fun getClassesByTeacher(teacherId: String): Result<List<Classroom>> = try {
        val querySnapshot = classesCollection
            .whereEqualTo("teacherId", teacherId)
            .get().await()

        val classes = querySnapshot.documents.mapNotNull { Classroom.fromSnapshot(it) }
        Result.success(classes)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // Añadir estudiante a una clase
    suspend fun addStudentToClass(classId: String, studentId: String): Result<Unit> = try {
        classesCollection.document(classId)
            .update("studentIds", FieldValue.arrayUnion(studentId)).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ---------- Sistema de Experiencia ----------

    // Añadir experiencia a un estudiante
    suspend fun addExperienceToStudent(
        studentId: String,
        expToAdd: Int
    ): Result<Pair<Int, Int>> = try {
        val userDoc = usersCollection.document(studentId)

        val result = db.runTransaction { transaction ->
            val snapshot = transaction.get(userDoc)
            val currentExp = snapshot.getLong("currentExp")?.toInt() ?: 0
            val totalExp = snapshot.getLong("totalExp")?.toInt() ?: 0
            val currentLevel = snapshot.getLong("currentLevel")?.toInt() ?: 1

            var newExp = currentExp + expToAdd
            var newLevel = currentLevel
            val expNeededForNextLevel = newLevel * 1000

            if (newExp >= expNeededForNextLevel) {
                newExp -= expNeededForNextLevel
                newLevel++
            }

            transaction.update(userDoc, "currentExp", newExp)
            transaction.update(userDoc, "totalExp", totalExp + expToAdd)
            transaction.update(userDoc, "currentLevel", newLevel)

            Pair(newExp, newLevel)
        }.await()

        Result.success(result)
    } catch (e: Exception) {
        Result.failure(e)
    }
}