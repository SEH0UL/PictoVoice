package com.example.pictovoice.adapters // Asegúrate que el package es correcto

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
            // Asegúrate que en tu res/layout/item_category_folder.xml existe un Button o TextView
            // con android:id="@+id/btnCategoryFolder"
            binding.btnCategoryFolder.text = category.name

            // Si las categorías tuvieran iconos locales o URL:
            // if (category.iconResourceId != 0) {
            //    binding.ivCategoryIcon.setImageResource(category.iconResourceId) // Necesitarías un ImageView ivCategoryIcon
            // } else if (category.iconUrl != null) {
            //     Glide.with(binding.ivCategoryIcon.context)
            //         .load(category.iconUrl)
            //         .into(binding.ivCategoryIcon)
            // }

            // El click listener puede ser en el root del item o en el botón específico
            binding.root.setOnClickListener { // o binding.btnCategoryFolder.setOnClickListener
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