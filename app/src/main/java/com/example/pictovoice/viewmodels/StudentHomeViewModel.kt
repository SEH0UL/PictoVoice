package com.example.pictovoice.viewmodels

import User
import android.app.Application
import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.pictovoice.Data.datasource.PictogramDataSource
import com.example.pictovoice.Data.model.Category
import com.example.pictovoice.Data.model.Pictogram
import com.example.pictovoice.Data.repository.FirestoreRepository
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers // IMPORTANTE para withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext // IMPORTANTE para withContext

private const val MAX_PHRASE_PICTOGRAMS = 25
private const val TAG = "StudentHomeVM"

/**
 * ViewModel para la [com.example.pictovoice.ui.home.HomeActivity].
 * Gestiona los datos y la lógica de la pantalla principal del alumno, incluyendo:
 * - Carga y escucha de datos del usuario actual.
 * - Presentación de pictogramas fijos y dinámicos (basados en categorías y nivel aprobado).
 * - Construcción de frases con pictogramas.
 * - Reproducción de audio de las frases.
 * - Otorgamiento de experiencia (EXP) y manejo de eventos de subida de nivel.
 *
 * Utiliza [PictogramDataSource] para obtener las definiciones locales de pictogramas y categorías,
 * y [FirestoreRepository] para interactuar con Firestore para datos de usuario.
 */
class StudentHomeViewModel(application: Application) : AndroidViewModel(application) {
    private val firestoreRepository = FirestoreRepository()

    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> get() = _currentUser

    private val _phrasePictograms = MutableLiveData<List<Pictogram>>(emptyList())
    val phrasePictograms: LiveData<List<Pictogram>> get() = _phrasePictograms

    private val _pronounPictograms = MutableLiveData<List<Pictogram>>()
    val pronounPictograms: LiveData<List<Pictogram>> get() = _pronounPictograms

    private val _fixedVerbPictograms = MutableLiveData<List<Pictogram>>()
    val fixedVerbPictograms: LiveData<List<Pictogram>> get() = _fixedVerbPictograms

    private val _dynamicPictograms = MutableLiveData<List<Pictogram>>()
    val dynamicPictograms: LiveData<List<Pictogram>> get() = _dynamicPictograms

    private val _availableCategories = MutableLiveData<List<Category>>()
    val availableCategories: LiveData<List<Category>> get() = _availableCategories

    private val _currentDynamicCategoryName = MutableLiveData<String>()
    val currentDynamicCategoryName: LiveData<String> get() = _currentDynamicCategoryName

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _isCategoryLoading = MutableLiveData<Boolean>(false)
    val isCategoryLoading: LiveData<Boolean> get() = _isCategoryLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    private val _playButtonVisibility = MutableLiveData<Boolean>(false)
    val playButtonVisibility: LiveData<Boolean> get() = _playButtonVisibility

    private var previousUserLevelForNotification: Int? = null
    private val _levelUpEvent = MutableLiveData<Int?>(null)
    val levelUpEvent: LiveData<Int?> get() = _levelUpEvent

    private var userDocumentListener: ListenerRegistration? = null

    private val allLocalPictograms: List<Pictogram>
    private val allDefinedDynamicCategories: List<Category>

    private var mediaPlayer: MediaPlayer? = null
    private var audioQueue: MutableList<Int> = mutableListOf()

    init {
        Log.d(TAG, "ViewModel inicializado.")
        allDefinedDynamicCategories = PictogramDataSource.getAllDynamicCategories()
        allLocalPictograms = PictogramDataSource.getAllPictograms()
        Log.d(TAG, "Pictogramas locales: ${allLocalPictograms.size}, Categorías dinámicas: ${allDefinedDynamicCategories.size}")
        refreshUiBasedOnCurrentUser()
    }

