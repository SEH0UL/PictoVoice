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
import kotlinx.coroutines.launch

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
 * y [FirestoreRepository] para interactuar con Firestore para datos de usuario y clases.
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
        Log.d(TAG, "Pictogramas locales cargados: ${allLocalPictograms.size}, Categorías dinámicas definidas: ${allDefinedDynamicCategories.size}")
        refreshUiBasedOnCurrentUser() // Llamada inicial con _currentUser.value (que será null)
    }

    /**
     * Inicia la carga de los datos del usuario especificado y establece un listener
     * en tiempo real para su documento en Firestore.
     * Cualquier cambio en el documento del usuario (ej. nivel, EXP, categorías desbloqueadas,
     * nivel de contenido aprobado) se reflejará automáticamente en la UI.
     * @param userId El ID del usuario cuyos datos se van a cargar y escuchar.
     */
    fun loadAndListenToUserData(userId: String) {
        if (userId.isBlank()) {
            _errorMessage.value = "ID de usuario no válido para la escucha de datos."
            Log.w(TAG, "loadAndListenToUserData: userId está vacío.")
            _isLoading.value = false // Asegurar que isLoading se resetea
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
                    Log.i(TAG, "Datos del usuario actualizados (listener): ${updatedUser.fullName}, Nivel: ${updatedUser.currentLevel}, EXP: ${updatedUser.currentExp}, MaxApprovedLvl: ${updatedUser.maxContentLevelApproved}, UnlockedCats: ${updatedUser.unlockedCategories.joinToString()}")

                    if (previousUserLevelForNotification == null && updatedUser.currentLevel >= 1) {
                        previousUserLevelForNotification = updatedUser.currentLevel
                    } else if (previousUserLevelForNotification != null && updatedUser.currentLevel > previousUserLevelForNotification!!) {
                        Log.i(TAG, "¡SUBIDA DE NIVEL DETECTADA (listener)! De $previousUserLevelForNotification a ${updatedUser.currentLevel}.")
                        _levelUpEvent.value = updatedUser.currentLevel
                        previousUserLevelForNotification = updatedUser.currentLevel
                    }

                    // Optimización: Solo refrescar la UI completa si hay cambios relevantes que afecten la visualización de pictogramas/categorías
                    if (oldUser == null || // Primera carga
                        oldUser.currentLevel != updatedUser.currentLevel ||
                        oldUser.maxContentLevelApproved != updatedUser.maxContentLevelApproved ||
                        oldUser.unlockedCategories != updatedUser.unlockedCategories ||
                        _availableCategories.value.isNullOrEmpty() ) {
                        Log.d(TAG, "Cambio significativo en usuario (o primera carga) detectado vía listener, refrescando UI completa.")
                        refreshUiBasedOnCurrentUser()
                    } else {
                        // Aunque solo cambie la EXP, no es necesario un refresh completo de listas de HomeActivity
                        // La EXP se verá actualizada en UserProfileActivity cuando se cargue.
                        Log.d(TAG, "Actualización de usuario (listener) sin cambios en nivel/permisos/categorías para Home. No se refresca UI completa.")
                    }
                } else {
                    Log.w(TAG, "Listener de usuario: datos de usuario nulos (posiblemente eliminado).")
                    refreshUiBasedOnCurrentUser()
                }
            },
            onError = { exception ->
                _isLoading.value = false
                _errorMessage.value = "Error al escuchar datos del usuario: ${exception.message}"
                Log.e(TAG, "Error en listener de datos del usuario:", exception)
                _currentUser.value = null
                refreshUiBasedOnCurrentUser()
            }
        )
    }

    /**
     * Refresca todos los elementos de la UI (categorías disponibles, pictogramas fijos,
     * categoría dinámica actual y sus pictogramas) basándose en el estado actual de [_currentUser].
     * Utiliza `maxContentLevelApproved` para determinar la visibilidad del contenido.
     */
    private fun refreshUiBasedOnCurrentUser() {
        val user = _currentUser.value
        val contentAccessLevel = if (user != null && user.maxContentLevelApproved > 0) user.maxContentLevelApproved else 1
        val userUnlockedCategoryIds = user?.unlockedCategories ?: listOf(PictogramDataSource.CATEGORY_ID_COMIDA)

        Log.d(TAG, "refreshUiBasedOnCurrentUser: NivelAccesoContenido=$contentAccessLevel, CarpetasDesbloqueadas=${userUnlockedCategoryIds.joinToString()}")

        _availableCategories.value = allDefinedDynamicCategories.filter { category ->
            userUnlockedCategoryIds.contains(category.categoryId)
        }.sortedBy { it.displayOrder }

        _pronounPictograms.value = allLocalPictograms.filter {
            it.category == PictogramDataSource.PRONOUNS_CATEGORY_ID && it.levelRequired <= contentAccessLevel
        }
        _fixedVerbPictograms.value = allLocalPictograms.filter {
            it.category == PictogramDataSource.FIXED_VERBS_CATEGORY_ID && it.levelRequired <= contentAccessLevel
        }

        var categoryToLoad: Category? = null
        val availableDynamicCats = _availableCategories.value
        if (!availableDynamicCats.isNullOrEmpty()) {
            val currentName = _currentDynamicCategoryName.value
            categoryToLoad = if (!currentName.isNullOrBlank()) availableDynamicCats.find { it.name == currentName } else null
            if (categoryToLoad == null) { categoryToLoad = availableDynamicCats.find { it.categoryId == PictogramDataSource.CATEGORY_ID_COMIDA } }
            if (categoryToLoad == null) { categoryToLoad = availableDynamicCats.first() }
        }

        if (categoryToLoad != null) {
            loadDynamicPictogramsByLocalCategory(categoryToLoad, contentAccessLevel)
        } else {
            _dynamicPictograms.value = emptyList()
            _currentDynamicCategoryName.value = "No hay categorías desbloqueadas"
            Log.w(TAG, "No hay categorías dinámicas disponibles para mostrar.")
        }
    }

    /**
     * Carga los pictogramas para una [Category] dinámica específica, filtrándolos por el
     * nivel de acceso al contenido aprobado para el usuario.
     * @param category La categoría dinámica seleccionada.
     * @param contentAccessLevelForFiltering El nivel máximo de contenido aprobado para el usuario actual.
     * Este valor se obtiene de `_currentUser.value?.maxContentLevelApproved`.
     */
    fun loadDynamicPictogramsByLocalCategory(
        category: Category,
        contentAccessLevelForFiltering: Int = _currentUser.value?.maxContentLevelApproved ?: 1
    ) {
        val user = _currentUser.value
        val actualContentAccessLevel = if (user != null && user.maxContentLevelApproved > 0) user.maxContentLevelApproved else 1

        _currentDynamicCategoryName.value = category.name
        Log.d(TAG, "Cargando pictogramas dinámicos para '${category.name}', NivelAccesoContenido REAL: $actualContentAccessLevel (param fue: $contentAccessLevelForFiltering)")

        val filteredPictos = allLocalPictograms.filter { pict ->
            pict.category == category.categoryId && pict.levelRequired <= actualContentAccessLevel
        }
        _dynamicPictograms.value = filteredPictos
        Log.d(TAG, "Encontrados ${filteredPictos.size} pictogramas para '${category.name}' con nivel acceso $actualContentAccessLevel")
    }

    /**
     * Añade un pictograma a la frase actual y otorga EXP al alumno.
     * La actualización del estado del usuario (nivel, EXP, categorías desbloqueadas)
     * se reflejará a través del listener de Firestore.
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
                viewModelScope.launch {
                    Log.d(TAG, "Añadiendo ${pictogram.baseExp} EXP al usuario $userId por pictograma '${pictogram.name}'")
                    val expResult = firestoreRepository.addExperienceToStudent(userId, pictogram.baseExp)
                    if (expResult.isSuccess) {
                        Log.d(TAG, "EXP añadida en Firestore. El listener de Firestore en loadAndListenToUserData se encargará de actualizar _currentUser y la UI.")
                        // No actualizamos _currentUser aquí; el listener lo hará para evitar doble refresco o datos inconsistentes.
                    } else {
                        _errorMessage.value = expResult.exceptionOrNull()?.message ?: "Error al añadir experiencia."
                        Log.e(TAG, "Fallo al añadir experiencia: ${expResult.exceptionOrNull()?.message}")
                    }
                }
            }
        } else {
            _errorMessage.value = "Has alcanzado el límite de $MAX_PHRASE_PICTOGRAMS pictogramas en la frase."
        }
    }

    /**
     * Llamado por la UI después de que la notificación de subida de nivel ha sido mostrada.
     * Resetea el evento para evitar que se muestre de nuevo.
     */
    fun levelUpNotificationShown() {
        _levelUpEvent.value = null
    }

    /**
     * Elimina el último pictograma añadido a la frase actual.
     */
    fun deleteLastPictogramFromPhrase() {
        val currentList = _phrasePictograms.value ?: emptyList()
        if (currentList.isNotEmpty()) {
            _phrasePictograms.value = currentList.dropLast(1)
            updatePlayButtonVisibility()
        }
    }

    /**
     * Limpia todos los pictogramas de la frase actual.
     */
    fun clearPhrase() {
        _phrasePictograms.value = emptyList()
        updatePlayButtonVisibility()
    }

    private fun updatePlayButtonVisibility() {
        _playButtonVisibility.value = _phrasePictograms.value?.isNotEmpty() == true
    }

    fun onPlayPhraseClicked() {
        val phrase = _phrasePictograms.value
        val student = _currentUser.value
        val userId = student?.userId
        Log.d(TAG, "onPlayPhraseClicked: Usuario $userId, Frase: ${phrase?.joinToString { it.name }}") // Log inicial

        if (phrase.isNullOrEmpty()) {
            _errorMessage.value = "No hay pictogramas en la frase para reproducir."
            return
        }

        if (userId != null && userId.isNotBlank()) {
            viewModelScope.launch {
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
            Log.i(TAG, "No hay audios locales en la frase, fallback a TTS (no implementado): $textToSpeak")
            _errorMessage.value = "Reproduciendo: $textToSpeak (Función TTS no implementada)"
        }
    }

    private fun playNextAudioFromQueue() {
        if (audioQueue.isEmpty()) {
            Log.d(TAG, "Cola de audio finalizada.")
            releaseMediaPlayer(); return
        }
        val audioResId = audioQueue.removeAt(0)
        Log.d(TAG, "Reproduciendo siguiente audio de la cola: ResId $audioResId. Restantes: ${audioQueue.size}")
        try {
            mediaPlayer = MediaPlayer.create(getApplication(), audioResId)?.apply {
                setOnCompletionListener {
                    Log.d(TAG, "Audio ResId $audioResId completado.")
                    playNextAudioFromQueue()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "Error durante reproducción de audio ResId $audioResId: what=$what, extra=$extra")
                    releaseMediaPlayer(); playNextAudioFromQueue(); true
                }
                start()
            }
            if (mediaPlayer == null) {
                Log.e(TAG, "MediaPlayer.create falló para ResId $audioResId. Saltando.")
                playNextAudioFromQueue()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción al crear o iniciar MediaPlayer para ResId $audioResId", e)
            releaseMediaPlayer(); playNextAudioFromQueue()
        }
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.run { if (isPlaying) stop(); release() }
        mediaPlayer = null
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel onCleared. Eliminando listener de documento de usuario y liberando MediaPlayer.")
        userDocumentListener?.remove()
        releaseMediaPlayer()
    }
}