package com.example.pictovoice.Data.Model

import com.google.firebase.firestore.DocumentSnapshot
import java.util.Date

data class Class(
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
        fun fromSnapshot(snapshot: DocumentSnapshot): Class? {
            return snapshot.toObject(Class::class.java)?.copy(classId = snapshot.id)
        }
    }
}
