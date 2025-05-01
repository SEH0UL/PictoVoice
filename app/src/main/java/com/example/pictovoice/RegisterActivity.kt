package com.example.pictovoice

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.pictovoice.databinding.ActivityRegisterBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val auth = Firebase.auth
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegister.setOnClickListener {
            val fullName = binding.etFullName.text.toString().trim()
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()

            if (validateInputs(fullName, username, password, confirmPassword)) {
                registerUser(fullName, username, password)
            }
        }
    }

    private fun validateInputs(fullName: String, username: String,
                               password: String, confirmPassword: String): Boolean {
        if (fullName.isEmpty()) {
            binding.etFullName.error = "Ingrese nombre completo"
            return false
        }
        if (username.isEmpty()) {
            binding.etUsername.error = "Ingrese un nombre de usuario"
            return false
        }
        if (password.length < 6) {
            binding.etPassword.error = "Mínimo 6 caracteres"
            return false
        }
        if (password != confirmPassword) {
            binding.etConfirmPassword.error = "Las contraseñas no coinciden"
            return false
        }
        return true
    }

    private fun registerUser(fullName: String, username: String, password: String) {
        val email = "$username@pictovoice.com"

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    saveUserToFirestore(fullName, username, auth.currentUser?.uid ?: "")
                } else {
                    Toast.makeText(
                        this,
                        "Error: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun saveUserToFirestore(fullName: String, username: String, userId: String) {
        val user = hashMapOf(
            "fullName" to fullName,
            "username" to username,
            "role" to "student",
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("users").document(userId)
            .set(user)
            .addOnSuccessListener {
                Toast.makeText(this, "¡Registro exitoso!", Toast.LENGTH_SHORT).show()
                // Navegación a MainActivity después del registro
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish() // Cierra RegisterActivity
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al guardar datos: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}