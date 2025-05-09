package com.example.pictovoice.ui.home // Asegúrate que el package es correcto

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels // Para by viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider // Para ViewModelProvider si no usas by viewModels directamente
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pictovoice.Data.Model.Pictogram
import com.example.pictovoice.adapters.CategoryAdapter
import com.example.pictovoice.adapters.PhrasePictogramAdapter
import com.example.pictovoice.adapters.SelectionPictogramAdapter
import com.example.pictovoice.databinding.ActivityHomeBinding
import com.example.pictovoice.ui.auth.MainActivity // Para logout o si el usuario no está autenticado
import com.example.pictovoice.viewmodels.StudentHomeViewModel // Tu nuevo ViewModel
import com.google.firebase.auth.FirebaseAuth

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val viewModel: StudentHomeViewModel by viewModels() // Inyecta el ViewModel

    private lateinit var phraseAdapter: PhrasePictogramAdapter
    private lateinit var pronounsAdapter: SelectionPictogramAdapter
    private lateinit var fixedVerbsAdapter: SelectionPictogramAdapter
    private lateinit var dynamicPictogramsAdapter: SelectionPictogramAdapter
    private lateinit var categoryAdapter: CategoryAdapter

    private val firebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Verificar autenticación
        if (firebaseAuth.currentUser == null) {
            navigateToLogin()
            return
        }

        setupRecyclerViews()
        setupClickListeners()
        observeViewModel()

        // Cargar datos iniciales
        firebaseAuth.currentUser?.uid?.let { userId ->
            viewModel.loadInitialData(userId)
        }
    }

    private fun setupRecyclerViews() {
        // Adaptador para la frase
        phraseAdapter = PhrasePictogramAdapter()
        binding.rvPhrasePictograms.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvPhrasePictograms.adapter = phraseAdapter

        // Adaptador para pronombres
        pronounsAdapter = SelectionPictogramAdapter { pictogram ->
            viewModel.addPictogramToPhrase(pictogram)
        }
        binding.rvPronouns.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvPronouns.adapter = pronounsAdapter

        // Adaptador para verbos fijos
        fixedVerbsAdapter = SelectionPictogramAdapter { pictogram ->
            viewModel.addPictogramToPhrase(pictogram)
        }
        binding.rvFixedVerbs.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvFixedVerbs.adapter = fixedVerbsAdapter

        // Adaptador para pictogramas dinámicos
        dynamicPictogramsAdapter = SelectionPictogramAdapter { pictogram ->
            viewModel.addPictogramToPhrase(pictogram)
        }
        // El spanCount es el de tu XML, ajústalo si es necesario
        binding.rvDynamicPictograms.layoutManager = GridLayoutManager(this, 6) // Span count del XML
        binding.rvDynamicPictograms.adapter = dynamicPictogramsAdapter

        // Adaptador para categorías
        categoryAdapter = CategoryAdapter { category ->
            viewModel.loadDynamicPictogramsByCategory(category)
        }
        binding.categoryNavigationContainer.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.categoryNavigationContainer.adapter = categoryAdapter
    }

    private fun setupClickListeners() {
        binding.btnPlayPhrase.setOnClickListener {
            viewModel.onPlayPhraseClicked()
        }

        binding.btnDeletePictogram.setOnClickListener {
            viewModel.deleteLastPictogramFromPhrase()
        }

        binding.btnDeletePictogram.setOnLongClickListener {
            // Confirmación para borrar toda la frase
            AlertDialog.Builder(this)
                .setTitle("Borrar Frase")
                .setMessage("¿Estás seguro de que quieres borrar toda la frase?")
                .setPositiveButton("Sí, borrar") { dialog, _ ->
                    viewModel.clearPhrase()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancelar", null)
                .show()
            true // Indica que el long click ha sido consumido
        }
    }

    private fun observeViewModel() {
        viewModel.phrasePictograms.observe(this) { pictograms ->
            phraseAdapter.submitList(pictograms)
            // Scroll al final de la frase si es necesario
            if (pictograms.isNotEmpty()) {
                binding.rvPhrasePictograms.smoothScrollToPosition(pictograms.size - 1)
            }
        }

        viewModel.playButtonVisibility.observe(this) { isVisible ->
            binding.btnPlayPhrase.visibility = if (isVisible) View.VISIBLE else View.GONE
        }

        viewModel.pronounPictograms.observe(this) { pictograms ->
            pronounsAdapter.submitList(pictograms)
        }

        viewModel.fixedVerbPictograms.observe(this) { pictograms ->
            fixedVerbsAdapter.submitList(pictograms)
        }

        viewModel.dynamicPictograms.observe(this) { pictograms ->
            dynamicPictogramsAdapter.submitList(pictograms)
        }

        viewModel.currentDynamicCategoryName.observe(this) { categoryName ->
            binding.tvCurrentCategoryName.text = categoryName
        }

        viewModel.availableCategories.observe(this) { categories ->
            categoryAdapter.submitList(categories)
        }

        viewModel.isLoading.observe(this) { isLoading ->
            // Aquí manejarías un ProgressBar general para la pantalla si lo tuvieras
            // binding.homeProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            if(isLoading) Toast.makeText(this, "Cargando...", Toast.LENGTH_SHORT).show() // Placeholder
        }

        viewModel.errorMessage.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                // Podrías tener un TextView para errores más persistentes
            }
        }

        viewModel.currentUser.observe(this) { user ->
            // Actualizar UI con datos del usuario si es necesario (ej. nombre, nivel)
            // supportActionBar?.title = "Hola, ${user?.fullName ?: "Usuario"}"
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}