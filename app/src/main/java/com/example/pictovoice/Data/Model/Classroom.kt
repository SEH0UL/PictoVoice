package com.example.pictovoice.Data.Model

import com.google.firebase.firestore.DocumentSnapshot
import java.util.Date

data class Classroom(
    val classId: String = "",
    val className: String = "",
    val teacherId: String = "",
    val studentIds: List<String> = emptyList(),
    val createdAt: Date = Date()
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "classId" to classId,
            "className" to className,
            "teacherId" to teacherId,
            "studentIds" to studentIds,
            "createdAt" to createdAt
        )
    }

    companion object {
        fun fromSnapshot(snapshot: DocumentSnapshot): Classroom? {
            return snapshot.toObject(Classroom::class.java)?.copy(classId = snapshot.id)
        }
    }
}
