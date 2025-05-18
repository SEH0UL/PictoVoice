package com.example.pictovoice.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pictovoice.Data.Model.Pictogram
import com.example.pictovoice.Data.Model.User
import com.example.pictovoice.Data.repository.FirestoreRepository
import com.example.pictovoice.data.datasource.PictogramDataSource // IMPORTANTE: Importar tu DataSource
import com.example.pictovoice.utils.Result
import kotlinx.coroutines.launch
import android.util.Log

class UserProfileViewModel(
    private val targetUserId: String,
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {

    private val _userProfile = MutableLiveData<User?>()
    val userProfile: LiveData<User?> = _userProfile

    private val _wordsUsedCount = MutableLiveData<Int>(0)
    val wordsUsedCount: LiveData<Int> = _wordsUsedCount
    private val _phrasesCreatedCount = MutableLiveData<Int>(0)
    val phrasesCreatedCount: LiveData<Int> = _phrasesCreatedCount
    private val _availableWordsCount = MutableLiveData<Int>(0)
    val availableWordsCount: LiveData<Int> = _availableWordsCount
    private val _lockedWordsCount = MutableLiveData<Int>(0)
    val lockedWordsCount: LiveData<Int> = _lockedWordsCount

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

    private fun getSystemPictogramList(): List<Pictogram> {
        return PictogramDataSource.getAllPictograms() // Obtener de la fuente de datos centralizada
    }

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

                    _wordsUsedCount.value = user?.wordsUsedCount ?: 0
                    _phrasesCreatedCount.value = user?.phrasesCreatedCount ?: 0

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
                            Log.d("UserProfileVM", "Stats Calculated: TotalInSystem=${allPictogramsInSystem.size}, UserLvl=${user.currentLevel}, MaxApprovedLvl=$maxApprovedLevel, UnlockedFolders=${userUnlockedCategoryIds.joinToString()}, Avail=${_availableWordsCount.value}, Locked=${_lockedWordsCount.value}")
                        } else {
                            _availableWordsCount.value = 0
                            _lockedWordsCount.value = 0
                            Log.w("UserProfileVM", "getSystemPictogramList() returned empty. Calculated stats are 0.")
                        }
                    } else {
                        _availableWordsCount.value = 0
                        _lockedWordsCount.value = 0
                    }

                    val canActuallyRequest = user?.role == "student" &&
                            user.hasPendingWordRequest == false &&
                            (user.currentLevel > user.levelWordsRequestedFor)
                    _canRequestWords.value = canActuallyRequest
                } else {
                    _errorMessage.value = "Error al cargar el perfil: ${result.exceptionOrNull()?.message}"
                    _wordsUsedCount.value = 0
                    _phrasesCreatedCount.value = 0
                    _availableWordsCount.value = 0
                    _lockedWordsCount.value = 0
                }
            } catch (e: Exception) {
                _errorMessage.value = "Excepción al cargar perfil: ${e.message}"
                _wordsUsedCount.value = 0
                _phrasesCreatedCount.value = 0
                _availableWordsCount.value = 0
                _lockedWordsCount.value = 0
            } finally {
                _isLoading.value = false
            }
        }
    }

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
                val result = firestoreRepository.recordStudentWordRequest(targetUserId, currentUser.currentLevel)
                if (result.isSuccess) {
                    _wordRequestOutcome.value = Result.Success(Unit)
                    val updatedUser = currentUser.copy(
                        hasPendingWordRequest = true,
                        levelWordsRequestedFor = currentUser.currentLevel
                    )
                    _userProfile.value = updatedUser
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

            try {
                val result = firestoreRepository.approveWordRequestAndSetContentLevel(targetUserId, levelToApproveContentFor)
                if (result.isSuccess) {
                    _approveWordRequestOutcome.value = Result.Success(Unit)
                    // El listener en UserProfileViewModel (si se implementara para este VM)
                    // o una nueva llamada a loadUserProfile() refrescaría _userProfile.
                    // Por ahora, actualizamos manualmente y recargamos para asegurar consistencia.
                    // loadUserProfile() // Esto refrescará _userProfile y recalculará _canRequestWords
                    // y las estadísticas.
                    // O actualiza localmente y confía en que es consistente con Firestore:
                    val updatedUserLocal = studentUser.copy(
                        hasPendingWordRequest = false,
                        maxContentLevelApproved = levelToApproveContentFor
                    )
                    _userProfile.value = updatedUserLocal
                    _canRequestWords.value = updatedUserLocal.role == "student" &&
                            updatedUserLocal.hasPendingWordRequest == false &&
                            (updatedUserLocal.currentLevel > updatedUserLocal.levelWordsRequestedFor)
                    _errorMessage.value = "Contenido para Nivel $levelToApproveContentFor aprobado para ${studentUser.fullName}."

                    // Para que las estadísticas se recalculen con el nuevo maxContentLevelApproved,
                    // es mejor llamar a loadUserProfile() para que rehaga todos los cálculos.
                    loadUserProfile()

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