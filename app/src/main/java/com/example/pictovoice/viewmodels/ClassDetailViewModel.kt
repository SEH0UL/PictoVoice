package com.example.pictovoice.viewmodels // Sugerencia: Mover a este paquete si estaba en ui.classroom

import User
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.pictovoice.Data.repository.FirestoreRepository
import com.example.pictovoice.data.model.Classroom
import kotlinx.coroutines.launch
import java.util.Locale

private const val TAG = "ClassDetailVM" // Tag para Logs

/**
 * Sealed class para representar los diferentes estados y resultados de las operaciones
 * en la pantalla de detalle de la clase ([com.example.pictovoice.ui.classroom.ClassDetailActivity]).
 * @param T El tipo de dato en caso de éxito.
 */
sealed class ClassDetailResult<out T> {
    /** Estado inicial o inactivo. */
    object Idle : ClassDetailResult<Nothing>()
    /** Estado de carga, operación en progreso. */
    object Loading : ClassDetailResult<Nothing>()
    /** Estado de éxito, contiene los datos [data] de tipo [T]. */
    data class Success<out T>(val data: T) : ClassDetailResult<T>()
    /** Estado de error, contiene un [message] descriptivo. */
    data class Error(val message: String) : ClassDetailResult<Nothing>()
}

/**
 * ViewModel para [com.example.pictovoice.ui.classroom.ClassDetailActivity].
 * Gestiona la lógica para mostrar los detalles de una clase, listar sus alumnos,
 * permitir la búsqueda de alumnos dentro de la clase, y añadir o eliminar alumnos de la misma.
 *
 * @param application La instancia de la aplicación.
 * @param classId El ID de la clase cuyos detalles se van a gestionar.
 */
