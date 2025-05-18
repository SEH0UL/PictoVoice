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
import com.google.firebase.auth.FirebaseAuth
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

        // --- INICIO: Lógica TEMPORAL para ejecutar la actualización UNA VEZ ---
        // Descomenta el siguiente bloque para ejecutar la actualización de `fullNameLowercase`.
        // Después de UNA ejecución exitosa, ¡COMENTA O ELIMINA ESTE BLOQUE DE NUEVO!
        /*
        lifecycleScope.launch {
            Log.d("MainActivity_Backfill", "Iniciando backfill de fullNameLowercase para alumnos existentes...")
            val firestoreRepo = FirestoreRepository() // Creamos una instancia para usar la función
            val updateMessage = firestoreRepo.backfillFullNameLowercaseForAllStudents()
            Toast.makeText(this@MainActivity, updateMessage, Toast.LENGTH_LONG).show()
            Log.d("MainActivity_Backfill", "Backfill completado: $updateMessage")
        }
        */
        // --- FIN: Lógica TEMPORAL ---


        // Comprobar si el usuario ya está logueado
        if (authRepository.isUserLoggedIn()) {
            Log.d("MainActivity", "Usuario previamente logueado. Intentando restaurar sesión...")
            // authRepository.logout() // ¡COMENTA O ELIMINA ESTA LÍNEA!

            // Ahora, en lugar de desloguear, vamos a cargar los datos del usuario
            // y redirigir a la pantalla correspondiente.
            // Puedes usar una corrutina para esto.
            lifecycleScope.launch {
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    // Aquí deberías tener una forma de obtener el objeto User completo
                    // ya sea a través de AuthRepository o FirestoreRepository.
                    // AuthRepository tiene un método login que devuelve User, pero para
                    // un usuario ya logueado, necesitamos obtener sus datos de Firestore.
                    // Usaremos FirestoreRepository directamente o un método específico en AuthRepository.

                    // Opción A: Usar FirestoreRepository (si tienes una instancia disponible o la creas)
                    // val firestoreRepo = FirestoreRepository()
                    // val userResult = firestoreRepo.getUser(uid)

                    // Opción B: Añadir un método a AuthRepository para obtener el User actual si ya está logueado
                    // (Esta sería una mejor práctica para encapsular la lógica)
                    // Por ahora, para hacerlo rápido, asumiremos que puedes obtener el User.
                    // Necesitarás un método como `getCurrentUserProfile(uid: String)` en AuthRepository
                    // o usar directamente FirestoreRepository.

                    // --- INICIO: Lógica para cargar el usuario y redirigir ---
                    // Vamos a usar el AuthRepository que ya tienes instanciado,
                    // asumiendo que podría tener (o le añadimos) un método para esto.
                    // Por simplicidad, y dado que getUser está en FirestoreRepository,
                    // y AuthRepository ya tiene una instancia de Firestore,
                    // podríamos llamar a getUser directamente si expones esa funcionalidad o
                    // AuthRepository lo usa internamente.

                    // La forma más directa con tu estructura actual es usar FirestoreRepository
                    // pero MainActivity ya tiene una instancia de AuthRepository.
                    // Vamos a simular que AuthRepository puede obtener el User.
                    // Lo ideal sería que AuthViewModel manejara esto.

                    // --- Bloque de código para restaurar sesión ---
                    // (Este es similar al "BLOQUE DE AUTO-LOGIN SIMPLE" que tenías comentado)
                    val userDocResult = com.example.pictovoice.Data.repository.FirestoreRepository().getUser(uid) // Creando instancia temporalmente
                    // Lo ideal es inyectar o que AuthRepo lo haga

                    if (userDocResult.isSuccess) {
                        val user = userDocResult.getOrNull()
                        if (user != null) {
                            Toast.makeText(this@MainActivity, "Sesión restaurada para ${user.fullName}", Toast.LENGTH_SHORT).show()
                            if (user.role == "teacher") {
                                navigateToTeacherHome()
                            } else {
                                navigateToStudentHome()
                            }
                            finish() // Cierra MainActivity para que el usuario no vuelva aquí con "Atrás"
                            return@launch // Salimos de la corrutina y de onCreate
                        } else {
                            // Usuario en Auth pero no en Firestore, o error de conversión
                            Log.e("MainActivity", "Error al obtener datos del usuario desde Firestore.")
                            // Considera desloguear para evitar un estado inconsistente
                            authRepository.logout()
                        }
                    } else {
                        // Error al obtener el documento del usuario
                        Log.e("MainActivity", "Fallo al cargar el perfil del usuario: ${userDocResult.exceptionOrNull()?.message}")
                        // Considera desloguear para evitar un estado inconsistente
                        authRepository.logout()
                    }
                    // --- FIN: Lógica para cargar el usuario y redirigir ---
                } else {
                    // No hay UID aunque isUserLoggedIn sea true (raro, pero posible si el user de Firebase es null)
                    Log.w("MainActivity", "isUserLoggedIn es true pero currentUser es null.")
                    authRepository.logout() // Limpiar estado
                }
            }
            // Si la corrutina no hizo return (porque falló la carga del usuario),
            // la ejecución continuará y se mostrará la pantalla de login.
            // Si la corrutina tuvo éxito y redirigió, esta parte no se alcanzará
            // debido al return@launch. Sin embargo, para mayor claridad,
            // el setup de listeners/observers debería estar en un 'else' del if(isUserLoggedIn).

            // Para asegurar que setupListeners y setupObservers solo se llaman si no se redirige:
            // (El return@launch dentro del if(uid != null) y if(userDocResult.isSuccess) ya se encarga de esto)
            // Si la carga del usuario falla y la corrutina termina, se ejecutarán los setups de abajo.

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