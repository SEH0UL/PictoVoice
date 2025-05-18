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
import androidx.lifecycle.Observer // Asegúrate de tener este import
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pictovoice.Data.Model.Classroom
import com.example.pictovoice.Data.repository.AuthRepository // Importado para AuthViewModelFactory
import com.example.pictovoice.R
import com.example.pictovoice.adapters.TeacherClassesAdapter
import com.example.pictovoice.databinding.ActivityTeacherHomeBinding
import com.example.pictovoice.ui.auth.MainActivity
import com.example.pictovoice.ui.classroom.ClassDetailActivity
import com.example.pictovoice.utils.AuthViewModel // Importar AuthViewModel
import com.example.pictovoice.utils.AuthViewModelFactory // Importar AuthViewModelFactory
import com.example.pictovoice.utils.TeacherHomeViewModelFactory
import com.example.pictovoice.viewmodels.TeacherHomeResult
import com.example.pictovoice.viewmodels.TeacherHomeViewModel
import com.google.firebase.auth.FirebaseAuth

class TeacherHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTeacherHomeBinding
    // ViewModel para la lógica de la home del profesor (renombrado para claridad)
    private val teacherHomeViewModel: TeacherHomeViewModel by viewModels {
        TeacherHomeViewModelFactory(application)
    }
    // ViewModel para la autenticación (logout)
    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(AuthRepository())
    }
    private lateinit var classesAdapter: TeacherClassesAdapter
    private val firebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTeacherHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (firebaseAuth.currentUser == null) {
            Log.w("TeacherHomeActivity", "Profesor no autenticado, redirigiendo a login.")
            navigateToLogin()
            return
        }

        setupRecyclerView()
        setupClickListeners() // Llamada a setupClickListeners
        setupObservers()    // Llamada a setupObservers
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
                Log.d("TeacherHomeActivity", "Acceder a clase ID: ${classroom.classId}, Nombre: ${classroom.className}")
                val intent = Intent(this, ClassDetailActivity::class.java).apply {
                    putExtra(ClassDetailActivity.EXTRA_CLASS_ID, classroom.classId)
                    putExtra(ClassDetailActivity.EXTRA_CLASS_NAME, classroom.className)
                }
                startActivity(intent)
            },
            onEditClick = { classroom ->
                Toast.makeText(this, "Editar: ${classroom.className}", Toast.LENGTH_SHORT).show()
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
        // Observadores para TeacherHomeViewModel (usando teacherHomeViewModel)
        teacherHomeViewModel.teacherData.observe(this) { teacher -> // CAMBIO: viewModel -> teacherHomeViewModel
            teacher?.let {
                binding.tvTeacherName.text = it.fullName
                Log.d("TeacherHomeActivity", "Datos del profesor actualizados en UI: ${it.fullName}")
            } ?: run {
                binding.tvTeacherName.text = getString(R.string.teacher_name_not_found) // Asegúrate de tener este string
                Log.w("TeacherHomeActivity", "Datos del profesor son nulos.")
            }
        }

        teacherHomeViewModel.classes.observe(this) { classrooms -> // CAMBIO: viewModel -> teacherHomeViewModel
            Log.d("TeacherHomeActivity", "Actualizando lista de clases en UI: ${classrooms?.size ?: 0} clases.")
            classesAdapter.submitList(classrooms)
            binding.rvTeacherClasses.visibility = if (classrooms.isNullOrEmpty()) View.GONE else View.VISIBLE
        }

        teacherHomeViewModel.uiState.observe(this) { result -> // CAMBIO: viewModel -> teacherHomeViewModel
            binding.progressBarTeacherHome.visibility = if (result is TeacherHomeResult.Loading) View.VISIBLE else View.GONE
            if (result is TeacherHomeResult.Error) {
                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
            }
        }

        teacherHomeViewModel.createClassResult.observe(this) { result -> // CAMBIO: viewModel -> teacherHomeViewModel
            binding.progressBarTeacherHome.visibility = if (result is TeacherHomeResult.Loading) View.VISIBLE else View.GONE
            when (result) {
                is TeacherHomeResult.Success -> {
                    Toast.makeText(this, "Clase creada exitosamente", Toast.LENGTH_SHORT).show()
                    teacherHomeViewModel.resetCreateClassResult() // CAMBIO: viewModel -> teacherHomeViewModel
                }
                is TeacherHomeResult.Error -> {
                    Toast.makeText(this, "Error al crear clase: ${result.message}", Toast.LENGTH_LONG).show()
                    teacherHomeViewModel.resetCreateClassResult() // CAMBIO: viewModel -> teacherHomeViewModel
                }
                else -> {} // Idle o Loading ya cubierto por visibilidad del progressbar
            }
        }

        teacherHomeViewModel.deleteClassResult.observe(this) { result -> // CAMBIO: viewModel -> teacherHomeViewModel
            binding.progressBarTeacherHome.visibility = if (result is TeacherHomeResult.Loading) View.VISIBLE else View.GONE
            when (result) {
                is TeacherHomeResult.Success -> {
                    Toast.makeText(this, "Clase eliminada exitosamente", Toast.LENGTH_SHORT).show()
                    teacherHomeViewModel.resetDeleteClassResult() // CAMBIO: viewModel -> teacherHomeViewModel
                }
                is TeacherHomeResult.Error -> {
                    Toast.makeText(this, "Error al eliminar clase: ${result.message}", Toast.LENGTH_LONG).show()
                    teacherHomeViewModel.resetDeleteClassResult() // CAMBIO: viewModel -> teacherHomeViewModel
                }
                else -> {} // Idle o Loading
            }
        }

        // --- INICIO: Nuevo Observador para Logout Event ---
        authViewModel.logoutEvent.observe(this, Observer { hasLoggedOut ->
            if (hasLoggedOut == true) { // Usar == true para LiveData<Boolean> que puede ser null
                Toast.makeText(this, "Sesión cerrada.", Toast.LENGTH_SHORT).show()
                navigateToLogin()
                authViewModel.onLogoutEventHandled() // Resetea el evento para que no se dispare de nuevo
            }
        })
        // --- FIN: Nuevo Observador para Logout Event ---
    }

    private fun setupClickListeners() {
        binding.btnCreateNewClass.setOnClickListener {
            showCreateClassDialog()
        }

        // --- INICIO: Listener para el botón de Logout del Profesor ---
        // Asegúrate de que el ID 'btnTeacherLogout' coincide con el que pusiste en tu XML
        binding.btnTeacherLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }
        // --- FIN: Listener para el botón de Logout del Profesor ---
    }

    // --- INICIO: Nueva función para el diálogo de confirmación de Logout ---
    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro de que quieres cerrar sesión?")
            .setPositiveButton("Sí, cerrar sesión") { dialog, _ ->
                authViewModel.logoutUser() // Llama a la función de logout en AuthViewModel
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    // --- FIN: Nueva función para el diálogo de confirmación de Logout ---

    private fun showCreateClassDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_class, null)
        val etClassName = dialogView.findViewById<EditText>(R.id.etDialogClassName)
        AlertDialog.Builder(this)
            .setTitle("Crear Nueva Clase")
            .setView(dialogView)
            .setPositiveButton("Crear") { _, _ ->
                val className = etClassName.text.toString().trim()
                if (className.isNotEmpty()) {
                    // Usar teacherHomeViewModel para la lógica de negocio de la clase
                    teacherHomeViewModel.createNewClass(className, emptyList())
                } else {
                    Toast.makeText(this, "El nombre de la clase no puede estar vacío", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showEditClassDialog(classroom: Classroom) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_class, null)
        val etClassName = dialogView.findViewById<EditText>(R.id.etDialogClassName)
        etClassName.setText(classroom.className)
        AlertDialog.Builder(this)
            .setTitle("Editar Clase")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val newClassName = etClassName.text.toString().trim()
                if (newClassName.isNotEmpty()) {
                    if (newClassName != classroom.className) {
                        val updatedClassroom = classroom.copy(className = newClassName)
                        teacherHomeViewModel.updateClass(updatedClassroom) // Usar teacherHomeViewModel
                    } else {
                        Toast.makeText(this, "No se detectaron cambios.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "El nombre de la clase no puede estar vacío", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showDeleteConfirmationDialog(classroom: Classroom) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Clase")
            .setMessage("¿Estás seguro de que quieres eliminar la clase \"${classroom.className}\"? Esta acción no se puede deshacer.")
            .setIcon(android.R.drawable.ic_dialog_alert) // Icono estándar de alerta
            .setPositiveButton("Sí, eliminar") { _, _ ->
                teacherHomeViewModel.deleteClass(classroom) // Usar teacherHomeViewModel
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}