package com.example.pictovoice.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.pictovoice.Data.Model.Pictogram
import com.example.pictovoice.R
import com.example.pictovoice.databinding.ItemPictogramFixedBinding // IMPORTANTE: Usar el nuevo binding

class FixedPictogramAdapter(
    private val onItemClick: (Pictogram) -> Unit
) : ListAdapter<Pictogram, FixedPictogramAdapter.ViewHolder>(FixedDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPictogramFixedBinding.inflate( // Usar el nuevo binding
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

    inner class ViewHolder(private val binding: ItemPictogramFixedBinding) : // Usar el nuevo binding
        RecyclerView.ViewHolder(binding.root) {

        fun bind(pictogram: Pictogram, onItemClick: (Pictogram) -> Unit) {
            if (pictogram.imageResourceId != 0) {
                binding.ivFixedPictogramImage.setImageResource(pictogram.imageResourceId) // Usar el ID del ImageView del nuevo layout
            } else {
                binding.ivFixedPictogramImage.setImageResource(R.drawable.ic_placeholder_image)
            }
            binding.root.setOnClickListener {
                onItemClick(pictogram)
            }
        }
    }

    class FixedDiffCallback : DiffUtil.ItemCallback<Pictogram>() { // Puede ser el mismo DiffUtil si la comparación es igual
        override fun areItemsTheSame(oldItem: Pictogram, newItem: Pictogram): Boolean {
            return oldItem.pictogramId == newItem.pictogramId
        }

        override fun areContentsTheSame(oldItem: Pictogram, newItem: Pictogram): Boolean {
            return oldItem == newItem
        }
    }
}