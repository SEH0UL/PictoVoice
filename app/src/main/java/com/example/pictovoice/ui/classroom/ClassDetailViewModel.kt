package com.example.pictovoice.ui.classroom

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.pictovoice.Data.Model.Classroom
import com.example.pictovoice.Data.Model.User
import com.example.pictovoice.Data.repository.FirestoreRepository
import kotlinx.coroutines.launch

// Podríamos reutilizar TeacherHomeResult o crear uno específico como ClassDetailResult
sealed class ClassDetailResult<out T> {
    object Idle : ClassDetailResult<Nothing>()
    object Loading : ClassDetailResult<Nothing>()
    data class Success<out T>(val data: T) : ClassDetailResult<T>()
    data class Error(val message: String) : ClassDetailResult<Nothing>()
}


class ClassDetailViewModel(
    application: Application,
    private val classId: String
) : AndroidViewModel(application) {

    private val firestoreRepository = FirestoreRepository()

    private val _classroomDetails = MutableLiveData<Classroom?>()
    val classroomDetails: LiveData<Classroom?> = _classroomDetails

    private val _studentsInClass = MutableLiveData<List<User>>() // Lista original de la clase
    val studentsInClass: LiveData<List<User>> = _studentsInClass

    private val _filteredStudentsInClass = MutableLiveData<List<User>>() // Para la UI principal
    val filteredStudentsInClass: LiveData<List<User>> = _filteredStudentsInClass

    private val _uiState = MutableLiveData<ClassDetailResult<Any>>(ClassDetailResult.Idle)
    val uiState: LiveData<ClassDetailResult<Any>> = _uiState

    private val _removeStudentResult = MutableLiveData<ClassDetailResult<Unit>>()
    val removeStudentResult: LiveData<ClassDetailResult<Unit>> = _removeStudentResult

    // --- Para la funcionalidad de Añadir Alumno ---
    private val _availableStudentsSearchResult = MutableLiveData<List<User>>() // Resultados para el diálogo
    val availableStudentsSearchResult: LiveData<List<User>> = _availableStudentsSearchResult

    private val _searchStudentsState = MutableLiveData<ClassDetailResult<List<User>>>() // Estado de la búsqueda en el diálogo
    val searchStudentsState: LiveData<ClassDetailResult<List<User>>> = _searchStudentsState

    private val _addStudentToClassResult = MutableLiveData<ClassDetailResult<Unit>>()
    val addStudentToClassResult: LiveData<ClassDetailResult<Unit>> = _addStudentToClassResult

    init {
        if (classId.isNotBlank()) {
            loadClassDetailsAndStudents()
        } else {
            _uiState.value = ClassDetailResult.Error("ID de clase no válido.")
            Log.e("ClassDetailVM", "classId está vacío en el constructor.")
        }
    }

    fun loadClassDetailsAndStudents() {
        _uiState.value = ClassDetailResult.Loading
        viewModelScope.launch {
            val classResult = firestoreRepository.getClassDetails(classId)
            if (classResult.isSuccess) {
                val classroom = classResult.getOrNull()
                _classroomDetails.value = classroom
                if (classroom != null && classroom.studentIds.isNotEmpty()) {
                    val studentsResult = firestoreRepository.getUsersByIds(classroom.studentIds)
                    if (studentsResult.isSuccess) {
                        val students = studentsResult.getOrNull() ?: emptyList()
                        _studentsInClass.value = students
                        _filteredStudentsInClass.value = students
                        _uiState.value = ClassDetailResult.Success(students)
                        Log.d("ClassDetailVM", "Clase y ${students.size} alumnos cargados.")
                    } else {
                        _studentsInClass.value = emptyList() // Limpiar en caso de error
                        _filteredStudentsInClass.value = emptyList()
                        _uiState.value = ClassDetailResult.Error(studentsResult.exceptionOrNull()?.message ?: "Error cargando alumnos.")
                        Log.e("ClassDetailVM", "Error cargando alumnos: ${studentsResult.exceptionOrNull()?.message}")
                    }
                } else if (classroom != null && classroom.studentIds.isEmpty()) {
                    _studentsInClass.value = emptyList()
                    _filteredStudentsInClass.value = emptyList()
                    _uiState.value = ClassDetailResult.Success(emptyList<User>())
                    Log.d("ClassDetailVM", "Clase cargada, no tiene alumnos.")
                } else {
                    _uiState.value = ClassDetailResult.Error("No se encontraron detalles para la clase ID: $classId")
                    Log.w("ClassDetailVM", "No se encontró la clase con ID: $classId")
                }
            } else {
                _uiState.value = ClassDetailResult.Error(classResult.exceptionOrNull()?.message ?: "Error cargando detalles de la clase.")
                Log.e("ClassDetailVM", "Error cargando detalles de clase: ${classResult.exceptionOrNull()?.message}")
            }
        }
    }

    fun filterStudents(query: String?) {
        val currentList = _studentsInClass.value ?: emptyList()
        if (query.isNullOrBlank()) {
            _filteredStudentsInClass.value = currentList
        } else {
            val lowercaseQuery = query.lowercase().trim()
            _filteredStudentsInClass.value = currentList.filter { student ->
                student.fullName.lowercase().contains(lowercaseQuery) ||
                        student.username.lowercase().contains(lowercaseQuery)
            }
        }
    }

    fun removeStudentFromCurrentClass(studentId: String) {
        _removeStudentResult.value = ClassDetailResult.Loading
        val currentClassId = _classroomDetails.value?.classId
        if (currentClassId.isNullOrBlank() || currentClassId != classId) {
            _removeStudentResult.value = ClassDetailResult.Error("Error interno: ID de clase inconsistente.")
            return
        }
        viewModelScope.launch {
            val result = firestoreRepository.removeStudentFromClassroom(currentClassId, studentId)
            if (result.isSuccess) {
                _removeStudentResult.value = ClassDetailResult.Success(Unit)
                loadClassDetailsAndStudents() // Recargar para reflejar cambio
            } else {
                _removeStudentResult.value = ClassDetailResult.Error(result.exceptionOrNull()?.message ?: "Error al eliminar alumno.")
            }
        }
    }

    fun resetRemoveStudentResult() {
        _removeStudentResult.value = ClassDetailResult.Idle
    }

    fun searchAvailableStudents(nameQuery: String) {
        if (nameQuery.isBlank()) {
            _availableStudentsSearchResult.value = emptyList()
            _searchStudentsState.value = ClassDetailResult.Idle // Estado neutral si no hay query
            return
        }
        // Evitar búsquedas demasiado cortas si se desea
        // if (nameQuery.length < 2) {
        //     _searchStudentsState.value = ClassDetailResult.Success(emptyList()) // O un Idle con mensaje "escribe más"
        //     return
        // }

        _searchStudentsState.value = ClassDetailResult.Loading
        viewModelScope.launch {
            val result = firestoreRepository.searchStudentsByName(nameQuery)
            if (result.isSuccess) {
                val foundStudents = result.getOrNull() ?: emptyList()
                val currentStudentIdsInClass = _studentsInClass.value?.map { it.userId } ?: emptyList()
                val available = foundStudents.filter { !currentStudentIdsInClass.contains(it.userId) }

                _availableStudentsSearchResult.value = available // Para el adapter del diálogo
                _searchStudentsState.value = ClassDetailResult.Success(available) // Para el estado del diálogo
            } else {
                _availableStudentsSearchResult.value = emptyList()
                _searchStudentsState.value = ClassDetailResult.Error(result.exceptionOrNull()?.message ?: "Error buscando alumnos.")
            }
        }
    }

    fun addStudentToCurrentClass(studentId: String) {
        _addStudentToClassResult.value = ClassDetailResult.Loading
        val currentClassId = _classroomDetails.value?.classId
        if (currentClassId.isNullOrBlank() || currentClassId != classId) {
            _addStudentToClassResult.value = ClassDetailResult.Error("Error interno: ID de clase inválido.")
            return
        }
        if (studentId.isBlank()){
            _addStudentToClassResult.value = ClassDetailResult.Error("ID de alumno inválido.")
            return
        }
        val isAlreadyInClass = _studentsInClass.value?.any { it.userId == studentId } ?: false
        if (isAlreadyInClass) {
            _addStudentToClassResult.value = ClassDetailResult.Error("Este alumno ya está en la clase.")
            return
        }

        viewModelScope.launch {
            val result = firestoreRepository.addStudentToClass(currentClassId, studentId)
            if (result.isSuccess) {
                _addStudentToClassResult.value = ClassDetailResult.Success(Unit)
                loadClassDetailsAndStudents() // Recargar para reflejar el nuevo alumno
            } else {
                _addStudentToClassResult.value = ClassDetailResult.Error(result.exceptionOrNull()?.message ?: "Error al añadir alumno.")
            }
        }
    }

    // MÉTODO NECESARIO
    fun resetAddStudentToClassResult() {
        _addStudentToClassResult.value = ClassDetailResult.Idle
    }

    // MÉTODO NECESARIO
    fun clearAvailableStudentSearchResults() {
        _availableStudentsSearchResult.value = emptyList()
        _searchStudentsState.value = ClassDetailResult.Idle
    }
}