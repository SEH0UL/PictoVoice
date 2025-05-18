package com.example.pictovoice.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pictovoice.Data.Model.User
import com.example.pictovoice.Data.repository.FirestoreRepository
import com.example.pictovoice.utils.Result
import kotlinx.coroutines.launch
import android.util.Log

class UserProfileViewModel(
    private val targetUserId: String,
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {

    private val _userProfile = MutableLiveData<User?>()
    val userProfile: LiveData<User?> = _userProfile

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _canRequestWords = MutableLiveData<Boolean>()
    val canRequestWords: LiveData<Boolean> = _canRequestWords

    private val _wordRequestOutcome = MutableLiveData<Result<Unit>?>()
    val wordRequestOutcome: LiveData<Result<Unit>?> = _wordRequestOutcome

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
                    // NUEVA LÓGICA para determinar si el botón "Solicitar Palabras" debe ser visible:
                    // 1. El usuario es un estudiante.
                    // 2. No hay una solicitud activa pendiente (hasPendingWordRequest == false).
                    // 3. El nivel actual del usuario es MAYOR que el nivel para el que ya solicitó palabras.
                    val canActuallyRequest = user?.role == "student" &&
                            user.hasPendingWordRequest == false &&
                            (user.currentLevel > user.levelWordsRequestedFor)
                    _canRequestWords.value = canActuallyRequest
                    Log.d("UserProfileVM", "loadUserProfile: User Level: ${user?.currentLevel}, RequestedForLevel: ${user?.levelWordsRequestedFor}, PendingReq: ${user?.hasPendingWordRequest}, CanRequest: $canActuallyRequest")
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
            // Validar si realmente puede solicitar según la lógica de _canRequestWords
            if (currentUser == null || currentUser.role != "student" ||
                currentUser.hasPendingWordRequest == true ||
                currentUser.currentLevel <= currentUser.levelWordsRequestedFor) {

                _errorMessage.value = "No se cumplen las condiciones para solicitar palabras ahora."
                Log.w("UserProfileVM", "requestWords: Pre-conditions not met. Level: ${currentUser?.currentLevel}, RequestedFor: ${currentUser?.levelWordsRequestedFor}, Pending: ${currentUser?.hasPendingWordRequest}")
                _isLoading.value = false
                // No actualizamos _wordRequestOutcome a Failure aquí necesariamente, ya que el botón no debería haber estado visible.
                // Pero si lo estuvo por algún error de sincronización, sí.
                _wordRequestOutcome.value = Result.Failure(Exception("Condiciones no cumplidas para la solicitud."))
                return@launch
            }

            try {
                // Usar la nueva función del repositorio que actualiza ambos campos
                val result = firestoreRepository.recordStudentWordRequest(targetUserId, currentUser.currentLevel)
                if (result.isSuccess) {
                    _wordRequestOutcome.value = Result.Success(Unit)
                    // Actualizar el perfil local para reflejar ambos cambios
                    _userProfile.value = currentUser.copy(
                        hasPendingWordRequest = true,
                        levelWordsRequestedFor = currentUser.currentLevel
                    )
                    _canRequestWords.value = false // Ocultar el botón tras la solicitud exitosa
                    _errorMessage.value = "Solicitud de palabras enviada para el Nivel ${currentUser.currentLevel}."
                } else {
                    _errorMessage.value = "Error al enviar la solicitud: ${result.exceptionOrNull()?.message}"
                    _wordRequestOutcome.value = Result.Failure(result.exceptionOrNull() ?: Exception("Error desconocido al registrar solicitud"))
                }
            } catch (e: Exception) {
                _errorMessage.value = "Excepción al enviar solicitud: ${e.message}"
                _wordRequestOutcome.value = Result.Failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun approveWordRequest() { // Esta función la usa el profesor
        _isLoading.value = true
        viewModelScope.launch {
            val studentUser = _userProfile.value // Este es el perfil del alumno que se está viendo
            if (studentUser == null || studentUser.role != "student") {
                _errorMessage.value = "Acción no permitida o usuario no válido para esta operación."
                _isLoading.value = false
                _approveWordRequestOutcome.value = Result.Failure(Exception("Usuario no es estudiante o es nulo."))
                return@launch
            }

            // El profesor solo necesita cambiar hasPendingWordRequest a false.
            // levelWordsRequestedFor ya fue establecido por el alumno.
            if (studentUser.hasPendingWordRequest == false) {
                _errorMessage.value = "El alumno no tiene una solicitud de palabras pendiente para aprobar."
                _isLoading.value = false
                return@launch
            }

            try {
                val result = firestoreRepository.updateUserWordRequestStatus(targetUserId, false) // targetUserId es el del alumno
                if (result.isSuccess) {
                    _approveWordRequestOutcome.value = Result.Success(Unit)
                    _userProfile.value = studentUser.copy(hasPendingWordRequest = false)
                    // La lógica de _canRequestWords en loadUserProfile se recalculará si se refresca el perfil,
                    // y correctamente resultará en false porque studentUser.currentLevel NO SERÁ > studentUser.levelWordsRequestedFor.
                    _errorMessage.value = "Solicitud de palabras aprobada para ${studentUser.fullName}."
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
        _approveWordRequestOutcome.value = null
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun clearWordRequestOutcome() {
        _wordRequestOutcome.value = null
    }
}