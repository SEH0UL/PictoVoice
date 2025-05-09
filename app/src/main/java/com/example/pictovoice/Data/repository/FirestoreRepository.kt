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

// Define un alias para kotlin.Result si quieres evitar colisiones con tu propia clase Result
// typealias KResult<T> = kotlin.Result<T> // O usa kotlin.Result directamente

class FirestoreRepository {
    private val db = FirebaseFirestore.getInstance()

    // Referencias a las colecciones
    private val usersCollection = db.collection("users")
    private val pictogramsCollection = db.collection("pictograms")
    private val classesCollection = db.collection("classes")
    private val categoriesCollection = db.collection("categories") // Nueva colección para las carpetas/categorías

    // ---------- Operaciones con Usuarios ----------

    // Crear o actualizar usuario (ya lo tenías, asegúrate que el modelo User es el actual)
    suspend fun saveUser(user: User): kotlin.Result<Unit> = try {
        usersCollection.document(user.userId).set(user.toMap()).await()
        kotlin.Result.success(Unit)
    } catch (e: Exception) {
        Log.e("FirestoreRepo", "Error saving user ${user.userId}", e)
        kotlin.Result.failure(e)
    }

    // Obtener usuario por ID (ya lo tenías)
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

    // Obtener todos los estudiantes de un profesor (ya lo tenías, revisa la lógica si es compleja)
    // Esta implementación es un placeholder, necesitarías una lógica más robusta
    // para vincular profesores y alumnos (probablemente a través de la colección 'classes')
    suspend fun getStudentsByTeacher(teacherId: String): kotlin.Result<List<User>> = try {
        // Ejemplo: si las clases guardan studentIds y queremos obtener esos User objects
        val teacherClassesResult = getClassesByTeacher(teacherId)
        if (teacherClassesResult.isFailure) {
            return kotlin.Result.failure(teacherClassesResult.exceptionOrNull() ?: Exception("Failed to get teacher classes"))
        }

        val studentIds = teacherClassesResult.getOrNull()?.flatMap { it.studentIds }?.distinct() ?: emptyList()

        if (studentIds.isEmpty()) {
            kotlin.Result.success(emptyList())
        } else {
            // Firestore 'in' query tiene un límite de 10 (o 30 para algunas APIs más nuevas) elementos.
            // Si tienes más, necesitas hacer múltiples queries.
            // Aquí simplificamos asumiendo pocos estudiantes o que manejas la paginación/batching.
            val querySnapshot = usersCollection
                .whereIn("userId", studentIds) // Busca usuarios cuyos IDs estén en la lista
                .get().await()
            val students = querySnapshot.documents.mapNotNull { User.fromSnapshot(it) }
            kotlin.Result.success(students)
        }
    } catch (e: Exception) {
        Log.e("FirestoreRepo", "Error getting students for teacher $teacherId", e)
        kotlin.Result.failure(e)
    }

    // ---------- Operaciones con Pictogramas ----------

    // Crear o actualizar pictograma (ya lo tenías)
    suspend fun savePictogram(pictogram: Pictogram): kotlin.Result<Unit> = try {
        val docRef = if (pictogram.pictogramId.isBlank()) {
            // Nuevo pictograma, Firestore generará el ID
            pictogramsCollection.document()
        } else {
            // Actualizar existente
            pictogramsCollection.document(pictogram.pictogramId)
        }
        // Si es nuevo, actualiza el modelo con el ID generado antes de guardar
        val pictogramToSave = if (pictogram.pictogramId.isBlank()) pictogram.copy(pictogramId = docRef.id) else pictogram
        docRef.set(pictogramToSave.toMap()).await()
        kotlin.Result.success(Unit)
    } catch (e: Exception) {
        Log.e("FirestoreRepo", "Error saving pictogram ${pictogram.name}", e)
        kotlin.Result.failure(e)
    }

