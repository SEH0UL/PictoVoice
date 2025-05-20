package com.example.pictovoice.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import com.example.pictovoice.utils.AuthViewModelFactory
import com.example.pictovoice.viewmodels.AuthResult
import com.example.pictovoice.viewmodels.AuthViewModel
import kotlinx.coroutines.flow.collectLatest // Usar collectLatest si solo interesa el último estado
import kotlinx.coroutines.launch

private const val TAG = "RegisterActivity" // Tag para Logs

/**
 * Activity para el registro de nuevos usuarios (alumnos o profesores).
 * Permite a los usuarios ingresar su información, seleccionar un rol y crear una cuenta.
 * Los profesores requieren un código de confirmación para registrarse.
 */
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    // La instancia de AuthRepository se crea dentro de la factory del ViewModel.
    // private val authRepository = AuthRepository() // Esta instancia local no es necesaria.
    private val viewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(AuthRepository())
    }

    // Código de confirmación para el registro de profesores.
    // Considera moverlo a un lugar más seguro o configurable si es necesario para producción.
    private val TEACHER_CONFIRMATION_CODE = "PROFE2025"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d(TAG, "onCreate: Iniciando RegisterActivity.")

        setupListeners()
        setupObservers()
    }

    /**
     * Configura los listeners para los elementos interactivos de la UI, como botones y RadioGroups.
     */
    private fun setupListeners() {
        Log.d(TAG, "Configurando listeners...")
        binding.rgRole.setOnCheckedChangeListener { _, checkedId ->
            binding.tilTeacherCode.visibility = if (checkedId == R.id.rbTeacher) View.VISIBLE else View.GONE
        }

        binding.btnRegister.setOnClickListener {
            Log.d(TAG, "Botón de Registro pulsado.")
            handleRegistration()
        }

        binding.btnLoginRedirect.setOnClickListener {
            Log.d(TAG, "Botón 'Ya tengo cuenta' (redirigir a Login) pulsado.")
            navigateToLogin()
        }
    }

    /**
     * Procesa la información ingresada por el usuario e inicia el proceso de registro
     * si la validación es exitosa.
     */
    private fun handleRegistration() {
        val fullName = binding.etFullName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()
        val isTeacher = binding.rbTeacher.isChecked
        val teacherCode = binding.etTeacherCode.text.toString().trim()
        val role = if (isTeacher) "teacher" else "student"

        if (validateInputs(fullName, email, password, confirmPassword, isTeacher, teacherCode)) {
            Log.d(TAG, "Inputs validados. Registrando usuario: $email, Rol: $role")
            viewModel.register(fullName, email, password, role)
        } else {
            Log.w(TAG, "Validación de inputs fallida.")
        }
    }

    /**
     * Configura los observadores para los LiveData/StateFlow del [AuthViewModel],
     * principalmente para reaccionar al resultado del proceso de registro.
     */
    private fun setupObservers() {
        Log.d(TAG, "Configurando observadores...")
        lifecycleScope.launch {
            // Usar collectLatest si solo te interesa la última emisión en caso de cambios rápidos,
            // aunque para un resultado de registro, collect suele ser suficiente.
            viewModel.registerResult.collect { result ->
                binding.progressBar.visibility = if (result.isLoading) View.VISIBLE else View.GONE

                when (result) {
                    is AuthResult.Success -> {
                        val generatedUsername = result.data
                        Log.i(TAG, "Observer: Registro exitoso. Username: $generatedUsername")
                        showRegistrationSuccessDialog(generatedUsername)
                    }
                    is AuthResult.Error -> {
                        Log.w(TAG, "Observer: Error en registro - ${result.message}")
                        Toast.makeText(this@RegisterActivity, result.message, Toast.LENGTH_LONG).show()
                    }
                    is AuthResult.Loading -> Log.d(TAG, "Observer: Registrando...")
                    is AuthResult.Idle -> Log.d(TAG, "Observer: Estado de registro Idle.")
                }
            }
        }
    }

    /**
     * Muestra un diálogo de alerta indicando que el registro fue exitoso.
     * Al cerrar el diálogo, navega a la pantalla de inicio de sesión ([MainActivity]).
     * @param username El nombre de usuario generado para el nuevo usuario.
     */
    private fun showRegistrationSuccessDialog(username: String) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.register_dialog_title_success))
            .setMessage(getString(R.string.register_dialog_message_success, username))
            .setPositiveButton(getString(R.string.dialog_button_ok)) { dialog, _ -> // Reutilizar dialog_button_ok
                dialog.dismiss()
                navigateToLogin()
            }
            .setCancelable(false)
            .show()
    }

    /**
     * Navega a [MainActivity] (pantalla de login) y finaliza la [RegisterActivity].
     */
    private fun navigateToLogin() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /**
     * Valida todos los campos de entrada del formulario de registro.
     * Muestra mensajes de error en los TextInputLayouts correspondientes si la validación falla.
     * @return `true` si todas las entradas son válidas, `false` en caso contrario.
     */
    private fun validateInputs(
        fullName: String, email: String, password: String, confirmPassword: String,
        isTeacher: Boolean, teacherCode: String
    ): Boolean {
        var isValid = true

        // Nombre Completo
        if (fullName.isEmpty()) {
            // CORRECCIÓN: Usar los nombres de string del strings.xml organizado
            binding.tilFullName.error = getString(R.string.register_error_fullname_required)
            isValid = false
        } else if (!fullName.trim().contains(" ")) {
            binding.tilFullName.error = getString(R.string.register_error_fullname_at_least_two_words)
            isValid = false
        } else {
            binding.tilFullName.error = null
        }

        // Email
        if (email.isEmpty()) {
            binding.tilEmail.error = getString(R.string.register_error_email_required)
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.register_error_email_invalid)
            isValid = false
        } else {
            binding.tilEmail.error = null
        }

        // Contraseña
        if (password.length < 6) {
            binding.tilPassword.error = getString(R.string.register_error_password_min_length, 6)
            isValid = false
        } else {
            binding.tilPassword.error = null
        }

        // Confirmar Contraseña
        if (password != confirmPassword) {
            binding.tilConfirmPassword.error = getString(R.string.register_error_password_mismatch)
            isValid = false
        } else {
            binding.tilConfirmPassword.error = null
        }

        // Código de Profesor (si aplica)
        if (isTeacher) {
            if (teacherCode.isEmpty()) {
                binding.tilTeacherCode.error = getString(R.string.register_error_teacher_code_required)
                isValid = false
            } else if (teacherCode != TEACHER_CONFIRMATION_CODE) {
                binding.tilTeacherCode.error = getString(R.string.register_error_teacher_code_incorrect)
                isValid = false
            } else {
                binding.tilTeacherCode.error = null
            }
        } else {
            binding.tilTeacherCode.error = null
        }
        return isValid
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: RegisterActivity destruida.")
    }
}