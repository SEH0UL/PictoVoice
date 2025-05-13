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
import com.example.pictovoice.Data.Model.Classroom // Asegúrate de importar Classroom
import com.example.pictovoice.R
import com.example.pictovoice.adapters.TeacherClassesAdapter
import com.example.pictovoice.databinding.ActivityTeacherHomeBinding
import com.example.pictovoice.ui.auth.MainActivity
import com.example.pictovoice.ui.classroom.ClassDetailActivity // Import para la navegación
import com.example.pictovoice.utils.TeacherHomeViewModelFactory
import com.example.pictovoice.viewmodels.TeacherHomeResult
import com.example.pictovoice.viewmodels.TeacherHomeViewModel
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

        if (firebaseAuth.currentUser == null) {
            Log.w("TeacherHomeActivity", "Profesor no autenticado, redirigiendo a login.")
            navigateToLogin()
            return
        }

        setupRecyclerView()
        setupObservers()
        setupClickListeners()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setupRecyclerView() {
        classesAdapter = TeacherClassesAdapter(
            onAccessClick = { classroom -> // classroom es de tipo Classroom aquí
                Log.d("TeacherHomeActivity", "Acceder a clase ID: ${classroom.classId}, Nombre: ${classroom.className}")
                val intent = Intent(this, ClassDetailActivity::class.java).apply {
                    putExtra(ClassDetailActivity.EXTRA_CLASS_ID, classroom.classId)
                    putExtra(ClassDetailActivity.EXTRA_CLASS_NAME, classroom.className)
                }
                startActivity(intent)
            },
            onEditClick = { classroom -> // classroom es de tipo Classroom
                Toast.makeText(this, "Editar: ${classroom.className}", Toast.LENGTH_SHORT).show()
                showEditClassDialog(classroom)
            },
            onDeleteClick = { classroom -> // classroom es de tipo Classroom
                showDeleteConfirmationDialog(classroom) // Llamada al método que debe existir
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
                Log.d("TeacherHomeActivity", "Datos del profesor actualizados en UI: ${it.fullName}")
            } ?: run {
                binding.tvTeacherName.text = getString(R.string.teacher_name_not_found)
                Log.w("TeacherHomeActivity", "Datos del profesor son nulos.")
            }
        }

        viewModel.classes.observe(this) { classrooms ->
            Log.d("TeacherHomeActivity", "Actualizando lista de clases en UI: ${classrooms?.size ?: 0} clases.")
            classesAdapter.submitList(classrooms)
            if (classrooms.isNullOrEmpty()) {
                binding.rvTeacherClasses.visibility = View.GONE
                // Podrías mostrar un TextView indicando que no hay clases aquí
            } else {
                binding.rvTeacherClasses.visibility = View.VISIBLE
            }
        }

        viewModel.uiState.observe(this) { result ->
            when (result) {
                is TeacherHomeResult.Loading -> binding.progressBarTeacherHome.visibility = View.VISIBLE
                is TeacherHomeResult.Success -> binding.progressBarTeacherHome.visibility = View.GONE
                is TeacherHomeResult.Error -> {
                    binding.progressBarTeacherHome.visibility = View.GONE
                    Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                }
                is TeacherHomeResult.Idle -> binding.progressBarTeacherHome.visibility = View.GONE
            }
        }

        viewModel.createClassResult.observe(this) { result ->
            when (result) {
                is TeacherHomeResult.Loading -> binding.progressBarTeacherHome.visibility = View.VISIBLE
                is TeacherHomeResult.Success -> {
                    binding.progressBarTeacherHome.visibility = View.GONE
                    Toast.makeText(this, "Clase creada exitosamente", Toast.LENGTH_SHORT).show()
                    viewModel.resetCreateClassResult()
                }
                is TeacherHomeResult.Error -> {
                    binding.progressBarTeacherHome.visibility = View.GONE
                    Toast.makeText(this, "Error al crear clase: ${result.message}", Toast.LENGTH_LONG).show()
                    viewModel.resetCreateClassResult()
                }
                is TeacherHomeResult.Idle -> binding.progressBarTeacherHome.visibility = View.GONE
            }
        }

        viewModel.deleteClassResult.observe(this) { result ->
            when (result) {
                is TeacherHomeResult.Loading -> binding.progressBarTeacherHome.visibility = View.VISIBLE
                is TeacherHomeResult.Success -> {
                    binding.progressBarTeacherHome.visibility = View.GONE
                    Toast.makeText(this, "Clase eliminada exitosamente", Toast.LENGTH_SHORT).show()
                    viewModel.resetDeleteClassResult()
                }
                is TeacherHomeResult.Error -> {
                    binding.progressBarTeacherHome.visibility = View.GONE
                    Toast.makeText(this, "Error al eliminar clase: ${result.message}", Toast.LENGTH_LONG).show()
                    viewModel.resetDeleteClassResult()
                }
                is TeacherHomeResult.Idle -> binding.progressBarTeacherHome.visibility = View.GONE
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnCreateNewClass.setOnClickListener {
            showCreateClassDialog()
        }
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
                    viewModel.createNewClass(className, emptyList()) // Lista de alumnos vacía por ahora
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
                        viewModel.updateClass(updatedClassroom)
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

    // MÉTODO NECESARIO PARA ELIMINAR CLASE
    private fun showDeleteConfirmationDialog(classroom: Classroom) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Clase")
            .setMessage("¿Estás seguro de que quieres eliminar la clase \"${classroom.className}\"? Esta acción no se puede deshacer.")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("Sí, eliminar") { _, _ ->
                viewModel.deleteClass(classroom) // Llama al método del ViewModel
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}