class ClassDetailViewModel(
    application: Application,
    private val classId: String // ID de la clase actual, pasado por la Factory
) : AndroidViewModel(application) {

    private val firestoreRepository = FirestoreRepository()

    private val _classroomDetails = MutableLiveData<Classroom?>()
    /** [LiveData] que contiene los detalles del objeto [Classroom] actual. */
    val classroomDetails: LiveData<Classroom?> get() = _classroomDetails

    // Lista interna de todos los alumnos cargados para la clase actual (sin filtrar)
    private val _studentsInClass = MutableLiveData<List<User>>(emptyList())
    // val studentsInClass: LiveData<List<User>> get() = _studentsInClass // No es necesario exponerla si solo se usa internamente

    private val _filteredStudentsInClass = MutableLiveData<List<User>>(emptyList())
    /** [LiveData] que contiene la lista de [User] (alumnos) de la clase, filtrada según el término de búsqueda. */
    val filteredStudentsInClass: LiveData<List<User>> get() = _filteredStudentsInClass

    private val _uiState = MutableLiveData<ClassDetailResult<Any>>(ClassDetailResult.Idle)
    /** [LiveData] que representa el estado general de la UI durante la carga inicial de datos. */
    val uiState: LiveData<ClassDetailResult<Any>> get() = _uiState

    private val _removeStudentResult = MutableLiveData<ClassDetailResult<Unit>>(ClassDetailResult.Idle)
    /** [LiveData] que emite el resultado de la operación de eliminar un alumno de la clase. */
    val removeStudentResult: LiveData<ClassDetailResult<Unit>> get() = _removeStudentResult

    // Para la búsqueda de alumnos disponibles para añadir a la clase
    private val _searchStudentsState = MutableLiveData<ClassDetailResult<List<User>>>(ClassDetailResult.Idle)
    /**
     * [LiveData] que representa el estado y el resultado de la búsqueda de alumnos
     * (que no están ya en la clase) para ser añadidos.
     * En caso de [ClassDetailResult.Success], `data` contiene la lista de [User] encontrados.
     */
    val searchStudentsState: LiveData<ClassDetailResult<List<User>>> get() = _searchStudentsState

    private val _addStudentToClassResult = MutableLiveData<ClassDetailResult<Unit>>(ClassDetailResult.Idle)
    /** [LiveData] que emite el resultado de la operación de añadir un alumno a la clase. */
    val addStudentToClassResult: LiveData<ClassDetailResult<Unit>> get() = _addStudentToClassResult

    init {
        if (classId.isNotBlank()) {
            Log.d(TAG, "ClassDetailViewModel inicializado para classId: $classId")
            loadClassDetailsAndStudents()
        } else {
            _uiState.value = ClassDetailResult.Error("ID de clase no proporcionado o inválido.")
            Log.e(TAG, "Error: classId está vacío en el constructor del ViewModel.")
        }
    }

    /**
     * Carga los detalles de la clase actual y la lista de alumnos inscritos en ella.
     * Actualiza [_classroomDetails], [_studentsInClass], [_filteredStudentsInClass] y [_uiState].
     */
    fun loadClassDetailsAndStudents() {
        if (classId.isBlank()) { // Doble chequeo por si se llama externamente con un ID inválido
            _uiState.value = ClassDetailResult.Error("No se puede cargar: ID de clase inválido.")
            return
        }
        _uiState.value = ClassDetailResult.Loading
        Log.d(TAG, "Cargando detalles y alumnos para la clase ID: $classId")
        viewModelScope.launch {
            val classResult = firestoreRepository.getClassDetails(classId)
            if (classResult.isSuccess) {
                val classroom = classResult.getOrNull()
                _classroomDetails.value = classroom
                if (classroom != null) {
                    Log.d(TAG, "Detalles de la clase '${classroom.className}' cargados.")
                    if (classroom.studentIds.isNotEmpty()) {
                        val studentsResult = firestoreRepository.getUsersByIds(classroom.studentIds)
                        if (studentsResult.isSuccess) {
                            val students = studentsResult.getOrNull() ?: emptyList()
                            _studentsInClass.value = students
                            _filteredStudentsInClass.value = students // Inicializar lista filtrada
                            _uiState.value = ClassDetailResult.Success(students) // Podría ser Success(Unit) si el foco es solo el estado
                            Log.d(TAG, "${students.size} alumnos cargados para la clase.")
                        } else {
                            handleStudentLoadError(studentsResult.exceptionOrNull())
                        }
                    } else { // No hay studentIds en la clase
                        _studentsInClass.value = emptyList()
                        _filteredStudentsInClass.value = emptyList()
                        _uiState.value = ClassDetailResult.Success(emptyList<User>()) // Éxito, pero lista de alumnos vacía
                        Log.d(TAG, "La clase '${classroom.className}' no tiene alumnos asignados.")
                    }
                } else { // classroom es null, la clase no se encontró
                    _uiState.value = ClassDetailResult.Error("No se encontraron detalles para la clase ID: $classId")
                    Log.w(TAG, "No se encontró la clase con ID: $classId")
                    clearStudentLists()
                }
            } else { // Falló la carga de detalles de la clase
                _uiState.value = ClassDetailResult.Error(classResult.exceptionOrNull()?.message ?: "Error cargando detalles de la clase.")
                Log.e(TAG, "Error al cargar detalles de la clase $classId: ${classResult.exceptionOrNull()?.message}")
                clearStudentLists()
            }
        }
    }

    private fun handleStudentLoadError(exception: Throwable?) {
        _studentsInClass.value = emptyList()
        _filteredStudentsInClass.value = emptyList()
        val errorMsg = exception?.message ?: "Error desconocido cargando alumnos."
        _uiState.value = ClassDetailResult.Error(errorMsg)
        Log.e(TAG, "Error al cargar alumnos de la clase: $errorMsg")
    }

    private fun clearStudentLists() {
        _studentsInClass.value = emptyList()
        _filteredStudentsInClass.value = emptyList()
    }

    /**
     * Filtra la lista de alumnos mostrada en la UI (_filteredStudentsInClass)
     * basándose en el [query] proporcionado. La búsqueda se hace sobre `fullName` y `username`.
     * Si el query es nulo o vacío, se muestra la lista completa de alumnos de la clase.
     *
     * @param query El término de búsqueda.
     */
    fun filterStudents(query: String?) {
        val currentFullList = _studentsInClass.value ?: emptyList()
        if (query.isNullOrBlank()) {
            _filteredStudentsInClass.value = currentFullList
            Log.d(TAG, "Filtro de alumnos limpiado. Mostrando ${currentFullList.size} alumnos.")
        } else {
            val lowercaseQuery = query.toLowerCase(Locale.ROOT).trim()
            val filtered = currentFullList.filter { student ->
                student.fullName.toLowerCase(Locale.ROOT).contains(lowercaseQuery) ||
                        student.username.toLowerCase(Locale.ROOT).contains(lowercaseQuery)
            }
            _filteredStudentsInClass.value = filtered
            Log.d(TAG, "Alumnos filtrados por '$lowercaseQuery'. Resultados: ${filtered.size}")
        }
    }

    /**
     * Elimina un alumno de la clase actual en Firestore.
     * Tras una eliminación exitosa, recarga los detalles de la clase y la lista de alumnos.
     * @param studentId El ID del alumno a eliminar.
     */
    fun removeStudentFromCurrentClass(studentId: String) {
        _removeStudentResult.value = ClassDetailResult.Loading
        if (classId.isBlank() || studentId.isBlank()) {
            val errorMsg = "ID de clase o alumno inválido para eliminar."
            _removeStudentResult.value = ClassDetailResult.Error(errorMsg)
            Log.w(TAG, "removeStudentFromCurrentClass: $errorMsg")
            return
        }
        Log.d(TAG, "Intentando eliminar alumno $studentId de la clase $classId")
        viewModelScope.launch {
            val result = firestoreRepository.removeStudentFromClassroom(classId, studentId)
            if (result.isSuccess) {
                _removeStudentResult.value = ClassDetailResult.Success(Unit)
                Log.i(TAG, "Alumno $studentId eliminado exitosamente de la clase $classId.")
                loadClassDetailsAndStudents() // Recargar para reflejar el cambio
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Error desconocido al eliminar alumno."
                _removeStudentResult.value = ClassDetailResult.Error(errorMsg)
                Log.e(TAG, "Error al eliminar alumno $studentId: $errorMsg")
            }
        }
    }

    /**
     * Resetea el estado de [removeStudentResult] a Idle.
     * Llamar desde la UI después de manejar el resultado.
     */
    fun resetRemoveStudentResult() {
        _removeStudentResult.value = ClassDetailResult.Idle
    }

    /**
     * Busca alumnos disponibles (que no estén ya en la clase actual) por su nombre o username.
     * Utiliza la búsqueda insensible a mayúsculas/minúsculas del repositorio.
     * Actualiza [_searchStudentsState] con el resultado.
     * @param nameQuery El término de búsqueda.
     */
    fun searchAvailableStudents(nameQuery: String) {
        if (nameQuery.isBlank()) {
            _searchStudentsState.value = ClassDetailResult.Idle // No buscar si el query está vacío
            Log.d(TAG, "Búsqueda de alumnos disponibles: query vacío, estado Idle.")
            return
        }
        Log.d(TAG, "Buscando alumnos disponibles con query: '$nameQuery'")
        _searchStudentsState.value = ClassDetailResult.Loading
        viewModelScope.launch {
            val result = firestoreRepository.searchStudentsByName(nameQuery) // Esta búsqueda ya es insensible
            if (result.isSuccess) {
                val foundStudents = result.getOrNull() ?: emptyList()
                val currentStudentIdsInClass = _studentsInClass.value?.map { it.userId } ?: emptyList()
                val availableStudents = foundStudents.filterNot { studentToFilter ->
                    currentStudentIdsInClass.contains(studentToFilter.userId)
                }
                _searchStudentsState.value = ClassDetailResult.Success(availableStudents)
                Log.d(TAG, "Búsqueda de alumnos disponibles encontró ${availableStudents.size} alumnos no en clase.")
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Error desconocido buscando alumnos."
                _searchStudentsState.value = ClassDetailResult.Error(errorMsg)
                Log.e(TAG, "Error en búsqueda de alumnos disponibles: $errorMsg")
            }
        }
    }

    /**
     * Resetea el estado de [_searchStudentsState] a Idle.
     * Útil para limpiar los resultados de búsqueda cuando el diálogo se cierra o el query se vacía.
     */
    fun resetSearchStudentsState() {
        _searchStudentsState.value = ClassDetailResult.Idle
        Log.d(TAG, "Estado de búsqueda de alumnos reseteado a Idle.")
    }

    /**
     * Añade un alumno a la clase actual en Firestore.
     * Tras un añadido exitoso, recarga los detalles de la clase y la lista de alumnos.
     * @param studentId El ID del alumno a añadir.
     */
    fun addStudentToCurrentClass(studentId: String) {
        _addStudentToClassResult.value = ClassDetailResult.Loading
        if (classId.isBlank() || studentId.isBlank()){
            val errorMsg = "ID de clase o alumno inválido para añadir."
            _addStudentToClassResult.value = ClassDetailResult.Error(errorMsg)
            Log.w(TAG, "addStudentToCurrentClass: $errorMsg")
            return
        }

        val isAlreadyInClass = _studentsInClass.value?.any { it.userId == studentId } ?: false
        if (isAlreadyInClass) {
            val errorMsg = "Este alumno ya está en la clase."
            _addStudentToClassResult.value = ClassDetailResult.Error(errorMsg)
            Log.w(TAG, "addStudentToCurrentClass: $errorMsg (Alumno $studentId ya en clase $classId)")
            return
        }
        Log.d(TAG, "Intentando añadir alumno $studentId a la clase $classId")

        viewModelScope.launch {
            val result = firestoreRepository.addStudentToClass(classId, studentId)
            if (result.isSuccess) {
                _addStudentToClassResult.value = ClassDetailResult.Success(Unit)
                Log.i(TAG, "Alumno $studentId añadido exitosamente a la clase $classId.")
                loadClassDetailsAndStudents() // Recargar para reflejar el nuevo alumno
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Error desconocido al añadir alumno."
                _addStudentToClassResult.value = ClassDetailResult.Error(errorMsg)
                Log.e(TAG, "Error al añadir alumno $studentId a clase $classId: $errorMsg")
            }
        }
    }

    /**
     * Resetea el estado de [addStudentToClassResult] a Idle.
     * Llamar desde la UI después de manejar el resultado.
     */
    fun resetAddStudentToClassResult() {
        _addStudentToClassResult.value = ClassDetailResult.Idle
    }
}