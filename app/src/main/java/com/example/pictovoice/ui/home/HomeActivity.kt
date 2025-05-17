package com.example.pictovoice.ui.home // Asegúrate que el package es correcto

import android.content.Intent // Asegúrate de tener este import
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pictovoice.Data.Model.Category // Importa tu modelo de Category
import com.example.pictovoice.adapters.CategoryAdapter
import com.example.pictovoice.adapters.FixedPictogramAdapter // Importaste FixedPictogramAdapter
import com.example.pictovoice.adapters.PhrasePictogramAdapter
import com.example.pictovoice.adapters.SelectionPictogramAdapter
import com.example.pictovoice.databinding.ActivityHomeBinding
import com.example.pictovoice.ui.auth.MainActivity
import com.example.pictovoice.ui.userprofile.UserProfileActivity // IMPORTA UserProfileActivity
import com.example.pictovoice.viewmodels.StudentHomeViewModel
import com.google.firebase.auth.FirebaseAuth

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val viewModel: StudentHomeViewModel by viewModels()

    private lateinit var phraseAdapter: PhrasePictogramAdapter
    private lateinit var pronounsAdapter: FixedPictogramAdapter
    private lateinit var fixedVerbsAdapter: FixedPictogramAdapter
    private lateinit var dynamicPictogramsAdapter: SelectionPictogramAdapter
    private lateinit var categoryAdapter: CategoryAdapter
    private val firebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d("HomeActivity", "onCreate called")

        if (firebaseAuth.currentUser == null) {
            Log.w("HomeActivity", "User not authenticated, navigating to login.")
            navigateToLogin()
            return
        }

        setupRecyclerViews()
        setupClickListeners() // El listener del nuevo botón se añadirá aquí
        observeViewModel()

        firebaseAuth.currentUser?.uid?.let { userId ->
            Log.d("HomeActivity", "Loading user data for UID: $userId")
            viewModel.loadCurrentUserData(userId)
        }
    }

    private fun setupRecyclerViews() {
        Log.d("HomeActivity", "Setting up RecyclerViews")
        // Adaptador para la frase
        phraseAdapter = PhrasePictogramAdapter()
        binding.rvPhrasePictograms.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvPhrasePictograms.adapter = phraseAdapter

        // Adaptador para pronombres
        pronounsAdapter = FixedPictogramAdapter { pictogram ->
            Log.d("HomeActivity", "Pronoun clicked: ${pictogram.name}")
            viewModel.addPictogramToPhrase(pictogram)
        }
        binding.rvPronouns.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvPronouns.adapter = pronounsAdapter

        // Adaptador para verbos fijos
        fixedVerbsAdapter = FixedPictogramAdapter { pictogram ->
            Log.d("HomeActivity", "Fixed verb clicked: ${pictogram.name}")
            viewModel.addPictogramToPhrase(pictogram)
        }
        binding.rvFixedVerbs.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvFixedVerbs.adapter = fixedVerbsAdapter

        // Adaptador para pictogramas dinámicos
        dynamicPictogramsAdapter = SelectionPictogramAdapter { pictogram ->
            Log.d("HomeActivity", "Dynamic pictogram clicked: ${pictogram.name}")
            viewModel.addPictogramToPhrase(pictogram)
        }
        // Asegúrate que el LayoutManager para rvDynamicPictograms se define correctamente
        // (preferiblemente en XML con spanCount="4", o aquí si es necesario)
        // Ejemplo si se configura aquí:
        // binding.rvDynamicPictograms.layoutManager = GridLayoutManager(this, 4)
        binding.rvDynamicPictograms.adapter = dynamicPictogramsAdapter


        // Adaptador para categorías
        categoryAdapter = CategoryAdapter { category ->
            Log.d("HomeActivity", "Category clicked: ${category.name}")
            viewModel.loadDynamicPictogramsByLocalCategory(category)
        }
        binding.categoryNavigationContainer.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.categoryNavigationContainer.adapter = categoryAdapter
    }

    private fun setupClickListeners() {
        Log.d("HomeActivity", "Setting up ClickListeners")
        binding.btnPlayPhrase.setOnClickListener {
            Log.d("HomeActivity", "Play Phrase button clicked")
            viewModel.onPlayPhraseClicked()
        }

        binding.btnDeletePictogram.setOnClickListener {
            Log.d("HomeActivity", "Delete Pictogram button clicked (short)")
            viewModel.deleteLastPictogramFromPhrase()
        }

        binding.btnDeletePictogram.setOnLongClickListener {
            Log.d("HomeActivity", "Delete Pictogram button clicked (LONG)")
            AlertDialog.Builder(this)
                .setTitle("Borrar Frase Completa")
                .setMessage("¿Estás seguro de que quieres borrar toda la frase?")
                .setPositiveButton("Sí, borrar") { dialog, _ ->
                    viewModel.clearPhrase()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancelar", null)
                .show()
            true // Indica que el long click ha sido consumido
        }

        // ***** INICIO: CÓDIGO AÑADIDO PARA NAVEGACIÓN AL PERFIL *****
        binding.btnUserProfile.setOnClickListener {
            val userId = firebaseAuth.currentUser?.uid
            if (userId != null) {
                val intent = Intent(this, UserProfileActivity::class.java).apply {
                    putExtra("USER_ID_EXTRA", userId)
                    // Opcional: Pasar el rol del visualizador.
                    // Como es el alumno viendo su propio perfil, UserProfileActivity
                    // ya tiene lógica para manejar esto si compara el targetUserId con el viewerUserId.
                    // putExtra("VIEWER_ROLE_EXTRA", "student")
                }
                startActivity(intent)
                Log.d("HomeActivity", "Navigating to UserProfileActivity for UID: $userId")
            } else {
                Toast.makeText(this, "Error: No se pudo obtener el ID del usuario.", Toast.LENGTH_SHORT).show()
                Log.e("HomeActivity", "btnUserProfile clicked but currentUser or UID is null.")
                // Considera desloguear o manejar este caso de error si ocurre frecuentemente.
            }
        }
        // ***** FIN: CÓDIGO AÑADIDO PARA NAVEGACIÓN AL PERFIL *****
    }

    private fun observeViewModel() {
        Log.d("HomeActivity", "Observing ViewModel LiveData")
        viewModel.phrasePictograms.observe(this) { pictograms ->
            Log.d("HomeActivity", "Phrase pictograms updated: ${pictograms?.size ?: 0} items")
            phraseAdapter.submitList(pictograms?.toList())
            if (pictograms?.isNotEmpty() == true) {
                binding.rvPhrasePictograms.smoothScrollToPosition(pictograms.size - 1)
            }
        }

        viewModel.playButtonVisibility.observe(this) { isVisible ->
            Log.d("HomeActivity", "Play button visibility changed: $isVisible")
            binding.btnPlayPhrase.visibility = if (isVisible) View.VISIBLE else View.GONE
        }

        viewModel.pronounPictograms.observe(this) { pictograms ->
            Log.d("HomeActivity", "Pronoun pictograms updated: ${pictograms?.size ?: 0} items")
            pronounsAdapter.submitList(pictograms?.toList())
        }

        viewModel.fixedVerbPictograms.observe(this) { pictograms ->
            Log.d("HomeActivity", "Fixed verb pictograms updated: ${pictograms?.size ?: 0} items")
            fixedVerbsAdapter.submitList(pictograms?.toList())
        }

        viewModel.dynamicPictograms.observe(this) { pictograms ->
            Log.d("HomeActivity", "Dynamic pictograms updated: ${pictograms?.size ?: 0} items. Category: ${viewModel.currentDynamicCategoryName.value}")
            dynamicPictogramsAdapter.submitList(pictograms?.toList())
        }

        viewModel.currentDynamicCategoryName.observe(this) { categoryName ->
            Log.d("HomeActivity", "Current dynamic category name updated: $categoryName")
            binding.tvCurrentCategoryName.text = categoryName
        }

        viewModel.availableCategories.observe(this) { categories ->
            Log.d("HomeActivity", "Available categories updated: ${categories?.size ?: 0} items")
            categoryAdapter.submitList(categories?.toList())
        }

        viewModel.isLoading.observe(this) { isLoading ->
            Log.d("HomeActivity", "isLoading state changed: $isLoading")
            if(isLoading) {
                // Toast.makeText(this, "Cargando...", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.errorMessage.observe(this) { errorMessage ->
            errorMessage?.let {
                Log.e("HomeActivity", "Error message received: $it")
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.currentUser.observe(this) { user ->
            user?.let {
                Log.d("HomeActivity", "Current user data updated: ${it.fullName}, Level: ${it.currentLevel}")
            }
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("HomeActivity", "onDestroy called")
    }
}