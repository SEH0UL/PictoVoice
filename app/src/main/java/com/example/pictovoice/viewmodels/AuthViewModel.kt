package com.example.pictovoice.viewmodels // Sugerencia de paquete

import User
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pictovoice.Data.repository.AuthRepository
import com.example.pictovoice.Data.repository.FirestoreRepository
import com.google.firebase.auth.FirebaseAuth // Necesario para attemptAutoLogin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private const val TAG = "AuthViewModel" // Tag para Logs

/**
 * Sealed class para representar los estados de las operaciones de autenticación.
 * @param T El tipo de dato en caso de éxito.
 */
sealed class AuthResult<out T> {
    /** Estado inicial o inactivo. */
    object Idle : AuthResult<Nothing>()
    /** Estado de carga, operación en progreso. */
    object Loading : AuthResult<Nothing>()
    /** Estado de éxito, contiene los datos [data] de tipo [T]. */
    data class Success<out T>(val data: T) : AuthResult<T>()
    /** Estado de error, contiene un [message] descriptivo. */
    data class Error(val message: String) : AuthResult<Nothing>()

    /** Booleano que indica si el estado actual es [Loading]. */
    val isLoading get() = this is Loading

    /**
     * Obtiene el mensaje de error si el estado actual es [Error].
     * @return El mensaje de error como [String], o null si no es un estado de error.
     */
    fun getErrorMessage(): String? {
        return if (this is Error) this.message else null
    }
}

/**
 * ViewModel para gestionar la lógica de autenticación: inicio de sesión, registro,
 * cierre de sesión y restauración automática de sesión.
 * Interactúa con [AuthRepository] y [FirestoreRepository].
 *
 * @property authRepository Repositorio para las operaciones de autenticación con Firebase Auth.
 * @property firestoreRepository Repositorio para acceder a datos de Firestore (usado para cargar perfil de usuario).
 */
