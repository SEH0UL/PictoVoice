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
import com.example.pictovoice.Data.repository.FirestoreRepository
import com.example.pictovoice.R // Importante para acceder a los recursos locales
import kotlinx.coroutines.launch
// No necesitas java.util.Locale aquí a menos que lo uses para algo específico

// --- Constantes para IDs de categorías ---
// Fijas (no se mostrarán como carpetas dinámicas seleccionables)
private const val PRONOUNS_CATEGORY_ID = "local_pronombres"
private const val FIXED_VERBS_CATEGORY_ID = "local_verbos"

// Dinámicas (seleccionables como carpetas)
private const val CATEGORY_ID_COMIDA = "local_comida" // Usado como categoría inicial si está desbloqueada
private const val CATEGORY_ID_ANIMALES = "local_animales"
private const val CATEGORY_ID_SENTIMIENTOS = "local_sentimientos"
private const val CATEGORY_ID_AFICIONES = "local_aficiones"
private const val CATEGORY_ID_VERBOS_2 = "local_verbos_2"
private const val CATEGORY_ID_CHARLA_RAPIDA = "local_charla_rapida"
private const val CATEGORY_ID_LUGARES = "local_lugares"
private const val CATEGORY_ID_FRASES_HECHAS = "local_frases_hechas"
private const val CATEGORY_ID_NUMEROS = "local_numeros"
private const val CATEGORY_ID_OBJETOS = "local_objetos" // Ejemplo si aún la quieres
private const val CATEGORY_ID_ACCIONES = "local_acciones" // Ejemplo si aún la quieres


private const val MAX_PHRASE_PICTOGRAMS = 25

class StudentHomeViewModel(application: Application) : AndroidViewModel(application) {
    private val firestoreRepository = FirestoreRepository()

    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser

    private val _phrasePictograms = MutableLiveData<List<Pictogram>>(emptyList())
    val phrasePictograms: LiveData<List<Pictogram>> = _phrasePictograms

    // Para los RecyclerViews fijos
    private val _pronounPictograms = MutableLiveData<List<Pictogram>>()
    val pronounPictograms: LiveData<List<Pictogram>> = _pronounPictograms

    private val _fixedVerbPictograms = MutableLiveData<List<Pictogram>>()
    val fixedVerbPictograms: LiveData<List<Pictogram>> = _fixedVerbPictograms

    // Para el RecyclerView de pictogramas de la categoría dinámica seleccionada
    private val _dynamicPictograms = MutableLiveData<List<Pictogram>>()
    val dynamicPictograms: LiveData<List<Pictogram>> = _dynamicPictograms

    // Para el RecyclerView de carpetas de categorías dinámicas (filtrado por desbloqueadas)
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

    private var allLocalPictograms: List<Pictogram> = emptyList()
    private var allDefinedDynamicCategories: List<Category> = emptyList() // Todas las dinámicas definidas

    private var mediaPlayer: MediaPlayer? = null
    private var audioQueue: MutableList<Int> = mutableListOf()

    private var previousUserLevel: Int? = null // Para rastrear el nivel anterior
    private val _levelUpEvent = MutableLiveData<Int?>(null) // Contendrá el nuevo nivel alcanzado, o null si no hay evento
    val levelUpEvent: LiveData<Int?> = _levelUpEvent

    init {
        Log.d("StudentHomeVM", "ViewModel created. Initializing local data.")
        allDefinedDynamicCategories = createLocalDynamicCategories()
        allLocalPictograms = createLocalPictograms()
        refreshUiBasedOnCurrentUser()
    }

