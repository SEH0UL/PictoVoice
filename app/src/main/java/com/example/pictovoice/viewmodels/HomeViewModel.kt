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
                when (val userResult = repository.getUser(userId)) {
                    is Result.Success -> {
                        _userData.value = userResult.data
                        userResult.data?.let { user ->
                            val maxLevel = if (user.role == "teacher") Int.MAX_VALUE else user.currentLevel
                            val categories = user.unlockedCategories

                            if (categories.isNotEmpty()) {
                                when (val pictogramsResult = repository.getPictogramsByCategoryAndLevel(
                                    categories.first(),
                                    maxLevel
                                )) {
                                    is Result.Success -> _pictograms.value = pictogramsResult.data
                                    is Result.Failure -> _errorMessage.value = "Error al cargar pictogramas"
                                }
                            }
                        }
                    }
                    is Result.Failure -> _errorMessage.value = "Error al cargar datos del usuario"
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
                repository.incrementPictogramUsage(pictogram.pictogramId)

                when (val result = repository.addExperienceToStudent(userId, pictogram.baseExp)) {
                    is Result.Success -> {
                        _userData.value = user.copy(
                            currentExp = result.data.first,
                            currentLevel = result.data.second,
                            totalExp = user.totalExp + pictogram.baseExp
                        )
                    }
                    is Result.Failure -> {
                        _errorMessage.value = result.message ?: "Error al actualizar experiencia"
                    }
                }
            }
        }
    }
}