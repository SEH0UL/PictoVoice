package com.example.pictovoice.utils

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.pictovoice.viewmodels.ClassDetailViewModel

/**
 * Factory para crear instancias de [ClassDetailViewModel].
 * Esta clase es necesaria porque [ClassDetailViewModel] es un [AndroidViewModel] y además
 * requiere parámetros en su constructor (como `classId`) que deben ser provistos
 * durante su creación.
 *
 * @property application La instancia de la aplicación, requerida por [AndroidViewModel].
 * @property classId El ID de la clase cuyos detalles gestionará el [ClassDetailViewModel].
 */
@Suppress("UNCHECKED_CAST")
class ClassDetailViewModelFactory(
    private val application: Application,
    private val classId: String
) : ViewModelProvider.Factory {
    /**
     * Crea una nueva instancia del ViewModel solicitado.
     *
     * @param modelClass La clase del ViewModel a crear.
     * @return Una instancia del ViewModel de tipo [T].
     * @throws IllegalArgumentException si [modelClass] no es [ClassDetailViewModel].
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ClassDetailViewModel::class.java)) {
            return ClassDetailViewModel(application, classId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name} in ClassDetailViewModelFactory")
    }
}