    private fun refreshUiBasedOnCurrentUser() {
        _isLoading.value = true
        val user = _currentUser.value
        // **CAMBIO IMPORTANTE AQUÍ:** Usamos maxContentLevelApproved para determinar el acceso al contenido.
        // Si el usuario es null o maxContentLevelApproved es 0 (o no está puesto), default a 1 (Nivel 1 accesible).
        val contentAccessLevel = if (user != null && user.maxContentLevelApproved > 0) user.maxContentLevelApproved else 1

        val userUnlockedCategoryIds = user?.unlockedCategories ?: listOf(CATEGORY_ID_COMIDA)

        Log.d("StudentHomeVM", "refreshUi: User ActualLvl: ${user?.currentLevel}, ContentAccessLvl: $contentAccessLevel, UnlockedFolders: $userUnlockedCategoryIds")


        // 1. Actualizar categorías disponibles (carpetas) - La visibilidad de la CARPETA sigue dependiendo de unlockedCategories
        _availableCategories.value = allDefinedDynamicCategories.filter { category ->
            userUnlockedCategoryIds.contains(category.categoryId)
            // Aquí podríamos añadir en el futuro: && (category.requiresLevel <= contentAccessLevel) si las categorías en sí mismas tuvieran un nivel requerido para ser "visibles con contenido".
            // Por ahora, si la carpeta está en unlockedCategories, se muestra. Su contenido se filtrará.
        }.sortedBy { it.displayOrder }
        Log.d("StudentHomeVM", "Filtered available folders: ${_availableCategories.value?.map { it.name }}")

        // 2. Actualizar pictogramas fijos (pronombres y verbos) usando contentAccessLevel
        _pronounPictograms.value = allLocalPictograms.filter {
            it.category == PRONOUNS_CATEGORY_ID && it.levelRequired <= contentAccessLevel // CAMBIO: usa contentAccessLevel
        }
        _fixedVerbPictograms.value = allLocalPictograms.filter {
            it.category == FIXED_VERBS_CATEGORY_ID && it.levelRequired <= contentAccessLevel // CAMBIO: usa contentAccessLevel
        }

        // 3. Determinar y cargar la categoría dinámica inicial/actual
        var categoryToLoad: Category? = null
        val availableDynamicCats = _availableCategories.value
        if (!availableDynamicCats.isNullOrEmpty()) {
            val currentName = _currentDynamicCategoryName.value
            if (!currentName.isNullOrBlank()) {
                categoryToLoad = availableDynamicCats.find { it.name == currentName }
            }
            if (categoryToLoad == null) {
                categoryToLoad = availableDynamicCats.find { it.categoryId == CATEGORY_ID_COMIDA }
            }
            if (categoryToLoad == null) {
                categoryToLoad = availableDynamicCats.first()
            }
        }

        if (categoryToLoad != null) {
            // **CAMBIO IMPORTANTE AQUÍ:** Pasamos contentAccessLevel a la función
            loadDynamicPictogramsByLocalCategory(categoryToLoad, contentAccessLevel)
        } else {
            _dynamicPictograms.value = emptyList()
            _currentDynamicCategoryName.value = "No hay categorías desbloqueadas"
            Log.w("StudentHomeVM", "No dynamic categories available or unlocked for display.")
        }
        _isLoading.value = false
    }

