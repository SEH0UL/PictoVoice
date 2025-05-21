package com.example.pictovoice.Data.model

import androidx.annotation.DrawableRes // Para asegurar que iconResourceId es un drawable

/**
 * Representa una categoría de pictogramas dentro de la aplicación.
 * Estas categorías se definen localmente a través de [PictogramDataSource].
 *
 * @property categoryId Identificador único para la categoría (ej. "local_comida").
 * @property name Nombre legible de la categoría que se mostrará al usuario (ej. "Comida").
 * @property displayOrder Entero para determinar el orden en que se muestran las carpetas de categoría.
 * @property iconResourceId Referencia opcional al recurso drawable para el icono de la carpeta de esta categoría.
 * Se usa si las carpetas tienen iconos visuales. Default a 0 si no hay icono.
 */
data class Category(
    val categoryId: String = "",
    val name: String = "",
    val displayOrder: Int = 0,
    @DrawableRes val iconResourceId: Int = 0 // Para R.drawable.ic_folder_food, etc.
)
// Ya no se necesitan los métodos toMap() ni fromSnapshot() si las categorías
// son definidas y manejadas exclusivamente de forma local a través de PictogramDataSource
// y no se persisten/recuperan individualmente desde una colección "/categories" en Firestore.
// El campo 'iconUrl' también se ha eliminado asumiendo que solo se usarán iconos locales.

