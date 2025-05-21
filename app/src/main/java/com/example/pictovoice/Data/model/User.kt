import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import android.util.Log
import java.util.Locale

// Constante para la categoría inicial por defecto, usada si el campo en Firestore es nulo/no existe.
// Asegúrate de que coincida con la definición en PictogramDataSource.
private const val INITIAL_DEFAULT_CATEGORY_ID_FOR_USER = "local_comida"

/**
 * Representa un usuario de la aplicación, ya sea Alumno o Profesor.
 * Este modelo se mapea directamente a documentos en la colección "users" de Firestore.
 *
 * @property userId ID único del usuario (generalmente el UID de Firebase Authentication).
 * @property username Nombre de usuario único, generado y almacenado en mayúsculas.
 * @property fullName Nombre completo del usuario.
 * @property fullNameLowercase Versión normalizada y en minúsculas de [fullName], usada para búsquedas insensibles a mayúsculas/minúsculas.
 * @property email Correo electrónico del usuario.
 * @property role Rol del usuario ("student" o "teacher").
 * @property createdAt Timestamp de cuándo se creó la cuenta del usuario.
 * @property lastLogin Timestamp de la última vez que el usuario inició sesión.
 * @property profileImageUrl URL a la imagen de perfil del usuario (actualmente no implementada la subida/uso).
 * @property currentLevel Nivel actual del alumno, basado en la experiencia ganada.
 * @property currentExp Puntos de experiencia actuales dentro del nivel [currentLevel]. Se resetea (o se ajusta) al subir de nivel.
 * @property totalExp Puntos de experiencia totales acumulados por el alumno a lo largo del tiempo.
 * @property unlockedCategories Lista de IDs de [Category] que el alumno ha desbloqueado y a cuyas carpetas tiene acceso.
 * @property hasPendingWordRequest Booleano que indica si el alumno tiene una solicitud de "nuevas palabras/contenido" pendiente de aprobación por el profesor.
 * @property levelWordsRequestedFor El nivel más alto para el cual el alumno ya ha realizado una solicitud de palabras.
 * @property maxContentLevelApproved El nivel más alto de contenido (pictogramas) que el profesor ha aprobado explícitamente para este alumno.
 * @property wordsUsedCount Contador de la cantidad total de pictogramas que el alumno ha usado en frases reproducidas.
 * @property phrasesCreatedCount Contador del número total de frases que el alumno ha reproducido.
 */
data class User(
    val userId: String = "",
    val username: String = "",
    val fullName: String = "",
    val fullNameLowercase: String = "", // Para búsquedas
    val email: String = "",
    val role: String = "student",
    val createdAt: Timestamp = Timestamp.now(),
    val lastLogin: Timestamp = Timestamp.now(),
    val profileImageUrl: String = "", // Placeholder por ahora si no se usa activamente
    val currentLevel: Int = 1,
    val currentExp: Int = 0,
    val totalExp: Int = 0,
    val unlockedCategories: List<String> = listOf(INITIAL_DEFAULT_CATEGORY_ID_FOR_USER),
    val hasPendingWordRequest: Boolean = false,
    val levelWordsRequestedFor: Int = 0,
    val maxContentLevelApproved: Int = 1, // Nivel 1 de contenido aprobado por defecto
    val wordsUsedCount: Int = 0,
    val phrasesCreatedCount: Int = 0
) {
    /**
     * Convierte el objeto User a un Mapa para ser almacenado en Firestore.
     * Se excluye userId ya que suele ser el ID del documento.
     */
    fun toMap(): Map<String, Any?> = mapOf(
        // No incluimos userId aquí, ya que es el ID del documento en Firestore
        "username" to username,
        "fullName" to fullName,
        "fullNameLowercase" to fullNameLowercase,
        "email" to email,
        "role" to role,
        "createdAt" to createdAt,
        "lastLogin" to lastLogin,
        "profileImageUrl" to profileImageUrl,
        "currentLevel" to currentLevel,
        "currentExp" to currentExp,
        "totalExp" to totalExp,
        "unlockedCategories" to unlockedCategories,
        "hasPendingWordRequest" to hasPendingWordRequest,
        "levelWordsRequestedFor" to levelWordsRequestedFor,
        "maxContentLevelApproved" to maxContentLevelApproved,
        "wordsUsedCount" to wordsUsedCount,
        "phrasesCreatedCount" to phrasesCreatedCount
    )

    companion object {
        /**
         * Crea un objeto User a partir de un DocumentSnapshot de Firestore.
         * @param snapshot El DocumentSnapshot a convertir.
         * @return Un objeto User, o null si la conversión falla o el snapshot no contiene datos.
         */
        fun fromSnapshot(snapshot: DocumentSnapshot): User? {
            return try {
                val data = snapshot.data
                if (data == null) {
                    Log.w("User.fromSnapshot", "El snapshot con ID ${snapshot.id} no contiene datos.")
                    return null
                }

                val fullName = data["fullName"] as? String ?: ""
                User(
                    userId = snapshot.id,
                    username = data["username"] as? String ?: "",
                    fullName = fullName,
                    // Si fullNameLowercase no existe en documentos antiguos, lo generamos a partir de fullName.
                    // Es importante que los nuevos usuarios lo tengan poblado correctamente al registrarse.
                    fullNameLowercase = data["fullNameLowercase"] as? String ?: fullName.trim().replace("\\s+".toRegex(), " ").toLowerCase(Locale.ROOT),
                    email = data["email"] as? String ?: "",
                    role = data["role"] as? String ?: "student",
                    createdAt = data["createdAt"] as? Timestamp ?: Timestamp.now(),
                    lastLogin = data["lastLogin"] as? Timestamp ?: Timestamp.now(),
                    profileImageUrl = data["profileImageUrl"] as? String ?: "",
                    currentLevel = (data["currentLevel"] as? Long)?.toInt() ?: 1,
                    currentExp = (data["currentExp"] as? Long)?.toInt() ?: 0,
                    totalExp = (data["totalExp"] as? Long)?.toInt() ?: 0,
                    unlockedCategories = data["unlockedCategories"] as? List<String> ?: listOf(INITIAL_DEFAULT_CATEGORY_ID_FOR_USER),
                    hasPendingWordRequest = data["hasPendingWordRequest"] as? Boolean ?: false,
                    levelWordsRequestedFor = (data["levelWordsRequestedFor"] as? Long)?.toInt() ?: 0,
                    maxContentLevelApproved = (data["maxContentLevelApproved"] as? Long)?.toInt() ?: 1,
                    wordsUsedCount = (data["wordsUsedCount"] as? Long)?.toInt() ?: 0,
                    phrasesCreatedCount = (data["phrasesCreatedCount"] as? Long)?.toInt() ?: 0
                )
            } catch (e: Exception) {
                Log.e("User.fromSnapshot", "Error al convertir snapshot a User para ID ${snapshot.id}: ${e.message}", e)
                null
            }
        }
    }
}