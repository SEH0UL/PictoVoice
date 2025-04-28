package com.example.pictovoice

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.pictovoice.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configura el view binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Botón de Login
        binding.btnLogin.setOnClickListener {
            val email = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (validateInputs(email, password)) {
                // Aquí irá la lógica de Firebase Auth después
                Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()

                // Navegar a la pantalla principal (crearemos esta actividad después)
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            }
        }

        // Botón de Registro
        binding.btnRegister.setOnClickListener {
            // Navegar a la actividad de registro (la crearemos después)
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun validateInputs(username: String, password: String): Boolean {
        // Validación de username
        if (username.isEmpty()) {
            binding.tilUsername.error = getString(R.string.error_username)
            return false
        } else if (username.length < 4) {
            binding.tilUsername.error = getString(R.string.error_username_length)
            return false
        } else {
            binding.tilUsername.error = null
        }

        // Validación de password (se mantiene igual)
        if (password.isEmpty()) {
            binding.tilPassword.error = "Ingrese su contraseña"
            return false
        } else if (password.length < 6) {
            binding.tilPassword.error = "Mínimo 6 caracteres"
            return false
        } else {
            binding.tilPassword.error = null
        }

        return true
    }

    private fun loginUser(username: String, password: String) {
        // Aquí implementarás la lógica con Firebase
        Toast.makeText(this, "Usuario: $username", Toast.LENGTH_SHORT).show()
    }
}