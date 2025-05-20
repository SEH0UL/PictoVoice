package com.example.pictovoice.viewmodels

import User
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.pictovoice.Data.repository.FirestoreRepository
import com.example.pictovoice.data.model.Classroom // Asegúrate que el path del import es correcto
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Date

private const val TAG = "TeacherHomeVM" // Tag para los logs

/**
 * Sealed class para representar los diferentes estados y resultados de las operaciones
 * en la pantalla principal del profesor ([com.example.pictovoice.ui.teacher.TeacherHomeActivity]).
 * @param T El tipo de dato en caso de éxito.
 */
sealed class TeacherHomeResult<out T> {
    /** Estado inicial o inactivo. */
    object Idle : TeacherHomeResult<Nothing>()
    /** Estado de carga, operación en progreso. */
    object Loading : TeacherHomeResult<Nothing>()
    /** Estado de éxito, contiene los datos [data] de tipo [T]. */
    data class Success<out T>(val data: T) : TeacherHomeResult<T>()
    /** Estado de error, contiene un [message] descriptivo. */
    data class Error(val message: String) : TeacherHomeResult<Nothing>()
}

/**
 * ViewModel para [com.example.pictovoice.ui.teacher.TeacherHomeActivity].
 * Gestiona la carga de los datos del profesor, la lista de sus clases,
 * y las operaciones de creación, actualización y eliminación de clases.
 *
 * @param application La instancia de la aplicación, necesaria para AndroidViewModel.
 */
class TeacherHomeViewModel(application: Application) : AndroidViewModel(application) {

    private val firestoreRepository = FirestoreRepository()
    private val firebaseAuth = FirebaseAuth.getInstance()

    private val _teacherData = MutableLiveData<User?>()
    /** [LiveData] que contiene los datos del perfil del profesor actualmente autenticado. */
    val teacherData: LiveData<User?> get() = _teacherData

    private val _classes = MutableLiveData<List<Classroom>>()
    /** [LiveData] que contiene la lista de [Classroom] pertenecientes al profesor. */
    val classes: LiveData<List<Classroom>> get() = _classes

    private val _uiState = MutableLiveData<TeacherHomeResult<Any>>(TeacherHomeResult.Idle)
    /**
     * [LiveData] que representa el estado general de la UI, especialmente durante la carga
     * inicial de datos (datos del profesor y lista de clases).
     */
    val uiState: LiveData<TeacherHomeResult<Any>> get() = _uiState

    private val _createClassResult = MutableLiveData<TeacherHomeResult<Unit>>(TeacherHomeResult.Idle)
    /** [LiveData] que emite el resultado de la operación de creación de una nueva clase. */
    val createClassResult: LiveData<TeacherHomeResult<Unit>> get() = _createClassResult

    private val _deleteClassResult = MutableLiveData<TeacherHomeResult<Unit>>(TeacherHomeResult.Idle)
    /** [LiveData] que emite el resultado de la operación de eliminación de una clase. */
    val deleteClassResult: LiveData<TeacherHomeResult<Unit>> get() = _deleteClassResult

    // Podrías añadir un _updateClassResult similar si quieres feedback específico para la actualización.
    // Por ahora, las actualizaciones usan _uiState.

    init {
        Log.d(TAG, "TeacherHomeViewModel inicializado.")
        loadTeacherDataAndClasses()
    }

