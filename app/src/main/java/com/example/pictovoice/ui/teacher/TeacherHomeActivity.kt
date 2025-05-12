package com.example.pictovoice.ui.teacher

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pictovoice.Data.Model.Classroom
import com.example.pictovoice.R
import com.example.pictovoice.adapters.TeacherClassesAdapter
import com.example.pictovoice.databinding.ActivityTeacherHomeBinding
import com.example.pictovoice.ui.auth.MainActivity
import com.example.pictovoice.utils.TeacherHomeViewModelFactory
import com.example.pictovoice.viewmodels.TeacherHomeResult
import com.example.pictovoice.viewmodels.TeacherHomeViewModel
// Descomentar si se usa Glide para imágenes de perfil:
// import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth

class TeacherHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTeacherHomeBinding
    private val viewModel: TeacherHomeViewModel by viewModels {
        TeacherHomeViewModelFactory(application)
    }
    private lateinit var classesAdapter: TeacherClassesAdapter
    private val firebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTeacherHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Redirigir a login si el usuario no está autenticado.
        if (firebaseAuth.currentUser == null) {
            Log.w("TeacherHomeActivity", "Profesor no autenticado, redirigiendo a login.")
            navigateToLogin()
            return // Importante para no continuar con el resto del onCreate.
        }

        setupRecyclerView()
        setupObservers()
        setupClickListeners()

        // La carga inicial de datos se realiza en el 'init' del ViewModel.
    }

    private fun navigateToLogin() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setupRecyclerView() {
        classesAdapter = TeacherClassesAdapter(
            onAccessClick = { classroom ->
                // TODO: Implementar navegación a la pantalla "Vista de Clase".
                Toast.makeText(this, "Accediendo a: ${classroom.className}", Toast.LENGTH_SHORT).show()
                Log.d("TeacherHomeActivity", "Acceder a clase ID: ${classroom.classId}")
                // val intent = Intent(this, ClassDetailActivity::class.java)
                // intent.putExtra("CLASS_ID", classroom.classId)
                // startActivity(intent)
            },
            onEditClick = { classroom ->
                // TODO: Implementar pantalla/diálogo de edición más completo si es necesario.
                showEditClassDialog(classroom)
            },
            onDeleteClick = { classroom ->
                showDeleteConfirmationDialog(classroom)
            }
        )
        binding.rvTeacherClasses.apply {
            layoutManager = LinearLayoutManager(this@TeacherHomeActivity)
            adapter = classesAdapter
        }
    }

    private fun setupObservers() {
        viewModel.teacherData.observe(this) { teacher ->
            teacher?.let {
                binding.tvTeacherName.text = it.fullName
                // Ejemplo para cargar imagen de perfil con Glide:
                // if (it.profileImageUrl.isNotBlank()) {
                //    Glide.with(this).load(it.profileImageUrl)
                //        .placeholder(R.drawable.ic_default_profile) // Un placeholder mientras carga
                //        .error(R.drawable.ic_default_profile) // Un drawable de error si falla
                //        .circleCrop() // Para hacerla redonda
                //        .into(binding.ivTeacherProfileImage)
                // } else {
                //    binding.ivTeacherProfileImage.setImageResource(R.drawable.ic_default_profile)
                // }
                Log.d("TeacherHomeActivity", "Datos del profesor cargados en UI: ${it.fullName}")
            } ?: run {
                binding.tvTeacherName.text = getString(R.string.teacher_name_not_found) // Usar recurso string
                Log.w("TeacherHomeActivity", "Datos del profesor (User) son nulos.")
            }
        }

        viewModel.classes.observe(this) { classrooms ->
            Log.d("TeacherHomeActivity", "Actualizando lista de clases en UI: ${classrooms?.size ?: 0} clases.")
            classesAdapter.submitList(classrooms)
            if (classrooms.isNullOrEmpty()) {
                binding.rvTeacherClasses.visibility = View.GONE
                // Aquí podrías mostrar un mensaje como "No tienes clases creadas."
                // binding.tvNoClassesMessage.visibility = View.VISIBLE
            } else {
                binding.rvTeacherClasses.visibility = View.VISIBLE
                // binding.tvNoClassesMessage.visibility = View.GONE
            }
        }

        // Observador para el estado general de la UI (carga inicial, errores generales)
        viewModel.uiState.observe(this) { result ->
            when (result) {
                is TeacherHomeResult.Loading -> {
                    binding.progressBarTeacherHome.visibility = View.VISIBLE
                }
                is TeacherHomeResult.Success -> {
                    binding.progressBarTeacherHome.visibility = View.GONE
                    // Los datos específicos (teacherData, classes) se actualizan por sus propios observers.
                }
                is TeacherHomeResult.Error -> {
                    binding.progressBarTeacherHome.visibility = View.GONE
                    Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                    Log.e("TeacherHomeActivity", "Error UI general: ${result.message}")
                }
                is TeacherHomeResult.Idle -> {
                    binding.progressBarTeacherHome.visibility = View.GONE
                }
            }
        }

        // Observador para el resultado de la creación de clases
        viewModel.createClassResult.observe(this) { result ->
            when (result) {
                is TeacherHomeResult.Loading -> {
                    binding.progressBarTeacherHome.visibility = View.VISIBLE
                }
                is TeacherHomeResult.Success -> {
                    binding.progressBarTeacherHome.visibility = View.GONE
                    Toast.makeText(this, "Clase creada exitosamente.", Toast.LENGTH_SHORT).show()
                    viewModel.resetCreateClassResult()
                }
                is TeacherHomeResult.Error -> {
                    binding.progressBarTeacherHome.visibility = View.GONE
                    Toast.makeText(this, "Error al crear clase: ${result.message}", Toast.LENGTH_LONG).show()
                    viewModel.resetCreateClassResult()
                }
                is TeacherHomeResult.Idle -> { /* No action needed */ }
            }
        }

        // Observador para el resultado de la eliminación de clases
        viewModel.deleteClassResult.observe(this) { result ->
            when (result) {
                is TeacherHomeResult.Loading -> {
                    binding.progressBarTeacherHome.visibility = View.VISIBLE
                }
                is TeacherHomeResult.Success -> {
                    binding.progressBarTeacherHome.visibility = View.GONE
                    Toast.makeText(this, "Clase eliminada exitosamente.", Toast.LENGTH_SHORT).show()
                    viewModel.resetDeleteClassResult()
                }
                is TeacherHomeResult.Error -> {
                    binding.progressBarTeacherHome.visibility = View.GONE
                    Toast.makeText(this, "Error al eliminar clase: ${result.message}", Toast.LENGTH_LONG).show()
                    viewModel.resetDeleteClassResult()
                }
                is TeacherHomeResult.Idle -> { /* No action needed */ }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnCreateNewClass.setOnClickListener {
            showCreateClassDialog()
        }
        // TODO: Añadir listener para un botón de logout si es necesario
    }

    private fun showCreateClassDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_class, null)
        val etClassName = dialogView.findViewById<EditText>(R.id.etDialogClassName)

        AlertDialog.Builder(this)
            .setTitle("Crear Nueva Clase")
            .setView(dialogView)
            .setPositiveButton("Crear") { _, _ ->
                val className = etClassName.text.toString().trim()
                if (className.isNotEmpty()) {
                    // TODO: Implementar selección de alumnos y pasar sus IDs.
                    val studentIds = emptyList<String>() // Por ahora, lista vacía.
                    viewModel.createNewClass(className, studentIds)
                } else {
                    Toast.makeText(this, "El nombre de la clase no puede estar vacío.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showEditClassDialog(classroom: Classroom) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_class, null) // Reutilizar layout
        val etClassName = dialogView.findViewById<EditText>(R.id.etDialogClassName)
        etClassName.setText(classroom.className)

        // TODO: Añadir funcionalidad para editar la lista de alumnos de la clase.

        AlertDialog.Builder(this)
            .setTitle("Editar Clase")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val newClassName = etClassName.text.toString().trim()
                if (newClassName.isNotEmpty()) {
                    if (newClassName != classroom.className /* || alumnosCambiaron */) {
                        val updatedClassroom = classroom.copy(className = newClassName) // Actualizar solo nombre por ahora
                        viewModel.updateClass(updatedClassroom)
                    } else {
                        Toast.makeText(this, "No se realizaron cambios.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "El nombre de la clase no puede estar vacío.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showDeleteConfirmationDialog(classroom: Classroom) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar Eliminación")
            .setMessage("¿Estás seguro de que quieres eliminar la clase \"${classroom.className}\"? Esta acción no se puede deshacer.")
            .setIcon(android.R.drawable.ic_dialog_alert) // Icono de advertencia estándar
            .setPositiveButton("Sí, Eliminar") { _, _ ->
                viewModel.deleteClass(classroom)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        // Considera si necesitas recargar los datos aquí, aunque el ViewModel debería manejar
        // las actualizaciones después de crear/editar/eliminar clases.
        // viewModel.loadTeacherDataAndClasses()
    }
}