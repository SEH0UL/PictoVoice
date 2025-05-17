package com.example.pictovoice.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.pictovoice.Data.repository.FirestoreRepository

class UserProfileViewModelFactory(
    private val targetUserId: String,
    private val firestoreRepository: FirestoreRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserProfileViewModel::class.java)) {
            return UserProfileViewModel(targetUserId, firestoreRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class for UserProfile")
    }
}