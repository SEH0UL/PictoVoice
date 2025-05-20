package com.example.pictovoice.ui.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer // Necesario para observar LiveData
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pictovoice.R // Para recursos como strings y drawables
import com.example.pictovoice.adapters.CategoryAdapter
import com.example.pictovoice.adapters.FixedPictogramAdapter
import com.example.pictovoice.adapters.PhrasePictogramAdapter
import com.example.pictovoice.adapters.SelectionPictogramAdapter
import com.example.pictovoice.databinding.ActivityHomeBinding
import com.example.pictovoice.ui.auth.MainActivity
import com.example.pictovoice.ui.userprofile.UserProfileActivity
import com.example.pictovoice.viewmodels.StudentHomeViewModel
import com.google.firebase.auth.FirebaseAuth

private const val TAG = "HomeActivity" // Tag para Logs

/**
 * Activity principal para el rol de Alumno.
 * Muestra pictogramas organizados en categorías fijas y dinámicas, permite la selección
 * de pictogramas para formar frases, reproduce el audio de las frases, y gestiona la
 * navegación al perfil del usuario.
 * Se actualiza en tiempo real a los cambios en los datos del usuario (nivel, EXP, contenido desbloqueado)
 * y notifica al alumno sobre subidas de nivel.
 */
class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val viewModel: StudentHomeViewModel by viewModels()

    private lateinit var phraseAdapter: PhrasePictogramAdapter
    private lateinit var pronounsAdapter: FixedPictogramAdapter
    private lateinit var fixedVerbsAdapter: FixedPictogramAdapter
    private lateinit var dynamicPictogramsAdapter: SelectionPictogramAdapter
    private lateinit var categoryAdapter: CategoryAdapter

    private val firebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d(TAG, "onCreate: Iniciando HomeActivity.")

        val currentUser = firebaseAuth.currentUser
        if (currentUser == null || currentUser.uid.isBlank()) {
            Log.w(TAG, "Usuario no autenticado o UID no disponible. Redirigiendo a Login.")
            navigateToLogin()
            return // Salir de onCreate si no hay usuario
        }

        setupRecyclerViews()
        setupClickListeners()
        observeViewModel()

        Log.d(TAG, "Solicitando carga y escucha de datos para el usuario UID: ${currentUser.uid}")
        viewModel.loadAndListenToUserData(currentUser.uid)
    }

    /**
     * Configura todos los RecyclerViews y sus adaptadores.
     */
    private fun setupRecyclerViews() {
        Log.d(TAG, "Configurando RecyclerViews...")

        // Adaptador para la frase (pictogramas seleccionados)
        phraseAdapter = PhrasePictogramAdapter()
        binding.rvPhrasePictograms.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvPhrasePictograms.adapter = phraseAdapter

        // Adaptador para pronombres (pictogramas fijos)
        pronounsAdapter = FixedPictogramAdapter { pictogram ->
            Log.d(TAG, "Pictograma de pronombre seleccionado: ${pictogram.name}")
            viewModel.addPictogramToPhrase(pictogram)
        }
        binding.rvPronouns.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvPronouns.adapter = pronounsAdapter

        // Adaptador para verbos fijos
        fixedVerbsAdapter = FixedPictogramAdapter { pictogram ->
            Log.d(TAG, "Pictograma de verbo fijo seleccionado: ${pictogram.name}")
            viewModel.addPictogramToPhrase(pictogram)
        }
        binding.rvFixedVerbs.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvFixedVerbs.adapter = fixedVerbsAdapter

        // Adaptador para pictogramas dinámicos (de la categoría seleccionada)
        dynamicPictogramsAdapter = SelectionPictogramAdapter { pictogram ->
            Log.d(TAG, "Pictograma dinámico seleccionado: ${pictogram.name}")
            viewModel.addPictogramToPhrase(pictogram)
        }
        // El GridLayoutManager y spanCount ya están definidos en el XML para rvDynamicPictograms
        // app:layoutManager="androidx.recyclerview.widget.GridLayoutManager"
        // app:spanCount="4"
        binding.rvDynamicPictograms.adapter = dynamicPictogramsAdapter

        // Adaptador para las carpetas de categorías dinámicas
        categoryAdapter = CategoryAdapter { category ->
            Log.d(TAG, "Carpeta de categoría seleccionada: ${category.name}")
            // El ViewModel internamente usará el maxContentLevelApproved del usuario actual
            viewModel.loadDynamicPictogramsByLocalCategory(category)
        }
        binding.categoryNavigationContainer.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.categoryNavigationContainer.adapter = categoryAdapter
    }

    /**
     * Configura los listeners para los botones y otros elementos interactivos de la UI.
     */
    private fun setupClickListeners() {
        Log.d(TAG, "Configurando ClickListeners...")
        binding.btnPlayPhrase.setOnClickListener {
            Log.d(TAG, "Botón 'Reproducir Frase' pulsado.")
            viewModel.onPlayPhraseClicked()
        }

        binding.btnDeletePictogram.setOnClickListener {
            Log.d(TAG, "Botón 'Borrar Último Pictograma' pulsado (clic corto).")
            viewModel.deleteLastPictogramFromPhrase()
        }

        binding.btnDeletePictogram.setOnLongClickListener {
            Log.d(TAG, "Botón 'Borrar Último Pictogram' pulsado (clic largo) -> Borrar frase completa.")
            showClearPhraseConfirmationDialog()
            true // Indica que el long click ha sido consumido
        }

        binding.btnUserProfile.setOnClickListener {
            Log.d(TAG, "Botón 'Perfil de Usuario' pulsado.")
            navigateToUserProfile()
        }
    }

    /**
     * Muestra un diálogo de confirmación antes de borrar la frase completa.
     */
    private fun showClearPhraseConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.home_dialog_title_clear_phrase))
            .setMessage(getString(R.string.home_dialog_message_clear_phrase))
            .setPositiveButton(getString(R.string.home_dialog_button_yes_clear)) { dialog, _ ->
                viewModel.clearPhrase()
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.dialog_button_cancel), null) // Reutilizar string común
            .show()
    }

    /**
     * Configura los observadores para los LiveData del [StudentHomeViewModel].
     * Actualiza la UI en respuesta a los cambios de datos.
     */
    private fun observeViewModel() {
        Log.d(TAG, "Configurando Observadores del ViewModel...")

        viewModel.phrasePictograms.observe(this, Observer { pictograms ->
            Log.d(TAG, "LiveData phrasePictograms actualizado: ${pictograms?.size ?: 0} items.")
            phraseAdapter.submitList(pictograms) // ListAdapter maneja nulls internamente como lista vacía
            if (!pictograms.isNullOrEmpty()) {
                binding.rvPhrasePictograms.smoothScrollToPosition(pictograms.size - 1)
            }
        })

        viewModel.playButtonVisibility.observe(this, Observer { isVisible ->
            Log.d(TAG, "LiveData playButtonVisibility actualizado: $isVisible.")
            binding.btnPlayPhrase.visibility = if (isVisible) View.VISIBLE else View.GONE
        })

        viewModel.pronounPictograms.observe(this, Observer { pictograms ->
            Log.d(TAG, "LiveData pronounPictograms actualizado: ${pictograms?.size ?: 0} items.")
            pronounsAdapter.submitList(pictograms)
        })

        viewModel.fixedVerbPictograms.observe(this, Observer { pictograms ->
            Log.d(TAG, "LiveData fixedVerbPictograms actualizado: ${pictograms?.size ?: 0} items.")
            fixedVerbsAdapter.submitList(pictograms)
        })

        viewModel.dynamicPictograms.observe(this, Observer { pictograms ->
            Log.d(TAG, "LiveData dynamicPictograms actualizado: ${pictograms?.size ?: 0} items para categoría '${viewModel.currentDynamicCategoryName.value}'.")
            dynamicPictogramsAdapter.submitList(pictograms)
        })

        viewModel.currentDynamicCategoryName.observe(this, Observer { categoryName ->
            Log.d(TAG, "LiveData currentDynamicCategoryName actualizado: $categoryName.")
            binding.tvCurrentCategoryName.text = categoryName
        })

        viewModel.availableCategories.observe(this, Observer { categories ->
            Log.d(TAG, "LiveData availableCategories actualizado: ${categories?.size ?: 0} items.")
            categoryAdapter.submitList(categories)
        })

        viewModel.isLoading.observe(this, Observer { isLoading ->
            Log.d(TAG, "LiveData isLoading actualizado: $isLoading.")
            // Aquí podrías controlar un ProgressBar general para la actividad si lo tuvieras.
            // Por ejemplo: binding.homeProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        })

        viewModel.errorMessage.observe(this, Observer { errorMessage ->
            errorMessage?.let {
                Log.w(TAG, "LiveData errorMessage actualizado: $it")
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                // Considerar llamar a viewModel.clearErrorMessage() si el mensaje es un evento de un solo uso.
            }
        })

        viewModel.currentUser.observe(this, Observer { user ->
            // El ViewModel se actualiza en tiempo real.
            // La UI de pictogramas/categorías se refresca a través de refreshUiBasedOnCurrentUser() en el VM.
            // Aquí podríamos actualizar elementos específicos de la UI que dependan directamente del usuario,
            // como un saludo, o el nivel si se mostrara en esta pantalla.
            user?.let {
                Log.i(TAG, "LiveData currentUser actualizado: ${it.fullName}, Nivel: ${it.currentLevel}, MaxApprovedLvl: ${it.maxContentLevelApproved}")
            }
        })

        viewModel.levelUpEvent.observe(this, Observer { newLevel ->
            newLevel?.let {
                Log.i(TAG, "LiveData levelUpEvent recibido para nivel $it.")
                showLevelUpDialog(it)
                viewModel.levelUpNotificationShown() // Notificar al ViewModel que el evento fue manejado
            }
        })
    }

    /**
     * Muestra un diálogo de felicitación cuando el alumno sube de nivel.
     * @param newLevel El nuevo nivel alcanzado.
     */
    private fun showLevelUpDialog(newLevel: Int) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.home_dialog_title_level_up))
            .setMessage(getString(R.string.home_dialog_message_level_up, newLevel))
            .setPositiveButton(getString(R.string.home_dialog_button_ok_level_up), null)
            .setCancelable(false)
            .show()
    }

    /**
     * Navega a la pantalla de perfil del usuario.
     */
    private fun navigateToUserProfile() {
        val userId = firebaseAuth.currentUser?.uid
        if (userId != null) {
            val intent = Intent(this, UserProfileActivity::class.java).apply {
                putExtra(UserProfileActivity.EXTRA_USER_ID, userId)
            }
            startActivity(intent)
        } else {
            Toast.makeText(this, getString(R.string.home_error_user_id_not_found), Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Error en navigateToUserProfile: userId es nulo.")
        }
    }

    /**
     * Navega a la pantalla de Login ([MainActivity]) y finaliza esta actividad.
     */
    private fun navigateToLogin() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: HomeActivity destruida.")
        // El ViewModel (StudentHomeViewModel) se encarga de limpiar su listener de Firestore en su onCleared().
    }
}