    /**
     * Inicia la carga de los datos del usuario especificado y establece un listener
     * en tiempo real para su documento en Firestore.
     * @param userId El ID del usuario cuyos datos se van a cargar y escuchar.
     */
    fun loadAndListenToUserData(userId: String) {
        if (userId.isBlank()) {
            _errorMessage.value = "ID de usuario no válido para la escucha de datos."
            Log.w(TAG, "loadAndListenToUserData: userId está vacío.")
            _isLoading.value = false
            return
        }
        _isLoading.value = true
        userDocumentListener?.remove()

        Log.d(TAG, "Estableciendo listener para el usuario: $userId")
        userDocumentListener = firestoreRepository.addUserDocumentListener(
            userId,
            onUpdate = { updatedUser ->
                _isLoading.value = false
                val oldUser = _currentUser.value
                _currentUser.value = updatedUser

                if (updatedUser != null) {
                    Log.i(TAG, "Datos usuario (listener): Nivel ${updatedUser.currentLevel}, EXP ${updatedUser.currentExp}, MaxApproved ${updatedUser.maxContentLevelApproved}")

                    if (previousUserLevelForNotification == null && updatedUser.currentLevel >= 1) {
                        previousUserLevelForNotification = updatedUser.currentLevel
                    } else if (previousUserLevelForNotification != null && updatedUser.currentLevel > previousUserLevelForNotification!!) {
                        Log.i(TAG, "¡SUBIDA DE NIVEL (listener)! De $previousUserLevelForNotification a ${updatedUser.currentLevel}.")
                        _levelUpEvent.value = updatedUser.currentLevel
                        previousUserLevelForNotification = updatedUser.currentLevel
                    }

                    val categoriesActuallyChanged = oldUser?.unlockedCategories != updatedUser.unlockedCategories
                    val maxApprovedLevelActuallyChanged = oldUser?.maxContentLevelApproved != updatedUser.maxContentLevelApproved
                    val currentLevelActuallyChanged = oldUser?.currentLevel != updatedUser.currentLevel
                    val isFirstLoadOrCategoriesEmpty = oldUser == null || _availableCategories.value.isNullOrEmpty()

                    if (isFirstLoadOrCategoriesEmpty || currentLevelActuallyChanged || maxApprovedLevelActuallyChanged || categoriesActuallyChanged) {
                        Log.d(TAG, "Cambio significativo en usuario o primera carga, refrescando UI completa.")
                        refreshUiBasedOnCurrentUser()
                    } else {
                        Log.d(TAG, "Actualización de usuario (listener) sin cambios relevantes para Home. No se refresca UI completa.")
                    }
                } else {
                    Log.w(TAG, "Listener: datos de usuario nulos.")
                    refreshUiBasedOnCurrentUser()
                }
            },
            onError = { exception ->
                _isLoading.value = false
                _errorMessage.value = "Error escuchando datos del usuario: ${exception.message}"
                Log.e(TAG, "Error en listener de datos del usuario:", exception)
                _currentUser.value = null
                refreshUiBasedOnCurrentUser()
            }
        )
    }

    /**
     * Refresca los elementos de la UI basados en el estado actual de [_currentUser].
     * Mueve las operaciones de filtrado de listas grandes a hilos de fondo.
     */
    private fun refreshUiBasedOnCurrentUser() {
        Log.d(TAG, "Iniciando refreshUiBasedOnCurrentUser...")
        viewModelScope.launch {
            // isLoading global podría manejarse aquí también si es necesario,
            // pero loadAndListenToUserData y loadDynamicPictogramsByLocalCategory tienen sus propios indicadores.
            // _isLoading.value = true // Marcar inicio de refresco

            val user = _currentUser.value
            val contentAccessLevel = if (user != null && user.maxContentLevelApproved > 0) user.maxContentLevelApproved else 1
            val userUnlockedCategoryIds = user?.unlockedCategories ?: listOf(PictogramDataSource.CATEGORY_ID_COMIDA)

            Log.d(TAG, "refreshUi: NivelAccesoContenido=$contentAccessLevel, CarpetasDesbloq=${userUnlockedCategoryIds.joinToString()}")

            // Filtrar categorías disponibles (generalmente rápido)
            val filteredAvailableCategories = withContext(Dispatchers.Default) {
                allDefinedDynamicCategories.filter { category ->
                    userUnlockedCategoryIds.contains(category.categoryId)
                }.sortedBy { it.displayOrder }
            }
            _availableCategories.postValue(filteredAvailableCategories)

            // Filtrar pictogramas fijos en background
            val pronouns = withContext(Dispatchers.Default) {
                allLocalPictograms.filter {
                    it.category == PictogramDataSource.PRONOUNS_CATEGORY_ID && it.levelRequired <= contentAccessLevel
                }
            }
            _pronounPictograms.postValue(pronouns)

            val fixedVerbs = withContext(Dispatchers.Default) {
                allLocalPictograms.filter {
                    it.category == PictogramDataSource.FIXED_VERBS_CATEGORY_ID && it.levelRequired <= contentAccessLevel
                }
            }
            _fixedVerbPictograms.postValue(fixedVerbs)

            Log.d(TAG, "Pictogramas fijos y categorías disponibles actualizados.")

            // Determinar y cargar la categoría dinámica
            var categoryToLoad: Category? = null
            // Usar el valor recién actualizado de _availableCategories
            if (!filteredAvailableCategories.isNullOrEmpty()) {
                val currentName = _currentDynamicCategoryName.value // Obtener el nombre actual antes de cambiarlo
                categoryToLoad = if (!currentName.isNullOrBlank()) filteredAvailableCategories.find { it.name == currentName } else null
                if (categoryToLoad == null) { categoryToLoad = filteredAvailableCategories.find { it.categoryId == PictogramDataSource.CATEGORY_ID_COMIDA } }
                if (categoryToLoad == null) { categoryToLoad = filteredAvailableCategories.first() }
            }

            if (categoryToLoad != null) {
                loadDynamicPictogramsByLocalCategory(categoryToLoad, contentAccessLevel)
            } else {
                _dynamicPictograms.postValue(emptyList())
                _currentDynamicCategoryName.postValue("No hay categorías desbloqueadas")
                _isCategoryLoading.postValue(false) // Asegurar que el loading de categoría termina
            }
            // _isLoading.value = false // Marcar fin de refresco general
        }
    }

