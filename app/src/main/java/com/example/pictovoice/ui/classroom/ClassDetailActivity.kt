package com.example.pictovoice.ui.classroom

import User
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pictovoice.R
import com.example.pictovoice.adapters.AvailableStudentSearchAdapter
import com.example.pictovoice.adapters.ClassStudentListAdapter
import com.example.pictovoice.databinding.ActivityClassDetailBinding
import com.example.pictovoice.ui.userprofile.UserProfileActivity
import com.example.pictovoice.utils.ClassDetailViewModelFactory
import com.example.pictovoice.viewmodels.ClassDetailResult
import com.example.pictovoice.viewmodels.ClassDetailViewModel

private const val TAG = "ClassDetailActivity"

/**
 * Activity que muestra los detalles de una clase específica, permitiendo al profesor
 * ver la lista de alumnos, añadir nuevos alumnos y eliminar existentes.
 * También permite la búsqueda de alumnos dentro de la clase.
 */
class ClassDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityClassDetailBinding
    private val viewModel: ClassDetailViewModel by viewModels {
        ClassDetailViewModelFactory(application, classId ?: "")
    }
    private lateinit var studentAdapter: ClassStudentListAdapter
    private lateinit var availableStudentSearchAdapter: AvailableStudentSearchAdapter

    private var classId: String? = null
    private var className: String? = null
    private var addStudentDialog: AlertDialog? = null

    companion object {
        const val EXTRA_CLASS_ID = "extra_class_id"
        const val EXTRA_CLASS_NAME = "extra_class_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClassDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d(TAG, "onCreate: Iniciando ClassDetailActivity.")

        classId = intent.getStringExtra(EXTRA_CLASS_ID)
        className = intent.getStringExtra(EXTRA_CLASS_NAME)

        if (classId.isNullOrBlank()) {
            Log.e(TAG, "Error: No se recibió el ID de la clase o está vacío. Finalizando actividad.")
            Toast.makeText(this, getString(R.string.class_detail_error_class_id_missing), Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupToolbar()
        setupRecyclerViews()
        setupSearchView()
        setupClickListeners()
        setupObservers()

        binding.tvClassDetailNameHeader.text = className ?: getString(R.string.class_detail_title_activity_default)
        Log.i(TAG, "Mostrando detalles para la clase: '$className' (ID: $classId)")
    }

    /**
     * Configura la Toolbar de la actividad, incluyendo el título y el botón de navegación "Atrás".
     */
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarClassDetail)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = className ?: getString(R.string.class_detail_title_activity_default)
    }

    /**
     * Configura los RecyclerViews y sus adaptadores.
     */
    private fun setupRecyclerViews() {
        Log.d(TAG, "Configurando RecyclerViews.")
        studentAdapter = ClassStudentListAdapter(
            onRemoveStudentClick = { student ->
                Log.d(TAG, "Solicitando eliminar alumno: ${student.fullName}")
                showRemoveStudentConfirmationDialog(student)
            },
            onStudentClick = { student -> // ESTA ES LA LAMBDA CLAVE
                Log.d(TAG, "Callback onStudentClick recibido en Activity para: ${student.fullName}") // Log para confirmar
                navigateToStudentProfile(student) // Llama a tu función de navegación
            }
        )
        binding.rvStudentsInClass.apply {
            layoutManager = LinearLayoutManager(this@ClassDetailActivity)
            adapter = studentAdapter
        }

        // Esto es para el diálogo de añadir alumno, no para la lista principal.
        availableStudentSearchAdapter = AvailableStudentSearchAdapter { selectedStudent ->
            Log.d(TAG, "Alumno seleccionado del diálogo de búsqueda: ${selectedStudent.fullName}")
            addStudentDialog?.dismiss()
            showConfirmAddStudentDialog(selectedStudent)
        }
    }

    /**
     * Configura el SearchView para filtrar la lista de alumnos en la clase.
     */
    private fun setupSearchView() {
        Log.d(TAG, "Configurando SearchView.")
        binding.searchViewStudents.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                Log.d(TAG, "SearchView: Búsqueda enviada - '$query'")
                viewModel.filterStudents(query)
                binding.searchViewStudents.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                Log.d(TAG, "SearchView: Texto cambiado - '$newText'")
                viewModel.filterStudents(newText)
                return true
            }
        })
        binding.searchViewStudents.setOnCloseListener {
            Log.d(TAG, "SearchView: Cerrado. Limpiando filtro.")
            viewModel.filterStudents(null)
            false
        }
    }

    /**
     * Configura los listeners para los botones principales de la actividad.
     */
    private fun setupClickListeners() {
        Log.d(TAG, "Configurando ClickListeners.")
        binding.btnAddStudentToClass.setOnClickListener {
            Log.d(TAG, "Botón 'Añadir Alumno a Clase' pulsado.")
            showAddStudentDialog()
        }
    }

    /**
     * Configura los observadores para los LiveData del [ClassDetailViewModel].
     */
    private fun setupObservers() {
        Log.d(TAG, "Configurando Observadores.")

        viewModel.classroomDetails.observe(this, Observer { /* No action needed here based on current logic */ })

        viewModel.filteredStudentsInClass.observe(this, Observer { students ->
            Log.d(TAG, "LiveData filteredStudentsInClass actualizado. Número de alumnos a mostrar: ${students?.size ?: 0}")
            studentAdapter.submitList(students)
            if (students.isNullOrEmpty()) {
                if (binding.searchViewStudents.query.isNullOrBlank()) {
                    binding.tvNoStudentsMessage.text = getString(R.string.class_detail_info_no_students_in_class)
                } else {
                    binding.tvNoStudentsMessage.text = getString(R.string.class_detail_info_no_students_match_search)
                }
                binding.tvNoStudentsMessage.visibility = View.VISIBLE
                binding.rvStudentsInClass.visibility = View.GONE
            } else {
                binding.tvNoStudentsMessage.visibility = View.GONE
                binding.rvStudentsInClass.visibility = View.VISIBLE
            }
        })

        viewModel.uiState.observe(this, Observer { result ->
            binding.progressBarClassDetail.visibility = if (result is ClassDetailResult.Loading) View.VISIBLE else View.GONE
            if (result is ClassDetailResult.Error) {
                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                Log.w(TAG, "Error de uiState: ${result.message}")
            }
        })

        viewModel.removeStudentResult.observe(this, Observer { result ->
            binding.progressBarClassDetail.visibility = if (result is ClassDetailResult.Loading) View.VISIBLE else View.GONE
            when (result) {
                is ClassDetailResult.Success -> {
                    Toast.makeText(this, getString(R.string.class_detail_toast_student_removed_successfully), Toast.LENGTH_SHORT).show()
                    Log.i(TAG, "Alumno eliminado exitosamente.")
                    viewModel.resetRemoveStudentResult()
                }
                is ClassDetailResult.Error -> {
                    Toast.makeText(this, getString(R.string.class_detail_toast_error_removing_student, result.message), Toast.LENGTH_LONG).show()
                    Log.e(TAG, "Error al eliminar alumno: ${result.message}")
                    viewModel.resetRemoveStudentResult()
                }
                else -> { /* Idle o Loading ya manejado */ }
            }
        })

        viewModel.searchStudentsState.observe(this, Observer { result ->
            val progressBarDialog = addStudentDialog?.findViewById<ProgressBar>(R.id.progressBarAddStudentDialog)
            val tvNoResultsDialog = addStudentDialog?.findViewById<TextView>(R.id.tvNoAvailableStudentsFound)
            val rvAvailableStudentsDialog = addStudentDialog?.findViewById<RecyclerView>(R.id.rvAvailableStudents)

            progressBarDialog?.visibility = if (result is ClassDetailResult.Loading) View.VISIBLE else View.GONE

            when (result) {
                is ClassDetailResult.Success -> {
                    val students = result.data
                    availableStudentSearchAdapter.submitList(students)
                    val noResultsVisible = students.isNullOrEmpty()
                    tvNoResultsDialog?.text = if (noResultsVisible) getString(R.string.class_detail_info_no_available_students_found_search) else ""
                    tvNoResultsDialog?.visibility = if (noResultsVisible) View.VISIBLE else View.GONE
                    rvAvailableStudentsDialog?.visibility = if (noResultsVisible) View.GONE else View.VISIBLE
                    Log.d(TAG, "Resultados de búsqueda de alumnos disponibles actualizados: ${students?.size ?: 0} encontrados.")
                }
                is ClassDetailResult.Error -> {
                    availableStudentSearchAdapter.submitList(emptyList())
                    tvNoResultsDialog?.text = result.message // El mensaje de error del ViewModel
                    tvNoResultsDialog?.visibility = View.VISIBLE
                    rvAvailableStudentsDialog?.visibility = View.GONE
                    Toast.makeText(this, getString(R.string.class_detail_toast_error_searching_students, result.message), Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Error buscando alumnos disponibles: ${result.message}")
                }
                is ClassDetailResult.Idle -> {
                    availableStudentSearchAdapter.submitList(emptyList())
                    tvNoResultsDialog?.text = getString(R.string.class_detail_info_type_to_search_students)
                    tvNoResultsDialog?.visibility = View.VISIBLE
                    rvAvailableStudentsDialog?.visibility = View.GONE
                    Log.d(TAG, "Estado de búsqueda de alumnos: Idle.")
                }
                else -> { /* Loading ya manejado por ProgressBar */ }
            }
        })

        viewModel.addStudentToClassResult.observe(this, Observer { result ->
            binding.progressBarClassDetail.visibility = if (result is ClassDetailResult.Loading) View.VISIBLE else View.GONE
            when (result) {
                is ClassDetailResult.Success -> {
                    Toast.makeText(this, getString(R.string.class_detail_toast_student_added_successfully), Toast.LENGTH_SHORT).show()
                    Log.i(TAG, "Alumno añadido a la clase exitosamente.")
                    addStudentDialog?.dismiss()
                    viewModel.resetAddStudentToClassResult()
                }
                is ClassDetailResult.Error -> {
                    Toast.makeText(this, getString(R.string.class_detail_toast_error_adding_student, result.message), Toast.LENGTH_LONG).show()
                    Log.e(TAG, "Error al añadir alumno: ${result.message}")
                    viewModel.resetAddStudentToClassResult()
                }
                else -> { /* Idle o Loading ya manejado */ }
            }
        })
    }

    /**
     * Muestra el diálogo para buscar y añadir un nuevo alumno a la clase.
     */
    private fun showAddStudentDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_student, null)
        val searchViewDialog = dialogView.findViewById<SearchView>(R.id.searchViewAvailableStudents)
        val rvAvailableStudentsDialog = dialogView.findViewById<RecyclerView>(R.id.rvAvailableStudents)

        availableStudentSearchAdapter = AvailableStudentSearchAdapter { selectedStudent ->
            Log.d(TAG, "Alumno seleccionado del diálogo de búsqueda: ${selectedStudent.fullName}")
            addStudentDialog?.dismiss()
            showConfirmAddStudentDialog(selectedStudent)
        }

        rvAvailableStudentsDialog.apply {
            layoutManager = LinearLayoutManager(this@ClassDetailActivity)
            adapter = availableStudentSearchAdapter
        }

        searchViewDialog.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.searchAvailableStudents(query ?: "")
                searchViewDialog.clearFocus()
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.searchAvailableStudents(newText ?: "")
                if (newText.isNullOrEmpty()) {
                    viewModel.resetSearchStudentsState()
                }
                return true
            }
        })
        searchViewDialog.setOnCloseListener {
            viewModel.resetSearchStudentsState()
            false
        }

        addStudentDialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.class_detail_dialog_title_add_student))
            .setView(dialogView)
            .setNegativeButton(getString(R.string.dialog_button_cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        addStudentDialog?.setOnDismissListener {
            Log.d(TAG, "Diálogo de añadir alumno cerrado. Reseteando estado de búsqueda.")
            viewModel.resetSearchStudentsState()
        }

        addStudentDialog?.show()
        viewModel.resetSearchStudentsState() // Resetear al mostrar
        searchViewDialog.setQuery("", false)
        searchViewDialog.isIconified = false
        searchViewDialog.requestFocus()
    }

    /**
     * Muestra un diálogo de confirmación antes de añadir un alumno seleccionado a la clase.
     * @param student El [User] (alumno) a añadir.
     */
    private fun showConfirmAddStudentDialog(student: User) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.class_detail_dialog_title_confirm_add_student))
            .setMessage(getString(R.string.class_detail_dialog_message_confirm_add_student, student.fullName, student.username))
            .setPositiveButton(getString(R.string.dialog_button_yes_add)) { dialog, _ ->
                viewModel.addStudentToCurrentClass(student.userId)
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.dialog_button_cancel), null)
            .show()
    }

    /**
     * Muestra un diálogo de confirmación antes de eliminar un alumno de la clase.
     * @param student El [User] (alumno) a eliminar.
     */
    private fun showRemoveStudentConfirmationDialog(student: User) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.class_detail_dialog_title_remove_student))
            .setMessage(getString(R.string.class_detail_dialog_message_remove_student, student.fullName))
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(getString(R.string.class_detail_dialog_button_yes_remove)) { _, _ ->
                viewModel.removeStudentFromCurrentClass(student.userId)
            }
            .setNegativeButton(getString(R.string.dialog_button_cancel), null)
            .show()
    }

    /**
     * Navega a la pantalla de perfil del alumno seleccionado.
     * @param student El [User] (alumno) cuyo perfil se va a mostrar.
     */
    private fun navigateToStudentProfile(student: User) {
        Log.d(TAG, "navigateToStudentProfile: Intentando navegar para alumno ID: ${student.userId}, Nombre: ${student.fullName}")
        if (student.userId.isBlank()) {
            Log.e(TAG, "navigateToStudentProfile: Error - userId del alumno está vacío.")
            Toast.makeText(this, getString(R.string.profile_error_user_id_not_found), Toast.LENGTH_SHORT).show() // Usar string resource
            return
        }
        val intent = Intent(this, UserProfileActivity::class.java).apply {
            putExtra(UserProfileActivity.EXTRA_USER_ID, student.userId)
        }
        try {
            startActivity(intent)
            Log.d(TAG, "navigateToStudentProfile: startActivity(intent) llamado.")
        } catch (e: Exception) {
            Log.e(TAG, "navigateToStudentProfile: Excepción al llamar a startActivity.", e)
            Toast.makeText(this, "Error al abrir el perfil.", Toast.LENGTH_SHORT).show() // Considera usar string resource
        }
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
        Log.d(TAG, "onDestroy: ClassDetailActivity destruida.")
        addStudentDialog?.dismiss()
        addStudentDialog = null
    }
}