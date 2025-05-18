package com.example.pictovoice.ui.userprofile // Ajusta el paquete si es diferente

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope // Necesario para viewModel.viewModelScope.launch
import com.example.pictovoice.Data.repository.FirestoreRepository
import com.example.pictovoice.R
import com.example.pictovoice.databinding.ActivityUserProfileBinding
import com.example.pictovoice.viewmodels.UserProfileViewModel
import com.example.pictovoice.viewmodels.UserProfileViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
// Asegúrate de que esta importación esté presente y correcta:
import com.example.pictovoice.utils.Result // <--- IMPORTACIÓN NECESARIA

class UserProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserProfileBinding
    private var targetUserId: String? = null
    private var viewerUserId: String? = null
    private var viewerRole: String? = null

    private val firestoreRepository = FirestoreRepository()

    private val viewModel: UserProfileViewModel by viewModels {
        UserProfileViewModelFactory(targetUserId ?: "", firestoreRepository)
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

        // Cargar el rol del visualizador si no se pasó por intent
        if (viewerRole == null && viewerUserId != null) {
            // Usar viewModelScope si la corrutina está relacionada con el ciclo de vida del ViewModel,
            // o lifecycleScope para el ciclo de vida de la Activity.
            // Aquí, como es una operación de inicialización de la Activity, lifecycleScope podría ser más apropiado,
            // o mantenerlo en viewModel.viewModelScope ya que actualiza una variable usada en setupUI.
            viewModel.viewModelScope.launch { // o lifecycleScope.launch
                val userResult = firestoreRepository.getUser(viewerUserId!!) // Esto devuelve kotlin.Result
                if (userResult.isSuccess) { // kotlin.Result SÍ tiene .isSuccess
                    viewerRole = userResult.getOrNull()?.role
                    setupUIBasedOnRoleAndProfile()
                } else {
                    Toast.makeText(this@UserProfileActivity, "Error al obtener rol del observador.", Toast.LENGTH_SHORT).show()
                    setupUIBasedOnRoleAndProfile() // Intentar con rol nulo o por defecto
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
        // Recargar los datos del perfil cada vez que la actividad vuelve a primer plano.
        // Esto asegurará que _canRequestWords se reevalúe con los datos más frescos de Firestore.
        // Es importante si el estado del usuario (nivel, solicitudes) pudo haber cambiado
        // mientras esta pantalla no estaba visible.
        if (targetUserId != null) { // Asegúrate de que targetUserId ya está inicializado
            Log.d("UserProfileActivity", "onResume: Reloading user profile for $targetUserId")
            viewModel.loadUserProfile()
        }
    }

    private fun setupUIBasedOnRoleAndProfile() {
        val isViewingOwnProfileAsStudent = targetUserId == viewerUserId && viewerRole == "student"
        val isTeacherViewingStudentProfile = viewerRole == "teacher"

        if (isViewingOwnProfileAsStudent) {
            binding.tvProfileTitle.text = "Tu Perfil"
            binding.btnDesbloquearPalabrasProfesor.visibility = View.GONE
            binding.btnGenerarInformeProfesor.visibility = View.GONE
            // La visibilidad de btnSolicitarPalabras se maneja en el observer de canRequestWords
        } else if (isTeacherViewingStudentProfile) {
            binding.tvProfileTitle.text = "Perfil del Alumno" // Se actualizará con el nombre
            binding.btnSolicitarPalabras.visibility = View.GONE
            binding.btnDesbloquearPalabrasProfesor.visibility = View.VISIBLE
            binding.btnGenerarInformeProfesor.visibility = View.VISIBLE
        } else {
            // Otro caso (ej. profesor viendo su propio perfil, o alumno viendo perfil de otro alumno - si se permite)
            binding.tvProfileTitle.text = "Perfil"
            binding.btnSolicitarPalabras.visibility = View.GONE
            binding.btnDesbloquearPalabrasProfesor.visibility = View.GONE
            binding.btnGenerarInformeProfesor.visibility = View.GONE
        }
    }

    private fun setupClickListeners() {
        binding.btnSolicitarPalabras.setOnClickListener {
            viewModel.requestWords()
        }

        binding.btnDesbloquearPalabrasProfesor.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Aprobar Solicitud")
                .setMessage("¿Estás seguro de que quieres aprobar la solicitud de palabras para este alumno?")
                .setPositiveButton("Sí, Aprobar") { dialog, _ ->
                    viewModel.approveWordRequest()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        binding.btnGenerarInformeProfesor.setOnClickListener {
            Toast.makeText(this, "Funcionalidad 'Generar Informe' pendiente.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(this, Observer { isLoading ->
            binding.btnSolicitarPalabras.isEnabled = !isLoading
            binding.btnDesbloquearPalabrasProfesor.isEnabled = !isLoading
            // Ejemplo si tuvieras un ProgressBar general:
            // binding.progressBarUserProfileActivity.visibility = if(isLoading) View.VISIBLE else View.GONE
        })

        viewModel.errorMessage.observe(this, Observer { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearErrorMessage()
            }
        })

        viewModel.userProfile.observe(this, Observer { user ->
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
                    val isLoadingValue = viewModel.isLoading.value ?: false
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

        viewModel.canRequestWords.observe(this, Observer { canRequest ->
            val isViewingOwnProfileAsStudent = targetUserId == viewerUserId && viewerRole == "student"
            if (isViewingOwnProfileAsStudent) {
                binding.btnSolicitarPalabras.visibility = if (canRequest) View.VISIBLE else View.GONE
                val isLoadingValue = viewModel.isLoading.value ?: false
                binding.btnSolicitarPalabras.isEnabled = canRequest && !isLoadingValue
                if (canRequest) {
                    binding.btnSolicitarPalabras.text = "Solicitar Palabras (Nivel ${viewModel.userProfile.value?.currentLevel ?: ""})"
                }
            } else {
                binding.btnSolicitarPalabras.visibility = View.GONE
            }
        })

        viewModel.wordRequestOutcome.observe(this, Observer { resultOutcome -> // Renombrado 'result' a 'resultOutcome'
            resultOutcome?.let { outcome ->
                if (outcome is Result.Success) { // Ahora esto usa tu clase com.example.pictovoice.utils.Result.Success
                    Toast.makeText(this, "Solicitud de palabras enviada.", Toast.LENGTH_SHORT).show()
                }
                viewModel.clearWordRequestOutcome()
            }
        })

        viewModel.approveWordRequestOutcome.observe(this, Observer { resultOutcome -> // Renombrado 'result' a 'resultOutcome'
            resultOutcome?.let { outcome ->
                // if (outcome is Result.Success) { // Ya gestionado por _errorMessage en ViewModel
                // }
                viewModel.clearApproveWordRequestOutcome()
            }
        })
    }
}