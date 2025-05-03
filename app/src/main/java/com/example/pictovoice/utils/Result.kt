// Crea un nuevo archivo llamado Result.kt en el paquete utils
package com.example.pictovoice.utils

sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Failure(val exception: Exception? = null, val message: String? = null) : Result<Nothing>()
}