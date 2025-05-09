package com.example.pictovoice.adapters // Asegúrate que el package es correcto

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.pictovoice.Data.Model.Category // Importa tu modelo Category
import com.example.pictovoice.databinding.ItemCategoryFolderBinding
// import com.bumptech.glide.Glide // Si tus categorías tienen iconos para cargar con Glide

class CategoryAdapter(
    private val onCategoryClick: (Category) -> Unit
) : ListAdapter<Category, CategoryAdapter.ViewHolder>(CategoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategoryFolderBinding.inflate(
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
            // Si las categorías tuvieran iconos que cargar:
            // if (category.iconUrl != null) {
            //     Glide.with(binding.btnCategoryFolder.context) // o un ImageView dedicado en el item
            //         .load(category.iconUrl)
            //         .into(binding.btnCategoryFolder) // necesitarías un ImageView
            // }
            binding.btnCategoryFolder.setOnClickListener {
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