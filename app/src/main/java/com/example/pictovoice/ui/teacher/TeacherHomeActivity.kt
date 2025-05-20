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
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pictovoice.Data.repository.AuthRepository
import com.example.pictovoice.R // Para acceder a R.string.*, R.layout.*, etc.
import com.example.pictovoice.data.model.Classroom
import com.example.pictovoice.adapters.TeacherClassesAdapter
import com.example.pictovoice.databinding.ActivityTeacherHomeBinding
import com.example.pictovoice.ui.auth.MainActivity
import com.example.pictovoice.ui.classroom.ClassDetailActivity
import com.example.pictovoice.utils.AuthViewModelFactory
import com.example.pictovoice.utils.TeacherHomeViewModelFactory
import com.example.pictovoice.viewmodels.AuthViewModel
import com.example.pictovoice.viewmodels.TeacherHomeResult
import com.example.pictovoice.viewmodels.TeacherHomeViewModel
import com.google.firebase.auth.FirebaseAuth

private const val TAG = "TeacherHomeActivity"

/**
 * Activity principal para el rol de Profesor.
 * Muestra la información del profesor, una lista de sus clases y permite la gestión
 * de estas (crear, editar, eliminar, acceder a detalles).
 * También proporciona la funcionalidad de cerrar sesión.
 */
class TeacherHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTeacherHomeBinding
    private val teacherHomeViewModel: TeacherHomeViewModel by viewModels {
        TeacherHomeViewModelFactory(application)
    }
    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(AuthRepository())
    }
    private lateinit var classesAdapter: TeacherClassesAdapter
    private val firebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTeacherHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d(TAG, "onCreate: Iniciando TeacherHomeActivity.")

        if (firebaseAuth.currentUser == null) {
            Log.w(TAG, "Profesor no autenticado. Redirigiendo a Login.")
            navigateToLogin()
            return
        }

        setupRecyclerView()
        setupClickListeners()
        setupObservers()
    }

    /**
     * Navega a [MainActivity] (pantalla de login) y finaliza la [TeacherHomeActivity].
     * Se utiliza cuando el usuario no está autenticado o cierra sesión.
     */
    private fun navigateToLogin() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /**
     * Configura el RecyclerView y su adaptador para mostrar la lista de clases del profesor.
     */
    private fun setupRecyclerView() {
        Log.d(TAG, "Configurando RecyclerView para clases.")
        classesAdapter = TeacherClassesAdapter(
            onAccessClick = { classroom ->
                Log.d(TAG, "Accediendo a clase: ${classroom.className} (ID: ${classroom.classId})")
                val intent = Intent(this, ClassDetailActivity::class.java).apply {
                    putExtra(ClassDetailActivity.EXTRA_CLASS_ID, classroom.classId)
                    putExtra(ClassDetailActivity.EXTRA_CLASS_NAME, classroom.className)
                }
                startActivity(intent)
            },
            onEditClick = { classroom ->
                Log.d(TAG, "Editando clase: ${classroom.className}")
                showEditClassDialog(classroom)
            },
            onDeleteClick = { classroom ->
                Log.d(TAG, "Solicitando eliminar clase: ${classroom.className}")
                showDeleteConfirmationDialog(classroom)
            }
        )
        binding.rvTeacherClasses.apply {
            layoutManager = LinearLayoutManager(this@TeacherHomeActivity)
            adapter = classesAdapter
        }
    }

    /**
     * Configura los listeners para los botones y otros elementos interactivos de la UI.
     */
    private fun setupClickListeners() {
        Log.d(TAG, "Configurando ClickListeners.")
        binding.btnCreateNewClass.setOnClickListener {
            Log.d(TAG, "Botón 'Crear Nueva Clase' pulsado.")
            showCreateClassDialog()
        }

        binding.btnTeacherLogout.setOnClickListener {
            Log.d(TAG, "Botón 'Cerrar Sesión' (Profesor) pulsado.")
            showLogoutConfirmationDialog()
        }
    }

    /**
     * Configura los observadores para los LiveData de los ViewModels.
     * Actualiza la UI en respuesta a los cambios de datos y resultados de operaciones.
     */
    private fun setupObservers() {
        Log.d(TAG, "Configurando Observadores.")

        teacherHomeViewModel.teacherData.observe(this, Observer { teacher ->
            teacher?.let {
                binding.tvTeacherName.text = it.fullName
                Log.i(TAG, "Datos del profesor actualizados en UI: ${it.fullName}")
            } ?: run {
                // CORRECCIÓN: Usar el nombre de string actualizado
                binding.tvTeacherName.text = getString(R.string.teacher_home_teacher_name_placeholder)
                Log.w(TAG, "Datos del profesor son nulos.")
            }
        })

        teacherHomeViewModel.classes.observe(this, Observer { classrooms ->
            Log.d(TAG, "Lista de clases actualizada. Número de clases: ${classrooms?.size ?: 0}")
            classesAdapter.submitList(classrooms)
            binding.rvTeacherClasses.visibility = if (classrooms.isNullOrEmpty()) View.GONE else View.VISIBLE
        })

        teacherHomeViewModel.uiState.observe(this, Observer { result ->
            binding.progressBarTeacherHome.visibility = if (result is TeacherHomeResult.Loading) View.VISIBLE else View.GONE
            if (result is TeacherHomeResult.Error) {
                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                Log.w(TAG, "Error de UIState: ${result.message}")
            }
        })

        teacherHomeViewModel.createClassResult.observe(this, Observer { result ->
            binding.progressBarTeacherHome.visibility = if (result is TeacherHomeResult.Loading) View.VISIBLE else View.GONE
            when (result) {
                is TeacherHomeResult.Success -> {
                    // CORRECCIÓN: Usar el nombre de string actualizado
                    Toast.makeText(this, getString(R.string.teacher_home_toast_class_created_successfully), Toast.LENGTH_SHORT).show()
                    Log.i(TAG, "Clase creada exitosamente.")
                    teacherHomeViewModel.resetCreateClassResult()
                }
                is TeacherHomeResult.Error -> {
                    // CORRECCIÓN: Usar el nombre de string actualizado
                    Toast.makeText(this, getString(R.string.teacher_home_toast_error_creating_class, result.message), Toast.LENGTH_LONG).show()
                    Log.e(TAG, "Error al crear clase: ${result.message}")
                    teacherHomeViewModel.resetCreateClassResult()
                }
                else -> {}
            }
        })

        teacherHomeViewModel.deleteClassResult.observe(this, Observer { result ->
            binding.progressBarTeacherHome.visibility = if (result is TeacherHomeResult.Loading) View.VISIBLE else View.GONE
            when (result) {
                is TeacherHomeResult.Success -> {
                    // CORRECCIÓN: Usar el nombre de string actualizado
                    Toast.makeText(this, getString(R.string.teacher_home_toast_class_deleted_successfully), Toast.LENGTH_SHORT).show()
                    Log.i(TAG, "Clase eliminada exitosamente.")
                    teacherHomeViewModel.resetDeleteClassResult()
                }
                is TeacherHomeResult.Error -> {
                    // CORRECCIÓN: Usar el nombre de string actualizado
                    Toast.makeText(this, getString(R.string.teacher_home_toast_error_deleting_class, result.message), Toast.LENGTH_LONG).show()
                    Log.e(TAG, "Error al eliminar clase: ${result.message}")
                    teacherHomeViewModel.resetDeleteClassResult()
                }
                else -> {}
            }
        })

        authViewModel.logoutEvent.observe(this, Observer { hasLoggedOut ->
            if (hasLoggedOut == true) {
                // CORRECCIÓN: Usar el nombre de string actualizado y genérico
                Toast.makeText(this, getString(R.string.toast_session_closed_generic), Toast.LENGTH_SHORT).show()
                Log.i(TAG, "Evento de logout recibido, navegando a login.")
                navigateToLogin()
                authViewModel.onLogoutEventHandled()
            }
        })
    }

    /**
     * Muestra un diálogo para que el profesor ingrese el nombre de una nueva clase.
     */
    private fun showCreateClassDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_class, null)
        val etClassName = dialogView.findViewById<EditText>(R.id.etDialogClassName)
        AlertDialog.Builder(this)
            // CORRECCIÓN: Usar el nombre de string actualizado
            .setTitle(getString(R.string.teacher_home_dialog_title_create_class))
            .setView(dialogView)
            // CORRECCIÓN: Usar el nombre de string actualizado
            .setPositiveButton(getString(R.string.dialog_button_create)) { _, _ ->
                val className = etClassName.text.toString().trim()
                if (className.isNotEmpty()) {
                    teacherHomeViewModel.createNewClass(className, emptyList())
                } else {
                    // CORRECCIÓN: Usar el nombre de string actualizado
                    Toast.makeText(this, getString(R.string.teacher_home_error_classname_required), Toast.LENGTH_SHORT).show()
                }
            }
            // CORRECCIÓN: Usar el nombre de string actualizado
            .setNegativeButton(getString(R.string.dialog_button_cancel), null)
            .show()
    }

    /**
     * Muestra un diálogo para que el profesor edite el nombre de una clase existente.
     * @param classroom La [Classroom] a editar.
     */
    private fun showEditClassDialog(classroom: Classroom) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_class, null)
        val etClassName = dialogView.findViewById<EditText>(R.id.etDialogClassName)
        etClassName.setText(classroom.className)
        AlertDialog.Builder(this)
            // CORRECCIÓN: Usar el nombre de string actualizado
            .setTitle(getString(R.string.teacher_home_dialog_title_edit_class))
            .setView(dialogView)
            // CORRECCIÓN: Usar el nombre de string actualizado
            .setPositiveButton(getString(R.string.dialog_button_save)) { _, _ ->
                val newClassName = etClassName.text.toString().trim()
                if (newClassName.isNotEmpty()) {
                    if (newClassName != classroom.className) {
                        val updatedClassroom = classroom.copy(className = newClassName)
                        teacherHomeViewModel.updateClass(updatedClassroom)
                    } else {
                        // CORRECCIÓN: Usar el nombre de string actualizado
                        Toast.makeText(this, getString(R.string.teacher_home_toast_no_changes_detected), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // CORRECCIÓN: Usar el nombre de string actualizado
                    Toast.makeText(this, getString(R.string.teacher_home_error_classname_required), Toast.LENGTH_SHORT).show()
                }
            }
            // CORRECCIÓN: Usar el nombre de string actualizado
            .setNegativeButton(getString(R.string.dialog_button_cancel), null)
            .show()
    }

    /**
     * Muestra un diálogo de confirmación antes de eliminar una clase.
     * @param classroom La [Classroom] a eliminar.
     */
    private fun showDeleteConfirmationDialog(classroom: Classroom) {
        AlertDialog.Builder(this)
            // CORRECCIÓN: Usar el nombre de string actualizado
            .setTitle(getString(R.string.teacher_home_dialog_title_delete_class))
            // CORRECCIÓN: Usar el nombre de string actualizado
            .setMessage(getString(R.string.teacher_home_dialog_message_delete_class, classroom.className))
            .setIcon(android.R.drawable.ic_dialog_alert)
            // CORRECCIÓN: Usar el nombre de string actualizado
            .setPositiveButton(getString(R.string.dialog_button_yes_delete)) { _, _ ->
                teacherHomeViewModel.deleteClass(classroom)
            }
            // CORRECCIÓN: Usar el nombre de string actualizado
            .setNegativeButton(getString(R.string.dialog_button_cancel), null)
            .show()
    }

    /**
     * Muestra un diálogo de confirmación antes de cerrar la sesión del profesor.
     */
    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            // CORRECCIÓN: Usar los nombres de string genéricos/actualizados
            .setTitle(getString(R.string.logout_dialog_title))
            .setMessage(getString(R.string.logout_dialog_message_confirm))
            .setPositiveButton(getString(R.string.dialog_button_yes_logout)) { dialog, _ ->
                authViewModel.logoutUser()
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.dialog_button_cancel), null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: TeacherHomeActivity destruida.")
    }
}