package com.example.pictovoice.adapters // Asegúrate que el package es correcto

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.pictovoice.Data.Model.Category // Importa tu modelo Category
import com.example.pictovoice.databinding.ItemCategoryFolderBinding
// import com.bumptech.glide.Glide // Si tus categorías tienen iconos URL

class CategoryAdapter(
    private val onCategoryClick: (Category) -> Unit
) : ListAdapter<Category, CategoryAdapter.ViewHolder>(CategoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategoryFolderBinding.inflate( // Asegúrate que item_category_folder.xml existe
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = getItem(position)
        holder.bind(category, onCategoryClick)
    }

    inner class ViewHolder(private val binding: ItemCategoryFolderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(category: Category, onCategoryClick: (Category) -> Unit) {
            binding.btnCategoryFolder.text = category.name

            // Comenta o elimina el listener en binding.root si lo tenías así:
            // binding.root.setOnClickListener { /* ... */ }

            // Y prueba a poner el listener directamente en el botón:
            binding.btnCategoryFolder.setOnClickListener {
                Log.d("CategoryAdapter", "btnCategoryFolder clicked for: ${category.name}") // Nuevo log para verificar
                onCategoryClick(category)
            }
        }
    }

    class CategoryDiffCallback : DiffUtil.ItemCallback<Category>() {
        override fun areItemsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem.categoryId == newItem.categoryId
        }

        override fun areContentsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem == newItem
        }
    }
}