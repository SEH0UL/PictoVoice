package com.example.pictovoice.utils

sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    // Corregido: Throwable? para permitir el valor por defecto null
    data class Failure(val exception: Throwable? = null, val message: String? = null) : Result<Nothing>()
}