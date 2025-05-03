package com.example.pictovoice.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.pictovoice.Data.repository.AuthRepository
import com.example.pictovoice.databinding.ActivityRegisterBinding
import com.example.pictovoice.utils.AuthViewModel
import com.example.pictovoice.utils.AuthViewModelFactory
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val authRepository = AuthRepository()
    private val viewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(authRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupObservers()
        setupListeners()
    }

    private fun setupListeners() {
        binding.btnRegister.setOnClickListener {
            val fullName = binding.etFullName.text.toString().trim()
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()

            if (validateInputs(fullName, username, password, confirmPassword)) {
                viewModel.register(fullName, username, password)
            }
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.registerResult.collect { result ->
                when {
                    result.isLoading -> {
                        // Mostrar progress bar
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    result.isSuccess -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this@RegisterActivity, "¡Registro exitoso!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
                        finish()
                    }
                    result.isError -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this@RegisterActivity, result.error, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun validateInputs(
        fullName: String,
        username: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        var isValid = true

        if (fullName.isEmpty()) {
            binding.etFullName.error = "Ingrese nombre completo"
            isValid = false
        } else {
            binding.etFullName.error = null
        }

        if (username.isEmpty()) {
            binding.etUsername.error = "Ingrese un nombre de usuario"
            isValid = false
        } else if (username.length < 4) {
            binding.etUsername.error = "Mínimo 4 caracteres"
            isValid = false
        } else {
            binding.etUsername.error = null
        }

        if (password.length < 6) {
            binding.etPassword.error = "Mínimo 6 caracteres"
            isValid = false
        } else {
            binding.etPassword.error = null
        }

        if (password != confirmPassword) {
            binding.etConfirmPassword.error = "Las contraseñas no coinciden"
            isValid = false
        } else {
            binding.etConfirmPassword.error = null
        }

        return isValid
    }
}