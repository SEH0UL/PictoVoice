package com.example.pictovoice.viewmodels

import android.app.Application
import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.pictovoice.Data.Model.Category
import com.example.pictovoice.Data.Model.Pictogram
import com.example.pictovoice.Data.Model.User
import com.example.pictovoice.Data.repository.FirestoreRepository
import com.example.pictovoice.data.datasource.PictogramDataSource

import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

private const val MAX_PHRASE_PICTOGRAMS = 25

class StudentHomeViewModel(application: Application) : AndroidViewModel(application) {
    private val firestoreRepository = FirestoreRepository()

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

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _playButtonVisibility = MutableLiveData<Boolean>(false)
    val playButtonVisibility: LiveData<Boolean> = _playButtonVisibility

    private var allLocalPictograms: List<Pictogram>
    private var allDefinedDynamicCategories: List<Category>

    private var mediaPlayer: MediaPlayer? = null
    private var audioQueue: MutableList<Int> = mutableListOf()

    private var previousUserLevelForNotification: Int? = null
    private val _levelUpEvent = MutableLiveData<Int?>(null)
    val levelUpEvent: LiveData<Int?> = _levelUpEvent

    private var userDocumentListener: ListenerRegistration? = null

    init {
        Log.d("StudentHomeVM", "ViewModel created.")
        allDefinedDynamicCategories = PictogramDataSource.getAllDynamicCategories()
        allLocalPictograms = PictogramDataSource.getAllPictograms()
        refreshUiBasedOnCurrentUser()
    }

    fun loadAndListenToUserData(userId: String) {
        if (userId.isBlank()) {
            _errorMessage.value = "ID de usuario no válido para escuchar."
            return
        }
        _isLoading.value = true
        userDocumentListener?.remove()

        userDocumentListener = firestoreRepository.addUserDocumentListener(
            userId,
            onUpdate = { updatedUser ->
                _isLoading.value = false
                _currentUser.value = updatedUser

                if (updatedUser != null) {
                    Log.d("StudentHomeVM", "User data updated via listener. Level: ${updatedUser.currentLevel}, MaxApproved: ${updatedUser.maxContentLevelApproved}")
                    if (previousUserLevelForNotification == null && updatedUser.currentLevel >= 1) {
                        previousUserLevelForNotification = updatedUser.currentLevel
                    } else if (previousUserLevelForNotification != null && updatedUser.currentLevel > previousUserLevelForNotification!!) {
                        Log.d("StudentHomeVM", "Level UP DETECTED via listener! From $previousUserLevelForNotification to ${updatedUser.currentLevel}. Posting event.")
                        _levelUpEvent.value = updatedUser.currentLevel
                        previousUserLevelForNotification = updatedUser.currentLevel
                    }
                }
                refreshUiBasedOnCurrentUser()
            },
            onError = { exception ->
                _isLoading.value = false
                _errorMessage.value = "Error escuchando datos del usuario: ${exception.message}"
                Log.e("StudentHomeVM", "Error listener user data", exception)
                _currentUser.value = null
                refreshUiBasedOnCurrentUser()
            }
        )
    }

    private fun refreshUiBasedOnCurrentUser() {
        val user = _currentUser.value
        val contentAccessLevel = if (user != null && user.maxContentLevelApproved > 0) user.maxContentLevelApproved else 1
        val userUnlockedCategoryIds = user?.unlockedCategories ?: listOf(PictogramDataSource.CATEGORY_ID_COMIDA)

        Log.d("StudentHomeVM", "refreshUi: User ActualLvl: ${user?.currentLevel}, ContentAccessLvl: $contentAccessLevel, UnlockedFolders: $userUnlockedCategoryIds")

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
            if (!currentName.isNullOrBlank()) {
                categoryToLoad = availableDynamicCats.find { it.name == currentName }
            }
            if (categoryToLoad == null) { categoryToLoad = availableDynamicCats.find { it.categoryId == PictogramDataSource.CATEGORY_ID_COMIDA } }
            if (categoryToLoad == null) { categoryToLoad = availableDynamicCats.first() }
        }

