package com.example.pictovoice.viewmodels // Asegúrate que el package es correcto

import android.app.Application
import android.media.MediaPlayer // Para reproducir audios locales
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.pictovoice.Data.Model.Category
import com.example.pictovoice.Data.Model.Pictogram
import com.example.pictovoice.Data.Model.User
import com.example.pictovoice.Data.repository.FirestoreRepository // Aún lo necesitamos para el usuario
import com.example.pictovoice.R // Importante para acceder a los recursos locales
import kotlinx.coroutines.launch
import java.util.Locale

// Constantes para IDs de categorías locales (debes tener pictogramas con estos categoryId)
private const val PRONOUNS_CATEGORY_ID = "local_pronombres"
private const val FIXED_VERBS_CATEGORY_ID = "local_verbos"
private const val INITIAL_DYNAMIC_CATEGORY_ID = "local_comida" // Ejemplo, asegúrate de tener esta categoría y pictos

private const val MAX_PHRASE_PICTOGRAMS = 25

class StudentHomeViewModel(application: Application) : AndroidViewModel(application) {
    private val firestoreRepository = FirestoreRepository()

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

    // --- Listas locales de datos ---
    private var allLocalPictograms: List<Pictogram> = emptyList()
    private var allLocalCategories: List<Category> = emptyList()

    // MediaPlayer para audios locales
    private var mediaPlayer: MediaPlayer? = null
    private var audioQueue: MutableList<Int> = mutableListOf()


    init {
        Log.d("StudentHomeVM", "ViewModel created. Initializing local data.")
        initializeLocalData()
    }

    private fun initializeLocalData() {
        _isLoading.value = true
        allLocalCategories = createLocalCategories()
        _availableCategories.value = allLocalCategories

        allLocalPictograms = createLocalPictograms()

        val userLevelForLocal = _currentUser.value?.currentLevel ?: 1

        _pronounPictograms.value = allLocalPictograms.filter {
            it.category == PRONOUNS_CATEGORY_ID && it.levelRequired <= userLevelForLocal
        }
        _fixedVerbPictograms.value = allLocalPictograms.filter {
            it.category == FIXED_VERBS_CATEGORY_ID && it.levelRequired <= userLevelForLocal
        }

        val initialCategory = allLocalCategories.find { it.categoryId == INITIAL_DYNAMIC_CATEGORY_ID }
        if (initialCategory != null) {
            loadDynamicPictogramsByLocalCategory(initialCategory, userLevelForLocal)
        } else if (allLocalCategories.isNotEmpty()) {
            // Fallback a la primera categoría si la inicial no se encuentra
            loadDynamicPictogramsByLocalCategory(allLocalCategories.first(), userLevelForLocal)
        } else {
            _dynamicPictograms.value = emptyList()
            _currentDynamicCategoryName.value = "Categorías no disponibles"
            Log.w("StudentHomeVM", "No categories available, including initial dynamic category.")
        }
        _isLoading.value = false
    }

