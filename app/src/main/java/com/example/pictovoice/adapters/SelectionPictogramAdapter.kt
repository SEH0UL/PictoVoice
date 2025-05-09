package com.example.pictovoice.adapters // Asegúrate que el package es correcto

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.pictovoice.Data.Model.Pictogram
import com.example.pictovoice.R
import com.example.pictovoice.databinding.ItemPictogramSelectionBinding

class SelectionPictogramAdapter(
    private val onItemClick: (Pictogram) -> Unit
) : ListAdapter<Pictogram, SelectionPictogramAdapter.ViewHolder>(SelectionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPictogramSelectionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val pictogram = getItem(position)
        holder.bind(pictogram, onItemClick)
    }

    inner class ViewHolder(private val binding: ItemPictogramSelectionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(pictogram: Pictogram, onItemClick: (Pictogram) -> Unit) {
            if (pictogram.imageUrl.isNotBlank()) {
                Glide.with(binding.ivSelectionPictogramImage.context)
                    .load(pictogram.imageUrl)
                    .placeholder(R.drawable.ic_placeholder_image)
                    .error(R.drawable.ic_error_image)
                    .into(binding.ivSelectionPictogramImage)
            } else {
                binding.ivSelectionPictogramImage.setImageResource(R.drawable.ic_placeholder_image)
            }
            // El texto está en la imagen, no se setea aquí.
            binding.root.setOnClickListener {
                onItemClick(pictogram)
            }
        }
    }

    class SelectionDiffCallback : DiffUtil.ItemCallback<Pictogram>() {
        override fun areItemsTheSame(oldItem: Pictogram, newItem: Pictogram): Boolean {
            return oldItem.pictogramId == newItem.pictogramId
        }

        override fun areContentsTheSame(oldItem: Pictogram, newItem: Pictogram): Boolean {
            return oldItem == newItem
        }
    }
}