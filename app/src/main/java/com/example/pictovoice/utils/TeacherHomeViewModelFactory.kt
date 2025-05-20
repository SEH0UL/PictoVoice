package com.example.pictovoice.utils

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.pictovoice.viewmodels.TeacherHomeViewModel

/**
 * Factory para crear instancias de [TeacherHomeViewModel].
 * Esta clase es necesaria porque [TeacherHomeViewModel] es un [AndroidViewModel] y, por lo tanto,
 * requiere la instancia de [Application] en su constructor.
 *
 * @property application La instancia de la aplicación que se pasará al [TeacherHomeViewModel].
 */
@Suppress("UNCHECKED_CAST")
class TeacherHomeViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    /**
     * Crea una nueva instancia del ViewModel solicitado.
     *
     * @param modelClass La clase del ViewModel a crear.
     * @return Una instancia del ViewModel de tipo [T].
     * @throws IllegalArgumentException si [modelClass] no es [TeacherHomeViewModel].
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TeacherHomeViewModel::class.java)) {
            // TeacherHomeViewModel actualmente instancia FirestoreRepository internamente.
            // Si en el futuro se modifica para inyectar FirestoreRepository,
            // esta factory necesitaría recibirlo y pasarlo aquí.
            return TeacherHomeViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name} in TeacherHomeViewModelFactory")
    }
}