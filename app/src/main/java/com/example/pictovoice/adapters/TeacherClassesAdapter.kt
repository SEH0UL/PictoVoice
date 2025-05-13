package com.example.pictovoice.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.pictovoice.Data.Model.Classroom
import com.example.pictovoice.databinding.ItemTeacherClassBinding

class TeacherClassesAdapter(
    private val onAccessClick: (Classroom) -> Unit,
    private val onEditClick: (Classroom) -> Unit,
    private val onDeleteClick: (Classroom) -> Unit // Callback para el clic en eliminar
) : ListAdapter<Classroom, TeacherClassesAdapter.ViewHolder>(ClassroomDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTeacherClassBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val classroom = getItem(position)
        // Pasar todos los callbacks al ViewHolder
        holder.bind(classroom, onAccessClick, onEditClick, onDeleteClick)
    }

    inner class ViewHolder(private val binding: ItemTeacherClassBinding) :
        RecyclerView.ViewHolder(binding.root) {

        // El ViewHolder ahora también recibe onDeleteClick
        fun bind(
            classroom: Classroom,
            onAccessClick: (Classroom) -> Unit,
            onEditClick: (Classroom) -> Unit,
            onDeleteClick: (Classroom) -> Unit
        ) {
            binding.tvClassName.text = classroom.className

            binding.btnAccessClass.setOnClickListener {
                onAccessClick(classroom)
            }

            binding.btnEditClass.setOnClickListener {
                onEditClick(classroom)
            }

            // Configurar el listener para el botón de eliminar
            binding.btnDeleteClass.setOnClickListener { // Asumiendo que tienes btnDeleteClass en item_teacher_class.xml
                onDeleteClick(classroom)
            }
        }
    }

    class ClassroomDiffCallback : DiffUtil.ItemCallback<Classroom>() {
        override fun areItemsTheSame(oldItem: Classroom, newItem: Classroom): Boolean {
            return oldItem.classId == newItem.classId
        }

        override fun areContentsTheSame(oldItem: Classroom, newItem: Classroom): Boolean {
            return oldItem == newItem
        }
    }
}