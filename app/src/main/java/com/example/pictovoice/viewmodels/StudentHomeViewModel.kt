package com.example.pictovoice.viewmodels // Asegúrate que el package es correcto

import android.app.Application
import android.speech.tts.TextToSpeech // Para la funcionalidad de TTS más adelante
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.pictovoice.Data.Model.Category
import com.example.pictovoice.Data.Model.Pictogram
import com.example.pictovoice.Data.Model.User
import com.example.pictovoice.Data.repository.FirestoreRepository
// Importa tu clase Result si la tienes definida globalmente
// import com.example.pictovoice.utils.Result
import kotlinx.coroutines.launch
import java.util.Locale

// Define constantes para las categorías fijas y la inicial
private const val PRONOUNS_CATEGORY_ID = "pronombres_fijos" // Debe coincidir con tu ID en Firestore
private const val FIXED_VERBS_CATEGORY_ID = "verbos_fijos"   // Debe coincidir con tu ID en Firestore
private const val INITIAL_DYNAMIC_CATEGORY_ID = "general"    // O la categoría por defecto que quieras

private const val MAX_PHRASE_PICTOGRAMS = 25

class StudentHomeViewModel(application: Application) : AndroidViewModel(application) {
    private val firestoreRepository = FirestoreRepository()
    // TextToSpeech engine - inicializar más tarde
    // private lateinit var tts: TextToSpeech

    // --- LiveData para la UI ---
    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser

    private val _phrasePictograms = MutableLiveData<List<Pictogram>>(emptyList())
    val phrasePictograms: LiveData<List<Pictogram>> = _phrasePictograms

    private val _pronounPictograms = MutableLiveData<List<Pictogram>>()
    val pronounPictograms: LiveData<List<Pictogram>> = _pronounPictograms

    private val _fixedVerbPictograms = MutableLiveData<List<Pictogram>>()
    val fixedVerbPictograms: LiveData<List<Pictogram>> = _fixedVerbPictograms

    private val _dynamicPictograms = MutableLiveData<List<Pictogram>>()
    val dynamicPictograms: LiveData<List<Pictogram>> = _dynamicPictograms

    private val _availableCategories = MutableLiveData<List<Category>>()
    val availableCategories: LiveData<List<Category>> = _availableCategories

    private val _currentDynamicCategoryName = MutableLiveData<String>()
    val currentDynamicCategoryName: LiveData<String> = _currentDynamicCategoryName

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _playButtonVisibility = MutableLiveData<Boolean>(false)
    val playButtonVisibility: LiveData<Boolean> = _playButtonVisibility


    init {
        // Inicializar TTS
        // tts = TextToSpeech(application) { status ->
        //    if (status == TextToSpeech.SUCCESS) {
        //        val result = tts.setLanguage(Locale("es", "ES")) // Español de España
        //        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
        //            Log.e("TTS", "Language not supported")
        //            _errorMessage.value = "El motor de Voz para Español no está disponible."
        //        } else {
        //            Log.i("TTS", "TTS Engine Initialized (Spanish)")
        //        }
        //    } else {
        //        Log.e("TTS", "Initialization failed")
        //        _errorMessage.value = "No se pudo inicializar el motor de Voz."
        //    }
        // }
    }

