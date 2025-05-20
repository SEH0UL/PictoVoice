package com.example.pictovoice.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.pictovoice.Data.repository.AuthRepository
import com.example.pictovoice.viewmodels.AuthViewModel

/**
 * Factory para crear instancias de [AuthViewModel].
 * Esta clase es necesaria porque [AuthViewModel] tiene dependencias en su constructor
 * (como [AuthRepository]) que deben ser provistas durante su creación.
 *
 * @property authRepository El repositorio de autenticación que se inyectará al [AuthViewModel].
 * @property firestoreRepository (Opcional) El repositorio de Firestore. Actualmente, [AuthViewModel]
 * lo instancia por defecto, pero podría ser inyectado para mejor testeabilidad.
 */
class AuthViewModelFactory(
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {

    /**
     * Crea una nueva instancia del ViewModel solicitado.
     *
     * @param modelClass La clase del ViewModel a crear.
     * @return Una instancia del ViewModel de tipo [T].
     * @throws IllegalArgumentException si [modelClass] no es [AuthViewModel].
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            return AuthViewModel(authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}