package com.example.pictovoice.Data.Model

data class Pictogram(
    val id: String = "",  // Firestore auto-genera IDs
    val name: String = "",
    val category: String = "",
    val imageUrl: String = "",
    val audioUrl: String = "",
    val levelRequired: Int = 1
)
