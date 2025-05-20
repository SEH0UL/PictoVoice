package com.example.pictovoice.utils

/**
 * Una clase sealed genérica para representar el resultado de una operación que puede ser
 * exitosa ([Success]) o fallida ([Failure]).
 * Similar a la clase Result de Kotlin, pero personalizada para el proyecto si es necesario.
 *
 * @param T El tipo de dato encapsulado en caso de éxito.
 */
sealed class Result<out T> {
    /**
     * Representa un resultado exitoso y contiene los [data] de la operación.
     * @param data Los datos producidos por la operación exitosa.
     */
    data class Success<out T>(val data: T) : Result<T>()

    /**
     * Representa un resultado fallido.
     * @param exception La excepción opcional ([Throwable]) que causó el fallo.
     * @param message Un mensaje descriptivo opcional sobre el error.
     */
    data class Failure(val exception: Throwable? = null, val message: String? = null) : Result<Nothing>()
}