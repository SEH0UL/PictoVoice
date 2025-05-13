package com.example.pictovoice.adapters // O ui.clasedetail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.pictovoice.Data.Model.User
import com.example.pictovoice.R
import com.example.pictovoice.databinding.ItemAvailableStudentSearchBinding
// import com.bumptech.glide.Glide

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

    inner class ViewHolder(private val binding: ItemAvailableStudentSearchBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onStudentSelected(getItem(position))
                }
            }
        }

        fun bind(student: User) {
            binding.tvAvailableStudentName.text = student.fullName
            binding.tvAvailableStudentUsername.text = "@${student.username}" // Añadir @ para distinguirlo

            // Cargar imagen de perfil (ejemplo con placeholder)
            // if (student.profileImageUrl.isNotBlank()) {
            //    Glide.with(binding.ivAvailableStudentProfile.context).load(student.profileImageUrl)
            //        .placeholder(R.drawable.ic_default_profile)
            //        .circleCrop()
            //        .into(binding.ivAvailableStudentProfile)
            // } else {
            binding.ivAvailableStudentProfile.setImageResource(R.drawable.ic_default_profile)
            // }
        }
    }

    class StudentSearchDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.userId == newItem.userId
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
}