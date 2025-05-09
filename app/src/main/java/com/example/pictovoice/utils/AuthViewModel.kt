package com.example.pictovoice.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pictovoice.Data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// (Revisar esta definición de AuthResult si no la has adaptado antes)
sealed class AuthResult {
    object Idle : AuthResult()
    object Loading : AuthResult()
    // Success ahora puede llevar datos, como el username generado en el registro
    data class Success(val data: Any? = null) : AuthResult()
    data class Error(val message: String) : AuthResult()

    val isLoading get() = this is Loading
    val isSuccess get() = this is Success
    val error get() = (this as? Error)?.message
}


class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _loginResult = MutableStateFlow<AuthResult>(AuthResult.Idle)
    val loginResult: StateFlow<AuthResult> = _loginResult

    private val _registerResult = MutableStateFlow<AuthResult>(AuthResult.Idle)
    val registerResult: StateFlow<AuthResult> = _registerResult

    // El login ahora recibe un "identifier" que puede ser username o email
    fun login(identifier: String, password: String) {
        _loginResult.value = AuthResult.Loading
        viewModelScope.launch {
            try {
                val result = authRepository.login(identifier, password) // El repo maneja la lógica
                if (result.isSuccess) {
                    _loginResult.value = AuthResult.Success() // No se necesita pasar data aquí para login
                } else {
                    _loginResult.value = AuthResult.Error(result.exceptionOrNull()?.message ?: "Error desconocido en login")
                }
            } catch (e: Exception) {
                _loginResult.value = AuthResult.Error(e.message ?: "Excepción en login")
            }
        }
    }

    // Register ya no recibe username, se generará en el repositorio
    fun register(fullName: String, email: String, password: String, role: String) {
        _registerResult.value = AuthResult.Loading
        viewModelScope.launch {
            try {
                val result = authRepository.register(fullName, email, password, role)
                if (result.isSuccess) {
                    // El resultado exitoso del repositorio debería contener el username generado
                    _registerResult.value = AuthResult.Success(result.getOrNull()) // getOrNull() de kotlin.Result
                } else {
                    _registerResult.value = AuthResult.Error(result.exceptionOrNull()?.message ?: "Error desconocido en registro")
                }
            } catch (e: Exception) {
                _registerResult.value = AuthResult.Error(e.message ?: "Excepción en registro")
            }
        }
    }
}