        if (categoryToLoad != null) {
            loadDynamicPictogramsByLocalCategory(categoryToLoad, contentAccessLevel)
        } else {
            _dynamicPictograms.value = emptyList()
            _currentDynamicCategoryName.value = "No hay categorías desbloqueadas"
        }
    }

    fun loadDynamicPictogramsByLocalCategory(
        category: Category,
        contentAccessLevelForFiltering: Int = _currentUser.value?.maxContentLevelApproved ?: 1
    ) {
        _isLoading.value = true
        _currentDynamicCategoryName.value = category.name
        Log.d("StudentHomeVM", "Loading dynamic pictos for ${category.name} (ID: ${category.categoryId}), using contentAccessLevel: $contentAccessLevelForFiltering")

        val filteredPictos = allLocalPictograms.filter { pict ->
            pict.category == category.categoryId && pict.levelRequired <= contentAccessLevelForFiltering
        }
        _dynamicPictograms.value = filteredPictos
        Log.d("StudentHomeVM", "Found ${filteredPictos.size} pictos for ${category.name}")
        _isLoading.value = false
    }

    fun addPictogramToPhrase(pictogram: Pictogram) {
        val currentList = _phrasePictograms.value ?: emptyList()
        if (currentList.size < MAX_PHRASE_PICTOGRAMS) {
            val newList = currentList + pictogram
            _phrasePictograms.value = newList
            updatePlayButtonVisibility()

            val student = _currentUser.value
            val userId = student?.userId
            if (userId != null && userId.isNotBlank() && pictogram.baseExp > 0) {
                viewModelScope.launch {
                    Log.d("StudentHomeVM", "Adding ${pictogram.baseExp} EXP to user $userId for pictogram ${pictogram.name}")
                    val expResult = firestoreRepository.addExperienceToStudent(userId, pictogram.baseExp)
                    if (expResult.isSuccess) {
                        Log.d("StudentHomeVM", "EXP added. Firestore listener will handle user data and UI refresh.")
                    } else {
                        _errorMessage.value = expResult.exceptionOrNull()?.message ?: "Error al añadir experiencia."
                    }
                }
            }
        } else {
            _errorMessage.value = "Has alcanzado el límite de $MAX_PHRASE_PICTOGRAMS pictogramas."
        }
    }

    fun levelUpNotificationShown() {
        _levelUpEvent.value = null
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
        val student = _currentUser.value
        val userId = student?.userId

        if (phrase.isNullOrEmpty()) {
            _errorMessage.value = "No hay nada que reproducir."
            return
        }

        if (userId != null && userId.isNotBlank()) {
            viewModelScope.launch {
                firestoreRepository.incrementPhrasesCreatedCount(userId)
                val wordsInPhraseCount = phrase.size
                if (wordsInPhraseCount > 0) {
                    firestoreRepository.incrementWordsUsedCount(userId, wordsInPhraseCount)
                }
            }
        }

        releaseMediaPlayer()
        audioQueue.clear()
        audioQueue.addAll(phrase.mapNotNull { it.audioResourceId.takeIf { id -> id != 0 } })

        if (audioQueue.isNotEmpty()) {
            Log.d("StudentHomeVM", "Starting audio queue with ${audioQueue.size} sounds.")
            playNextAudioFromQueue()
        } else {
            val textToSpeak = phrase.joinToString(separator = " ") { it.name }
            Log.i("StudentHomeVM_TTS", "No local audios in phrase, fallback to TTS (not implemented): $textToSpeak")
            _errorMessage.value = "Reproduciendo: $textToSpeak (TTS no implementado)"
        }
    }

    private fun playNextAudioFromQueue() {
        if (audioQueue.isEmpty()) {
            Log.d("StudentHomeVM", "Audio queue finished.")
            releaseMediaPlayer(); return
        }
        val audioResId = audioQueue.removeAt(0)
        try {
            mediaPlayer = MediaPlayer.create(getApplication(), audioResId)
            if (mediaPlayer == null) {
                Log.e("StudentHomeVM_MediaPlayer", "MediaPlayer.create failed for ResId $audioResId. Skipping.")
                playNextAudioFromQueue(); return
            }
            mediaPlayer?.setOnCompletionListener { playNextAudioFromQueue() }
            mediaPlayer?.setOnErrorListener { _, what, extra ->
                Log.e("StudentHomeVM_MediaPlayer", "Error playback: what=$what, extra=$extra for ResId=$audioResId")
                releaseMediaPlayer(); playNextAudioFromQueue(); true
            }
            mediaPlayer?.start()
        } catch (e: Exception) {
            Log.e("StudentHomeVM_MediaPlayer", "Exception for ResId=$audioResId", e)
            releaseMediaPlayer(); playNextAudioFromQueue()
        }
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.run { if (isPlaying) stop(); release() }
        mediaPlayer = null
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("StudentHomeVM", "ViewModel onCleared. Removing user document listener.")
        userDocumentListener?.remove()
        releaseMediaPlayer()
    }
}