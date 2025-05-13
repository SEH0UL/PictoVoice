package com.example.pictovoice.utils

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.pictovoice.ui.classroom.ClassDetailViewModel

@Suppress("UNCHECKED_CAST")
class ClassDetailViewModelFactory(
    private val application: Application,
    private val classId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ClassDetailViewModel::class.java)) {
            return ClassDetailViewModel(application, classId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}