    /**
     * Carga los datos del perfil del profesor autenticado y la lista de clases que ha creado.
     * Actualiza [_teacherData], [_classes] y [_uiState] según el resultado.
     */
    fun loadTeacherDataAndClasses() {
        _uiState.value = TeacherHomeResult.Loading
        val userId = firebaseAuth.currentUser?.uid
        if (userId == null) {
            _uiState.value = TeacherHomeResult.Error("Profesor no autenticado.")
            Log.e(TAG, "loadTeacherDataAndClasses: El ID del usuario es nulo.")
            _teacherData.value = null
            _classes.value = emptyList()
            return
        }

        Log.d(TAG, "Cargando datos para el profesor ID: $userId")
        viewModelScope.launch {
            // Cargar datos del profesor
            val userResult = firestoreRepository.getUser(userId)
            if (userResult.isSuccess) {
                _teacherData.value = userResult.getOrNull()
                if (_teacherData.value == null) {
                    Log.w(TAG, "loadTeacherDataAndClasses: Datos del profesor son nulos desde Firestore para UID: $userId, aunque la consulta fue exitosa.")
                    // No se considera un error fatal para _uiState aquí, se intentará cargar clases.
                } else {
                    Log.d(TAG, "Datos del profesor cargados: ${_teacherData.value?.fullName}")
                }
            } else {
                val errorMsg = userResult.exceptionOrNull()?.message ?: "Error desconocido al cargar datos del profesor."
                _teacherData.value = null // Limpiar en caso de error
                // Podríamos poner _uiState en error aquí, pero si las clases cargan bien, podría ser confuso.
                // Se prioriza el resultado de la carga de clases para el _uiState general.
                Log.e(TAG, "loadTeacherDataAndClasses: Error al obtener datos del profesor: $errorMsg")
            }

            // Cargar clases del profesor
            val classesResult = firestoreRepository.getClassesByTeacher(userId)
            if (classesResult.isSuccess) {
                val fetchedClasses = classesResult.getOrNull() ?: emptyList()
                _classes.value = fetchedClasses
                _uiState.value = TeacherHomeResult.Success(fetchedClasses) // Éxito general si las clases se cargan
                Log.d(TAG, "Clases del profesor cargadas. Número de clases: ${fetchedClasses.size}")
            } else {
                val errorMsg = classesResult.exceptionOrNull()?.message ?: "Error desconocido al cargar las clases."
                _classes.value = emptyList() // Limpiar en caso de error
                _uiState.value = TeacherHomeResult.Error(errorMsg)
                Log.e(TAG, "loadTeacherDataAndClasses: Error al obtener clases: $errorMsg")
            }
        }
    }

    /**
     * Crea una nueva clase en Firestore.
     * @param className El nombre para la nueva clase. No puede estar vacío.
     * @param studentIds Lista de IDs de los alumnos a añadir inicialmente a la clase.
     * Asume que esta lista es proporcionada por la UI después de la selección de alumnos.
     */
    fun createNewClass(className: String, studentIds: List<String>) {
        _createClassResult.value = TeacherHomeResult.Loading
        val teacherId = firebaseAuth.currentUser?.uid
        if (teacherId == null) {
            _createClassResult.value = TeacherHomeResult.Error("Profesor no autenticado para crear clase.")
            Log.w(TAG, "createNewClass: Intento de crear clase sin profesor autenticado.")
            return
        }
        if (className.isBlank()) {
            _createClassResult.value = TeacherHomeResult.Error("El nombre de la clase no puede estar vacío.")
            Log.w(TAG, "createNewClass: Intento de crear clase con nombre vacío.")
            return
        }

        val newClass = Classroom(
            // classId será generado por Firestore si se deja vacío en el objeto y el repositorio lo maneja
            className = className.trim(),
            teacherId = teacherId,
            studentIds = studentIds,
            createdAt = Date()
        )
        Log.d(TAG, "Creando nueva clase: '${newClass.className}' por profesor $teacherId con ${studentIds.size} alumnos.")

        viewModelScope.launch {
            val result = firestoreRepository.saveClass(newClass)
            if (result.isSuccess) {
                _createClassResult.value = TeacherHomeResult.Success(Unit)
                Log.i(TAG, "Clase '${newClass.className}' creada exitosamente.")
                loadTeacherDataAndClasses() // Refrescar la lista de clases
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Error desconocido al crear la clase."
                _createClassResult.value = TeacherHomeResult.Error(errorMsg)
                Log.e(TAG, "Error al crear clase '${newClass.className}': $errorMsg")
            }
        }
    }