    /**
     * Carga los pictogramas para una [Category] dinámica específica, filtrándolos por el
     * nivel de acceso al contenido aprobado para el usuario. El filtrado se hace en background.
     * @param category La categoría dinámica seleccionada.
     * @param contentAccessLevelForFiltering El nivel máximo de contenido aprobado.
     */
    fun loadDynamicPictogramsByLocalCategory(
        category: Category,
        contentAccessLevelForFiltering: Int = _currentUser.value?.maxContentLevelApproved ?: 1 // VALOR POR DEFECTO ESENCIAL
    ) {
        _isCategoryLoading.value = true // Usar _isCategoryLoading si lo tienes para este propósito
        // o _isLoading.value = true;
        _currentDynamicCategoryName.value = category.name
        Log.d(TAG, "Cargando pictogramas dinámicos para '${category.name}', NivelAccesoContenido: $contentAccessLevelForFiltering")

        viewModelScope.launch { // Asegúrate de que el filtrado pesado se haga en un contexto de fondo
            val filteredPictos = withContext(Dispatchers.Default) {
                allLocalPictograms.filter { pict ->
                    pict.category == category.categoryId && pict.levelRequired <= contentAccessLevelForFiltering
                }
            }
            _dynamicPictograms.postValue(filteredPictos)
            Log.d(TAG, "Encontrados ${filteredPictos.size} pictogramas para '${category.name}' usando nivel acceso $contentAccessLevelForFiltering")
            _isCategoryLoading.postValue(false)
            // o _isLoading.postValue(false);
        }
    }

    /**
     * Añade un pictograma a la frase actual y solicita la adición de EXP al alumno.
     * La actualización del estado del usuario se reflejará a través del listener de Firestore.
     * @param pictogram El [Pictogram] a añadir.
     */
    fun addPictogramToPhrase(pictogram: Pictogram) {
        val currentList = _phrasePictograms.value ?: emptyList()
        if (currentList.size < MAX_PHRASE_PICTOGRAMS) {
            _phrasePictograms.value = currentList + pictogram
            updatePlayButtonVisibility()

            val student = _currentUser.value
            val userId = student?.userId
            if (userId != null && userId.isNotBlank() && pictogram.baseExp > 0) {
                viewModelScope.launch(Dispatchers.IO) { // Operación de Firestore en IO
                    Log.d(TAG, "Añadiendo ${pictogram.baseExp} EXP al usuario $userId por pictograma '${pictogram.name}'")
                    val expResult = firestoreRepository.addExperienceToStudent(userId, pictogram.baseExp)
                    // No es necesario actualizar _currentUser aquí; el listener se encarga.
                    // Loguear el resultado es útil para depuración.
                    withContext(Dispatchers.Main) {
                        if (expResult.isSuccess) {
                            Log.d(TAG, "EXP añadida en Firestore. El listener se encargará de actualizar el UI.")
                        } else {
                            _errorMessage.value = expResult.exceptionOrNull()?.message ?: "Error al añadir experiencia."
                            Log.e(TAG, "Fallo al añadir experiencia: ${expResult.exceptionOrNull()?.message}")
                        }
                    }
                }
            }
        } else {
            _errorMessage.value = "Has alcanzado el límite de $MAX_PHRASE_PICTOGRAMS pictogramas en la frase."
        }
    }

    /**
     * Llamado por la UI después de que la notificación de subida de nivel ha sido mostrada.
     */
    fun levelUpNotificationShown() {
        _levelUpEvent.value = null
    }

    /**
     * Elimina el último pictograma de la frase.
     */
    fun deleteLastPictogramFromPhrase() {
        val currentList = _phrasePictograms.value ?: emptyList()
        if (currentList.isNotEmpty()) {
            _phrasePictograms.value = currentList.dropLast(1)
            updatePlayButtonVisibility()
        }
    }

