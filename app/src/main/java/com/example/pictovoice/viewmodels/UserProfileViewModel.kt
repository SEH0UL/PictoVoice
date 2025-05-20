package com.example.pictovoice.viewmodels

import User
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pictovoice.Data.repository.FirestoreRepository
import com.example.pictovoice.data.datasource.PictogramDataSource
import com.example.pictovoice.data.model.Pictogram
import com.example.pictovoice.utils.Result
import kotlinx.coroutines.launch

private const val TAG = "UserProfileVM"

/**
 * ViewModel para la [com.example.pictovoice.ui.userprofile.UserProfileActivity].
 * Gestiona la carga de datos del perfil de un usuario específico (ya sea el propio alumno o
 * un alumno visto por un profesor) y las acciones relacionadas como solicitar palabras o aprobarlas.
 * También calcula y expone las estadísticas del alumno.
 *
 * @property targetUserId El ID del usuario cuyo perfil se está mostrando.
 * @property firestoreRepository Repositorio para interactuar con Firestore.
 */
class UserProfileViewModel(
    private val targetUserId: String,
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {

    private val _userProfile = MutableLiveData<User?>()
    val userProfile: LiveData<User?> get() = _userProfile

    // LiveData para estadísticas
    private val _wordsUsedCount = MutableLiveData<Int>(0)
    val wordsUsedCount: LiveData<Int> get() = _wordsUsedCount
    private val _phrasesCreatedCount = MutableLiveData<Int>(0)
    val phrasesCreatedCount: LiveData<Int> get() = _phrasesCreatedCount
    private val _availableWordsCount = MutableLiveData<Int>(0)
    val availableWordsCount: LiveData<Int> get() = _availableWordsCount
    private val _lockedWordsCount = MutableLiveData<Int>(0)
    val lockedWordsCount: LiveData<Int> get() = _lockedWordsCount

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> get() = _isLoading
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    // Para el botón "Solicitar Palabras" del alumno
    private val _canRequestWords = MutableLiveData<Boolean>(false)
    val canRequestWords: LiveData<Boolean> get() = _canRequestWords
    private val _wordRequestOutcome = MutableLiveData<Result<Unit>?>() // Evento para el resultado de la solicitud
    val wordRequestOutcome: LiveData<Result<Unit>?> get() = _wordRequestOutcome

    // Para el botón "Aprobar Solicitud/Desbloquear Palabras" del profesor
    private val _approveWordRequestOutcome = MutableLiveData<Result<Unit>?>() // Evento para el resultado de la aprobación
    val approveWordRequestOutcome: LiveData<Result<Unit>?> get() = _approveWordRequestOutcome

    // Obtiene la lista completa de pictogramas desde la fuente de datos centralizada.
    private fun getSystemPictogramList(): List<Pictogram> {
        return PictogramDataSource.getAllPictograms()
    }

    init {
        loadUserProfile()
    }

    /**
     * Carga (o recarga) el perfil del usuario [targetUserId] desde Firestore.
     * Actualiza todos los LiveData relevantes, incluyendo el perfil, la elegibilidad para
     * solicitar palabras y las estadísticas calculadas.
     */
    fun loadUserProfile() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                Log.d(TAG, "Cargando perfil para usuario: $targetUserId")
                val result = firestoreRepository.getUser(targetUserId)
                if (result.isSuccess) {
                    val user = result.getOrNull()
                    _userProfile.value = user

                    // Actualizar estadísticas almacenadas directamente desde el objeto User
                    _wordsUsedCount.value = user?.wordsUsedCount ?: 0
                    _phrasesCreatedCount.value = user?.phrasesCreatedCount ?: 0

                    // Calcular y actualizar estadísticas dinámicas (disponibles/bloqueadas)
                    if (user != null) {
                        val allPictogramsInSystem = getSystemPictogramList()
                        if (allPictogramsInSystem.isNotEmpty()) {
                            val maxApprovedLevel = user.maxContentLevelApproved
                            val userUnlockedCategoryIds = user.unlockedCategories

                            val availablePictos = allPictogramsInSystem.filter { pict ->
                                pict.levelRequired <= maxApprovedLevel && userUnlockedCategoryIds.contains(pict.category)
                            }
                            _availableWordsCount.value = availablePictos.size
                            _lockedWordsCount.value = allPictogramsInSystem.size - availablePictos.size
                            Log.d(TAG, "Estadísticas Calculadas: TotalSistema=${allPictogramsInSystem.size}, NivelUsuario=${user.currentLevel}, MaxNivelAprobado=$maxApprovedLevel, CarpetasDesbloq=${userUnlockedCategoryIds.joinToString()}, Disp=${_availableWordsCount.value}, Bloq=${_lockedWordsCount.value}")
                        } else {
                            _availableWordsCount.value = 0
                            _lockedWordsCount.value = 0
                            Log.w(TAG, "getSystemPictogramList() devolvió vacío. Estadísticas calculadas serán 0.")
                        }
                    } else { // user es null
                        _availableWordsCount.value = 0
                        _lockedWordsCount.value = 0
                    }

                    // Determinar si el alumno puede solicitar palabras
                    _canRequestWords.value = user?.role == "student" &&
                            user.hasPendingWordRequest == false &&
                            (user.currentLevel > user.levelWordsRequestedFor)
                    Log.d(TAG, "Puede solicitar palabras: ${_canRequestWords.value}")
                } else {
                    _errorMessage.value = "Error al cargar el perfil: ${result.exceptionOrNull()?.message}"
                    // Resetear estadísticas si falla la carga del perfil
                    resetStatsToZero()
                }
            } catch (e: Exception) {
                _errorMessage.value = "Excepción al cargar perfil: ${e.message}"
                Log.e(TAG, "Excepción en loadUserProfile: ", e)
                resetStatsToZero()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun resetStatsToZero() {
        _wordsUsedCount.value = 0
        _phrasesCreatedCount.value = 0
        _availableWordsCount.value = 0
        _lockedWordsCount.value = 0
    }

    /**
     * Registra la solicitud de palabras del alumno para su nivel actual.
     * Actualiza `hasPendingWordRequest` a true y `levelWordsRequestedFor` al nivel actual.
     */
    fun requestWords() {
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
                Log.d(TAG, "Alumno ${currentUser.userId} solicitando palabras para nivel ${currentUser.currentLevel}")
                val result = firestoreRepository.recordStudentWordRequest(targetUserId, currentUser.currentLevel)
                if (result.isSuccess) {
                    _wordRequestOutcome.value = Result.Success(Unit)
                    // Actualizar localmente el perfil para reflejar la solicitud y re-evaluar 'canRequestWords'
                    val updatedUser = currentUser.copy(
                        hasPendingWordRequest = true,
                        levelWordsRequestedFor = currentUser.currentLevel
                    )
                    _userProfile.value = updatedUser // Esto disparará la reevaluación de _canRequestWords en el observer de userProfile
                    _canRequestWords.value = false // O directamente aquí para inmediatez
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

    /**
     * Aprueba la solicitud de palabras pendiente de un alumno (targetUserId).
     * Establece `hasPendingWordRequest` a `false` y actualiza `maxContentLevelApproved`
     * al nivel actual del alumno para el que se hizo la solicitud.
     * Llamado por un profesor.
     */
    fun approveWordRequest() {
        _isLoading.value = true
        viewModelScope.launch {
            val studentUser = _userProfile.value
            if (studentUser == null || studentUser.role != "student") {
                _errorMessage.value = "Usuario no válido para esta operación."
                _isLoading.value = false
                _approveWordRequestOutcome.value = Result.Failure(Exception("Usuario no es estudiante o es nulo."))
                return@launch
            }
            if (!studentUser.hasPendingWordRequest) {
                _errorMessage.value = "El alumno no tiene una solicitud de palabras pendiente."
                _isLoading.value = false
                return@launch
            }

            val levelToApproveContentFor = studentUser.currentLevel
            Log.d(TAG, "Profesor aprobando palabras para alumno ${studentUser.userId} en nivel $levelToApproveContentFor")

            try {
                val result = firestoreRepository.approveWordRequestAndSetContentLevel(targetUserId, levelToApproveContentFor)
                if (result.isSuccess) {
                    _approveWordRequestOutcome.value = Result.Success(Unit)
                    // Volver a cargar el perfil para reflejar todos los cambios y recalcular todo,
                    // incluyendo estadísticas y el estado de 'canRequestWords'.
                    loadUserProfile()
                    _errorMessage.value = "Contenido para Nivel $levelToApproveContentFor aprobado para ${studentUser.fullName}."
                } else {
                    _errorMessage.value = "Error al aprobar la solicitud: ${result.exceptionOrNull()?.message}"
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

    /** Llamado desde la UI para resetear el evento de resultado de solicitud de palabras. */
    fun clearWordRequestOutcome() {
        _wordRequestOutcome.value = null
    }

    /** Llamado desde la UI para resetear el evento de resultado de aprobación de palabras. */
    fun clearApproveWordRequestOutcome() {
        _approveWordRequestOutcome.value = null
    }

    /** Llamado desde la UI para limpiar un mensaje de error después de ser mostrado. */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}