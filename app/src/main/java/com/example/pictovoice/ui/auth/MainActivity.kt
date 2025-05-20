package com.example.pictovoice.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View // Para View.VISIBLE/GONE
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Observer // Para observar LiveData
import androidx.lifecycle.lifecycleScope
import androidx.appcompat.app.AppCompatActivity
import com.example.pictovoice.Data.repository.AuthRepository // Necesario para la Factory
import com.example.pictovoice.R
import com.example.pictovoice.databinding.ActivityMainBinding
import com.example.pictovoice.ui.home.HomeActivity
import com.example.pictovoice.ui.teacher.TeacherHomeActivity
import com.example.pictovoice.utils.AuthViewModelFactory
import com.example.pictovoice.viewmodels.AuthResult
import com.example.pictovoice.viewmodels.AuthViewModel
// Eliminar import com.google.firebase.auth.FirebaseAuth (ya no se usa directamente aquí para el auto-login)
import kotlinx.coroutines.launch

private const val TAG = "MainActivity" // Tag para Logs

/**
 * Activity principal de la aplicación.
 * Funciona como la pantalla de inicio de sesión (login).
 * También maneja la lógica inicial para verificar si un usuario ya está autenticado
 * y redirigirlo a la pantalla correspondiente (Home del Alumno o Home del Profesor).
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    // AuthRepository ya no se instancia aquí directamente si el ViewModel lo maneja todo.
    // private val authRepository = AuthRepository()
    private val authViewModel: AuthViewModel by viewModels {
        // AuthRepository se instancia dentro de AuthViewModel ahora, o se inyecta en la factory.
        // Si AuthViewModel lo instancia, no necesitas pasarlo aquí.
        // Si tu AuthViewModelFactory requiere AuthRepository:
        AuthViewModelFactory(AuthRepository())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d(TAG, "onCreate: Iniciando MainActivity.")

        // ELIMINAR COMPLETAMENTE EL BLOQUE DE CÓDIGO TEMPORAL PARA BACKFILL
        // Si aún estaba aquí.

        // Iniciar el intento de auto-login a través del ViewModel
        authViewModel.attemptAutoLogin()

        setupListeners()
        setupObservers() // Los observadores manejarán la navegación o la muestra del login
    }

    /**
     * Configura los listeners para los elementos de la UI (botones de login y registro).
     */
    private fun setupListeners() {
        Log.d(TAG, "Configurando listeners...")
        binding.btnLogin.setOnClickListener {
            val identifier = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString()
            if (validateInputs(identifier, password)) {
                Log.d(TAG, "Botón Login pulsado. Identificador: $identifier")
                authViewModel.login(identifier, password)
            }
        }
        binding.btnRegister.setOnClickListener {
            Log.d(TAG, "Botón Registro pulsado.")
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    /**
     * Configura los observadores para los LiveData del [AuthViewModel].
     * Maneja las respuestas de las operaciones de login y auto-login.
     */
    private fun setupObservers() {
        Log.d(TAG, "Configurando observadores...")
        // Observador para el resultado del login manual
        lifecycleScope.launch { // O puedes usar authViewModel.loginResult.observe(this, Observer { ... }) si prefieres
            authViewModel.loginResult.collect { result ->
                binding.progressBarLogin.visibility = if (result.isLoading) View.VISIBLE else View.GONE

                when (result) {
                    is AuthResult.Success -> {
                        val user = result.data
                        Log.i(TAG, "Login Observer: Éxito para ${user.username}, Rol: ${user.role}")
                        Toast.makeText(this@MainActivity, "Login exitoso: ${user.fullName}", Toast.LENGTH_SHORT).show()
                        navigateToDashboard(user.role)
                        // finish() // navigateToDashboard debería finalizar esta activity
                    }
                    is AuthResult.Error -> {
                        Log.w(TAG, "Login Observer: Error - ${result.message}")
                        Toast.makeText(this@MainActivity, result.message, Toast.LENGTH_LONG).show()
                    }
                    is AuthResult.Loading -> Log.d(TAG, "Login Observer: Cargando...")
                    is AuthResult.Idle -> Log.d(TAG, "Login Observer: Idle.")
                }
            }
        }

        // Observador para el resultado del intento de auto-login
        authViewModel.autoLoginResult.observe(this, Observer { result ->
            // Podrías usar el mismo progressBarLogin o uno diferente si el feedback visual debe variar.
            // binding.progressBarLogin.visibility = if (result is AuthResult.Loading) View.VISIBLE else View.GONE

            when (result) {
                is AuthResult.Success -> {
                    val user = result.data
                    Log.i(TAG, "AutoLogin Observer: Éxito para ${user.username}, Rol: ${user.role}")
                    Toast.makeText(this@MainActivity, "Sesión restaurada para ${user.fullName}", Toast.LENGTH_SHORT).show()
                    navigateToDashboard(user.role)
                    // authViewModel.onAutoLoginEventHandled() // No es necesario si navegamos y finalizamos
                }
                is AuthResult.Error -> {
                    // Si el auto-login falla (ej. usuario en Auth pero no en Firestore, o error de red),
                    // nos quedamos en la pantalla de login. El error ya se logueó en el ViewModel.
                    // Se podría mostrar un Toast discreto si se desea, pero a menudo es mejor no molestar al usuario
                    // y simplemente mostrar la pantalla de login.
                    Log.w(TAG, "AutoLogin Observer: Error - ${result.message}. Mostrando pantalla de login.")
                    binding.progressBarLogin.visibility = View.GONE // Asegurar que el progress bar se oculta
                    authViewModel.onAutoLoginEventHandled() // Resetear para futuros intentos si la activity sobrevive
                }
                is AuthResult.Loading -> {
                    Log.d(TAG, "AutoLogin Observer: Cargando...")
                    binding.progressBarLogin.visibility = View.VISIBLE
                }
                is AuthResult.Idle -> {
                    Log.d(TAG, "AutoLogin Observer: Idle. Esperando acción del usuario (pantalla de login).")
                    binding.progressBarLogin.visibility = View.GONE
                }
            }
        })
    }

    /**
     * Navega a la pantalla principal correspondiente según el rol del usuario
     * (Home del Alumno o Home del Profesor) y finaliza esta MainActivity.
     * @param role El rol del usuario ("student" o "teacher").
     */
    private fun navigateToDashboard(role: String) {
        val intent = if (role == "teacher") {
            Intent(this, TeacherHomeActivity::class.java)
        } else {
            Intent(this, HomeActivity::class.java)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish() // Finalizar MainActivity para que no se pueda volver a ella con el botón "Atrás"
    }

    /**
     * Valida que los campos de identificador y contraseña no estén vacíos.
     * Muestra errores en los TextInputLayout correspondientes si están vacíos.
     * @param identifier El identificador ingresado (username o email).
     * @param password La contraseña ingresada.
     * @return `true` si ambos campos son válidos (no vacíos), `false` en caso contrario.
     */
    private fun validateInputs(identifier: String, password: String): Boolean {
        var isValid = true
        if (identifier.isEmpty()) {
            binding.tilUsername.error = getString(R.string.login_error_identifier_required) // CORREGIDO
            isValid = false
        } else {
            binding.tilUsername.error = null
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = getString(R.string.login_error_password_required) // CORREGIDO
            isValid = false
        } else {
            binding.tilPassword.error = null
        }
        return isValid
    }
}