    fun loadCurrentUserData(userId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val userResult = firestoreRepository.getUser(userId)
                if (userResult.isSuccess) {
                    _currentUser.value = userResult.getOrNull()
                    // Recargar pictogramas locales con el nivel de usuario correcto
                    // Esto es importante si el nivel del usuario afecta la visibilidad
                    val userLevel = _currentUser.value?.currentLevel ?: 1
                    _pronounPictograms.value = allLocalPictograms.filter { it.category == PRONOUNS_CATEGORY_ID && it.levelRequired <= userLevel }
                    _fixedVerbPictograms.value = allLocalPictograms.filter { it.category == FIXED_VERBS_CATEGORY_ID && it.levelRequired <= userLevel }

                    val currentCategory = _availableCategories.value?.find { it.name == _currentDynamicCategoryName.value }
                        ?: allLocalCategories.find { it.categoryId == INITIAL_DYNAMIC_CATEGORY_ID }
                        ?: allLocalCategories.firstOrNull()

                    currentCategory?.let {
                        loadDynamicPictogramsByLocalCategory(it, userLevel)
                    }

                } else {
                    _errorMessage.value = "Error al cargar datos del usuario: ${userResult.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Excepción al cargar datos del usuario: ${e.message}"
                Log.e("StudentHomeVM", "loadCurrentUserData Exception", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun createLocalCategories(): List<Category> {
        Log.d("StudentHomeVM", "Creating local categories")
        // IMPORTANTE: Añade tus propios iconos para carpetas si los tienes en res/drawable
        return listOf(
            Category(categoryId = PRONOUNS_CATEGORY_ID, name = "Pronombres", displayOrder = 0 /*, iconResourceId = R.drawable.ic_folder_pronouns*/),
            Category(categoryId = FIXED_VERBS_CATEGORY_ID, name = "Verbos", displayOrder = 1 /*, iconResourceId = R.drawable.ic_folder_verbs*/),
            Category(categoryId = INITIAL_DYNAMIC_CATEGORY_ID, name = "Comida", displayOrder = 2 /*, iconResourceId = R.drawable.ic_folder_food*/),
            Category(categoryId = "local_animales", name = "Animales", displayOrder = 3 /*, iconResourceId = R.drawable.ic_folder_animals*/),
            Category(categoryId = "local_acciones", name = "Acciones", displayOrder = 4 /*, iconResourceId = R.drawable.ic_folder_actions*/),
            Category(categoryId = "local_objetos", name = "Objetos", displayOrder = 5)
        )
    }

    private fun createLocalPictograms(): List<Pictogram> {
        Log.d("StudentHomeVM", "Creating local pictograms list")
        // ¡¡¡RECUERDA!!!
        // Los nombres de archivo en R.drawable y R.raw deben coincidir
        // con los archivos que tienes en tus carpetas res/drawable y res/raw.
        // Ejemplo: R.drawable.picto_yo debe corresponder a un archivo picto_yo.png (o .xml, etc.)
        // en res/drawable. Y R.raw.audio_yo a un archivo audio_yo.mp3 (u otro formato) en res/raw.

        val pictos = mutableListOf<Pictogram>()

        // --- PRONOMBRES ---
        // Usando PRONOUNS_CATEGORY_ID = "local_pronombres"
        pictos.add(Pictogram(pictogramId = "local_pro_001", name = "Yo", category = PRONOUNS_CATEGORY_ID, imageResourceId = R.drawable.picto_yo, audioResourceId = R.raw.audio_yo, levelRequired = 1))
        pictos.add(Pictogram(pictogramId = "local_pro_002", name = "Tú", category = PRONOUNS_CATEGORY_ID, imageResourceId = R.drawable.picto_tu, audioResourceId = R.raw.audio_tu, levelRequired = 1))
        pictos.add(Pictogram(pictogramId = "local_pro_003", name = "Él", category = PRONOUNS_CATEGORY_ID, imageResourceId = R.drawable.picto_el, audioResourceId = R.raw.audio_el, levelRequired = 1))
        pictos.add(Pictogram(pictogramId = "local_pro_004", name = "Ella", category = PRONOUNS_CATEGORY_ID, imageResourceId = R.drawable.picto_ella, audioResourceId = R.raw.audio_ella, levelRequired = 1))
        pictos.add(Pictogram(pictogramId = "local_pro_005", name = "Nosotros", category = PRONOUNS_CATEGORY_ID, imageResourceId = R.drawable.picto_nosotros, audioResourceId = R.raw.audio_nosotros, levelRequired = 1))
        pictos.add(Pictogram(pictogramId = "local_pro_006", name = "Vosotros", category = PRONOUNS_CATEGORY_ID, imageResourceId = R.drawable.picto_vosotros, audioResourceId = R.raw.audio_vosotros, levelRequired = 1))
        pictos.add(Pictogram(pictogramId = "local_pro_007", name = "Ellos", category = PRONOUNS_CATEGORY_ID, imageResourceId = R.drawable.picto_ellos, audioResourceId = R.raw.audio_ellos, levelRequired = 1)) // "Ellos/Ellas" - ajusta el 'name' si es necesario

        // --- VERBOS FIJOS ---
        // Usando FIXED_VERBS_CATEGORY_ID = "local_verbos"
        pictos.add(Pictogram(pictogramId = "local_vrb_001", name = "Ser", category = FIXED_VERBS_CATEGORY_ID, imageResourceId = R.drawable.picto_ser, audioResourceId = R.raw.audio_ser, levelRequired = 1)) // O "Ser/Estar"
        pictos.add(Pictogram(pictogramId = "local_vrb_002", name = "Querer", category = FIXED_VERBS_CATEGORY_ID, imageResourceId = R.drawable.picto_querer, audioResourceId = R.raw.audio_querer, levelRequired = 1)) // Asumo que tenías 'picto_querer' aunque no esté en la imagen
        pictos.add(Pictogram(pictogramId = "local_vrb_003", name = "Ir", category = FIXED_VERBS_CATEGORY_ID, imageResourceId = R.drawable.picto_ir, audioResourceId = R.raw.audio_ir, levelRequired = 1))
        pictos.add(Pictogram(pictogramId = "local_vrb_004", name = "Tener", category = FIXED_VERBS_CATEGORY_ID, imageResourceId = R.drawable.picto_tener, audioResourceId = R.raw.audio_tener, levelRequired = 1)) // Asumo que tenías 'picto_tener'
        pictos.add(Pictogram(pictogramId = "local_vrb_005", name = "Ver", category = FIXED_VERBS_CATEGORY_ID, imageResourceId = R.drawable.picto_ver, audioResourceId = R.raw.audio_ver, levelRequired = 1)) // Asumo que tenías 'picto_ver'
        pictos.add(Pictogram(pictogramId = "local_vrb_006", name = "Poder", category = FIXED_VERBS_CATEGORY_ID, imageResourceId = R.drawable.picto_poder, audioResourceId = R.raw.audio_poder, levelRequired = 1))
        pictos.add(Pictogram(pictogramId = "local_vrb_007", name = "Dar", category = FIXED_VERBS_CATEGORY_ID, imageResourceId = R.drawable.picto_dar, audioResourceId = R.raw.audio_dar, levelRequired = 1))
        pictos.add(Pictogram(pictogramId = "local_vrb_008", name = "Venir", category = FIXED_VERBS_CATEGORY_ID, imageResourceId = R.drawable.picto_venir, audioResourceId = R.raw.audio_venir, levelRequired = 1))
        pictos.add(Pictogram(pictogramId = "local_vrb_009", name = "Coger", category = FIXED_VERBS_CATEGORY_ID, imageResourceId = R.drawable.picto_coger, audioResourceId = R.raw.audio_coger, levelRequired = 1))
        pictos.add(Pictogram(pictogramId = "local_vrb_010", name = "Ayudar", category = FIXED_VERBS_CATEGORY_ID, imageResourceId = R.drawable.picto_ayudar, audioResourceId = R.raw.audio_ayudar, levelRequired = 1))


        // --- AQUÍ AÑADIRÍAS EL RESTO DE TUS PICTOGRAMAS PARA OTRAS CATEGORÍAS ---
        // Ejemplo para la categoría inicial dinámica (comida)
        // private const val INITIAL_DYNAMIC_CATEGORY_ID = "local_comida"
        // --- COMIDA ---
        pictos.add(Pictogram(pictogramId = "local_com_001", name = "Desayuno", category = "local_comida", imageResourceId = R.drawable.picto_desayuno, audioResourceId = R.raw.audio_desayuno, levelRequired = 1))
        pictos.add(Pictogram(pictogramId = "local_com_002", name = "Almuerzo", category = "local_comida", imageResourceId = R.drawable.picto_almuerzo, audioResourceId = R.raw.audio_almuerzo, levelRequired = 1))
        pictos.add(Pictogram(pictogramId = "local_com_003", name = "Comida", category = "local_comida", imageResourceId = R.drawable.picto_comida, audioResourceId = R.raw.audio_comida, levelRequired = 1)) // Referido a la comida principal del día
        pictos.add(Pictogram(pictogramId = "local_com_004", name = "Merienda", category = "local_comida", imageResourceId = R.drawable.picto_merienda, audioResourceId = R.raw.audio_merienda, levelRequired = 1))
        pictos.add(Pictogram(pictogramId = "local_com_005", name = "Cena", category = "local_comida", imageResourceId = R.drawable.picto_cena, audioResourceId = R.raw.audio_cena, levelRequired = 1))
        pictos.add(Pictogram(pictogramId = "local_com_006", name = "Agua", category = "local_comida", imageResourceId = R.drawable.picto_agua, audioResourceId = R.raw.audio_agua, levelRequired = 1))
        pictos.add(Pictogram(pictogramId = "local_com_007", name = "Verdura", category = "local_comida", imageResourceId = R.drawable.picto_verdura, audioResourceId = R.raw.audio_verduras, levelRequired = 1))
        pictos.add(Pictogram(pictogramId = "local_com_008", name = "Pasta", category = "local_comida", imageResourceId = R.drawable.picto_pasta, audioResourceId = R.raw.audio_pasta, levelRequired = 1))
        pictos.add(Pictogram(pictogramId = "local_com_009", name = "Hortalizas", category = "local_comida", imageResourceId = R.drawable.picto_hortalizas, audioResourceId = R.raw.audio_hortalizas, levelRequired = 1))
        pictos.add(Pictogram(pictogramId = "local_com_010", name = "Lácteos", category = "local_comida", imageResourceId = R.drawable.picto_lacteos, audioResourceId = R.raw.audio_lacteos, levelRequired = 1))
        pictos.add(Pictogram(pictogramId = "local_com_011", name = "Frutas", category = "local_comida", imageResourceId = R.drawable.picto_frutas, audioResourceId = R.raw.audio_frutas, levelRequired = 1))
        pictos.add(Pictogram(pictogramId = "local_com_012", name = "Dulces", category = "local_comida", imageResourceId = R.drawable.picto_dulces, audioResourceId = R.raw.audio_dulces, levelRequired = 1))
        pictos.add(Pictogram(pictogramId = "local_com_013", name = "Carne", category = "local_comida", imageResourceId = R.drawable.picto_carne, audioResourceId = R.raw.audio_carne, levelRequired = 1))
        pictos.add(Pictogram(pictogramId = "local_com_014", name = "Sopa", category = "local_comida", imageResourceId = R.drawable.picto_sopa, audioResourceId = R.raw.audio_sopa, levelRequired = 1))
        pictos.add(Pictogram(pictogramId = "local_com_015", name = "Pizza", category = "local_comida", imageResourceId = R.drawable.picto_pizza, audioResourceId = R.raw.audio_pizza, levelRequired = 1))
        pictos.add(Pictogram(pictogramId = "local_com_016", name = "Pescado", category = "local_comida", imageResourceId = R.drawable.picto_pescado, audioResourceId = R.raw.audio_pescado, levelRequired = 1))
        pictos.add(Pictogram(pictogramId = "local_com_017", name = "Paella", category = "local_comida", imageResourceId = R.drawable.picto_paella, audioResourceId = R.raw.audio_paella, levelRequired = 1))
        pictos.add(Pictogram(pictogramId = "local_com_018", name = "Miel", category = "local_comida", imageResourceId = R.drawable.picto_miel, audioResourceId = R.raw.audio_miel, levelRequired = 1))
        pictos.add(Pictogram(pictogramId = "local_com_019", name = "Macarrones", category = "local_comida", imageResourceId = R.drawable.picto_macarrones, audioResourceId = R.raw.audio_macarrones, levelRequired = 1))
        pictos.add(Pictogram(pictogramId = "local_com_020", name = "Lentejas", category = "local_comida", imageResourceId = R.drawable.picto_lentejas, audioResourceId = R.raw.audio_lentejas, levelRequired = 1))
        pictos.add(Pictogram(pictogramId = "local_com_021", name = "Jamón", category = "local_comida", imageResourceId = R.drawable.picto_jamon, audioResourceId = R.raw.audio_jamon, levelRequired = 1)) // Recurso: picto_jamon
        pictos.add(Pictogram(pictogramId = "local_com_022", name = "Hamburguesa", category = "local_comida", imageResourceId = R.drawable.picto_hamburguesa, audioResourceId = R.raw.audio_hamburguesa, levelRequired = 1))
        pictos.add(Pictogram(pictogramId = "local_com_023", name = "Guisantes", category = "local_comida", imageResourceId = R.drawable.picto_guisantes, audioResourceId = R.raw.audio_guisantes, levelRequired = 1))
        pictos.add(Pictogram(pictogramId = "local_com_024", name = "Espaguetis", category = "local_comida", imageResourceId = R.drawable.picto_espaguetis, audioResourceId = R.raw.audio_espaguetis, levelRequired = 1))
        pictos.add(Pictogram(pictogramId = "local_com_025", name = "Canelones", category = "local_comida", imageResourceId = R.drawable.picto_canelones, audioResourceId = R.raw.audio_canelones, levelRequired = 1))
        pictos.add(Pictogram(pictogramId = "local_com_026", name = "Arroz con tomate", category = "local_comida", imageResourceId = R.drawable.picto_arroz_con_tomate, audioResourceId = R.raw.audio_arroz_con_tomate, levelRequired = 1)) // Recurso: picto_arroz_con_tomate
        pictos.add(Pictogram(pictogramId = "local_com_027", name = "Ensalada", category = "local_comida", imageResourceId = R.drawable.picto_ensalada, audioResourceId = R.raw.audio_ensalada, levelRequired = 1))
        pictos.add(Pictogram(pictogramId = "local_com_028", name = "Chocolate", category = "local_comida", imageResourceId = R.drawable.picto_chocolate, audioResourceId = R.raw.audio_chocolate, levelRequired = 1))
        pictos.add(Pictogram(pictogramId = "local_com_001", name = "Manzana", category = INITIAL_DYNAMIC_CATEGORY_ID, imageResourceId = R.drawable.picto_manzana, audioResourceId = R.raw.audio_manzana, levelRequired = 1))
        pictos.add(Pictogram(pictogramId = "local_com_003", name = "Galleta", category = INITIAL_DYNAMIC_CATEGORY_ID, imageResourceId = R.drawable.picto_galleta, audioResourceId = R.raw.audio_galleta, levelRequired = 1))
        pictos.add(Pictogram(pictogramId = "local_com_004", name = "Leche", category = INITIAL_DYNAMIC_CATEGORY_ID, imageResourceId = R.drawable.picto_leche, audioResourceId = R.raw.audio_leche, levelRequired = 2))
//
//
//        // Ejemplo para otra categoría
        pictos.add(Pictogram(pictogramId = "local_ani_001", name = "Perro", category = "local_animales", imageResourceId = R.drawable.picto_perro, audioResourceId = R.raw.audio_perro, levelRequired = 1))
        pictos.add(Pictogram(pictogramId = "local_ani_002", name = "Gato", category = "local_animales", imageResourceId = R.drawable.picto_gato, audioResourceId = R.raw.audio_gato, levelRequired = 1))
        pictos.add(Pictogram(pictogramId = "local_ani_003", name = "Vaca", category = "local_animales", imageResourceId = R.drawable.picto_vaca, audioResourceId = R.raw.audio_vaca, levelRequired = 1))
        pictos.add(Pictogram(pictogramId = "local_ani_004", name = "Delfín", category = "local_animales", imageResourceId = R.drawable.picto_delfin, audioResourceId = R.raw.audio_delfin, levelRequired = 1)) // Nombre de recurso: picto_delfin
        pictos.add(Pictogram(pictogramId = "local_ani_005", name = "Tigre", category = "local_animales", imageResourceId = R.drawable.picto_tigre, audioResourceId = R.raw.audio_tigre, levelRequired = 1))
        pictos.add(Pictogram(pictogramId = "local_ani_006", name = "Zorro", category = "local_animales", imageResourceId = R.drawable.picto_zorro, audioResourceId = R.raw.audio_zorro, levelRequired = 1))
        pictos.add(Pictogram(pictogramId = "local_ani_007", name = "León", category = "local_animales", imageResourceId = R.drawable.picto_leon, audioResourceId = R.raw.audio_leon, levelRequired = 1)) // Nombre de recurso: picto_leon
        pictos.add(Pictogram(pictogramId = "local_ani_008", name = "Caballo", category = "local_animales", imageResourceId = R.drawable.picto_caballo, audioResourceId = R.raw.audio_caballo, levelRequired = 1))
        pictos.add(Pictogram(pictogramId = "local_ani_009", name = "Elefante", category = "local_animales", imageResourceId = R.drawable.picto_elefante, audioResourceId = R.raw.audio_elefante, levelRequired = 1))
        pictos.add(Pictogram(pictogramId = "local_ani_010", name = "Insecto", category = "local_animales", imageResourceId = R.drawable.picto_insecto, audioResourceId = R.raw.audio_insecto, levelRequired = 1))
        pictos.add(Pictogram(pictogramId = "local_ani_011", name = "Pingüino", category = "local_animales", imageResourceId = R.drawable.picto_pinguino, audioResourceId = R.raw.audio_pinguino, levelRequired = 1)) // Nombre de recurso: picto_pinguino
        pictos.add(Pictogram(pictogramId = "local_ani_012", name = "Serpiente", category = "local_animales", imageResourceId = R.drawable.picto_serpiente, audioResourceId = R.raw.audio_serpiente, levelRequired = 1))
        pictos.add(Pictogram(pictogramId = "local_ani_013", name = "Pez", category = "local_animales", imageResourceId = R.drawable.picto_pez, audioResourceId = R.raw.audio_pez, levelRequired = 1))
        pictos.add(Pictogram(pictogramId = "local_ani_014", name = "Rinoceronte", category = "local_animales", imageResourceId = R.drawable.picto_rinoceronte, audioResourceId = R.raw.audio_rinoceronte, levelRequired = 1))
        pictos.add(Pictogram(pictogramId = "local_ani_014", name = "Araña", category = "local_animales", imageResourceId = R.drawable.picto_aranya, audioResourceId = R.raw.audio_aranya, levelRequired = 1))

        return pictos
    }

    fun loadDynamicPictogramsByLocalCategory(category: Category, userLevel: Int = _currentUser.value?.currentLevel ?: 1) {
        _isLoading.value = true
        _currentDynamicCategoryName.value = category.name
        Log.d("StudentHomeVM", "Loading dynamic pictos for category: ${category.name} (ID: ${category.categoryId}), userLevel: $userLevel")
        val filteredPictos = allLocalPictograms.filter { it.category == category.categoryId && it.levelRequired <= userLevel }
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
        } else {
            _errorMessage.value = "Has alcanzado el límite de $MAX_PHRASE_PICTOGRAMS pictogramas."
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

        releaseMediaPlayer() // Detener cualquier reproducción anterior
        audioQueue.clear()
        audioQueue.addAll(phrase.mapNotNull { it.audioResourceId.takeIf { id -> id != 0 } })

        if (audioQueue.isNotEmpty()) {
            Log.d("StudentHomeVM", "Starting audio queue with ${audioQueue.size} sounds.")
            playNextAudioFromQueue()
        } else {
            val textToSpeak = phrase.joinToString(separator = " ") { it.name }
            Log.i("StudentHomeVM_TTS", "No local audios in phrase, fallback to TTS (not implemented): $textToSpeak")
            _errorMessage.value = "Reproduciendo: $textToSpeak (TTS no implementado)"
            // Aquí iría la lógica de TextToSpeech si no hay audio local para ningún pictograma de la frase.
        }
    }

    private fun playNextAudioFromQueue() {
        if (audioQueue.isEmpty()) {
            Log.d("StudentHomeVM", "Audio queue finished.")
            releaseMediaPlayer()
            return
        }

        val audioResId = audioQueue.removeAt(0)
        Log.d("StudentHomeVM", "Playing next audio from queue: ResId $audioResId. Remaining: ${audioQueue.size}")

        // Asegúrate de que mediaPlayer se crea en el hilo principal si hay problemas de UI/Looper
        try {
            mediaPlayer = MediaPlayer.create(getApplication(), audioResId)
            if (mediaPlayer == null) {
                Log.e("StudentHomeVM_MediaPlayer", "MediaPlayer.create failed for ResId $audioResId (returned null). Skipping.")
                playNextAudioFromQueue() // Intenta el siguiente
                return
            }

            mediaPlayer?.setOnCompletionListener {
                Log.d("StudentHomeVM_MediaPlayer", "Audio ResId $audioResId completed.")
                // No es necesario liberar 'it' aquí, ya que 'mediaPlayer' es una propiedad de clase
                playNextAudioFromQueue() // Reproduce el siguiente
            }
            mediaPlayer?.setOnErrorListener { mp, what, extra ->
                Log.e("StudentHomeVM_MediaPlayer", "Error during playback: what=$what, extra=$extra for ResId=$audioResId")
                releaseMediaPlayer() // Liberar en caso de error
                playNextAudioFromQueue() // Intenta el siguiente de la cola
                true
            }
            mediaPlayer?.start()
        } catch (e: Exception) {
            Log.e("StudentHomeVM_MediaPlayer", "Exception creating or starting MediaPlayer for ResId=$audioResId", e)
            releaseMediaPlayer()
            playNextAudioFromQueue() // Intenta el siguiente
        }
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
        Log.d("StudentHomeVM", "MediaPlayer released.")
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("StudentHomeVM", "ViewModel onCleared, releasing MediaPlayer.")
        releaseMediaPlayer()
    }
}