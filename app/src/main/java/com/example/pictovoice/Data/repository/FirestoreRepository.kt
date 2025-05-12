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

class FirestoreRepository {
    private val db = FirebaseFirestore.getInstance()

    // Referencias a las colecciones principales
    private val usersCollection = db.collection("users")
    private val pictogramsCollection = db.collection("pictograms")
    private val classesCollection = db.collection("classes")
    private val categoriesCollection = db.collection("categories") // Asumiendo que tienes esta para las carpetas/categorías de pictogramas

    // ---------- Operaciones con Usuarios ----------

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

    suspend fun getStudentsByTeacher(teacherId: String): kotlin.Result<List<User>> = withContext(Dispatchers.IO) {
        try {
            val teacherClassesResult = getClassesByTeacher(teacherId) // Obtener las clases del profesor
            if (teacherClassesResult.isFailure) {
                return@withContext kotlin.Result.failure(teacherClassesResult.exceptionOrNull() ?: Exception("Fallo al obtener las clases del profesor."))
            }

            // Extraer todos los IDs de estudiantes de todas las clases del profesor, evitando duplicados.
            val studentIds = teacherClassesResult.getOrNull()?.flatMap { it.studentIds }?.distinct() ?: emptyList()

            if (studentIds.isEmpty()) {
                Log.d("FirestoreRepo", "El profesor $teacherId no tiene alumnos asignados en sus clases.")
                return@withContext kotlin.Result.success(emptyList())
            }

            // Firestore tiene un límite en el número de elementos para cláusulas 'in' (actualmente 30).
            // Si se esperan más, se necesitaría dividir la consulta.
            if (studentIds.size > 30) {
                Log.w("FirestoreRepo", "La lista de studentIds (${studentIds.size}) excede el límite de 30 para consultas 'in'. Se necesitaría paginación o múltiples consultas.")
                // Implementar lógica de chunking si es necesario. Por ahora, se procede con la lista completa.
            }

            val querySnapshot = usersCollection.whereIn("userId", studentIds).get().await()
            val students = querySnapshot.documents.mapNotNull { User.fromSnapshot(it) }
            kotlin.Result.success(students)

        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error obteniendo alumnos para el profesor $teacherId", e)
            kotlin.Result.failure(e)
        }
    }


    // ---------- Operaciones con Clases ----------

    /**
     * Guarda o actualiza una clase en Firestore.
     * Si classData.classId está vacío, se crea un nuevo documento con ID generado por Firestore.
     * Si classData.classId tiene valor, se actualiza el documento existente.
     */
    suspend fun saveClass(classData: Classroom): kotlin.Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val docRef = if (classData.classId.isBlank()) {
                classesCollection.document() // Nuevo documento, Firestore genera ID
            } else {
                classesCollection.document(classData.classId) // Documento existente
            }
            // Asegurarse de que el objeto a guardar tiene el ID correcto si era uno nuevo.
            val classToSave = if (classData.classId.isBlank()) classData.copy(classId = docRef.id) else classData
            docRef.set(classToSave.toMap()).await()
            Log.d("FirestoreRepo", "Clase guardada/actualizada: ${classToSave.classId}")
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
                .orderBy("createdAt", Query.Direction.DESCENDING) // Mostrar las más recientes primero
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

    // ---------- Operaciones con Pictogramas ----------
    // (Asumiendo que estas funciones son para pictogramas almacenados en Firestore, no los locales)

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
            Log.d("FirestoreRepo", "Buscando pictogramas para categoría: $categoryName, nivel máx: $maxLevel")
            val querySnapshot = pictogramsCollection
                .whereEqualTo("category", categoryName) // Asegúrate que 'category' es el campo correcto en Firestore
                .whereLessThanOrEqualTo("levelRequired", maxLevel)
                .orderBy("name", Query.Direction.ASCENDING)
                .get()
                .await()
            val pictograms = querySnapshot.documents.mapNotNull { Pictogram.fromSnapshot(it) }
            Log.d("FirestoreRepo", "Encontrados ${pictograms.size} pictogramas para $categoryName")
            kotlin.Result.success(pictograms)
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error buscando pictogramas para categoría $categoryName y nivel $maxLevel", e)
            kotlin.Result.failure(e)
        }
    }

    // ---------- Operaciones con Categorías de Pictogramas (si se gestionan en Firestore) ----------

    suspend fun getStudentCategories(): kotlin.Result<List<Category>> = withContext(Dispatchers.IO) {
        try {
            // Asumiendo que las categorías tienen un campo 'displayOrder' para ordenarlas.
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

    // ---------- Sistema de Experiencia ----------

    suspend fun addExperienceToStudent(studentId: String, expToAdd: Int): kotlin.Result<Pair<Int, Int>> = withContext(Dispatchers.IO) {
        try {
            val userDocRef = usersCollection.document(studentId)
            val resultPair: Pair<Int, Int> = db.runTransaction { transaction ->
                val snapshot = transaction.get(userDocRef)
                val currentExp = snapshot.getLong("currentExp")?.toInt() ?: 0
                val totalExp = snapshot.getLong("totalExp")?.toInt() ?: 0
                val currentLevel = snapshot.getLong("currentLevel")?.toInt() ?: 1

                val newTotalExp = totalExp + expToAdd
                var newCurrentExp = currentExp + expToAdd
                var newLevel = currentLevel

                var expNeededForNextLevel = newLevel * 1000 // Ejemplo: 1000 EXP para el siguiente nivel. Ajustar según necesidad.

                // Bucle para subir múltiples niveles si se gana suficiente EXP.
                while (newCurrentExp >= expNeededForNextLevel && newLevel < 100) { // Límite de nivel 100
                    newCurrentExp -= expNeededForNextLevel
                    newLevel++
                    expNeededForNextLevel = newLevel * 1000 // Actualizar EXP necesaria para el nuevo nivel.
                }
                // Si se alcanza el nivel máximo, la EXP actual podría quedar como la sobrante o ajustarse.
                if (newLevel >= 100) {
                    newCurrentExp = if(newCurrentExp >= expNeededForNextLevel) expNeededForNextLevel -1 else newCurrentExp // Evitar que supere el cap del último nivel
                }


                transaction.update(userDocRef, "currentExp", newCurrentExp)
                transaction.update(userDocRef, "totalExp", newTotalExp)
                transaction.update(userDocRef, "currentLevel", newLevel)
                Pair(newCurrentExp, newLevel) // Devuelve la nueva EXP actual y el nuevo nivel.
            }.await()
            kotlin.Result.success(resultPair)
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error añadiendo experiencia al alumno $studentId", e)
            kotlin.Result.failure(e)
        }
    }
}