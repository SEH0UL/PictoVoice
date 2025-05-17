package com.example.pictovoice.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pictovoice.Data.Model.User
import com.example.pictovoice.Data.repository.FirestoreRepository
import kotlinx.coroutines.launch

class UserProfileViewModel(
    private val targetUserId: String,
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {

    private val _userProfile = MutableLiveData<User?>()
    val userProfile: LiveData<User?> = _userProfile

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // LiveData para la visibilidad del botón "Solicitar Palabras"
    private val _canRequestWords = MutableLiveData<Boolean>()
    val canRequestWords: LiveData<Boolean> = _canRequestWords

    // LiveData para feedback tras enviar la solicitud
    private val _wordRequestOutcome = MutableLiveData<Result<Unit>>()
    val wordRequestOutcome: LiveData<Result<Unit>> = _wordRequestOutcome


    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = firestoreRepository.getUser(targetUserId)
                if (result.isSuccess) {
                    val user = result.getOrNull()
                    _userProfile.value = user
                    // Determinar si el botón "Solicitar Palabras" debe ser visible
                    // Lógica simple: visible si no hay solicitud pendiente y es un estudiante.
                    // Se podría añadir lógica de nivel aquí si es necesario.
                    _canRequestWords.value = user?.role == "student" && user.hasPendingWordRequest == false
                } else {
                    _errorMessage.value = "Error al cargar el perfil: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Excepción al cargar perfil: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun requestWords() {
        _isLoading.value = true
        viewModelScope.launch {
            val currentUser = _userProfile.value
            if (currentUser == null || currentUser.role != "student") {
                _errorMessage.value = "Acción no permitida o usuario no válido."
                _isLoading.value = false
                _wordRequestOutcome.value = Result.failure(Exception("Acción no permitida o usuario no válido."))
                return@launch
            }

            if (currentUser.hasPendingWordRequest == true) {
                _errorMessage.value = "Ya existe una solicitud pendiente."
                _isLoading.value = false
                _wordRequestOutcome.value = Result.failure(Exception("Ya existe una solicitud pendiente."))
                return@launch
            }

            try {
                val result = firestoreRepository.updateUserWordRequestStatus(targetUserId, true)
                if (result.isSuccess) {
                    _wordRequestOutcome.value = Result.success(Unit)
                    // Actualizar el perfil local para reflejar el cambio
                    _userProfile.value = currentUser.copy(hasPendingWordRequest = true)
                    _canRequestWords.value = false // Ocultar el botón tras la solicitud
                } else {
                    _errorMessage.value = "Error al enviar la solicitud: ${result.exceptionOrNull()?.message}"
                    _wordRequestOutcome.value = Result.failure(result.exceptionOrNull() ?: Exception("Error desconocido"))
                }
            } catch (e: Exception) {
                _errorMessage.value = "Excepción al enviar solicitud: ${e.message}"
                _wordRequestOutcome.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
    fun clearWordRequestOutcome() {
        _wordRequestOutcome.value = null // O un estado Idle si lo prefieres
    }
}