    fun loadInitialData(userId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                // Cargar datos del usuario
                val userResult = firestoreRepository.getUser(userId)
                if (userResult.isSuccess) {
                    _currentUser.value = userResult.getOrNull()
                    val user = userResult.getOrNull()

                    user?.let {
                        // Cargar pronombres
                        loadFixedCategory(PRONOUNS_CATEGORY_ID, _pronounPictograms, it.currentLevel)
                        // Cargar verbos fijos
                        loadFixedCategory(FIXED_VERBS_CATEGORY_ID, _fixedVerbPictograms, it.currentLevel)
                        // Cargar la categoría dinámica inicial
                        loadDynamicPictogramsByCategory(getCategoryByName(INITIAL_DYNAMIC_CATEGORY_ID) ?: Category(INITIAL_DYNAMIC_CATEGORY_ID, INITIAL_DYNAMIC_CATEGORY_ID.capitalize(Locale.ROOT)) , it.currentLevel)
                    }
                } else {
                    _errorMessage.value = "Error al cargar datos del usuario: ${userResult.exceptionOrNull()?.message}"
                }

                // Cargar lista de categorías disponibles (carpetas)
                // Esto es un placeholder, necesitas implementar la carga real de categorías.
                // Podrías tener una colección "categories" en Firestore.
                val categoriesResult = firestoreRepository.getStudentCategories() // Necesitarás este método
                if (categoriesResult.isSuccess) {
                    _availableCategories.value = categoriesResult.getOrNull()
                } else {
                    _availableCategories.value = listOf( // Fallback
                        Category("cat1", "Comida", 1),
                        Category("cat2", "Animales", 2),
                        Category("cat3", "Acciones", 3),
                        Category("cat4", "Lugares", 4),
                        Category(INITIAL_DYNAMIC_CATEGORY_ID, INITIAL_DYNAMIC_CATEGORY_ID.capitalize(Locale.ROOT), 0)
                    )
                    _errorMessage.value = "Error al cargar categorías: ${categoriesResult.exceptionOrNull()?.message}"
                }


            } catch (e: Exception) {
                _errorMessage.value = "Error inesperado: ${e.message}"
                Log.e("StudentHomeVM", "loadInitialData Error", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadFixedCategory(categoryId: String, liveData: MutableLiveData<List<Pictogram>>, userLevel: Int) {
        val result = firestoreRepository.getPictogramsByCategoryAndLevel(categoryId, userLevel)
        if (result.isSuccess) {
            liveData.postValue(result.getOrNull() ?: emptyList())
        } else {
            Log.e("StudentHomeVM", "Error loading fixed category $categoryId: ${result.exceptionOrNull()?.message}")
            liveData.postValue(emptyList()) // Para evitar que quede null
            // _errorMessage.postValue("Error cargando pictogramas para $categoryId") // Evita spam de errores
        }
    }

    fun loadDynamicPictogramsByCategory(category: Category, userLevel: Int? = _currentUser.value?.currentLevel) {
        if (userLevel == null) {
            _errorMessage.value = "Nivel de usuario no disponible para cargar pictogramas."
            return
        }
        _isLoading.value = true
        _currentDynamicCategoryName.value = category.name
        viewModelScope.launch {
            val result = firestoreRepository.getPictogramsByCategoryAndLevel(category.categoryId, userLevel)
            if (result.isSuccess) {
                _dynamicPictograms.value = result.getOrNull() ?: emptyList()
            } else {
                _dynamicPictograms.value = emptyList()
                _errorMessage.value = "Error cargando pictogramas para ${category.name}: ${result.exceptionOrNull()?.message}"
            }
            _isLoading.value = false
        }
    }

    private fun getCategoryByName(categoryName: String): Category? {
        return _availableCategories.value?.find { it.name.equals(categoryName, ignoreCase = true) || it.categoryId.equals(categoryName, ignoreCase = true) }
    }


    fun addPictogramToPhrase(pictogram: Pictogram) {
        val currentList = _phrasePictograms.value ?: emptyList()
        if (currentList.size < MAX_PHRASE_PICTOGRAMS) {
            val newList = currentList + pictogram
            _phrasePictograms.value = newList
            updatePlayButtonVisibility()
        } else {
            _errorMessage.value = "Has alcanzado el límite de ${MAX_PHRASE_PICTOGRAMS} pictogramas en la frase."
        }
    }

    fun deleteLastPictogramFromPhrase() {
        val currentList = _phrasePictograms.value ?: emptyList()
        if (currentList.isNotEmpty()) {
            _phrasePictograms.value = currentList.dropLast(1)
            updatePlayButtonVisibility()
        }
    }

    fun clearPhrase() {
        _phrasePictograms.value = emptyList()
        updatePlayButtonVisibility()
    }

    private fun updatePlayButtonVisibility() {
        _playButtonVisibility.value = _phrasePictograms.value?.isNotEmpty() == true
    }

    fun onPlayPhraseClicked() {
        val phrase = _phrasePictograms.value
        if (phrase.isNullOrEmpty()) {
            _errorMessage.value = "No hay nada que reproducir."
            return
        }

        val textToSpeak = phrase.joinToString(separator = " ") { it.name }
        Log.i("TTS", "Intentando reproducir: $textToSpeak")

        // Lógica de TTS (Text-To-Speech)
        // if (::tts.isInitialized) {
        //     tts.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "UtteranceId")
        // } else {
        //    _errorMessage.value = "El motor de voz no está listo."
        // }
        // Por ahora, solo un Toast o Log
        _errorMessage.value = "Reproduciendo: $textToSpeak (TTS no implementado aún)"
    }

    // Necesitarás crear estos métodos en FirestoreRepository.kt
    // suspend fun getPictogramsByCategory(categoryName: String, userLevel: Int): Result<List<Pictogram>>
    // suspend fun getStudentCategories(): Result<List<Category>>


    override fun onCleared() {
        super.onCleared()
        // if (::tts.isInitialized) {
        //     tts.stop()
        //     tts.shutdown()
        // }
    }
}