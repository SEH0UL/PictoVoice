package com.example.pictovoice.data.model


import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import java.util.Date // Necesario para el campo createdAt

/**
 * Representa una clase creada por un profesor.
 * Este modelo se mapea directamente a documentos en la colección "classes" de Firestore.
 *
 * @property classId ID único de la clase (generalmente el ID del documento de Firestore).
 * @property className Nombre de la clase asignado por el profesor.
 * @property teacherId ID del usuario (profesor) que creó y es dueño de esta clase.
 * @property studentIds Lista de IDs de usuarios (alumnos) que pertenecen a esta clase.
 * @property createdAt Fecha y hora de cuándo se creó la clase.
 */
data class Classroom(
    val classId: String = "",
    val className: String = "",
    val teacherId: String = "",
    val studentIds: List<String> = emptyList(),
    val createdAt: Date = Date() // Fecha de creación por defecto al momento de instanciar
) {
    /**
     * Convierte el objeto Classroom a un Mapa para ser almacenado en Firestore.
     * Se podría excluir classId si siempre es el ID del documento, pero incluirlo puede ser útil.
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "classId" to classId, // Incluirlo puede ser útil para algunas queries o referencias
            "className" to className,
            "teacherId" to teacherId,
            "studentIds" to studentIds,
            "createdAt" to createdAt
        )
    }

    companion object {
        /**
         * Crea un objeto Classroom a partir de un DocumentSnapshot de Firestore.
         * @param snapshot El DocumentSnapshot a convertir.
         * @return Un objeto Classroom, o null si la conversión falla.
         */
        fun fromSnapshot(snapshot: DocumentSnapshot): Classroom? {
            // Usar toObject es más conciso y maneja mejor los tipos,
            // pero requiere que los nombres de campo coincidan o usar @PropertyName.
            // El copy(classId = snapshot.id) asegura que el ID del documento se asigne.
            return try {
                snapshot.toObject(Classroom::class.java)?.copy(classId = snapshot.id)
            } catch (e: Exception) {
                Log.e("Classroom.fromSnapshot", "Error al convertir snapshot a Classroom para ID ${snapshot.id}: ${e.message}", e)
                null
            }
        }
    }
}