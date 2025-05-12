package com.example.pictovoice.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.pictovoice.Data.Model.Classroom
import com.example.pictovoice.Data.Model.User
import com.example.pictovoice.Data.repository.FirestoreRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Date

/**
 * Sealed class para representar los diferentes estados de las operaciones en la UI del profesor.
 * Puede ser Idle (inactivo), Loading (cargando), Success (éxito con datos) o Error (fallo con mensaje).
 */
sealed class TeacherHomeResult<out T> {
    object Idle : TeacherHomeResult<Nothing>()
    object Loading : TeacherHomeResult<Nothing>()
    data class Success<out T>(val data: T) : TeacherHomeResult<T>()
    data class Error(val message: String) : TeacherHomeResult<Nothing>()
}

class TeacherHomeViewModel(application: Application) : AndroidViewModel(application) {

    private val firestoreRepository = FirestoreRepository()
    private val firebaseAuth = FirebaseAuth.getInstance()

    // LiveData para los datos del profesor autenticado.
    private val _teacherData = MutableLiveData<User?>()
    val teacherData: LiveData<User?> = _teacherData

    // LiveData para la lista de clases del profesor.
    private val _classes = MutableLiveData<List<Classroom>>()
    val classes: LiveData<List<Classroom>> = _classes

    // LiveData para el estado general de la UI (carga inicial, errores generales).
    private val _uiState = MutableLiveData<TeacherHomeResult<Any>>(TeacherHomeResult.Idle)
    val uiState: LiveData<TeacherHomeResult<Any>> = _uiState

    // LiveData para el resultado de la operación de crear una nueva clase.
    private val _createClassResult = MutableLiveData<TeacherHomeResult<Unit>>()
    val createClassResult: LiveData<TeacherHomeResult<Unit>> = _createClassResult

    // LiveData para el resultado de la operación de eliminar una clase.
    private val _deleteClassResult = MutableLiveData<TeacherHomeResult<Unit>>()
    val deleteClassResult: LiveData<TeacherHomeResult<Unit>> = _deleteClassResult

    init {
        // Cargar los datos iniciales cuando el ViewModel se crea.
        loadTeacherDataAndClasses()
    }

    /**
     * Carga los datos del profesor y sus clases desde Firestore.
     * Actualiza _teacherData, _classes y _uiState.
     */
    fun loadTeacherDataAndClasses() {
        _uiState.value = TeacherHomeResult.Loading
        val userId = firebaseAuth.currentUser?.uid
        if (userId == null) {
            _uiState.value = TeacherHomeResult.Error("Profesor no autenticado.")
            Log.e("TeacherHomeVM", "loadTeacherDataAndClasses: El ID del usuario es nulo.")
            return
        }

        viewModelScope.launch {
            // Cargar datos del profesor.
            val userResult = firestoreRepository.getUser(userId)
            if (userResult.isSuccess) {
                _teacherData.value = userResult.getOrNull()
                if (_teacherData.value == null) {
                    Log.e("TeacherHomeVM", "loadTeacherDataAndClasses: Datos del profesor son nulos desde Firestore para UID: $userId")
                    _uiState.value = TeacherHomeResult.Error("No se pudieron obtener los datos del perfil del profesor.")
                    return@launch // No continuar si el perfil del profesor no se carga.
                }
            } else {
                _uiState.value = TeacherHomeResult.Error(userResult.exceptionOrNull()?.message ?: "Error desconocido al cargar datos del profesor.")
                Log.e("TeacherHomeVM", "loadTeacherDataAndClasses: Error al obtener usuario: ${userResult.exceptionOrNull()?.message}")
                return@launch
            }

            // Si el profesor se cargó, cargar sus clases.
            val classesResult = firestoreRepository.getClassesByTeacher(userId)
            if (classesResult.isSuccess) {
                _classes.value = classesResult.getOrNull() ?: emptyList()
                _uiState.value = TeacherHomeResult.Success(_classes.value ?: emptyList<Classroom>()) // Indicar éxito general.
                Log.d("TeacherHomeVM", "Datos y clases del profesor cargados. Clases encontradas: ${_classes.value?.size ?: 0}")
            } else {
                _uiState.value = TeacherHomeResult.Error(classesResult.exceptionOrNull()?.message ?: "Error desconocido al cargar las clases.")
                Log.e("TeacherHomeVM", "loadTeacherDataAndClasses: Error al obtener clases: ${classesResult.exceptionOrNull()?.message}")
            }
        }
    }

