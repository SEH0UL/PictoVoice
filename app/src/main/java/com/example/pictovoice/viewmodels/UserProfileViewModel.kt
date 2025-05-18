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
                    val canActuallyRequest = user?.role == "student" &&
                            user.hasPendingWordRequest == false &&
                            (user.currentLevel > user.levelWordsRequestedFor)
                    _canRequestWords.value = canActuallyRequest
                    Log.d("UserProfileVM", "loadUserProfile: User Level: ${user?.currentLevel}, ReqForLvl: ${user?.levelWordsRequestedFor}, Pending: ${user?.hasPendingWordRequest}, MaxApprovedLvl: ${user?.maxContentLevelApproved}, CanRequest: $canActuallyRequest")
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

    fun requestWords() { // Llamado por el alumno
        _isLoading.value = true
        viewModelScope.launch {
            val currentUser = _userProfile.value
            if (currentUser == null || currentUser.role != "student" ||
                currentUser.hasPendingWordRequest == true ||
                currentUser.currentLevel <= currentUser.levelWordsRequestedFor) {
                _errorMessage.value = "No se cumplen las condiciones para solicitar palabras ahora."
                _isLoading.value = false
                _wordRequestOutcome.value = Result.Failure(Exception("Condiciones no cumplidas para la solicitud."))
                return@launch
            }

            try {
                val result = firestoreRepository.recordStudentWordRequest(targetUserId, currentUser.currentLevel)
                if (result.isSuccess) {
                    _wordRequestOutcome.value = Result.Success(Unit)
                    _userProfile.value = currentUser.copy(
                        hasPendingWordRequest = true,
                        levelWordsRequestedFor = currentUser.currentLevel
                    )
                    _canRequestWords.value = false
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

    fun approveWordRequest() { // Llamado por el profesor desde el perfil del alumno (targetUserId)
        _isLoading.value = true
        viewModelScope.launch {
            val studentUser = _userProfile.value // Este es el User del alumno (targetUserId)
            if (studentUser == null || studentUser.role != "student") {
                _errorMessage.value = "Usuario no válido para esta operación."
                _isLoading.value = false
                _approveWordRequestOutcome.value = Result.Failure(Exception("Usuario no es estudiante o es nulo."))
                return@launch
            }

            if (!studentUser.hasPendingWordRequest) {
                _errorMessage.value = "El alumno no tiene una solicitud de palabras pendiente."
                _isLoading.value = false
                // Opcionalmente, podrías permitir desbloquear proactivamente aquí si no hay solicitud
                // pero eso complicaría el estado levelWordsRequestedFor. Por ahora, solo aprobamos si hay solicitud.
                return@launch
            }

            // El profesor aprueba el contenido para el nivel actual del alumno (que es studentUser.currentLevel)
            // Este es el nivel para el cual el alumno solicitó, y es el nivel que estamos aprobando.
            val levelToApproveContentFor = studentUser.currentLevel

            try {
                // Usar la nueva función del repositorio
                val result = firestoreRepository.approveWordRequestAndSetContentLevel(targetUserId, levelToApproveContentFor)
                if (result.isSuccess) {
                    _approveWordRequestOutcome.value = Result.Success(Unit)
                    // Actualizar el perfil local para reflejar los cambios
                    _userProfile.value = studentUser.copy(
                        hasPendingWordRequest = false,
                        maxContentLevelApproved = levelToApproveContentFor
                    )
                    // La lógica de _canRequestWords se reevaluará debido al cambio en _userProfile,
                    // y debería seguir siendo false porque studentUser.currentLevel no será > studentUser.levelWordsRequestedFor.
                    _errorMessage.value = "Contenido para Nivel $levelToApproveContentFor aprobado para ${studentUser.fullName}."
                } else {
                    _errorMessage.value = "Error al aprobar la solicitud y establecer nivel de contenido: ${result.exceptionOrNull()?.message}"
                    _approveWordRequestOutcome.value = Result.Failure(result.exceptionOrNull() ?: Exception("Error desconocido al aprobar"))
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