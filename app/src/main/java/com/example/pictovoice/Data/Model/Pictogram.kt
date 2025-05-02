package com.example.pictovoice.Data.Model

import com.google.firebase.firestore.DocumentSnapshot
import java.util.Date

data class Pictogram(
    val pictogramId: String = "",
    val name: String = "",
    val category: String = "",
    val imageUrl: String = "",
    val audioUrl: String = "",
    val levelRequired: Int = 1,
    val baseExp: Int = 100,
    val timesUsed: Int = 0,
    val createdAt: Date = Date(),
    val createdBy: String = "" // ID del profesor que lo creó
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "pictogramId" to pictogramId,
            "name" to name,
            "category" to category,
            "imageUrl" to imageUrl,
            "audioUrl" to audioUrl,
            "levelRequired" to levelRequired,
            "baseExp" to baseExp,
            "timesUsed" to timesUsed,
            "createdAt" to createdAt,
            "createdBy" to createdBy
        )
    }

    companion object {
        fun fromSnapshot(snapshot: DocumentSnapshot): Pictogram? {
            return snapshot.toObject(Pictogram::class.java)?.copy(pictogramId = snapshot.id)
        }
    }
}
