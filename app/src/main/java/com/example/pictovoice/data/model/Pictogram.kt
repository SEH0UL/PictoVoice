package com.example.pictovoice.data.model


import androidx.annotation.DrawableRes
import androidx.annotation.RawRes

/**
 * Representa un pictograma individual utilizado en la aplicación.
 * Los pictogramas se definen localmente a través de [PictogramDataSource].
 *
 * @property pictogramId Identificador único para el pictograma (ej. "local_com_manzana").
 * @property name Nombre del pictograma (ej. "Manzana"), que se usa para TTS si no hay audio.
 * @property category El `categoryId` de la [Category] a la que pertenece este pictograma.
 * @property imageResourceId Referencia al recurso drawable para la imagen del pictograma.
 * @property audioResourceId Referencia opcional al recurso raw para el audio asociado al pictograma.
 * Default a 0 si no hay audio específico.
 * @property levelRequired Nivel mínimo que el alumno debe tener (y que debe estar aprobado por el profesor
 * en `User.maxContentLevelApproved`) para que este pictograma sea accesible.
 * @property baseExp Cantidad de puntos de experiencia que el alumno gana al usar este pictograma
 * (actualmente, al añadirlo a la frase).
 */
data class Pictogram(
    val pictogramId: String = "",
    val name: String = "",
    val category: String = "", // Este es el categoryId
    @DrawableRes val imageResourceId: Int = 0,
    @RawRes val audioResourceId: Int = 0,
    val levelRequired: Int = 1,
    val baseExp: Int = 20 // Un valor por defecto razonable, puedes ajustarlo
)