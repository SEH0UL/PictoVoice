package com.example.pictovoice.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View // Necesario para View.VISIBLE / View.GONE
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.pictovoice.Data.repository.AuthRepository // Asegúrate que la ruta es correcta
import com.example.pictovoice.databinding.ActivityMainBinding
import com.example.pictovoice.ui.home.HomeActivity // Asegúrate que la ruta es correcta
import com.example.pictovoice.utils.AuthResult // Asegúrate que la ruta y la definición de AuthResult son correctas
import com.example.pictovoice.utils.AuthViewModel // Asegúrate que la ruta es correcta
import com.example.pictovoice.utils.AuthViewModelFactory // Asegúrate que la ruta es correcta
import kotlinx.coroutines.launch
import java.util.Locale // Para normalizar el input a mayúsculas si es necesario

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    // Considera la inyección de dependencias para el repositorio en una app más grande
    private val authRepository = AuthRepository()
    private val viewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(authRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ------------------------------------------------------------------------------------- //
        // INICIO DE LA SECCIÓN A COMENTAR/ELIMINAR PARA EVITAR EL LOGIN AUTOMÁTICO               //
        // ------------------------------------------------------------------------------------- //
        // Verificar si el usuario ya está autenticado (por ejemplo, si cerró la app sin desloguearse)
        // Esto asume que tienes el método isUserLoggedIn() en tu AuthRepository
        /* <--- Comienza a comentar aquí
        if (authRepository.isUserLoggedIn()) {
             navigateToHome() // Si está logueado, va directo a HomeActivity
             return // Evita ejecutar el resto del onCreate (setupObservers, setupListeners) si ya está logueado
        }
        */  // <--- Termina de comentar aquí
        // ------------------------------------------------------------------------------------- //
        // FIN DE LA SECCIÓN A COMENTAR/ELIMINAR                                                 //
        // ------------------------------------------------------------------------------------- //

        // Estas líneas se ejecutarán siempre si comentas el bloque de arriba,
        // mostrando siempre la pantalla de login.
        setupObservers()
        setupListeners()
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            // El campo etUsername (o como lo llames en tu XML) ahora puede contener username o email
            val identifier = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString() // Generalmente no se trimea la contraseña

            if (validateInputs(identifier, password)) {
                // Si tus usernames generados están siempre en mayúsculas,
                // y quieres que el login por username sea case-insensitive,
                // podrías normalizar 'identifier' aquí ANTES de pasarlo al ViewModel,
                // SOLO si no es un email.
                // Ejemplo simple (mejorar esta lógica si se implementa):
                // val finalIdentifier = if (!Patterns.EMAIL_ADDRESS.matcher(identifier).matches()) {
                //    identifier.toUpperCase(Locale.ROOT)
                // } else {
                //    identifier
                // }
                // viewModel.login(finalIdentifier, password)
                // Por ahora, pasamos el identifier tal cual, el AuthRepository lo maneja.
                viewModel.login(identifier, password)
            }
        }

        binding.btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            // No es necesario finish() aquí si quieres que el usuario pueda volver
            // a la pantalla de login desde el registro (antes de registrarse efectivamente)
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.loginResult.collect { result ->
                // Si tienes un ProgressBar en activity_main.xml, puedes manejar su visibilidad aquí
                // Ejemplo: binding.progressBarLogin.visibility = if (result.isLoading) View.VISIBLE else View.GONE

                when (result) {
                    is AuthResult.Success -> {
                        navigateToHome()
                    }
                    is AuthResult.Error -> {
                        // binding.progressBarLogin.visibility = View.GONE // Asegúrate de ocultarlo en caso de error también
                        Toast.makeText(this@MainActivity, result.error, Toast.LENGTH_LONG).show()
                    }
                    AuthResult.Idle -> {
                        // binding.progressBarLogin.visibility = View.GONE
                        // No action needed or hide progress bar if shown by default
                    }
                    AuthResult.Loading -> {
                        // binding.progressBarLogin.visibility = View.VISIBLE
                        // Ya cubierto por la lógica de arriba si la descomentas
                    }
                }
            }
        }
    }

    private fun navigateToHome() {
        val intent = Intent(this@MainActivity, HomeActivity::class.java)
        // Limpiar el stack para que el usuario no pueda volver a la pantalla de login con el botón "atrás"
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish() // Finaliza MainActivity
    }

    private fun validateInputs(identifier: String, password: String): Boolean {
        var isValid = true

        // Validación para el campo de "Usuario o Correo Electrónico"
        if (identifier.isEmpty()) {
            binding.tilUsername.error = "Ingresa tu usuario o correo electrónico" // Asegúrate que el ID 'tilUsername' es correcto
            isValid = false
        } else {
            binding.tilUsername.error = null // Limpiar error
        }

        // Validación para el campo de contraseña
        if (password.isEmpty()) {
            binding.tilPassword.error = "Ingresa tu contraseña" // Asegúrate que el ID 'tilPassword' es correcto
            isValid = false
        }
        /* else if (password.length < 6) { // Opcional: validación de longitud mínima si aplica también en login
            binding.tilPassword.error = "La contraseña debe tener al menos 6 caracteres"
            isValid = false
        } */
        else {
            binding.tilPassword.error = null // Limpiar error
        }

        return isValid
    }
}