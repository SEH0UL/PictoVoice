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
            // Listener para eliminar alumno
            binding.btnRemoveStudentFromClass.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onRemoveStudentClick(getItem(position))
                }
            }
            // Listener para el item completo (navegar al perfil del alumno)
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

            // Cargar imagen de perfil (ejemplo con placeholder, puedes usar Glide)
            // if (student.profileImageUrl.isNotBlank()) {
            //    Glide.with(binding.ivStudentItemProfile.context).load(student.profileImageUrl)
            //        .placeholder(R.drawable.ic_default_profile)
            //        .error(R.drawable.ic_default_profile)
            //        .circleCrop()
            //        .into(binding.ivStudentItemProfile)
            // } else {
            binding.ivStudentItemProfile.setImageResource(R.drawable.ic_default_profile)
            // }

            // Lógica para el indicador de notificación (ejemplo básico)
            // Deberás tener una forma de saber si el alumno tiene notificaciones pendientes.
            // val hasNotification = student.hasPendingNotification // Este campo no existe en tu User.kt actual
            val hasNotification = false // Placeholder, cámbialo según tu lógica de notificaciones
            if (hasNotification) {
                binding.ivStudentItemNotification.setImageResource(R.drawable.ic_notifications) // Icono de notificación activa
                binding.ivStudentItemNotification.visibility = View.VISIBLE
            } else {
                binding.ivStudentItemNotification.setImageResource(R.drawable.ic_notifications_none) // Icono de no notificaciones
                binding.ivStudentItemNotification.visibility = View.VISIBLE // O View.GONE si prefieres ocultarlo sin notificaciones
            }
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