    /**
     * Obtiene pictogramas por nombre de categoría y nivel máximo requerido por el usuario.
     * Ordena por nombre por defecto.
     */
    suspend fun getPictogramsByCategoryAndLevel(categoryName: String, maxLevel: Int): kotlin.Result<List<Pictogram>> {
        return try {
            Log.d("FirestoreRepo", "Fetching pictos for category: $categoryName, maxLevel: $maxLevel")
            val querySnapshot = pictogramsCollection
                .whereEqualTo("category", categoryName)
                .whereLessThanOrEqualTo("levelRequired", maxLevel)
                .orderBy("name", Query.Direction.ASCENDING) // Opcional: ordenar por nombre o algún otro campo
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

    // Incrementar contador de usos de un pictograma (ya lo tenías)
    suspend fun incrementPictogramUsage(pictogramId: String): kotlin.Result<Unit> = try {
        pictogramsCollection.document(pictogramId)
            .update("timesUsed", FieldValue.increment(1)).await()
        kotlin.Result.success(Unit)
    } catch (e: Exception) {
        Log.e("FirestoreRepo", "Error incrementing usage for pictogram $pictogramId", e)
        kotlin.Result.failure(e)
    }

    // ---------- Operaciones con Categorías (para las carpetas) ----------

    /**
     * Obtiene la lista de categorías/carpetas que el alumno puede ver.
     * Asume una colección "categories" y un campo "displayOrder" para ordenar.
     */
    suspend fun getStudentCategories(): kotlin.Result<List<Category>> {
        return try {
            val querySnapshot = categoriesCollection
                .orderBy("displayOrder", Query.Direction.ASCENDING) // Asegúrate de tener este campo y un índice
                .get().await()
            val categories = querySnapshot.documents.mapNotNull { Category.fromSnapshot(it) }
            kotlin.Result.success(categories)
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error fetching student categories", e)
            kotlin.Result.failure(e)
        }
    }


    // ---------- Operaciones con Clases ----------

    // Crear o actualizar clase (ya lo tenías)
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

    // Obtener clases de un profesor (ya lo tenías)
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

    // Añadir estudiante a una clase (ya lo tenías)
    suspend fun addStudentToClass(classId: String, studentId: String): kotlin.Result<Unit> = try {
        classesCollection.document(classId)
            .update("studentIds", FieldValue.arrayUnion(studentId)).await()
        kotlin.Result.success(Unit)
    } catch (e: Exception) {
        Log.e("FirestoreRepo", "Error adding student $studentId to class $classId", e)
        kotlin.Result.failure(e)
    }

    // ---------- Sistema de Experiencia (ya lo tenías) ----------

    // Añadir experiencia a un estudiante
    suspend fun addExperienceToStudent(
        studentId: String,
        expToAdd: Int
    ): kotlin.Result<Pair<Int, Int>> = try { // Devuelve (newExp, newLevel)
        val userDocRef = usersCollection.document(studentId)

        val resultPair: Pair<Int, Int> = db.runTransaction { transaction ->
            val snapshot = transaction.get(userDocRef)
            val currentExp = snapshot.getLong("currentExp")?.toInt() ?: 0
            val totalExp = snapshot.getLong("totalExp")?.toInt() ?: 0 // Necesario para actualizar el totalExp
            val currentLevel = snapshot.getLong("currentLevel")?.toInt() ?: 1

            val newTotalExp = totalExp + expToAdd
            var newCurrentExp = currentExp + expToAdd
            var newLevel = currentLevel

            // Lógica de subida de nivel (ajusta 1000 si es diferente)
            // Ejemplo: Nivel 1 necesita 1000 EXP para Nivel 2.
            // Nivel 2 necesita 2000 EXP para Nivel 3 (desde 0 EXP de ese nivel).
            var expNeededForNextLevel = newLevel * 1000 // O la fórmula que estés usando

            while (newCurrentExp >= expNeededForNextLevel && newLevel < 100) { // Limite de nivel 100 por ejemplo
                newCurrentExp -= expNeededForNextLevel
                newLevel++
                // Actualizar la EXP necesaria para el siguiente nivel (si la fórmula cambia con el nivel)
                expNeededForNextLevel = newLevel * 1000 // Actualiza si es dinámico
            }

            transaction.update(userDocRef, "currentExp", newCurrentExp)
            transaction.update(userDocRef, "totalExp", newTotalExp)
            transaction.update(userDocRef, "currentLevel", newLevel)

            Pair(newCurrentExp, newLevel) // Devuelve la nueva exp actual y el nuevo nivel
        }.await()

        kotlin.Result.success(resultPair)
    } catch (e: Exception) {
        Log.e("FirestoreRepo", "Error adding experience to student $studentId", e)
        kotlin.Result.failure(e)
    }
}