package com.example.pictovoice.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.pictovoice.data.model.Classroom // Asegúrate que el path a Classroom es correcto
import com.example.pictovoice.databinding.ItemTeacherClassBinding

/**
 * Adaptador para mostrar una lista de clases ([Classroom]) creadas por un profesor
 * en [com.example.pictovoice.ui.teacher.TeacherHomeActivity].
 *
 * Cada item de la lista proporciona opciones para acceder, editar o eliminar la clase.
 * Utiliza [ListAdapter] para un manejo eficiente de las actualizaciones de la lista.
 *
 * @param onAccessClick Lambda que se ejecuta cuando se pulsa el botón "Acceder" de una clase.
 * Recibe el objeto [Classroom] correspondiente.
 * @param onEditClick Lambda que se ejecuta cuando se pulsa el botón "Editar" de una clase.
 * Recibe el objeto [Classroom] correspondiente.
 * @param onDeleteClick Lambda que se ejecuta cuando se pulsa el botón "Eliminar" de una clase.
 * Recibe el objeto [Classroom] correspondiente.
 */
class TeacherClassesAdapter(
    private val onAccessClick: (Classroom) -> Unit,
    private val onEditClick: (Classroom) -> Unit,
    private val onDeleteClick: (Classroom) -> Unit
) : ListAdapter<Classroom, TeacherClassesAdapter.ViewHolder>(ClassroomDiffCallback()) {

    /**
     * Crea nuevas vistas (invocado por el layout manager).
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTeacherClassBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    /**
     * Reemplaza el contenido de una vista (invocado por el layout manager).
     * Obtiene la [Classroom] en la [position] dada y vincula los datos al [ViewHolder].
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val classroom = getItem(position)
        holder.bind(classroom) // Los listeners de clic ya están en el ViewHolder init
    }

    /**
     * ViewHolder para cada item de clase en la lista del profesor.
     * Muestra el nombre de la clase y gestiona los clics en los botones de acción.
     *
     * @param binding El ViewBinding para el layout del item (`item_teacher_class.xml`).
     */
    inner class ViewHolder(private val binding: ItemTeacherClassBinding) :
        RecyclerView.ViewHolder(binding.root) { // itemView es binding.root

        init {
            binding.btnAccessClass.setOnClickListener {
                val position = layoutPosition // Usamos layoutPosition por consistencia
                if (position != RecyclerView.NO_POSITION) {
                    onAccessClick(getItem(position))
                }
            }

            binding.btnEditClass.setOnClickListener {
                val position = layoutPosition
                if (position != RecyclerView.NO_POSITION) {
                    onEditClick(getItem(position))
                }
            }

            binding.btnDeleteClass.setOnClickListener {
                val position = layoutPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDeleteClick(getItem(position))
                }
            }
        }

        /**
         * Vincula los datos de la [classroom] a las vistas del item.
         * @param classroom El objeto [Classroom] a mostrar.
         */
        fun bind(classroom: Classroom) {
            binding.tvClassName.text = classroom.className
            // El layout item_teacher_class.xml contiene los botones, cuyos listeners
            // ya están configurados en el bloque init del ViewHolder.
        }
    }

    /**
     * Callback para calcular la diferencia entre dos listas de [Classroom] para [ListAdapter].
     */
    private class ClassroomDiffCallback : DiffUtil.ItemCallback<Classroom>() {
        override fun areItemsTheSame(oldItem: Classroom, newItem: Classroom): Boolean {
            return oldItem.classId == newItem.classId
        }

        override fun areContentsTheSame(oldItem: Classroom, newItem: Classroom): Boolean {
            // El data class Classroom ya implementa equals() basado en todas sus propiedades.
            return oldItem == newItem
        }
    }
}