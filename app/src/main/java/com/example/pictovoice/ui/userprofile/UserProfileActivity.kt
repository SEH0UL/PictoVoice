package com.example.pictovoice.ui.userprofile // Ajusta el paquete si es diferente

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import com.example.pictovoice.Data.repository.FirestoreRepository
import com.example.pictovoice.R
import com.example.pictovoice.databinding.ActivityUserProfileBinding // Asegúrate que es el nombre correcto de tu binding
import com.example.pictovoice.viewmodels.UserProfileViewModel
import com.example.pictovoice.viewmodels.UserProfileViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class UserProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserProfileBinding
    private var targetUserId: String? = null
    private var viewerUserId: String? = null // El UID del usuario que está viendo el perfil
    private var viewerRole: String? = null   // El rol del usuario que está viendo el perfil ("student" o "teacher")

    private val firestoreRepository = FirestoreRepository() // Considera inyección de dependencias

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
        // Para determinar quién está viendo, podrías pasar el rol del visualizador
        // o determinarlo aquí basado en el usuario actualmente autenticado.
        viewerUserId = FirebaseAuth.getInstance().currentUser?.uid
        // viewerRole = intent.getStringExtra("VIEWER_ROLE_EXTRA") // O cárgalo desde Firestore si es necesario

        if (targetUserId.isNullOrEmpty()) {
            Toast.makeText(this, "ID de usuario del perfil no encontrado.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        if (viewerUserId.isNullOrEmpty()){
            Toast.makeText(this, "No se pudo identificar al observador del perfil.", Toast.LENGTH_LONG).show()
            // Podrías redirigir a login o manejarlo de otra forma
            finish()
            return
        }

        // Cargar el rol del visualizador si no se pasó por intent
        // (Esto es un ejemplo, idealmente ya tendrías esta info)
        if (viewerRole == null && viewerUserId != null) {
            viewModel.viewModelScope.launch { // Usar el scope del ViewModel o crear uno nuevo
                val result = firestoreRepository.getUser(viewerUserId!!)
                if (result.isSuccess) {
                    viewerRole = result.getOrNull()?.role
                    setupUIBasedOnRoleAndProfile()
                } else {
                    Toast.makeText(this@UserProfileActivity, "Error al obtener rol del observador.", Toast.LENGTH_SHORT).show()
                    setupUIBasedOnRoleAndProfile() // Intentar con rol nulo o por defecto
                }
            }
        } else {
            setupUIBasedOnRoleAndProfile()
        }


        setupObservers()

        binding.btnSolicitarPalabras.setOnClickListener {
            viewModel.requestWords()
        }
        // Añade aquí los listeners para btnDesbloquearPalabrasProfesor y btnGenerarInformeProfesor si es necesario
        // Ejemplo:
         binding.btnDesbloquearPalabrasProfesor.setOnClickListener { /* Lógica profesor */ }
         binding.btnGenerarInformeProfesor.setOnClickListener { /* Lógica profesor */ }
    }

    private fun setupUIBasedOnRoleAndProfile() {
        val isViewingOwnProfileAsStudent = targetUserId == viewerUserId && viewerRole == "student"
        val isTeacherViewingStudentProfile = viewerRole == "teacher" // Y targetUser es un alumno

        if (isViewingOwnProfileAsStudent) {
            binding.tvProfileTitle.text = "Tu Perfil"
            // El botón btnSolicitarPalabras será gestionado por LiveData 'canRequestWords'
            binding.btnDesbloquearPalabrasProfesor.visibility = View.GONE
            binding.btnGenerarInformeProfesor.visibility = View.GONE
        } else if (isTeacherViewingStudentProfile) {
            // El nombre del alumno se cargará en el observer de userProfile
            binding.tvProfileTitle.text = "Perfil del Alumno" // Se actualizará con el nombre
            binding.btnSolicitarPalabras.visibility = View.GONE
            binding.btnDesbloquearPalabrasProfesor.visibility = View.VISIBLE // Lógica de habilitación/texto pendiente
            binding.btnGenerarInformeProfesor.visibility = View.VISIBLE // Lógica pendiente
        } else {
            // Otro caso (ej. profesor viendo su propio perfil, o alumno viendo perfil de otro alumno - si se permite)
            binding.tvProfileTitle.text = "Perfil"
            binding.btnSolicitarPalabras.visibility = View.GONE
            binding.btnDesbloquearPalabrasProfesor.visibility = View.GONE
            binding.btnGenerarInformeProfesor.visibility = View.GONE
        }
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(this, Observer { isLoading ->
            // Podrías tener un ProgressBar general, o deshabilitar botones mientras carga
            if (isLoading) {
                // Ejemplo: binding.btnSolicitarPalabras.isEnabled = false
            } else {
                // Ejemplo: binding.btnSolicitarPalabras.isEnabled = true (si aplica)
            }
            // Si tuvieras un ProgressBar global en el XML:
            // binding.someGlobalProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
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
                binding.tvLevelEnd.text = (it.currentLevel + 1).toString() // Nivel siguiente

                val expNeededForNextLevel = it.currentLevel * 1000 // Asumiendo 1000 EXP por nivel
                binding.progressBarLevel.max = expNeededForNextLevel
                binding.progressBarLevel.progress = it.currentExp

                // Actualizar el título del Toolbar o el tvProfileTitle si es un profesor viendo el perfil
                if (viewerRole == "teacher" && targetUserId != viewerUserId) {
                    supportActionBar?.title = "Perfil de ${it.fullName}"
                    binding.tvProfileTitle.text = "Perfil de ${it.fullName}"
                } else if (targetUserId == viewerUserId) {
                    supportActionBar?.title = "Mi Perfil"
                    binding.tvProfileTitle.text = "Mi Perfil"
                }


                // Placeholder para estadísticas (deberás implementarlo cuando tengas la lógica)
                binding.tvPalabrasUsadasCount.text = "0" // Placeholder
                binding.tvPalabrasNuevasCount.text = "0" // Placeholder
                binding.tvPalabrasDesbloqueadasCount.text = "0" // Placeholder
                binding.tvPalabrasBloqueadasCount.text = "0" // Placeholder

                // Lógica para el texto del botón del profesor "Desbloquear Palabras"
                if (viewerRole == "teacher"){
                    binding.btnDesbloquearPalabrasProfesor.text = "Desbloquear Palabras (Nivel ${it.currentLevel})"
                }
            }
        })

        viewModel.canRequestWords.observe(this, Observer { canRequest ->
            val isViewingOwnProfileAsStudent = targetUserId == viewerUserId && viewerRole == "student"
            if (isViewingOwnProfileAsStudent) {
                binding.btnSolicitarPalabras.visibility = if (canRequest) View.VISIBLE else View.GONE
                binding.btnSolicitarPalabras.isEnabled = canRequest
                if (canRequest) {
                    binding.btnSolicitarPalabras.text = "Solicitar Palabras (Nivel ${viewModel.userProfile.value?.currentLevel ?: ""})"
                }
            } else {
                binding.btnSolicitarPalabras.visibility = View.GONE
            }
        })

        viewModel.wordRequestOutcome.observe(this, Observer { result ->
            result?.let {
                if (it.isSuccess) {
                    Toast.makeText(this, "Solicitud de palabras enviada.", Toast.LENGTH_SHORT).show()
                } else {
                    // El mensaje de error ya se muestra a través de viewModel.errorMessage
                    // Pero si quieres un Toast específico aquí para este outcome:
                    // val errorMsg = (it as? com.example.pictovoice.utils.Result.Failure)?.message ?: "No se pudo enviar la solicitud."
                    // Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                }
                viewModel.clearWordRequestOutcome()
            }
        })
    }
}