    fun loadCurrentUserData(userId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val userResult = firestoreRepository.getUser(userId)
                if (userResult.isSuccess) {
                    val loadedUser = userResult.getOrNull()
                    if (_currentUser.value == null && loadedUser != null) {
                        previousUserLevel = loadedUser.currentLevel
                    }
                    _currentUser.value = loadedUser // Actualiza el usuario completo
                    Log.d("StudentHomeVM", "User data loaded. ActualLvl: ${currentUser.value?.currentLevel}, MaxApprovedLvl: ${currentUser.value?.maxContentLevelApproved}, UnlockedFolders: ${currentUser.value?.unlockedCategories}")
                    refreshUiBasedOnCurrentUser() // Esta función ahora usará el maxContentLevelApproved del usuario actualizado
                } else {
                    _errorMessage.value = "Error al cargar datos del usuario: ${userResult.exceptionOrNull()?.message}"
                    _currentUser.value = null
                    refreshUiBasedOnCurrentUser()
                }
            } catch (e: Exception) {
                _errorMessage.value = "Excepción al cargar datos del usuario: ${e.message}"
                _currentUser.value = null
                refreshUiBasedOnCurrentUser()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Esta función ahora solo define las categorías DINÁMICAS que pueden aparecer como carpetas
    private fun createLocalDynamicCategories(): List<Category> {
        Log.d("StudentHomeVM", "Defining all potential local dynamic categories")
        return listOf(
            Category(categoryId = CATEGORY_ID_COMIDA, name = "Comida", displayOrder = 0),
            Category(categoryId = CATEGORY_ID_ANIMALES, name = "Animales", displayOrder = 1),
            Category(categoryId = CATEGORY_ID_SENTIMIENTOS, name = "Sentimientos", displayOrder = 2),
            Category(categoryId = CATEGORY_ID_NUMEROS, name = "Números", displayOrder = 3),
            Category(categoryId = CATEGORY_ID_AFICIONES, name = "Aficiones", displayOrder = 4),
            Category(categoryId = CATEGORY_ID_LUGARES, name = "Lugares", displayOrder = 5),
            Category(categoryId = CATEGORY_ID_VERBOS_2, name = "Verbos II", displayOrder = 6),
            Category(categoryId = CATEGORY_ID_CHARLA_RAPIDA, name = "Charla Rápida", displayOrder = 7),
            Category(categoryId = CATEGORY_ID_FRASES_HECHAS, name = "Frases Hechas", displayOrder = 8),
            Category(categoryId = CATEGORY_ID_OBJETOS, name = "Objetos", displayOrder = 9), // Si aún la quieres
            Category(categoryId = CATEGORY_ID_ACCIONES, name = "Acciones", displayOrder = 10) // Si aún la quieres
            // Recuerda añadir iconos R.drawable... si los tienes para cada una.
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

        // --- COMIDA ---
        pictos.add(Pictogram(pictogramId = "local_com_001", name = "Desayuno", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_desayuno, audioResourceId = R.raw.audio_desayuno, levelRequired = 1, baseExp = 20)) // Asumiendo baseExp
        pictos.add(Pictogram(pictogramId = "local_com_002", name = "Almuerzo", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_almuerzo, audioResourceId = R.raw.audio_almuerzo, levelRequired = 1, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_com_003", name = "Comida", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_comida, audioResourceId = R.raw.audio_comida, levelRequired = 1, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_com_004", name = "Merienda", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_merienda, audioResourceId = R.raw.audio_merienda, levelRequired = 1, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_com_005", name = "Cena", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_cena, audioResourceId = R.raw.audio_cena, levelRequired = 1, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_com_006", name = "Agua", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_agua, audioResourceId = R.raw.audio_agua, levelRequired = 1, baseExp = 5)) // Menos EXP por agua, por ejemplo
        pictos.add(Pictogram(pictogramId = "local_com_007", name = "Verdura", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_verdura, audioResourceId = R.raw.audio_verduras, levelRequired = 1, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_com_008", name = "Pasta", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_pasta, audioResourceId = R.raw.audio_pasta, levelRequired = 1, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_com_009", name = "Hortalizas", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_hortalizas, audioResourceId = R.raw.audio_hortalizas, levelRequired = 1, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_com_010", name = "Lácteos", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_lacteos, audioResourceId = R.raw.audio_lacteos, levelRequired = 1, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_com_011", name = "Frutas", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_frutas, audioResourceId = R.raw.audio_frutas, levelRequired = 1, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_com_012", name = "Dulces", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_dulces, audioResourceId = R.raw.audio_dulces, levelRequired = 1, baseExp = 10)) // Menos EXP por dulces
        pictos.add(Pictogram(pictogramId = "local_com_013", name = "Carne", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_carne, audioResourceId = R.raw.audio_carne, levelRequired = 1, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_com_014", name = "Sopa", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_sopa, audioResourceId = R.raw.audio_sopa, levelRequired = 1, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_com_015", name = "Pizza", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_pizza, audioResourceId = R.raw.audio_pizza, levelRequired = 1, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_com_016", name = "Pescado", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_pescado, audioResourceId = R.raw.audio_pescado, levelRequired = 1, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_com_017", name = "Paella", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_paella, audioResourceId = R.raw.audio_paella, levelRequired = 1, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_com_018", name = "Miel", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_miel, audioResourceId = R.raw.audio_miel, levelRequired = 1, baseExp = 10))
        pictos.add(Pictogram(pictogramId = "local_com_019", name = "Macarrones", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_macarrones, audioResourceId = R.raw.audio_macarrones, levelRequired = 1, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_com_020", name = "Lentejas", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_lentejas, audioResourceId = R.raw.audio_lentejas, levelRequired = 1, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_com_021", name = "Jamón", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_jamon, audioResourceId = R.raw.audio_jamon, levelRequired = 1, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_com_022", name = "Hamburguesa", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_hamburguesa, audioResourceId = R.raw.audio_hamburguesa, levelRequired = 1, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_com_023", name = "Guisantes", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_guisantes, audioResourceId = R.raw.audio_guisantes, levelRequired = 1, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_com_024", name = "Espaguetis", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_espaguetis, audioResourceId = R.raw.audio_espaguetis, levelRequired = 1, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_com_025", name = "Canelones", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_canelones, audioResourceId = R.raw.audio_canelones, levelRequired = 1, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_com_026", name = "Arroz con tomate", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_arroz_con_tomate, audioResourceId = R.raw.audio_arroz_con_tomate, levelRequired = 1, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_com_027", name = "Ensalada", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_ensalada, audioResourceId = R.raw.audio_ensalada, levelRequired = 1, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_com_028", name = "Chocolate", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_chocolate, audioResourceId = R.raw.audio_chocolate, levelRequired = 1, baseExp = 10))

// IDs corregidos para evitar duplicados con los primeros de la lista de comida (Desayuno, Comida, Merienda)
        pictos.add(Pictogram(pictogramId = "local_com_029_manzana", name = "Manzana", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_manzana, audioResourceId = R.raw.audio_manzana, levelRequired = 1, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_com_030_galleta", name = "Galleta", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_galleta, audioResourceId = R.raw.audio_galleta, levelRequired = 1, baseExp = 10))
        pictos.add(Pictogram(pictogramId = "local_com_031_leche", name = "Leche", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_leche, audioResourceId = R.raw.audio_leche, levelRequired = 1, baseExp = 15)) // Cambiado levelRequired a 1 para que esté disponible inicialmente

        // Ejemplo para otra categoría
        pictos.add(Pictogram(pictogramId = "local_ani_001", name = "Perro", category = CATEGORY_ID_ANIMALES, imageResourceId = R.drawable.picto_perro, audioResourceId = R.raw.audio_perro, levelRequired = 2, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_ani_002", name = "Gato", category = CATEGORY_ID_ANIMALES, imageResourceId = R.drawable.picto_gato, audioResourceId = R.raw.audio_gato, levelRequired = 2, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_ani_003", name = "Vaca", category = CATEGORY_ID_ANIMALES, imageResourceId = R.drawable.picto_vaca, audioResourceId = R.raw.audio_vaca, levelRequired = 2, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_ani_004", name = "Delfín", category = CATEGORY_ID_ANIMALES, imageResourceId = R.drawable.picto_delfin, audioResourceId = R.raw.audio_delfin, levelRequired = 2, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_ani_005", name = "Tigre", category = CATEGORY_ID_ANIMALES, imageResourceId = R.drawable.picto_tigre, audioResourceId = R.raw.audio_tigre, levelRequired = 2, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_ani_006", name = "Zorro", category = CATEGORY_ID_ANIMALES, imageResourceId = R.drawable.picto_zorro, audioResourceId = R.raw.audio_zorro, levelRequired = 2, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_ani_007", name = "León", category = CATEGORY_ID_ANIMALES, imageResourceId = R.drawable.picto_leon, audioResourceId = R.raw.audio_leon, levelRequired = 2, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_ani_008", name = "Caballo", category = CATEGORY_ID_ANIMALES, imageResourceId = R.drawable.picto_caballo, audioResourceId = R.raw.audio_caballo, levelRequired = 2, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_ani_009", name = "Elefante", category = CATEGORY_ID_ANIMALES, imageResourceId = R.drawable.picto_elefante, audioResourceId = R.raw.audio_elefante, levelRequired = 2, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_ani_010", name = "Insecto", category = CATEGORY_ID_ANIMALES, imageResourceId = R.drawable.picto_insecto, audioResourceId = R.raw.audio_insecto, levelRequired = 2, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_ani_011", name = "Pingüino", category = CATEGORY_ID_ANIMALES, imageResourceId = R.drawable.picto_pinguino, audioResourceId = R.raw.audio_pinguino, levelRequired = 2, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_ani_012", name = "Serpiente", category = CATEGORY_ID_ANIMALES, imageResourceId = R.drawable.picto_serpiente, audioResourceId = R.raw.audio_serpiente, levelRequired = 2, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_ani_013", name = "Pez", category = CATEGORY_ID_ANIMALES, imageResourceId = R.drawable.picto_pez, audioResourceId = R.raw.audio_pez, levelRequired = 2, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_ani_014", name = "Rinoceronte", category = CATEGORY_ID_ANIMALES, imageResourceId = R.drawable.picto_rinoceronte, audioResourceId = R.raw.audio_rinoceronte, levelRequired = 2, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_ani_015", name = "Araña", category = CATEGORY_ID_ANIMALES, imageResourceId = R.drawable.picto_aranya, audioResourceId = R.raw.audio_aranya, levelRequired = 2, baseExp = 25)) // ID corregido a local_ani_015

        // **SENTIMIENTOS (CATEGORY_ID_SENTIMIENTOS = "local_sentimientos")**
        // Define levelRequired según tu mapeo (ej. Nivel 3)
        // EJEMPLO: pictos.add(Pictogram(pictogramId = "local_sen_feliz", name = "Feliz", category = CATEGORY_ID_SENTIMIENTOS, imageResourceId = R.drawable.picto_feliz, audioResourceId = R.raw.audio_feliz, levelRequired = 3, baseExp = 30))
        // ¡¡¡DEBES AÑADIR TUS PICTOGRAMAS AQUÍ!!!

        // **NÚMEROS (CATEGORY_ID_NUMEROS = "local_numeros")**
        // Define levelRequired (ej. Nivel 4)
        // ¡¡¡DEBES AÑADIR TUS PICTOGRAMAS AQUÍ!!!

        // **AFICIONES (CATEGORY_ID_AFICIONES = "local_aficiones")**
        // Define levelRequired (ej. Nivel 5)
        // ¡¡¡DEBES AÑADIR TUS PICTOGRAMAS AQUÍ!!!

        // **LUGARES (CATEGORY_ID_LUGARES = "local_lugares")**
        // Define levelRequired (ej. Nivel 6)
        // ¡¡¡DEBES AÑADIR TUS PICTOGRAMAS AQUÍ!!!

        // **VERBOS II (CATEGORY_ID_VERBOS_2 = "local_verbos_2")**
        // Define levelRequired (ej. Nivel 7)
        // ¡¡¡DEBES AÑADIR TUS PICTOGRAMAS AQUÍ!!!

        // **CHARLA RÁPIDA (CATEGORY_ID_CHARLA_RAPIDA = "local_charla_rapida")**
        // Define levelRequired (ej. Nivel 8)
        // ¡¡¡DEBES AÑADIR TUS PICTOGRAMAS AQUÍ!!!

        // **FRASES HECHAS (CATEGORY_ID_FRASES_HECHAS = "local_frases_hechas")**
        // Define levelRequired (ej. Nivel 9)
        // ¡¡¡DEBES AÑADIR TUS PICTOGRAMAS AQUÍ!!!

        // **OBJETOS (CATEGORY_ID_OBJETOS = "local_objetos")** - Si la usas
        // Define levelRequired
        // ¡¡¡DEBES AÑADIR TUS PICTOGRAMAS AQUÍ!!!

        // **ACCIONES (CATEGORY_ID_ACCIONES = "local_acciones")** - Si la usas
        // Define levelRequired
        // ¡¡¡DEBES AÑADIR TUS PICTOGRAMAS AQUÍ!!!

        Log.d("StudentHomeVM", "Total local pictograms created: ${pictos.size}")
        return pictos
    }

    fun loadDynamicPictogramsByLocalCategory(
        category: Category,
        contentAccessLevelForFiltering: Int = _currentUser.value?.maxContentLevelApproved ?: 1 // VALOR POR DEFECTO AQUÍ
    ) {
        _isLoading.value = true
        _currentDynamicCategoryName.value = category.name
        Log.d("StudentHomeVM", "Loading dynamic pictos for category: ${category.name} (ID: ${category.categoryId}), using contentAccessLevel: $contentAccessLevelForFiltering")

        val filteredPictos = allLocalPictograms.filter { pict ->
            pict.category == category.categoryId && pict.levelRequired <= contentAccessLevelForFiltering
        }
        _dynamicPictograms.value = filteredPictos
        Log.d("StudentHomeVM", "Found ${filteredPictos.size} pictos for ${category.name} using access level $contentAccessLevelForFiltering")
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
                val levelBeforeAddingExp = student.currentLevel // Nivel ANTES de añadir EXP
                viewModelScope.launch {
                    Log.d("StudentHomeVM", "Adding ${pictogram.baseExp} EXP to user $userId for pictogram ${pictogram.name}")
                    val expResult = firestoreRepository.addExperienceToStudent(userId, pictogram.baseExp)
                    if (expResult.isSuccess) {
                        val (newCurrentExp, newLevelAfterExp) = expResult.getOrThrow()
                        var updatedStudent = student.copy( // Copia inicial con nueva exp y nivel
                            currentExp = newCurrentExp,
                            currentLevel = newLevelAfterExp
                        )
                        if (newLevelAfterExp > levelBeforeAddingExp) {
                            Log.d("StudentHomeVM", "Level UP! From $levelBeforeAddingExp to $newLevelAfterExp. Posting event & fetching updated user.")
                            _levelUpEvent.value = newLevelAfterExp // Notificar a la UI
                            // Re-leer el usuario para obtener 'unlockedCategories' y 'maxContentLevelApproved' actualizados de Firestore
                            val freshUserResult = firestoreRepository.getUser(userId)
                            if (freshUserResult.isSuccess) {
                                updatedStudent = freshUserResult.getOrNull() ?: updatedStudent
                            }
                        }
                        _currentUser.value = updatedStudent // Actualizar LiveData con el usuario (potencialmente) más fresco
                        refreshUiBasedOnCurrentUser() // Refrescar toda la UI, incluyendo visibilidad de pictogramas y categorías
                    } else {
                        _errorMessage.value = expResult.exceptionOrNull()?.message ?: "Error al añadir experiencia."
                        Log.e("StudentHomeVM", "Failed to add experience: ${expResult.exceptionOrNull()?.message}")
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

    // --- Funciones de manejo de frase y MediaPlayer (sin cambios respecto a tu versión) ---
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
        Log.d("StudentHomeVM", "ViewModel onCleared, releasing MediaPlayer.")
        releaseMediaPlayer()
    }
}