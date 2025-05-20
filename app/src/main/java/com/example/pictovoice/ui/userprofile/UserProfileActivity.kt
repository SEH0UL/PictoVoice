package com.example.pictovoice.ui.userprofile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope // Aunque no se use directamente aquí, es bueno tenerlo si se necesitara
import com.example.pictovoice.Data.repository.AuthRepository
import com.example.pictovoice.Data.repository.FirestoreRepository
import com.example.pictovoice.R // Import para R.string.*
import com.example.pictovoice.databinding.ActivityUserProfileBinding
import com.example.pictovoice.ui.auth.MainActivity
import com.example.pictovoice.utils.AuthViewModelFactory
import com.example.pictovoice.utils.Result // Tu clase Result para los outcomes
import com.example.pictovoice.viewmodels.AuthViewModel
import com.example.pictovoice.viewmodels.UserProfileViewModel
import com.example.pictovoice.viewmodels.UserProfileViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

// El import de kotlinx.coroutines.launch no es necesario si solo usas viewModelScope en el ViewModel

private const val TAG = "UserProfileActivity"

/**
 * Activity que muestra el perfil de un usuario (alumno).
 * Puede ser visualizado por el propio alumno o por un profesor.
 * Muestra información del perfil, nivel, EXP, estadísticas, y permite acciones
 * como solicitar palabras (alumno) o aprobarlas (profesor), y cerrar sesión (alumno).
 */
class UserProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserProfileBinding
    private var targetUserId: String? = null
    private var viewerUserId: String? = null
    private var viewerRole: String? = null

    private val firestoreRepository = FirestoreRepository()

    private val userProfileViewModel: UserProfileViewModel by viewModels {
        UserProfileViewModelFactory(targetUserId ?: "", firestoreRepository)
    }
    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(AuthRepository())
    }

    companion object {
        const val EXTRA_USER_ID = "USER_ID_EXTRA"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d(TAG, "onCreate: Iniciando UserProfileActivity.")

        targetUserId = intent.getStringExtra(EXTRA_USER_ID)
        viewerUserId = FirebaseAuth.getInstance().currentUser?.uid

        if (targetUserId.isNullOrBlank()) {
            Log.e(TAG, "Error: No se recibió targetUserId. Finalizando actividad.")
            // CORRECCIÓN: Usar el nombre de string del strings.xml organizado
            Toast.makeText(this, getString(R.string.profile_error_user_id_not_found), Toast.LENGTH_LONG).show()
            finish()
            return
        }

        if (viewerUserId.isNullOrBlank()) {
            Log.e(TAG, "Error: No se pudo identificar al viewerUserId (usuario actual).")
            // CORRECCIÓN: Usar el nombre de string del strings.xml organizado
            Toast.makeText(this, getString(R.string.profile_error_viewer_id_not_found), Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupToolbar()
        determineViewerRoleAndSetupUI()
        setupClickListeners()
        // setupObservers() se llama desde determineViewerRoleAndSetupUI
    }

    override fun onResume() {
        super.onResume()
        if (targetUserId != null) { // Solo cargar si targetUserId es válido
            Log.d(TAG, "onResume: Recargando perfil para targetUserId: $targetUserId")
            userProfileViewModel.loadUserProfile()
        }
    }

    /**
     * Configura la Toolbar de la actividad.
     */
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarUserProfile)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // CORRECCIÓN: Usar el nombre de string del strings.xml organizado
        supportActionBar?.title = getString(R.string.profile_title_loading)
    }

    /**
     * Determina el rol del visualizador y configura la UI y los observadores.
     */
    private fun determineViewerRoleAndSetupUI() {
        if (targetUserId == viewerUserId) {
            viewerRole = "student"
            Log.d(TAG, "Visualizador es el propio alumno.")
            setupUIBasedOnRoleAndProfile()
            setupObservers()
        } else {
            Log.d(TAG, "Visualizador es diferente al targetUser. Obteniendo rol del visualizador ($viewerUserId).")
            userProfileViewModel.viewModelScope.launch {
                val result = firestoreRepository.getUser(viewerUserId!!)
                if (result.isSuccess) {
                    viewerRole = result.getOrNull()?.role
                    Log.d(TAG, "Rol del visualizador obtenido: $viewerRole")
                } else {
                    viewerRole = null
                    Log.w(TAG, "No se pudo obtener el rol del visualizador: ${result.exceptionOrNull()?.message}")
                    // CORRECCIÓN: Usar el nombre de string del strings.xml organizado
                    Toast.makeText(this@UserProfileActivity, getString(R.string.profile_error_fetching_viewer_role), Toast.LENGTH_SHORT).show()
                }
                setupUIBasedOnRoleAndProfile()
                setupObservers()
            }
        }
    }

    /**
     * Configura la UI basándose en el rol del visualizador.
     */
    private fun setupUIBasedOnRoleAndProfile() {
        Log.d(TAG, "setupUIBasedOnRoleAndProfile: viewerRole=$viewerRole, targetUserId=$targetUserId, viewerUserId=$viewerUserId")
        val isOwnProfileStudent = targetUserId == viewerUserId && viewerRole == "student" // Simplificado, ya que si son el mismo, viewerRole se setea a student
        val isTeacherViewing = viewerRole == "teacher"

        binding.btnSolicitarPalabras.visibility = if (isOwnProfileStudent) View.VISIBLE else View.GONE
        binding.btnDesbloquearPalabrasProfesor.visibility = if (isTeacherViewing) View.VISIBLE else View.GONE
        // binding.btnGenerarInformeProfesor.visibility = if (isTeacherViewing) View.VISIBLE else View.GONE // Botón eliminado del XML
        binding.btnUserLogout.visibility = if (isOwnProfileStudent) View.VISIBLE else View.GONE

        if (isOwnProfileStudent) {
            // CORRECCIÓN: Usar los nombres de string del strings.xml organizado
            binding.tvProfileTitle.text = getString(R.string.profile_title_own)
            supportActionBar?.title = getString(R.string.profile_title_own)
        } else if (isTeacherViewing) {
            binding.tvProfileTitle.text = getString(R.string.profile_title_student_by_teacher)
            // El título del action bar se actualizará con el nombre del alumno en el observador de userProfile
        } else {
            binding.tvProfileTitle.text = getString(R.string.profile_title_generic)
            supportActionBar?.title = getString(R.string.profile_title_generic)
        }
    }

    /**
     * Configura los listeners de los botones.
     */
    private fun setupClickListeners() {
        Log.d(TAG, "Configurando ClickListeners.")
        binding.btnSolicitarPalabras.setOnClickListener {
            Log.d(TAG, "Botón 'Solicitar Palabras' pulsado.")
            userProfileViewModel.requestWords()
        }

        binding.btnDesbloquearPalabrasProfesor.setOnClickListener {
            Log.d(TAG, "Botón 'Desbloquear/Aprobar Palabras' (Profesor) pulsado.")
            showApproveConfirmationDialog()
        }

        // El botón btnGenerarInformeProfesor fue eliminado del XML
        // binding.btnGenerarInformeProfesor.setOnClickListener {
        //     Log.d(TAG, "Botón 'Generar Informe' pulsado.")
        //     Toast.makeText(this, getString(R.string.feature_not_implemented_pdf), Toast.LENGTH_SHORT).show()
        // }

        binding.btnUserLogout.setOnClickListener {
            Log.d(TAG, "Botón 'Cerrar Sesión' (Alumno) pulsado.")
            showLogoutConfirmationDialog()
        }
    }

    /**
     * Muestra diálogo de confirmación para aprobar solicitud de palabras.
     */
    private fun showApproveConfirmationDialog() {
        AlertDialog.Builder(this)
            // CORRECCIÓN: Usar los nombres de string del strings.xml organizado
            .setTitle(getString(R.string.profile_dialog_title_approve_request))
            .setMessage(getString(R.string.profile_dialog_message_approve_request_confirm))
            .setPositiveButton(getString(R.string.dialog_button_yes_approve)) { dialog, _ ->
                userProfileViewModel.approveWordRequest()
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.dialog_button_cancel), null)
            .show()
    }

    /**
     * Muestra diálogo de confirmación para cerrar sesión.
     */
    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            // CORRECCIÓN: Usar los nombres de string del strings.xml organizado
            .setTitle(getString(R.string.logout_dialog_title))
            .setMessage(getString(R.string.logout_dialog_message_confirm))
            .setPositiveButton(getString(R.string.dialog_button_yes_logout)) { dialog, _ ->
                authViewModel.logoutUser()
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.dialog_button_cancel), null)
            .show()
    }

    /**
     * Configura los observadores para los LiveData.
     */
    private fun setupObservers() {
        Log.d(TAG, "Configurando Observadores.")

        userProfileViewModel.isLoading.observe(this, Observer { isLoading ->
            Log.d(TAG, "LiveData isLoading actualizado: $isLoading")
            binding.btnSolicitarPalabras.isEnabled = !isLoading
            // El estado de btnDesbloquearPalabrasProfesor se maneja también en el observer de userProfile
            binding.btnDesbloquearPalabrasProfesor.isEnabled = binding.btnDesbloquearPalabrasProfesor.isEnabled && !isLoading
            // binding.btnGenerarInformeProfesor.isEnabled = !isLoading // Botón eliminado
            binding.btnUserLogout.isEnabled = !isLoading
        })

        userProfileViewModel.errorMessage.observe(this, Observer { errorMessage ->
            errorMessage?.let {
                Log.w(TAG, "LiveData errorMessage actualizado: $it")
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                userProfileViewModel.clearErrorMessage()
            }
        })

        userProfileViewModel.userProfile.observe(this, Observer { user ->
            user?.let {
                Log.i(TAG, "LiveData userProfile actualizado: ${it.fullName}, Nivel: ${it.currentLevel}")
                binding.tvUserProfileName.text = it.fullName
                binding.tvLevelStart.text = it.currentLevel.toString()
                binding.tvLevelEnd.text = (it.currentLevel + 1).toString()

                val expForNext = it.currentLevel * 1000
                binding.progressBarLevel.max = if (expForNext > 0) expForNext else 100
                binding.progressBarLevel.progress = it.currentExp

                if (viewerRole == "teacher" && targetUserId != viewerUserId) {
                    // CORRECCIÓN: Usar el nombre de string del strings.xml organizado
                    val profileTitle = getString(R.string.profile_title_student_dynamic, it.fullName)
                    supportActionBar?.title = profileTitle
                    binding.tvProfileTitle.text = profileTitle
                } else if (targetUserId == viewerUserId) {
                    supportActionBar?.title = getString(R.string.profile_title_own)
                    binding.tvProfileTitle.text = getString(R.string.profile_title_own)
                }

                if (viewerRole == "teacher") {
                    val isLoadingValue = userProfileViewModel.isLoading.value ?: false
                    if (it.hasPendingWordRequest) {
                        // CORRECCIÓN: Usar el nombre de string del strings.xml organizado
                        binding.btnDesbloquearPalabrasProfesor.text = getString(R.string.profile_button_approve_request_level, it.currentLevel)
                        binding.btnDesbloquearPalabrasProfesor.isEnabled = !isLoadingValue
                    } else {
                        // CORRECCIÓN: Usar el nombre de string del strings.xml organizado
                        binding.btnDesbloquearPalabrasProfesor.text = getString(R.string.profile_button_words_approved_level, it.currentLevel)
                        binding.btnDesbloquearPalabrasProfesor.isEnabled = false
                    }
                }
            } ?: run {
                Log.w(TAG, "LiveData userProfile es nulo.")
            }
        })

        userProfileViewModel.canRequestWords.observe(this, Observer { canRequest ->
            Log.d(TAG, "LiveData canRequestWords actualizado: $canRequest")
            if (binding.btnSolicitarPalabras.visibility == View.VISIBLE) {
                val isLoadingValue = userProfileViewModel.isLoading.value ?: false
                binding.btnSolicitarPalabras.isEnabled = canRequest && !isLoadingValue
                if (canRequest) {
                    // CORRECCIÓN: Usar el nombre de string del strings.xml organizado
                    binding.btnSolicitarPalabras.text = getString(
                        R.string.profile_button_request_words_level,
                        userProfileViewModel.userProfile.value?.currentLevel ?: 1
                    )
                } else {
                    if (userProfileViewModel.userProfile.value?.hasPendingWordRequest == true) {
                        // CORRECCIÓN: Usar el nombre de string del strings.xml organizado
                        binding.btnSolicitarPalabras.text = getString(R.string.profile_button_request_sent)
                    } else if (userProfileViewModel.userProfile.value?.let { it.currentLevel <= it.levelWordsRequestedFor } == true){
                        // CORRECCIÓN: Usar el nombre de string del strings.xml organizado
                        binding.btnSolicitarPalabras.text = getString(R.string.profile_button_words_requested_for_level, userProfileViewModel.userProfile.value?.currentLevel ?: 1 )
                    }
                }
            }
        })

        // Observadores para estadísticas
        userProfileViewModel.wordsUsedCount.observe(this, Observer { count ->
            binding.tvPalabrasUsadasCount.text = count.toString()
        })
        userProfileViewModel.phrasesCreatedCount.observe(this, Observer { count ->
            binding.tvFrasesCreadasCount.text = count.toString() // Asegúrate que tu XML usa tvFrasesCreadasCount
        })
        userProfileViewModel.availableWordsCount.observe(this, Observer { count ->
            binding.tvPalabrasDisponiblesCount.text = count.toString() // Asegúrate que tu XML usa tvPalabrasDisponiblesCount
        })
        userProfileViewModel.lockedWordsCount.observe(this, Observer { count ->
            binding.tvPalabrasBloqueadasCount.text = count.toString()
        })

        userProfileViewModel.wordRequestOutcome.observe(this, Observer { resultOutcome ->
            resultOutcome?.let { outcome ->
                // El feedback ya se maneja con _errorMessage
                userProfileViewModel.clearWordRequestOutcome()
            }
        })

        userProfileViewModel.approveWordRequestOutcome.observe(this, Observer { resultOutcome ->
            resultOutcome?.let { outcome ->
                // El feedback ya se maneja con _errorMessage
                userProfileViewModel.clearApproveWordRequestOutcome()
            }
        })

        authViewModel.logoutEvent.observe(this, Observer { hasLoggedOut ->
            if (hasLoggedOut == true) {
                // CORRECCIÓN: Usar el nombre de string actualizado y genérico
                Toast.makeText(this, getString(R.string.toast_session_closed_generic), Toast.LENGTH_SHORT).show()
                navigateToLogin()
                authViewModel.onLogoutEventHandled()
            }
        })
    }

    /**
     * Navega a [MainActivity] (pantalla de login) y finaliza esta actividad.
     */
    private fun navigateToLogin() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: UserProfileActivity destruida.")
    }
}