    /**
     * Crea una nueva clase en Firestore.
     * @param className Nombre de la nueva clase.
     * @param studentIds Lista de IDs de los alumnos a añadir (puede estar vacía).
     */
    fun createNewClass(className: String, studentIds: List<String>) {
        _createClassResult.value = TeacherHomeResult.Loading
        val teacherId = firebaseAuth.currentUser?.uid
        if (teacherId == null) {
            _createClassResult.value = TeacherHomeResult.Error("Profesor no autenticado para crear clase.")
            return
        }
        if (className.isBlank()) {
            _createClassResult.value = TeacherHomeResult.Error("El nombre de la clase no puede estar vacío.")
            return
        }

        val newClass = Classroom(
            className = className.trim(),
            teacherId = teacherId,
            studentIds = studentIds,
            createdAt = Date() // Firestore convierte Date a Timestamp.
        )

        viewModelScope.launch {
            val result = firestoreRepository.saveClass(newClass)
            if (result.isSuccess) {
                _createClassResult.value = TeacherHomeResult.Success(Unit)
                Log.d("TeacherHomeVM", "Clase '$className' creada exitosamente.")
                loadTeacherDataAndClasses() // Recargar datos para mostrar la nueva clase.
            } else {
                _createClassResult.value = TeacherHomeResult.Error(result.exceptionOrNull()?.message ?: "Error desconocido al crear la clase.")
            }
        }
    }

    /**
     * Actualiza una clase existente en Firestore.
     * @param classroom Objeto Classroom con los datos actualizados.
     */
    fun updateClass(classroom: Classroom) {
        _uiState.value = TeacherHomeResult.Loading // Se podría usar un LiveData específico para 'updateClassResult'.
        val teacherId = firebaseAuth.currentUser?.uid
        if (teacherId == null || classroom.teacherId != teacherId) {
            _uiState.value = TeacherHomeResult.Error("No autorizado para actualizar esta clase.")
            return
        }
        if (classroom.classId.isBlank()){
            _uiState.value = TeacherHomeResult.Error("ID de clase inválido para actualizar.")
            return
        }


        viewModelScope.launch {
            val result = firestoreRepository.saveClass(classroom) // saveClass también sirve para actualizar si el ID existe.
            if (result.isSuccess) {
                Log.d("TeacherHomeVM", "Clase '${classroom.className}' actualizada exitosamente.")
                loadTeacherDataAndClasses() // Recargar datos.
            } else {
                _uiState.value = TeacherHomeResult.Error(result.exceptionOrNull()?.message ?: "Error desconocido al actualizar la clase.")
            }
        }
    }

    /**
     * Elimina una clase de Firestore.
     * @param classroom La clase a eliminar.
     */
    fun deleteClass(classroom: Classroom) {
        _deleteClassResult.value = TeacherHomeResult.Loading
        val currentTeacherId = firebaseAuth.currentUser?.uid

        if (currentTeacherId == null) {
            _deleteClassResult.value = TeacherHomeResult.Error("Usuario no autenticado.")
            return
        }
        // Verificación adicional: solo el profesor dueño debería poder eliminar.
        // Las reglas de Firestore son la principal capa de seguridad.
        if (classroom.teacherId != currentTeacherId) {
            _deleteClassResult.value = TeacherHomeResult.Error("No tienes permiso para eliminar esta clase.")
            Log.w("TeacherHomeVM", "Intento de eliminación de clase por usuario no propietario. UID: $currentTeacherId, TeacherID clase: ${classroom.teacherId}")
            return
        }
        if (classroom.classId.isBlank()){
            _deleteClassResult.value = TeacherHomeResult.Error("ID de clase inválido para eliminar.")
            return
        }

        viewModelScope.launch {
            val result = firestoreRepository.deleteClass(classroom.classId)
            if (result.isSuccess) {
                _deleteClassResult.value = TeacherHomeResult.Success(Unit)
                Log.d("TeacherHomeVM", "Clase '${classroom.className}' eliminada exitosamente.")
                loadTeacherDataAndClasses() // Recargar la lista de clases.
            } else {
                _deleteClassResult.value = TeacherHomeResult.Error(result.exceptionOrNull()?.message ?: "Error desconocido al eliminar la clase.")
            }
        }
    }

    /** Resetea el estado de _createClassResult a Idle. */
    fun resetCreateClassResult() {
        _createClassResult.value = TeacherHomeResult.Idle
    }

    /** Resetea el estado de _deleteClassResult a Idle. */
    fun resetDeleteClassResult() {
        _deleteClassResult.value = TeacherHomeResult.Idle
    }
}