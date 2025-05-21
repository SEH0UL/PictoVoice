package com.example.pictovoice.adapters // O com.example.pictovoice.ui.clasedetail según tu estructura final

import User
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.pictovoice.R
import com.example.pictovoice.databinding.ItemStudentInClassBinding
/**
 * Adaptador para mostrar una lista de alumnos ([User]) inscritos en una clase específica.
 * Utilizado en [com.example.pictovoice.ui.classroom.ClassDetailActivity].
 *
 * Permite acciones como eliminar un alumno de la clase y navegar al perfil del alumno.
 * También muestra un indicador de notificación si el alumno tiene solicitudes pendientes.
 *
 * Utiliza [ListAdapter] para un manejo eficiente de las actualizaciones de la lista.
 *
 * @param onRemoveStudentClick Lambda que se ejecuta cuando se pulsa el botón de eliminar alumno.
 * Recibe el [User] correspondiente.
 * @param onStudentClick Lambda que se ejecuta cuando se pulsa sobre el item de un alumno (para ver su perfil).
 * Recibe el [User] correspondiente.
 */
class ClassStudentListAdapter(
    private val onRemoveStudentClick: (User) -> Unit,
    private val onStudentClick: (User) -> Unit
) : ListAdapter<User, ClassStudentListAdapter.ViewHolder>(StudentDiffCallback()) {
    /**
     * Crea nuevas vistas (invocado por el layout manager).
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStudentInClassBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    /**
     * Reemplaza el contenido de una vista (invocado por el layout manager).
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val student = getItem(position)
        holder.bind(student)
    }

    /**
     * ViewHolder para cada item de alumno en la lista de la clase.
     * Muestra el nombre, nivel, imagen de perfil (placeholder) y un icono de notificación.
     *
     * @param binding El ViewBinding para el layout del item (`item_student_in_class.xml`).
     */
    inner class ViewHolder(private val binding: ItemStudentInClassBinding) :
        RecyclerView.ViewHolder(binding.root) { // itemView es binding.root

        init {
            binding.btnRemoveStudentFromClass.setOnClickListener {
                val position = layoutPosition
                if (position != RecyclerView.NO_POSITION) {
                    Log.d("AdapterClick", "Botón ELIMINAR pulsado para: ${getItem(position).fullName}")
                    onRemoveStudentClick(getItem(position))
                }
            }

            itemView.setOnClickListener {
                val position = layoutPosition
                if (position != RecyclerView.NO_POSITION) {
                    val student = getItem(position)
                    Log.d("AdapterClick", "itemView pulsado, llamando a onStudentClick para: ${student.fullName}") // Log para confirmar
                    onStudentClick(student) // Llama a la lambda del adaptador
                } else {
                    Log.w("AdapterClick", "itemView pulsado, pero la posición no es válida (NO_POSITION)")
                }
            }
        }

        /**
         * Vincula los datos del [student] a las vistas del item.
         * @param student El objeto [User] (alumno) a mostrar.
         */
        fun bind(student: User) {
            binding.tvStudentItemName.text = student.fullName
            // Es recomendable usar un recurso de string para textos formateados por localización.
            // Ejemplo: binding.tvStudentItemLevel.text = itemView.context.getString(R.string.label_level_prefix, student.currentLevel)
            // Donde en strings.xml tendrías: <string name="label_level_prefix">Nivel: %d</string>
            binding.tvStudentItemLevel.text = "Nivel: ${student.currentLevel}" // Mantenido como estaba si prefieres

            // TODO: Implementar carga de imagen de perfil real si 'profileImageUrl' está disponible.
            binding.ivStudentItemProfile.setImageResource(R.drawable.ic_default_profile)

            // Lógica para el icono de notificación basado en 'hasPendingWordRequest'
            if (student.hasPendingWordRequest) {
                binding.ivStudentItemNotification.setImageResource(R.drawable.ic_notifications) // Icono activo
                binding.ivStudentItemNotification.visibility = View.VISIBLE
                // Podrías añadir un contentDescription dinámico si es necesario para accesibilidad
                // binding.ivStudentItemNotification.contentDescription = itemView.context.getString(R.string.notification_pending_description)
            } else {
                // Opción actual: mostrar icono de "sin notificaciones"
                binding.ivStudentItemNotification.setImageResource(R.drawable.ic_notifications_none)
                binding.ivStudentItemNotification.visibility = View.VISIBLE
                // Alternativa: Ocultar el icono si no hay notificaciones
                // binding.ivStudentItemNotification.visibility = View.GONE
                // binding.ivStudentItemNotification.contentDescription = itemView.context.getString(R.string.notification_none_description)
            }
        }
    }

    /**
     * Callback para calcular la diferencia entre dos listas de [User] para [ListAdapter].
     */
    private class StudentDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.userId == newItem.userId
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            // El Data class User ya implementa equals() basado en todas sus propiedades,
            // lo que es correcto para determinar si el contenido ha cambiado.
            return oldItem == newItem
        }
    }
}