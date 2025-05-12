package com.example.pictovoice.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.pictovoice.Data.repository.AuthRepository
import com.example.pictovoice.R
import com.example.pictovoice.databinding.ActivityRegisterBinding
import com.example.pictovoice.utils.AuthResult
import com.example.pictovoice.utils.AuthViewModel
import com.example.pictovoice.utils.AuthViewModelFactory
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val authRepository = AuthRepository()
    private val viewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(authRepository)
    }

    private val TEACHER_CONFIRMATION_CODE = "PROFE2025"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupObservers()
        setupListeners()
    }

    private fun setupListeners() {
        binding.rgRole.setOnCheckedChangeListener { _, checkedId ->
            binding.tilTeacherCode.visibility = if (checkedId == R.id.rbTeacher) View.VISIBLE else View.GONE
        }

        binding.btnRegister.setOnClickListener {
            val fullName = binding.etFullName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()
            val isTeacher = binding.rbTeacher.isChecked
            val teacherCode = binding.etTeacherCode.text.toString().trim()
            val role = if (isTeacher) "teacher" else "student"

            if (validateInputs(fullName, email, password, confirmPassword, isTeacher, teacherCode)) {
                viewModel.register(fullName, email, password, role)
            }
        }

        binding.btnLoginRedirect.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.registerResult.collect { result: AuthResult<String> -> // Espera AuthResult<String>
                binding.progressBar.visibility = if (result.isLoading) View.VISIBLE else View.GONE
                when (result) {
                    is AuthResult.Success -> {
                        val generatedUsername = result.data // 'data' es ahora el String del username
                        showRegistrationSuccessDialog(generatedUsername)
                    }
                    is AuthResult.Error -> {
                        Toast.makeText(this@RegisterActivity, result.message, Toast.LENGTH_LONG).show()
                    }
                    is AuthResult.Loading -> { /* Ya manejado por progressBar */ }
                    is AuthResult.Idle -> { /* No action */ }
                }
            }
        }
    }

    private fun showRegistrationSuccessDialog(username: String) {
        AlertDialog.Builder(this)
            .setTitle("¡Registro Exitoso!")
            .setMessage("Tu cuenta ha sido creada.\nTu nombre de usuario es: $username\n\nSerás redirigido a la pantalla de inicio de sesión.")
            .setPositiveButton("Entendido") { dialog, _ ->
                dialog.dismiss()
                val intent = Intent(this@RegisterActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun validateInputs(
        fullName: String, email: String, password: String, confirmPassword: String,
        isTeacher: Boolean, teacherCode: String
    ): Boolean {
        var isValid = true

        if (fullName.isEmpty()) {
            binding.tilFullName.error = "Ingresa tu nombre completo"
            isValid = false
        } else if (!fullName.trim().contains(" ")) {
            binding.tilFullName.error = "Ingresa al menos un nombre y un apellido"
            isValid = false
        } else {
            binding.tilFullName.error = null
        }

        if (email.isEmpty()) {
            binding.tilEmail.error = "Ingresa tu correo electrónico"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Correo electrónico no válido"
            isValid = false
        } else {
            binding.tilEmail.error = null
        }

        if (password.length < 6) {
            binding.tilPassword.error = "La contraseña debe tener al menos 6 caracteres"
            isValid = false
        } else {
            binding.tilPassword.error = null
        }

        if (password != confirmPassword) {
            binding.tilConfirmPassword.error = "Las contraseñas no coinciden"
            isValid = false
        } else {
            binding.tilConfirmPassword.error = null
        }

        if (isTeacher) {
            if (teacherCode.isEmpty()) {
                binding.tilTeacherCode.error = "Ingresa el código de profesor"
                isValid = false
            } else if (teacherCode != TEACHER_CONFIRMATION_CODE) {
                binding.tilTeacherCode.error = "Código de profesor incorrecto"
                isValid = false
            } else {
                binding.tilTeacherCode.error = null
            }
        } else {
            binding.tilTeacherCode.error = null
        }
        return isValid
    }
}