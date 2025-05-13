package com.example.pictovoice.ui.userprofile // O el paquete que elijas para esta nueva actividad

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.pictovoice.R // Asegúrate que este import es correcto
import com.example.pictovoice.databinding.ActivityUserProfileBinding
import com.google.firebase.auth.FirebaseAuth

class UserProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserProfileBinding
    private var targetUserId: String? = null // El ID del usuario cuyo perfil se está viendo
    private var viewerRole: String? = null   // Quién está viendo el perfil: "alumno" o "profesor"

    companion object {
        const val EXTRA_USER_ID = "extra_user_id"
        const val EXTRA_VIEWER_ROLE = "extra_viewer_role"
        const val ROLE_STUDENT = "alumno"
        const val ROLE_TEACHER = "profesor"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflar el layout que ya creaste: activity_user_profile.xml
        binding = ActivityUserProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Recoger los datos pasados a esta actividad
        targetUserId = intent.getStringExtra(EXTRA_USER_ID)
        viewerRole = intent.getStringExtra(EXTRA_VIEWER_ROLE)

        // Lógica para determinar el ID del usuario a mostrar
        if (targetUserId.isNullOrBlank()) {
            if (viewerRole == ROLE_STUDENT) {
                // Si el alumno ve su propio perfil y no se pasó ID, usar el del usuario logueado
                targetUserId = FirebaseAuth.getInstance().currentUser?.uid
                if (targetUserId.isNullOrBlank()){
                    Toast.makeText(this, "Error: No se pudo identificar al usuario.", Toast.LENGTH_LONG).show()
                    finish()
                    return
                }
            } else {
                // Si es un profesor y no se especificó qué alumno ver, es un error.
                Toast.makeText(this, "Error: No se especificó el perfil del alumno a ver.", Toast.LENGTH_LONG).show()
                finish()
                return
            }
        }

        setupToolbar()
        configureUiBasedOnRole() // Configurar la UI inicial y visibilidad de botones
        loadUserProfileData() // Cargar y mostrar datos (por ahora, placeholders)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarUserProfile)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // Determinar el título de la Toolbar
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        if (viewerRole == ROLE_STUDENT && targetUserId == currentUserUid) {
            supportActionBar?.title = "Mi Perfil"
        } else {
            supportActionBar?.title = "Perfil del Alumno"
        }
    }

    private fun configureUiBasedOnRole() {
        val isOwnProfile = viewerRole == ROLE_STUDENT && targetUserId == FirebaseAuth.getInstance().currentUser?.uid

        if (viewerRole == ROLE_STUDENT) { // Vista del alumno (viendo su propio perfil o el de otro si se permitiera)
            // El botón "Solicitar Palabras" solo es relevante si está viendo su propio perfil
            // y si cumple las condiciones para solicitar (lógica futura).
            binding.btnSolicitarPalabras.visibility = if (isOwnProfile) View.VISIBLE else View.GONE // Placeholder: visible por ahora
            binding.btnDesbloquearPalabrasProfesor.visibility = View.GONE
            binding.btnGenerarInformeProfesor.visibility = View.GONE
        } else if (viewerRole == ROLE_TEACHER) { // Vista del profesor
            binding.btnSolicitarPalabras.visibility = View.GONE
            binding.btnDesbloquearPalabrasProfesor.visibility = View.VISIBLE
            binding.btnGenerarInformeProfesor.visibility = View.VISIBLE
        } else {
            // Rol desconocido, ocultar todos los botones de acción específicos
            binding.btnSolicitarPalabras.visibility = View.GONE
            binding.btnDesbloquearPalabrasProfesor.visibility = View.GONE
            binding.btnGenerarInformeProfesor.visibility = View.GONE
        }
    }

    private fun loadUserProfileData() {
        // --- ESTA ES LA PARTE DE LÓGICA QUE DEJAREMOS PARA MÁS TARDE ---
        // Aquí es donde crearías una instancia de UserProfileViewModel,
        // le pasarías el targetUserId, y observarías los LiveData para actualizar la UI.

        // Por AHORA, usamos datos de placeholder para que la pantalla no esté vacía:
        // Título principal de la pantalla (ya no es necesario si la Toolbar lo tiene)
        // binding.tvProfileTitle.text = (si el alumno ve su perfil) ? "Tu Perfil" : "Perfil Alumno";

        // Nombre del alumno (debería venir del ViewModel)
        binding.tvUserProfileName.text = "Cargando nombre..." // Placeholder inicial

        // Nivel y ProgressBar (debería venir del ViewModel)
        binding.tvLabelNivel.text = "NIVEL" // Esto es fijo
        binding.tvLevelStart.text = "1"
        binding.progressBarLevel.progress = 30 // Ejemplo
        binding.progressBarLevel.max = 100 // Asumiendo que el progreso va de 0 a 100 para el nivel actual
        binding.tvLevelEnd.text = "2" // (Nivel actual + 1)

        // Estadísticas (deberían venir del ViewModel)
        binding.tvPalabrasUsadasCount.text = "--"
        binding.tvPalabrasNuevasCount.text = "--"
        binding.tvPalabrasDesbloqueadasCount.text = "--"
        binding.tvPalabrasBloqueadasCount.text = "--"

        // Lógica del botón "Solicitar Palabras" (placeholder)
        if (binding.btnSolicitarPalabras.visibility == View.VISIBLE) {
            // El texto real del botón debería ser dinámico, ej. "Solicitar Palabras (Nivel ${siguienteNivel})"
            binding.btnSolicitarPalabras.text = getString(R.string.solicitar_palabras_placeholder, 2) // Necesitarás este string
            // La visibilidad real de este botón dependerá de la lógica de si el alumno
            // ha subido de nivel y aún no ha solicitado las palabras.
        }

        // Lógica del botón "Desbloquear Palabras" (placeholder)
        if(binding.btnDesbloquearPalabrasProfesor.visibility == View.VISIBLE) {
            binding.btnDesbloquearPalabrasProfesor.text = getString(R.string.desbloquear_palabras_placeholder, 1) // Nivel actual del alumno
        }


        // Placeholder para la imagen de perfil
        binding.ivUserProfileImage.setImageResource(R.drawable.ic_default_profile) // O un placeholder con iniciales

        // TODO: Una vez que tengas el ViewModel:
        // viewModel.loadUserData(targetUserId)
        // viewModel.userData.observe(this) { user -> actualiza la UI }
        // viewModel.levelUpStatus.observe(this) { status -> actualiza botón solicitar }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed() // Correcto para el botón de atrás de la Toolbar
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}