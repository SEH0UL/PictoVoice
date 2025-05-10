package com.example.pictovoice.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.pictovoice.Data.Model.Pictogram
import com.example.pictovoice.Data.Model.User
import com.example.pictovoice.Data.repository.FirestoreRepository
import com.example.pictovoice.utils.Result // Asegúrate de importar tu Result personalizado
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FirestoreRepository()

    private val _pictograms = MutableLiveData<List<Pictogram>>()
    val pictograms: LiveData<List<Pictogram>> = _pictograms

    private val _userData = MutableLiveData<User?>()
    val userData: LiveData<User?> = _userData

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun loadUserDataAndPictograms(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                // Usamos kotlin.Result como está definido en FirestoreRepository
                val userResult = repository.getUser(userId) // Esto devuelve kotlin.Result<User?>

                if (userResult.isSuccess) {
                    val user: User? = userResult.getOrNull() // Obtiene el User o null
                    _userData.value = user
                    user?.let { validUser -> // Solo procede si el usuario no es null
                        // Determina el nivel máximo para cargar pictogramas
                        val maxLevel = if (validUser.role == "teacher") Int.MAX_VALUE else validUser.currentLevel
                        val categoriesToLoad = validUser.unlockedCategories.ifEmpty { listOf("basico") } // Carga "basico" si no hay ninguna

                        // Cargar pictogramas para la primera categoría desbloqueada (o "basico")
                        // Podrías querer una lógica más compleja para múltiples categorías aquí
                        if (categoriesToLoad.isNotEmpty()) {
                            val pictogramsResult = repository.getPictogramsByCategoryAndLevel(
                                categoriesToLoad.first(), // Carga para la primera categoría de la lista
                                maxLevel
                            ) // Esto devuelve kotlin.Result<List<Pictogram>>

                            if (pictogramsResult.isSuccess) {
                                _pictograms.value = pictogramsResult.getOrNull() ?: emptyList()
                            } else {
                                _errorMessage.value = "Error al cargar pictogramas: ${pictogramsResult.exceptionOrNull()?.message}"
                            }
                        } else {
                            _pictograms.value = emptyList() // No hay categorías para cargar
                        }
                    }
                } else { // userResult.isFailure
                    _errorMessage.value = "Error al cargar datos del usuario: ${userResult.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error inesperado: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onPictogramSelected(userId: String, pictogram: Pictogram) {
        viewModelScope.launch {
            _userData.value?.takeIf { it.role == "student" }?.let { user ->
                // Incrementar uso del pictograma (si aún lo necesitas desde Firestore)
                // repository.incrementPictogramUsage(pictogram.pictogramId) // Descomenta si es necesario

                // Añadir experiencia
                val expResult = repository.addExperienceToStudent(userId, pictogram.baseExp)

                if (expResult.isSuccess) {
                    val (newExp, newLevel) = expResult.getOrThrow() // Par (Int, Int)
                    _userData.value = user.copy(
                        currentExp = newExp,
                        currentLevel = newLevel,
                        totalExp = user.totalExp + pictogram.baseExp // Asegúrate que totalExp se actualiza correctamente
                    )
                } else {
                    _errorMessage.value = expResult.exceptionOrNull()?.message ?: "Error al actualizar experiencia"
                }
            }
        }
    }
}