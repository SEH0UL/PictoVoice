package com.example.pictovoice.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.pictovoice.Data.Model.User // Importar User
import com.example.pictovoice.Data.repository.AuthRepository
import com.example.pictovoice.databinding.ActivityMainBinding
import com.example.pictovoice.ui.home.HomeActivity // Para rol estudiante
import com.example.pictovoice.ui.teacher.TeacherHomeActivity // Para rol profesor
import com.example.pictovoice.utils.AuthResult
import com.example.pictovoice.utils.AuthViewModel
import com.example.pictovoice.utils.AuthViewModelFactory
import kotlinx.coroutines.launch
// import java.util.Locale // Si lo usas para normalizar el input

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val authRepository = AuthRepository() // Considera DI para el repo
    private val viewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(authRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Comprobar si el usuario ya está logueado
        if (authRepository.isUserLoggedIn()) {
            // Si está logueado, necesitamos saber su rol para redirigir correctamente.
            // Esto requeriría cargar el User de Firestore aquí o que el ViewModel lo gestione al inicio.
            // Por simplicidad, si está logueado, por ahora solo lo mandamos a una pantalla genérica
            // o intentamos una carga rápida.
            // Lo ideal sería que el ViewModel tuviera un "checkCurrentUserSession"
            // que emita un User o nada.
            // --- INICIO BLOQUE DE AUTO-LOGIN MEJORADO (REQUIERE MÁS LÓGICA EN VM) ---
            /*
            viewModel.checkActiveSession() // Un nuevo método en AuthViewModel
            lifecycleScope.launch {
                viewModel.activeUser.collect { user -> // Un nuevo StateFlow en AuthViewModel
                    if (user != null) {
                        if (user.role == "teacher") {
                            navigateToTeacherHome()
                        } else {
                            navigateToStudentHome()
                        }
                    } else {
                        // No hay sesión activa o error al cargar, quedarse en login
                        setupListeners() // Asegurar que los listeners se configuran si no hay auto-login
                        setupObservers()
                    }
                }
            }
            */
            // --- FIN BLOQUE DE AUTO-LOGIN MEJORADO ---

            // --- INICIO BLOQUE DE AUTO-LOGIN SIMPLE (SOLO SI isUserLoggedIn) ---
            // Este bloque es más simple pero no conoce el rol sin una consulta adicional.
            // Si el usuario cierra y abre la app, lo ideal es llevarlo a su home respectiva.
            // Para ello, el ViewModel debería tener una forma de cargar el usuario actual al iniciar.
            // Por ahora, para evitar que el código se bloquee aquí, lo comentaremos
            // y dejaremos que el usuario haga login manualmente.
            // El flujo de "recordar sesión" se puede refinar después.
            Log.d("MainActivity", "Usuario previamente logueado. Deslogueando para testeo manual.")
            authRepository.logout() // FORZAR LOGOUT PARA TESTEAR EL FLUJO DE LOGIN COMPLETO
            // Si decides mantener el auto-login, necesitarás cargar el User aquí.
            // Ejemplo:
            // lifecycleScope.launch {
            //     val uid = FirebaseAuth.getInstance().currentUser?.uid
            //     if (uid != null) {
            //         val userResult = AuthRepository().getUser(uid) // Usar instancia o inyectar
            //         if (userResult.isSuccess) {
            //             val user = userResult.getOrNull()
            //             if (user?.role == "teacher") navigateToTeacherHome() else navigateToStudentHome()
            //             return@launch
            //         }
            //     }
            //      // Si falla la carga o no hay uid, configurar listeners y observers
            //     setupListeners()
            //     setupObservers()
            // }
            // return // Evita ejecutar el resto del onCreate si ya está logueado y redirigido
            // --- FIN BLOQUE DE AUTO-LOGIN SIMPLE ---
        }

        // Estas líneas se ejecutarán siempre si el bloque de auto-login no redirige.
        setupListeners()
        setupObservers()
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val identifier = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString()

            if (validateInputs(identifier, password)) {
                // El AuthRepository ahora maneja si es username o email
                viewModel.login(identifier, password)
            }
        }

        binding.btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            // Observar loginResult (AuthResult<User>)
            viewModel.loginResult.collect { result ->
                // progressBarLogin.visibility = if (result.isLoading) ...
                when (result) {
                    is AuthResult.Success -> {
                        // progressBarLogin.visibility = View.GONE
                        val user = result.data // 'data' es ahora el objeto User
                        Toast.makeText(this@MainActivity, "Login exitoso: ${user.fullName}", Toast.LENGTH_SHORT).show()
                        if (user.role == "teacher") {
                            navigateToTeacherHome()
                        } else {
                            navigateToStudentHome()
                        }
                    }
                    is AuthResult.Error -> {
                        // progressBarLogin.visibility = View.GONE
                        Toast.makeText(this@MainActivity, result.message, Toast.LENGTH_LONG).show()
                    }
                    is AuthResult.Loading -> {
                        // progressBarLogin.visibility = View.VISIBLE
                        // Podrías mostrar un ProgressBar aquí
                    }
                    is AuthResult.Idle -> {
                        // progressBarLogin.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun navigateToStudentHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun navigateToTeacherHome() {
        val intent = Intent(this, TeacherHomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun validateInputs(identifier: String, password: String): Boolean {
        var isValid = true
        if (identifier.isEmpty()) {
            binding.tilUsername.error = "Ingresa tu usuario o correo electrónico"
            isValid = false
        } else {
            binding.tilUsername.error = null
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = "Ingresa tu contraseña"
            isValid = false
        } else {
            binding.tilPassword.error = null
        }
        return isValid
    }
}