package com.example.pictovoice.adapters // O com.example.pictovoice.ui.clasedetail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.pictovoice.Data.Model.User
import com.example.pictovoice.R // Para acceder a los drawables
import com.example.pictovoice.databinding.ItemStudentInClassBinding
// import com.bumptech.glide.Glide // Si usas imágenes de perfil desde URL

class ClassStudentListAdapter(
    private val onRemoveStudentClick: (User) -> Unit,
    private val onStudentClick: (User) -> Unit // Para navegar al perfil del alumno (futuro)
) : ListAdapter<User, ClassStudentListAdapter.ViewHolder>(StudentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStudentInClassBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val student = getItem(position)
        holder.bind(student)
    }

    inner class ViewHolder(private val binding: ItemStudentInClassBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.btnRemoveStudentFromClass.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onRemoveStudentClick(getItem(position))
                }
            }
            itemView.setOnClickListener{
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onStudentClick(getItem(position))
                }
            }
        }

        fun bind(student: User) {
            binding.tvStudentItemName.text = student.fullName
            binding.tvStudentItemLevel.text = "Nivel: ${student.currentLevel}"

            // Cargar imagen de perfil (tu lógica existente)
            // if (student.profileImageUrl.isNotBlank()) { ... } else { ... }
            binding.ivStudentItemProfile.setImageResource(R.drawable.ic_default_profile) // Placeholder actual

            // --- INICIO: Lógica actualizada para el icono de notificación ---
            if (student.hasPendingWordRequest) {
                binding.ivStudentItemNotification.setImageResource(R.drawable.ic_notifications) // Icono para notificación activa
                binding.ivStudentItemNotification.visibility = View.VISIBLE
                // Opcional: Cambiar el color del icono si es un vector drawable y quieres tintarlo
                // binding.ivStudentItemNotification.setColorFilter(ContextCompat.getColor(itemView.context, R.color.tu_color_notificacion_activa))
            } else {
                // Decide qué hacer si no hay notificación:
                // Opción 1: Ocultar el icono completamente
                // binding.ivStudentItemNotification.visibility = View.GONE

                // Opción 2: Mostrar un icono de "sin notificaciones" o hacerlo menos prominente
                binding.ivStudentItemNotification.setImageResource(R.drawable.ic_notifications_none) // Icono para no notificaciones
                binding.ivStudentItemNotification.visibility = View.VISIBLE // O View.INVISIBLE si prefieres mantener el espacio
                // Opcional: Cambiar el color para que sea más tenue
                // binding.ivStudentItemNotification.setColorFilter(ContextCompat.getColor(itemView.context, R.color.tu_color_notificacion_inactiva))
            }
            // --- FIN: Lógica actualizada para el icono de notificación ---
        }
    }

    class StudentDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.userId == newItem.userId
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem // El data class User compara todos los campos
        }
    }
}