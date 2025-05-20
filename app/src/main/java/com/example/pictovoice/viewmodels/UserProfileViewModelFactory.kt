package com.example.pictovoice.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.pictovoice.Data.repository.FirestoreRepository
// Asegúrate de importar UserProfileViewModel desde su ubicación correcta
// import com.example.pictovoice.viewmodels.UserProfileViewModel // Si está en el mismo paquete, no es estrictamente necesario.

/**
 * Factory para crear instancias de [UserProfileViewModel].
 * Esta clase es necesaria porque [UserProfileViewModel] tiene dependencias en su constructor
 * (como `targetUserId` y `firestoreRepository`) que deben ser provistas durante su creación.
 *
 * @property targetUserId El ID del usuario cuyo perfil gestionará el [UserProfileViewModel].
 * @property firestoreRepository El repositorio de Firestore que se inyectará al [UserProfileViewModel].
 */
class UserProfileViewModelFactory(
    private val targetUserId: String,
    private val firestoreRepository: FirestoreRepository
) : ViewModelProvider.Factory {

    /**
     * Crea una nueva instancia del ViewModel solicitado.
     *
     * @param modelClass La clase del ViewModel a crear.
     * @return Una instancia del ViewModel de tipo [T].
     * @throws IllegalArgumentException si [modelClass] no es [UserProfileViewModel].
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserProfileViewModel::class.java)) {
            // UserProfileViewModel accede a PictogramDataSource directamente como un Singleton,
            // por lo que no necesita ser inyectado aquí.
            return UserProfileViewModel(targetUserId, firestoreRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class for UserProfileViewModelFactory: ${modelClass.name}")
    }
}