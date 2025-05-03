package com.example.pictovoice.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pictovoice.Data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch



class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _loginResult = MutableStateFlow<AuthResult>(AuthResult.Idle)
    val loginResult: StateFlow<AuthResult> = _loginResult

    private val _registerResult = MutableStateFlow<AuthResult>(AuthResult.Idle)
    val registerResult: StateFlow<AuthResult> = _registerResult

    fun login(username: String, password: String) {
        _loginResult.value = AuthResult.Loading
        viewModelScope.launch {
            try {
                val result = authRepository.login(username, password)
                if (result.isSuccess) {
                    _loginResult.value = AuthResult.Success
                } else {
                    _loginResult.value =
                        AuthResult.Error(result.exceptionOrNull()?.message ?: "Error desconocido")
                }
            } catch (e: Exception) {
                _loginResult.value = AuthResult.Error(e.message ?: "Error desconocido")
            }
        }
    }

    fun register(fullName: String, username: String, password: String) {
        _registerResult.value = AuthResult.Loading
        viewModelScope.launch {
            try {
                val result = authRepository.register(fullName, username, password)
                if (result.isSuccess) {
                    _registerResult.value = AuthResult.Success
                } else {
                    _registerResult.value =
                        AuthResult.Error(result.exceptionOrNull()?.message ?: "Error desconocido")
                }
            } catch (e: Exception) {
                _registerResult.value = AuthResult.Error(e.message ?: "Error desconocido")
            }
        }
    }
}

sealed class AuthResult {
    object Idle : AuthResult()
    object Loading : AuthResult()
    object Success : AuthResult()
    data class Error(val message: String) : AuthResult()

    val isIdle get() = this is Idle
    val isLoading get() = this is Loading
    val isSuccess get() = this is Success
    val isError get() = this is Error
    val error get() = (this as? Error)?.message
}