package com.example.pictovoice.utils // o com.example.pictovoice.ui.teacher

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.pictovoice.viewmodels.TeacherHomeViewModel // Asegúrate que la ruta es correcta

@Suppress("UNCHECKED_CAST")
class TeacherHomeViewModelFactory(
    private val application: Application
    // Puedes añadir aquí el FirestoreRepository si prefieres inyectarlo directamente
    // en lugar de instanciarlo en el ViewModel.
    // private val firestoreRepository: FirestoreRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TeacherHomeViewModel::class.java)) {
            return TeacherHomeViewModel(application /*, firestoreRepository */) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}