class AuthViewModel(
    private val authRepository: AuthRepository,
    // Instanciamos FirestoreRepository aquí para simplificar; considera inyección para mejor testeabilidad.
    private val firestoreRepository: FirestoreRepository = FirestoreRepository()
) : ViewModel() {

    private val _loginResult = MutableStateFlow<AuthResult<User>>(AuthResult.Idle)
    /**
     * [StateFlow] que emite el resultado de la operación de inicio de sesión manual.
     */
    val loginResult: StateFlow<AuthResult<User>> get() = _loginResult

    private val _registerResult = MutableStateFlow<AuthResult<String>>(AuthResult.Idle)
    /**
     * [StateFlow] que emite el resultado de la operación de registro.
     * En caso de éxito, `data` es el nombre de usuario [String] generado.
     */
    val registerResult: StateFlow<AuthResult<String>> get() = _registerResult

    private val _logoutEvent = MutableLiveData<Boolean>()
    /**
     * [LiveData] que emite `true` cuando se ha completado la operación de cierre de sesión.
     */
    val logoutEvent: LiveData<Boolean> get() = _logoutEvent

    private val _autoLoginResult = MutableLiveData<AuthResult<User>>(AuthResult.Idle)
    /**
     * [LiveData] que emite el resultado del intento de auto-login/restauración de sesión.
     */
    val autoLoginResult: LiveData<AuthResult<User>> get() = _autoLoginResult

    /**
     * Intenta realizar un auto-login si ya existe un usuario autenticado en Firebase.
     * Carga los datos del usuario desde Firestore y actualiza [_autoLoginResult].
     * Esta función debe ser llamada al iniciar la Activity principal (ej. [com.example.pictovoice.ui.auth.MainActivity]).
     */
    fun attemptAutoLogin() {
        Log.d(TAG, "Intentando auto-login...")
        if (authRepository.isUserLoggedIn()) {
            val firebaseUser = FirebaseAuth.getInstance().currentUser // Obtener el usuario actual de Firebase Auth
            if (firebaseUser?.uid != null) {
                Log.d(TAG, "Usuario logueado con Firebase Auth (UID: ${firebaseUser.uid}). Obteniendo datos de Firestore.")
                _autoLoginResult.value = AuthResult.Loading
                viewModelScope.launch {
                    val userResult = firestoreRepository.getUser(firebaseUser.uid)
                    if (userResult.isSuccess) {
                        val user = userResult.getOrNull()
                        if (user != null) {
                            _autoLoginResult.value = AuthResult.Success(user)
                            Log.i(TAG, "Auto-login: Datos de usuario obtenidos correctamente de Firestore para ${user.username}")
                        } else {
                            _autoLoginResult.value = AuthResult.Error("Usuario autenticado pero no encontrado en Firestore o datos corruptos.")
                            Log.w(TAG, "Auto-login: Usuario autenticado en Auth, pero datos de Firestore nulos o error de conversión.")
                            authRepository.logout() // Desloguear para evitar un estado inconsistente
                        }
                    } else {
                        val errorMsg = userResult.exceptionOrNull()?.message ?: "Error al cargar datos del usuario para auto-login."
                        _autoLoginResult.value = AuthResult.Error(errorMsg)
                        Log.e(TAG, "Auto-login: Fallo al obtener datos de Firestore: $errorMsg")
                        authRepository.logout() // Desloguear si no se pueden cargar los datos
                    }
                }
            } else {
                Log.w(TAG, "Auto-login: isUserLoggedIn era true, pero currentUser o UID de Firebase es nulo.")
                authRepository.logout() // Limpiar estado de Auth
                _autoLoginResult.value = AuthResult.Idle // No se pudo auto-loguear
            }
        } else {
            Log.d(TAG, "Auto-login: No hay usuario actualmente logueado en Firebase Auth.")
            _autoLoginResult.value = AuthResult.Idle // No hay sesión que restaurar
        }
    }

    /**
     * Llamado por la UI después de que el evento de [autoLoginResult] ha sido manejado
     * (por ejemplo, después de una navegación exitosa o de decidir mostrar la pantalla de login).
     * Resetea el [_autoLoginResult] a [AuthResult.Idle] para evitar acciones repetidas.
     */
    fun onAutoLoginEventHandled() {
        if (_autoLoginResult.value !is AuthResult.Idle) {
            _autoLoginResult.value = AuthResult.Idle
        }
    }

    /**
     * Inicia el proceso de login del usuario.
     * @param identifier Email o nombre de usuario.
     * @param password Contraseña.
     */
    fun login(identifier: String, password: String) {
        Log.d(TAG, "Intento de login manual para: $identifier")
        _loginResult.value = AuthResult.Loading
        viewModelScope.launch {
            val result = authRepository.login(identifier, password)
            if (result.isSuccess) {
                val user = result.getOrNull()
                if (user != null) {
                    _loginResult.value = AuthResult.Success(user)
                    Log.i(TAG, "Login manual exitoso para usuario: ${user.username}")
                } else {
                    _loginResult.value = AuthResult.Error("Error inesperado al obtener datos del usuario tras login.")
                    Log.e(TAG, "Login manual exitoso en Auth pero datos de usuario nulos.")
                }
            } else {
                val errorMessage = result.exceptionOrNull()?.message ?: "Error desconocido en login"
                _loginResult.value = AuthResult.Error(errorMessage)
                Log.w(TAG, "Login manual fallido: $errorMessage")
            }
        }
    }

    /**
     * Inicia el proceso de registro de un nuevo usuario.
     * @param fullName Nombre completo.
     * @param email Correo electrónico.
     * @param password Contraseña.
     * @param role Rol ("student" o "teacher").
     */
    fun register(fullName: String, email: String, password: String, role: String) {
        Log.d(TAG, "Intento de registro para: $email, Rol: $role")
        _registerResult.value = AuthResult.Loading
        viewModelScope.launch {
            val result = authRepository.register(fullName, email, password, role)
            if (result.isSuccess) {
                val username = result.getOrNull()
                if (username != null) {
                    _registerResult.value = AuthResult.Success(username)
                    Log.i(TAG, "Registro exitoso. Username generado: $username")
                } else {
                    _registerResult.value = AuthResult.Error("No se pudo obtener el username generado tras registro.")
                    Log.e(TAG, "Registro exitoso en Auth pero username nulo.")
                }
            } else {
                val errorMessage = result.exceptionOrNull()?.message ?: "Error desconocido en registro"
                _registerResult.value = AuthResult.Error(errorMessage)
                Log.w(TAG, "Registro fallido: $errorMessage")
            }
        }
    }

    /**
     * Realiza el cierre de sesión del usuario actual.
     */
    fun logoutUser() {
        Log.d(TAG, "Cerrando sesión del usuario...")
        authRepository.logout()
        _logoutEvent.value = true
    }

    /**
     * Llamado por la UI después de que el evento de [logoutEvent] ha sido manejado.
     * Resetea el evento a `false`.
     */
    fun onLogoutEventHandled() {
        if (_logoutEvent.value == true) {
            _logoutEvent.value = false
        }
    }
}