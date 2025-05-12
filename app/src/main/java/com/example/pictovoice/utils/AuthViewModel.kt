package com.example.pictovoice.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pictovoice.Data.Model.User
// import com.example.pictovoice.Data.Model.User
import com.example.pictovoice.Data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthResult<out T> { // Hacerlo genérico
    object Idle : AuthResult<Nothing>()
    object Loading : AuthResult<Nothing>()
    data class Success<out T>(val data: T) : AuthResult<T>() // data es de tipo T
    data class Error(val message: String) : AuthResult<Nothing>() // Error no necesita T

    val isLoading get() = this is Loading
    // val isSuccess get() = this is Success // Puedes mantenerlo o acceder a 'data' directamente
    // val error get() = (this as? Error)?.message // No se puede castear a AuthResult.Error directamente
    fun getErrorMessage(): String? {
        return if (this is Error) this.message else null
    }
}


class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    // loginResult ahora es AuthResult<User>
    private val _loginResult = MutableStateFlow<AuthResult<User>>(AuthResult.Idle)
    val loginResult: StateFlow<AuthResult<User>> = _loginResult

    // registerResult ahora es AuthResult<String> (para el username)
    private val _registerResult = MutableStateFlow<AuthResult<String>>(AuthResult.Idle)
    val registerResult: StateFlow<AuthResult<String>> = _registerResult

    fun login(identifier: String, password: String) {
        _loginResult.value = AuthResult.Loading
        viewModelScope.launch {
            val result = authRepository.login(identifier, password)
            if (result.isSuccess) {
                val user = result.getOrNull()
                if (user != null) {
                    _loginResult.value = AuthResult.Success(user)
                } else {
                    _loginResult.value = AuthResult.Error("Error inesperado al obtener datos del usuario.")
                }
            } else {
                _loginResult.value = AuthResult.Error(result.exceptionOrNull()?.message ?: "Error desconocido en login")
            }
        }
    }

    fun register(fullName: String, email: String, password: String, role: String) {
        _registerResult.value = AuthResult.Loading
        viewModelScope.launch {
            val result = authRepository.register(fullName, email, password, role) // Devuelve kotlin.Result<String>
            if (result.isSuccess) {
                val username = result.getOrNull()
                if (username != null) {
                    _registerResult.value = AuthResult.Success(username) // Pasar el username (String)
                } else {
                    _registerResult.value = AuthResult.Error("No se pudo obtener el username generado.")
                }
            } else {
                _registerResult.value = AuthResult.Error(result.exceptionOrNull()?.message ?: "Error desconocido en registro")
            }
        }
    }
}