package com.example.pictovoice.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pictovoice.Data.Model.User
import com.example.pictovoice.Data.repository.FirestoreRepository
// Asegúrate de que tu clase Result está importada si está en otro paquete.
// Asumiendo que está en com.example.pictovoice.utils.Result
import com.example.pictovoice.utils.Result // Importa tu clase Result
import kotlinx.coroutines.launch

class UserProfileViewModel(
    private val targetUserId: String,
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {

    private val _userProfile = MutableLiveData<User?>()
    val userProfile: LiveData<User?> = _userProfile

    private val _isLoading = MutableLiveData<Boolean>(false) // Inicializar con un valor
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // LiveData para la visibilidad del botón "Solicitar Palabras"
    private val _canRequestWords = MutableLiveData<Boolean>()
    val canRequestWords: LiveData<Boolean> = _canRequestWords

    // LiveData para feedback tras enviar la solicitud - AHORA NULABLE
    private val _wordRequestOutcome = MutableLiveData<Result<Unit>?>()
    val wordRequestOutcome: LiveData<Result<Unit>?> = _wordRequestOutcome

    // LiveData para feedback tras aprobar la solicitud - AHORA NULABLE
    private val _approveWordRequestOutcome = MutableLiveData<Result<Unit>?>()
    val approveWordRequestOutcome: LiveData<Result<Unit>?> = _approveWordRequestOutcome

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
                _wordRequestOutcome.value = Result.Failure(Exception("Acción no permitida o usuario no válido.")) // Asegúrate que Result.Failure es correcto
                return@launch
            }

            if (currentUser.hasPendingWordRequest == true) {
                _errorMessage.value = "Ya existe una solicitud pendiente."
                _isLoading.value = false
                _wordRequestOutcome.value = Result.Failure(Exception("Ya existe una solicitud pendiente."))
                return@launch
            }

            try {
                val result = firestoreRepository.updateUserWordRequestStatus(targetUserId, true)
                if (result.isSuccess) {
                    _wordRequestOutcome.value = Result.Success(Unit)
                    _userProfile.value = currentUser.copy(hasPendingWordRequest = true)
                    _canRequestWords.value = false
                } else {
                    _errorMessage.value = "Error al enviar la solicitud: ${result.exceptionOrNull()?.message}"
                    _wordRequestOutcome.value = Result.Failure(result.exceptionOrNull() ?: Exception("Error desconocido"))
                }
            } catch (e: Exception) {
                _errorMessage.value = "Excepción al enviar solicitud: ${e.message}"
                _wordRequestOutcome.value = Result.Failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun approveWordRequest() {
        _isLoading.value = true
        viewModelScope.launch {
            val currentUser = _userProfile.value
            if (currentUser == null || currentUser.role != "student") {
                _errorMessage.value = "Acción no permitida o usuario no válido para esta operación."
                _isLoading.value = false
                _approveWordRequestOutcome.value = Result.Failure(Exception("Usuario no es estudiante o es nulo."))
                return@launch
            }

            if (currentUser.hasPendingWordRequest == false) {
                _errorMessage.value = "El alumno no tiene una solicitud de palabras pendiente."
                // Si decides no tratar esto como un error que detiene el flujo, puedes simplemente no hacer nada o
                // _approveWordRequestOutcome.value = Result.Success(Unit) // O un mensaje informativo si no quieres un error
                // _errorMessage.value = "No hay solicitud pendiente para aprobar." (opcional)
                _isLoading.value = false // Asegúrate de que isLoading se establece en false
                return@launch
            }

            try {
                val result = firestoreRepository.updateUserWordRequestStatus(targetUserId, false)
                if (result.isSuccess) {
                    _approveWordRequestOutcome.value = Result.Success(Unit)
                    _userProfile.value = currentUser.copy(hasPendingWordRequest = false)
                    // _canRequestWords se actualizará automáticamente cuando userProfile cambie,
                    // si la Activity observa userProfile y recalcula canRequestWords,
                    // o si actualizas _canRequestWords aquí también:
                    _canRequestWords.value = true // El alumno ahora puede volver a solicitar si sube de nivel, etc.
                    // La lógica de _canRequestWords = user?.role == "student" && user.hasPendingWordRequest == false
                    // se encargará de esto al actualizar _userProfile.value.

                    _errorMessage.value = "Solicitud de palabras aprobada para ${currentUser.fullName}."
                } else {
                    _errorMessage.value = "Error al aprobar la solicitud: ${result.exceptionOrNull()?.message}"
                    _approveWordRequestOutcome.value = Result.Failure(result.exceptionOrNull() ?: Exception("Error desconocido al aprobar solicitud"))
                }
            } catch (e: Exception) {
                _errorMessage.value = "Excepción al aprobar solicitud: ${e.message}"
                _approveWordRequestOutcome.value = Result.Failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearApproveWordRequestOutcome() {
        _approveWordRequestOutcome.value = null // Ahora esto es válido
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun clearWordRequestOutcome() {
        _wordRequestOutcome.value = null // Ahora esto es válido
    }
}