package com.example.pictovoice.ui.classroom // Asegúrate que el paquete sea correcto, el tuyo era ui.classroom

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pictovoice.Data.Model.User
import com.example.pictovoice.R
import com.example.pictovoice.adapters.AvailableStudentSearchAdapter
import com.example.pictovoice.adapters.ClassStudentListAdapter
import com.example.pictovoice.databinding.ActivityClassDetailBinding
import com.example.pictovoice.utils.ClassDetailViewModelFactory
// ClassDetailResult debería estar en el mismo paquete que ClassDetailViewModel o importado correctamente


class ClassDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityClassDetailBinding
    private val viewModel: ClassDetailViewModel by viewModels {
        ClassDetailViewModelFactory(application, classId!!) // classId se valida en onCreate
    }
    private lateinit var studentAdapter: ClassStudentListAdapter // Para la lista principal de alumnos en clase
    private lateinit var availableStudentSearchAdapter: AvailableStudentSearchAdapter // Para el diálogo de búsqueda

    private var classId: String? = null
    private var className: String? = null

    private var addStudentDialog: AlertDialog? = null // Referencia al diálogo para poder cerrarlo

    companion object {
        const val EXTRA_CLASS_ID = "extra_class_id"
        const val EXTRA_CLASS_NAME = "extra_class_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClassDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        classId = intent.getStringExtra(EXTRA_CLASS_ID)
        className = intent.getStringExtra(EXTRA_CLASS_NAME)

        if (classId.isNullOrBlank()) {
            Log.e("ClassDetailActivity", "Error: No se recibió el ID de la clase o está vacío.")
            Toast.makeText(this, "Error: No se pudo cargar la clase.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupToolbar()
        setupRecyclerView()
        setupSearchView()       // Para filtrar la lista principal de alumnos
        setupClickListeners()   // Para el botón "+ Añadir Alumno"
        setupObservers()

        binding.tvClassDetailNameHeader.text = className // Mostrar nombre de la clase
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarClassDetail)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = className ?: "Detalles de Clase"
    }

    private fun setupRecyclerView() {
        studentAdapter = ClassStudentListAdapter(
            onRemoveStudentClick = { student ->
                showRemoveStudentConfirmationDialog(student)
            },
            onStudentClick = { student ->
                // TODO: Navegar al perfil detallado del alumno seleccionado.
                Toast.makeText(this, "Ver perfil de: ${student.fullName}", Toast.LENGTH_SHORT).show()
            }
        )
        binding.rvStudentsInClass.apply {
            layoutManager = LinearLayoutManager(this@ClassDetailActivity)
            adapter = studentAdapter
        }
    }

    private fun setupSearchView() {
        binding.searchViewStudents.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.filterStudents(query) // Filtra la lista principal de alumnos
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.filterStudents(newText) // Filtra la lista principal de alumnos
                return true
            }
        })
        binding.searchViewStudents.setOnCloseListener {
            viewModel.filterStudents(null)
            false
        }
    }

    private fun setupClickListeners() {
        binding.btnAddStudentToClass.setOnClickListener {
            showAddStudentDialog() // Mostrar el diálogo para añadir alumnos
        }
    }

    @SuppressLint("SetTextI18n") // Para tvNoStudentsMessage.text y tvNoResultsDialog?.text
    private fun setupObservers() {
        viewModel.classroomDetails.observe(this) { classroom ->
            classroom?.let {
                Log.d("ClassDetailActivity", "Detalles de la clase actualizados: ${it.className}")
                // Podrías actualizar otros elementos de la UI si fuera necesario
            }
        }

        viewModel.filteredStudentsInClass.observe(this) { students ->
            studentAdapter.submitList(students)
            if (students.isNullOrEmpty()) {
                if (binding.searchViewStudents.query.isNullOrBlank()) {
                    binding.tvNoStudentsMessage.text = "No hay alumnos en esta clase."
                } else {
                    binding.tvNoStudentsMessage.text = "Ningún alumno coincide con la búsqueda."
                }
                binding.tvNoStudentsMessage.visibility = View.VISIBLE
                binding.rvStudentsInClass.visibility = View.GONE
            } else {
                binding.tvNoStudentsMessage.visibility = View.GONE
                binding.rvStudentsInClass.visibility = View.VISIBLE
            }
        }

        viewModel.uiState.observe(this) { result ->
            when (result) {
                is ClassDetailResult.Loading -> binding.progressBarClassDetail.visibility = View.VISIBLE
                is ClassDetailResult.Success -> binding.progressBarClassDetail.visibility = View.GONE
                is ClassDetailResult.Error -> {
                    binding.progressBarClassDetail.visibility = View.GONE
                    Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                }
                is ClassDetailResult.Idle -> binding.progressBarClassDetail.visibility = View.GONE
            }
        }

        viewModel.removeStudentResult.observe(this) { result ->
            when (result) {
                is ClassDetailResult.Loading -> binding.progressBarClassDetail.visibility = View.VISIBLE
                is ClassDetailResult.Success -> {
                    binding.progressBarClassDetail.visibility = View.GONE
                    Toast.makeText(this, "Alumno eliminado de la clase.", Toast.LENGTH_SHORT).show()
                    viewModel.resetRemoveStudentResult()
                }
                is ClassDetailResult.Error -> {
                    binding.progressBarClassDetail.visibility = View.GONE
                    Toast.makeText(this, "Error al eliminar alumno: ${result.message}", Toast.LENGTH_LONG).show()
                    viewModel.resetRemoveStudentResult()
                }
                is ClassDetailResult.Idle -> { /* No action */ }
            }
        }

        // Observador para el estado de búsqueda de alumnos disponibles (para el diálogo)
        viewModel.searchStudentsState.observe(this) { result ->
            val progressBarDialog = addStudentDialog?.findViewById<ProgressBar>(R.id.progressBarAddStudentDialog)
            val tvNoResultsDialog = addStudentDialog?.findViewById<TextView>(R.id.tvNoAvailableStudentsFound)
            val rvAvailableStudentsDialog = addStudentDialog?.findViewById<RecyclerView>(R.id.rvAvailableStudents)


            progressBarDialog?.visibility = if (result is ClassDetailResult.Loading) View.VISIBLE else View.GONE

            when (result) {
                is ClassDetailResult.Success -> {
                    val students = result.data
                    availableStudentSearchAdapter.submitList(students) // Actualiza el adapter del diálogo
                    if (students.isEmpty()) {
                        tvNoResultsDialog?.text = "No se encontraron alumnos disponibles con ese nombre."
                        tvNoResultsDialog?.visibility = View.VISIBLE
                        rvAvailableStudentsDialog?.visibility = View.GONE
                    } else {
                        tvNoResultsDialog?.visibility = View.GONE
                        rvAvailableStudentsDialog?.visibility = View.VISIBLE
                    }
                }
                is ClassDetailResult.Error -> {
                    availableStudentSearchAdapter.submitList(emptyList())
                    tvNoResultsDialog?.text = result.message
                    tvNoResultsDialog?.visibility = View.VISIBLE
                    rvAvailableStudentsDialog?.visibility = View.GONE
                    Toast.makeText(this, "Error buscando alumnos: ${result.message}", Toast.LENGTH_SHORT).show()
                }
                is ClassDetailResult.Idle -> {
                    availableStudentSearchAdapter.submitList(emptyList())
                    tvNoResultsDialog?.text = "Introduce un nombre para buscar alumnos."
                    tvNoResultsDialog?.visibility = View.VISIBLE
                    rvAvailableStudentsDialog?.visibility = View.GONE
                }
                // No es necesario manejar Loading aquí explícitamente si el ProgressBar ya lo hace
                else -> {}
            }
        }

        // Observador para el resultado de añadir un alumno a la clase
        viewModel.addStudentToClassResult.observe(this) { result ->
            when (result) {
                is ClassDetailResult.Loading -> {
                    binding.progressBarClassDetail.visibility = View.VISIBLE
                }
                is ClassDetailResult.Success -> {
                    binding.progressBarClassDetail.visibility = View.GONE
                    Toast.makeText(this, "Alumno añadido a la clase exitosamente.", Toast.LENGTH_SHORT).show()
                    addStudentDialog?.dismiss()
                    viewModel.resetAddStudentToClassResult()
                }
                is ClassDetailResult.Error -> {
                    binding.progressBarClassDetail.visibility = View.GONE
                    Toast.makeText(this, "Error al añadir alumno: ${result.message}", Toast.LENGTH_LONG).show()
                    viewModel.resetAddStudentToClassResult()
                }
                is ClassDetailResult.Idle -> { /* No action */ }
            }
        }
    }

    private fun showAddStudentDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_student, null)
        val searchViewDialog = dialogView.findViewById<SearchView>(R.id.searchViewAvailableStudents)
        val rvAvailableStudentsDialog = dialogView.findViewById<RecyclerView>(R.id.rvAvailableStudents)

        availableStudentSearchAdapter = AvailableStudentSearchAdapter { selectedStudent ->
            // Alumno seleccionado desde el diálogo de búsqueda.
            // Cerramos el diálogo de búsqueda ANTES de mostrar el de confirmación.
            addStudentDialog?.dismiss()
            showConfirmAddStudentDialog(selectedStudent) // Mostramos diálogo de confirmación
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
                    viewModel.clearAvailableStudentSearchResults()
                }
                return true
            }
        })

        searchViewDialog.setOnCloseListener {
            viewModel.clearAvailableStudentSearchResults()
            false
        }

        addStudentDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setNegativeButton("Cancelar") { dialog, _ ->
                viewModel.clearAvailableStudentSearchResults()
                dialog.dismiss()
            }
            .create()

        addStudentDialog?.setOnDismissListener {
            viewModel.clearAvailableStudentSearchResults()
        }

        addStudentDialog?.show()
        viewModel.clearAvailableStudentSearchResults()
        searchViewDialog.setQuery("", false)
        searchViewDialog.requestFocus()
    }

    // MÉTODO ANTERIOR (lo renombramos o modificamos)
    // private fun confirmAndAddStudent(student: User) {
    //     viewModel.addStudentToCurrentClass(student.userId)
    // }

    // NUEVO MÉTODO PARA MOSTRAR DIÁLOGO DE CONFIRMACIÓN ANTES DE AÑADIR
    private fun showConfirmAddStudentDialog(student: User) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar Añadir Alumno")
            .setMessage("¿Estás seguro de que quieres añadir a ${student.fullName} (@${student.username}) a esta clase?")
            .setPositiveButton("Sí, Añadir") { dialog, _ ->
                viewModel.addStudentToCurrentClass(student.userId)
                // No necesitamos cerrar el addStudentDialog aquí porque ya lo hicimos
                // al seleccionar el alumno en el adapter.
            }
            .setNegativeButton("Cancelar", null) // Simplemente cierra este diálogo de confirmación
            .show()
    }


    private fun showRemoveStudentConfirmationDialog(student: User) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Alumno")
            .setMessage("¿Estás seguro de que quieres eliminar a ${student.fullName} de esta clase?")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("Sí, Eliminar") { _, _ ->
                viewModel.removeStudentFromCurrentClass(student.userId)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed() // Correcto para manejar botón atrás de la Toolbar
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Para evitar memory leaks si el diálogo sigue mostrándose y la actividad se destruye
        addStudentDialog?.dismiss()
        addStudentDialog = null
    }
}