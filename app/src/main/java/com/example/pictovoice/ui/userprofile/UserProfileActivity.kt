package com.example.pictovoice.ui.userprofile

import android.content.Intent // Necesario
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import com.example.pictovoice.Data.repository.AuthRepository
import com.example.pictovoice.Data.repository.FirestoreRepository
import com.example.pictovoice.R
import com.example.pictovoice.databinding.ActivityUserProfileBinding
import com.example.pictovoice.ui.auth.MainActivity // Necesario
import com.example.pictovoice.utils.AuthViewModel
import com.example.pictovoice.utils.AuthViewModelFactory
import com.example.pictovoice.viewmodels.UserProfileViewModel
import com.example.pictovoice.viewmodels.UserProfileViewModelFactory
import com.example.pictovoice.utils.Result // Importar tu clase Result
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class UserProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserProfileBinding
    private var targetUserId: String? = null
    private var viewerUserId: String? = null
    private var viewerRole: String? = null

    private val firestoreRepository = FirestoreRepository()

    // Usaremos una sola instancia de UserProfileViewModel
    private val userProfileViewModel: UserProfileViewModel by viewModels {
        UserProfileViewModelFactory(targetUserId ?: "", firestoreRepository)
    }
    // ViewModel para la autenticación (incluyendo logout)
    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(AuthRepository())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarUserProfile)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarUserProfile.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        targetUserId = intent.getStringExtra("USER_ID_EXTRA")
        viewerUserId = FirebaseAuth.getInstance().currentUser?.uid

        if (targetUserId.isNullOrEmpty()) {
            Toast.makeText(this, "ID de usuario del perfil no encontrado.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        if (viewerUserId.isNullOrEmpty()){
            Toast.makeText(this, "No se pudo identificar al observador del perfil.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        if (viewerRole == null && viewerUserId != null) {
            // Usar la instancia correcta del ViewModel
            userProfileViewModel.viewModelScope.launch {
                val userResult = firestoreRepository.getUser(viewerUserId!!)
                if (userResult.isSuccess) {
                    viewerRole = userResult.getOrNull()?.role
                    setupUIBasedOnRoleAndProfile()
                } else {
                    Toast.makeText(this@UserProfileActivity, "Error al obtener rol del observador.", Toast.LENGTH_SHORT).show()
                    setupUIBasedOnRoleAndProfile()
                }
            }
        } else {
            setupUIBasedOnRoleAndProfile()
        }

        setupClickListeners()
        setupObservers()
    }

    override fun onResume() {
        super.onResume()
        if (targetUserId != null) {
            Log.d("UserProfileActivity", "onResume: Reloading user profile for $targetUserId")
            userProfileViewModel.loadUserProfile()
        }
    }

    private fun setupUIBasedOnRoleAndProfile() {
        val isViewingOwnProfileAsStudent = targetUserId == viewerUserId && viewerRole == "student"
        val isTeacherViewingStudentProfile = viewerRole == "teacher"

        binding.btnUserLogout.visibility = if (isViewingOwnProfileAsStudent) View.VISIBLE else View.GONE

        if (isViewingOwnProfileAsStudent) {
            binding.tvProfileTitle.text = "Tu Perfil"
            binding.btnDesbloquearPalabrasProfesor.visibility = View.GONE
            binding.btnGenerarInformeProfesor.visibility = View.GONE
        } else if (isTeacherViewingStudentProfile) {
            binding.tvProfileTitle.text = "Perfil del Alumno"
            binding.btnSolicitarPalabras.visibility = View.GONE
            binding.btnDesbloquearPalabrasProfesor.visibility = View.VISIBLE
            binding.btnGenerarInformeProfesor.visibility = View.VISIBLE
        } else {
            binding.tvProfileTitle.text = "Perfil"
            binding.btnSolicitarPalabras.visibility = View.GONE
            binding.btnDesbloquearPalabrasProfesor.visibility = View.GONE
            binding.btnGenerarInformeProfesor.visibility = View.GONE
        }
    }

    private fun setupClickListeners() {
        binding.btnSolicitarPalabras.setOnClickListener {
            userProfileViewModel.requestWords() // Usar userProfileViewModel
        }

        binding.btnDesbloquearPalabrasProfesor.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Aprobar Solicitud")
                .setMessage("¿Estás seguro de que quieres aprobar la solicitud de palabras para este alumno?")
                .setPositiveButton("Sí, Aprobar") { dialog, _ ->
                    userProfileViewModel.approveWordRequest() // Usar userProfileViewModel
                    dialog.dismiss()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        binding.btnGenerarInformeProfesor.setOnClickListener {
            Toast.makeText(this, "Funcionalidad 'Generar Informe' pendiente.", Toast.LENGTH_SHORT).show()
        }

        binding.btnUserLogout.setOnClickListener {
            showLogoutConfirmationDialog() // Llamada a la función que ahora existe
        }
    }

    // --- INICIO: Definición de la función que faltaba ---
    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro de que quieres cerrar sesión?")
            .setPositiveButton("Sí, cerrar sesión") { dialog, _ ->
                authViewModel.logoutUser()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    // --- FIN: Definición de la función que faltaba ---

    private fun setupObservers() {
        userProfileViewModel.isLoading.observe(this, Observer { isLoading -> // Usar userProfileViewModel
            binding.btnSolicitarPalabras.isEnabled = !isLoading
            binding.btnDesbloquearPalabrasProfesor.isEnabled = !isLoading
        })

        userProfileViewModel.errorMessage.observe(this, Observer { errorMessage -> // Usar userProfileViewModel
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                userProfileViewModel.clearErrorMessage() // Usar userProfileViewModel
            }
        })

        userProfileViewModel.userProfile.observe(this, Observer { user -> // Usar userProfileViewModel
            user?.let {
                binding.tvUserProfileName.text = it.fullName
                binding.tvLevelStart.text = it.currentLevel.toString()
                binding.tvLevelEnd.text = (it.currentLevel + 1).toString()

                val expNeededForNextLevel = it.currentLevel * 1000
                binding.progressBarLevel.max = expNeededForNextLevel
                binding.progressBarLevel.progress = it.currentExp

                if (viewerRole == "teacher" && targetUserId != viewerUserId) {
                    supportActionBar?.title = "Perfil de ${it.fullName}"
                    binding.tvProfileTitle.text = "Perfil de ${it.fullName}"
                } else if (targetUserId == viewerUserId) {
                    supportActionBar?.title = "Mi Perfil"
                    binding.tvProfileTitle.text = "Mi Perfil"
                }

                binding.tvPalabrasUsadasCount.text = "0"
                binding.tvPalabrasNuevasCount.text = "0"
                binding.tvPalabrasDesbloqueadasCount.text = "0"
                binding.tvPalabrasBloqueadasCount.text = "0"

                if (viewerRole == "teacher"){
                    val isLoadingValue = userProfileViewModel.isLoading.value ?: false // Usar userProfileViewModel
                    if (it.hasPendingWordRequest) {
                        binding.btnDesbloquearPalabrasProfesor.text = "Aprobar Solicitud (Nivel ${it.currentLevel})"
                        binding.btnDesbloquearPalabrasProfesor.isEnabled = !isLoadingValue
                    } else {
                        binding.btnDesbloquearPalabrasProfesor.text = "Palabras Aprobadas (Nivel ${it.currentLevel})"
                        binding.btnDesbloquearPalabrasProfesor.isEnabled = false
                    }
                }
            }
        })

        userProfileViewModel.canRequestWords.observe(this, Observer { canRequest -> // Usar userProfileViewModel
            val isViewingOwnProfileAsStudent = targetUserId == viewerUserId && viewerRole == "student"
            if (isViewingOwnProfileAsStudent) {
                binding.btnSolicitarPalabras.visibility = if (canRequest) View.VISIBLE else View.GONE
                val isLoadingValue = userProfileViewModel.isLoading.value ?: false // Usar userProfileViewModel
                binding.btnSolicitarPalabras.isEnabled = canRequest && !isLoadingValue
                if (canRequest) {
                    binding.btnSolicitarPalabras.text = "Solicitar Palabras (Nivel ${userProfileViewModel.userProfile.value?.currentLevel ?: ""})" // Usar userProfileViewModel
                }
            } else {
                binding.btnSolicitarPalabras.visibility = View.GONE
            }
        })

        userProfileViewModel.wordRequestOutcome.observe(this, Observer { resultOutcome -> // Usar userProfileViewModel
            resultOutcome?.let { outcome ->
                if (outcome is Result.Success) {
                    Toast.makeText(this, "Solicitud de palabras enviada.", Toast.LENGTH_SHORT).show()
                }
                userProfileViewModel.clearWordRequestOutcome() // Usar userProfileViewModel
            }
        })

        userProfileViewModel.approveWordRequestOutcome.observe(this, Observer { resultOutcome -> // Usar userProfileViewModel
            resultOutcome?.let { outcome ->
                // El feedback de éxito/error ya se maneja con _errorMessage
                userProfileViewModel.clearApproveWordRequestOutcome() // Usar userProfileViewModel
            }
        })

        // Observador para el evento de logout desde AuthViewModel
        authViewModel.logoutEvent.observe(this, Observer { hasLoggedOut ->
            if (hasLoggedOut == true) {
                Toast.makeText(this, "Sesión cerrada.", Toast.LENGTH_SHORT).show()
                navigateToLogin()
                authViewModel.onLogoutEventHandled()
            }
        })
    }

    // --- INICIO: Definición de la función que faltaba para navegar al login ---
    private fun navigateToLogin() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    // --- FIN: Definición de la función que faltaba para navegar al login ---
}