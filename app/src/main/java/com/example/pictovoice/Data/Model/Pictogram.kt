package com.example.pictovoice.Data.Model

import com.google.firebase.firestore.DocumentSnapshot
import java.util.Date

data class Pictogram(
    val pictogramId: String = "",
    val name: String = "",
    val category: String = "", // Para la organización local, usaremos un categoryId

    // Campos para Firebase (opcionales si se gestiona solo localmente)
    val imageUrl: String? = null,
    val audioUrl: String? = null,

    // Nuevos campos para gestión local
    val imageResourceId: Int = 0, // Ejemplo: R.drawable.picto_manzana
    val audioResourceId: Int = 0, // Ejemplo: R.raw.audio_manzana

    val levelRequired: Int = 1,
    val baseExp: Int = 100,
    val timesUsed: Int = 0,
    val createdAt: Date = Date(), // Menos relevante para datos locales fijos
    val createdBy: String = ""    // Menos relevante para datos locales fijos
) {
    // El método toMap() es principalmente para Firestore.
    // Si solo usas datos locales, podrías incluso quitarlo o no usarlo.
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
            // No mapeamos imageResourceId ni audioResourceId a Firestore aquí
        )
    }

    companion object {
        // fromSnapshot es para Firestore. No se usará directamente para cargar datos locales.
        fun fromSnapshot(snapshot: DocumentSnapshot): Pictogram? {
            // Si decides tener un modelo híbrido, aquí decidirías cómo poblarlo.
            // Por ahora, lo mantenemos como estaba para no romper otras partes si existen.
            return snapshot.toObject(Pictogram::class.java)?.copy(pictogramId = snapshot.id)
        }
    }
}