package com.example.pictovoice.Data.Model

import com.google.firebase.firestore.DocumentSnapshot
// import com.google.firebase.firestore.PropertyName // Descomenta si tus campos en Firestore se llaman diferente

data class Category(
    val categoryId: String = "", // Este será el ID del documento en Firestore o un ID local único
    val name: String = "",
    val displayOrder: Int = 0,
    val iconUrl: String? = null, // Para URLs de Firebase Storage (si aún las usas para iconos de categoría)
    val iconResourceId: Int = 0  // Para R.drawable.ic_categoria_comida (si usas iconos locales)
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            // No incluimos categoryId en el map si Firestore lo genera o es el ID del documento
            "name" to name,
            "displayOrder" to displayOrder,
            "iconUrl" to iconUrl
            // iconResourceId no se suele guardar en Firestore
        )
    }

    companion object {
        fun fromSnapshot(snapshot: DocumentSnapshot): Category? {
            return try {
                val data = snapshot.data ?: return null // Si no hay datos, retorna null
                Category(
                    categoryId = snapshot.id, // Usamos el ID del documento como categoryId
                    name = data["name"] as? String ?: "",
                    displayOrder = (data["displayOrder"] as? Long)?.toInt() ?: 0, // Firestore guarda números como Long
                    iconUrl = data["iconUrl"] as? String
                    // iconResourceId se asignaría localmente si es necesario, no desde Firestore
                )
            } catch (e: Exception) {
                // Log.e("CategoryModel", "Error al convertir snapshot a Category", e)
                null // Retorna null si hay un error en la conversión
            }
        }
    }
}