    /**
     * Actualiza los datos de una clase existente en Firestore.
     * Actualmente, solo permite actualizar el nombre de la clase.
     * @param classroom El objeto [Classroom] con los datos actualizados (importante: debe tener `classId`).
     */
    fun updateClass(classroom: Classroom) {
        // Se usa _uiState para el feedback de esta operación por ahora.
        // Considerar un LiveData específico como _updateClassResult si se necesita un manejo más granular.
        _uiState.value = TeacherHomeResult.Loading
        val currentTeacherId = firebaseAuth.currentUser?.uid

        if (currentTeacherId == null) {
            _uiState.value = TeacherHomeResult.Error("Profesor no autenticado para actualizar clase.")
            Log.w(TAG, "updateClass: Intento sin autenticación.")
            return
        }
        if (classroom.teacherId != currentTeacherId) {
            _uiState.value = TeacherHomeResult.Error("No está autorizado para actualizar esta clase.")
            Log.w(TAG, "updateClass: Intento de actualizar clase por profesor no propietario.")
            return
        }
        if (classroom.classId.isBlank()){
            _uiState.value = TeacherHomeResult.Error("ID de clase inválido para actualizar.")
            Log.w(TAG, "updateClass: ID de clase vacío.")
            return
        }
        Log.d(TAG, "Actualizando clase: '${classroom.className}' (ID: ${classroom.classId})")

        viewModelScope.launch {
            // saveClass en FirestoreRepository maneja tanto creación como actualización
            val result = firestoreRepository.saveClass(classroom)
            if (result.isSuccess) {
                Log.i(TAG, "Clase '${classroom.className}' actualizada exitosamente.")
                loadTeacherDataAndClasses() // Refrescar lista, lo que pondrá _uiState a Success si todo va bien.
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Error desconocido al actualizar la clase."
                _uiState.value = TeacherHomeResult.Error(errorMsg)
                Log.e(TAG, "Error al actualizar clase '${classroom.className}': $errorMsg")
            }
        }
    }

    /**
     * Elimina una clase de Firestore.
     * @param classroom El objeto [Classroom] a eliminar.
     */
    fun deleteClass(classroom: Classroom) {
        _deleteClassResult.value = TeacherHomeResult.Loading
        val currentTeacherId = firebaseAuth.currentUser?.uid

        if (currentTeacherId == null) {
            _deleteClassResult.value = TeacherHomeResult.Error("Usuario no autenticado.")
            Log.w(TAG, "deleteClass: Intento sin autenticación.")
            return
        }
        if (classroom.teacherId != currentTeacherId) {
            _deleteClassResult.value = TeacherHomeResult.Error("No tienes permiso para eliminar esta clase.")
            Log.w(TAG, "deleteClass: Intento de eliminación por usuario no propietario.")
            return
        }
        if (classroom.classId.isBlank()){
            _deleteClassResult.value = TeacherHomeResult.Error("ID de clase inválido para eliminar.")
            Log.w(TAG, "deleteClass: ID de clase vacío.")
            return
        }
        Log.d(TAG, "Eliminando clase: '${classroom.className}' (ID: ${classroom.classId})")

        viewModelScope.launch {
            val result = firestoreRepository.deleteClass(classroom.classId)
            if (result.isSuccess) {
                _deleteClassResult.value = TeacherHomeResult.Success(Unit)
                Log.i(TAG, "Clase '${classroom.className}' eliminada exitosamente.")
                loadTeacherDataAndClasses() // Refrescar la lista de clases
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Error desconocido al eliminar la clase."
                _deleteClassResult.value = TeacherHomeResult.Error(errorMsg)
                Log.e(TAG, "Error al eliminar clase '${classroom.className}': $errorMsg")
            }
        }
    }

    /**
     * Resetea el estado de [createClassResult] a Idle.
     * Llamar desde la UI después de manejar el resultado de la creación de clase.
     */
    fun resetCreateClassResult() {
        _createClassResult.value = TeacherHomeResult.Idle
    }

    /**
     * Resetea el estado de [deleteClassResult] a Idle.
     * Llamar desde la UI después de manejar el resultado de la eliminación de clase.
     */
    fun resetDeleteClassResult() {
        _deleteClassResult.value = TeacherHomeResult.Idle
    }
}