    /**
     * Limpia todos los pictogramas de la frase.
     */
    fun clearPhrase() {
        _phrasePictograms.value = emptyList()
        updatePlayButtonVisibility()
    }

    /**
     * Actualiza la visibilidad del botón de reproducir.
     */
    private fun updatePlayButtonVisibility() {
        _playButtonVisibility.value = _phrasePictograms.value?.isNotEmpty() == true
    }

    /**
     * Inicia la reproducción de audio de la frase y actualiza estadísticas.
     */
    fun onPlayPhraseClicked() {
        Log.i(TAG, "onPlayPhraseClicked: ¡FUNCIÓN LLAMADA!")
        val phrase = _phrasePictograms.value
        val student = _currentUser.value
        val userId = student?.userId
        Log.d(TAG, "onPlayPhraseClicked: Usuario $userId, Frase: ${phrase?.joinToString { it.name }}")

        if (phrase.isNullOrEmpty()) {
            _errorMessage.value = "No hay pictogramas en la frase para reproducir."
            return
        }

        if (userId != null && userId.isNotBlank()) {
            viewModelScope.launch(Dispatchers.IO) { // Operaciones de Firestore en IO
                val phrasesResult = firestoreRepository.incrementPhrasesCreatedCount(userId)
                Log.d(TAG, "Resultado incrementPhrasesCreatedCount: ${phrasesResult.isSuccess}")

                val wordsInPhraseCount = phrase.size
                if (wordsInPhraseCount > 0) {
                    val wordsResult = firestoreRepository.incrementWordsUsedCount(userId, wordsInPhraseCount)
                    Log.d(TAG, "Resultado incrementWordsUsedCount ($wordsInPhraseCount palabras): ${wordsResult.isSuccess}")
                }
            }
        }

        releaseMediaPlayer()
        audioQueue.clear()
        audioQueue.addAll(phrase.mapNotNull { it.audioResourceId.takeIf { resId -> resId != 0 } })

        if (audioQueue.isNotEmpty()) {
            Log.d(TAG, "Iniciando cola de audio con ${audioQueue.size} sonidos.")
            playNextAudioFromQueue()
        } else {
            val textToSpeak = phrase.joinToString(separator = " ") { it.name }
            _errorMessage.value = "Reproduciendo: $textToSpeak (Función TTS no implementada)"
        }
    }

    /**
     * Reproduce el siguiente audio en la cola.
     * La creación de MediaPlayer se hace en un hilo de fondo.
     */
    private fun playNextAudioFromQueue() {
        if (audioQueue.isEmpty()) {
            Log.d(TAG, "Cola de audio finalizada.")
            releaseMediaPlayer(); return
        }
        val audioResId = audioQueue.removeAt(0)
        Log.d(TAG, "Reproduciendo siguiente audio de la cola: ResId $audioResId. Restantes: ${audioQueue.size}")

        viewModelScope.launch { // Usar viewModelScope para la corrutina
            var mp: MediaPlayer? = null
            var creationSuccess = false
            withContext(Dispatchers.IO) { // MediaPlayer.create puede hacer I/O
                try {
                    mp = MediaPlayer.create(getApplication(), audioResId)
                    creationSuccess = (mp != null)
                } catch (e: Exception) {
                    Log.e(TAG, "Excepción al crear MediaPlayer para ResId $audioResId en hilo IO", e)
                }
            }

            if (creationSuccess) {
                mediaPlayer = mp
                mediaPlayer?.setOnCompletionListener {
                    Log.d(TAG, "Audio ResId $audioResId completado.")
                    playNextAudioFromQueue()
                }
                mediaPlayer?.setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "Error durante reproducción de audio ResId $audioResId: what=$what, extra=$extra")
                    releaseMediaPlayer()
                    playNextAudioFromQueue()
                    true
                }
                try {
                    mediaPlayer?.start()
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "IllegalStateException al hacer start() en MediaPlayer para ResId $audioResId", e)
                    releaseMediaPlayer()
                    playNextAudioFromQueue()
                }
            } else {
                Log.e(TAG, "MediaPlayer.create falló para ResId $audioResId (devolvió null o excepción). Saltando.")
                playNextAudioFromQueue()
            }
        }
    }

    /**
     * Libera los recursos del MediaPlayer.
     */
    private fun releaseMediaPlayer() {
        mediaPlayer?.run {
            try {
                if (isPlaying) {
                    stop()
                }
                release()
            } catch (e: IllegalStateException) {
                Log.e(TAG, "IllegalStateException al liberar MediaPlayer.", e)
            }
        }
        mediaPlayer = null
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel onCleared. Eliminando listener y liberando MediaPlayer.")
        userDocumentListener?.remove()
        releaseMediaPlayer()
    }
}