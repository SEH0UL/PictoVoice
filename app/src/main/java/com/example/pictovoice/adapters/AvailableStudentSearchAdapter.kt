package com.example.pictovoice.adapters

import User
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.pictovoice.R
import com.example.pictovoice.databinding.ItemAvailableStudentSearchBinding

/**
 * Adaptador para mostrar una lista de alumnos ([User]) disponibles en un RecyclerView.
 * @param onStudentSelected Lambda que se ejecuta cuando un alumno es seleccionado.
 */
class AvailableStudentSearchAdapter(
    private val onStudentSelected: (User) -> Unit
) : ListAdapter<User, AvailableStudentSearchAdapter.ViewHolder>(StudentSearchDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAvailableStudentSearchBinding.inflate(
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

    /**
     * ViewHolder para cada item de alumno.
     * @param binding El ViewBinding para el layout del item.
     */
    inner class ViewHolder(private val binding: ItemAvailableStudentSearchBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            itemView.setOnClickListener {
                val position = layoutPosition // Usando layoutPosition ya que bindingAdapterPosition da errores extraños
                if (position != RecyclerView.NO_POSITION) {
                    onStudentSelected(getItem(position))
                } else {
                    Log.w("AdapterClick", "Clicked item with NO_POSITION (using layoutPosition)")
                }
            }
        }

        fun bind(student: User) {
            binding.tvAvailableStudentName.text = student.fullName
            binding.tvAvailableStudentUsername.text = "@${student.username}"
            binding.ivAvailableStudentProfile.setImageResource(R.drawable.ic_default_profile)
        }
    }

    /**
     * Callback para calcular la diferencia entre dos listas de [User].
     */
    private class StudentSearchDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.userId == newItem.userId
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
}