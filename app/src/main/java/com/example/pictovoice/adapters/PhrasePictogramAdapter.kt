package com.example.pictovoice.adapters // Asegúrate que el package es correcto

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.pictovoice.Data.Model.Pictogram
import com.example.pictovoice.R // Para el placeholder si lo tienes
import com.example.pictovoice.databinding.ItemPictogramPhraseBinding

class PhrasePictogramAdapter :
    ListAdapter<Pictogram, PhrasePictogramAdapter.ViewHolder>(PhraseDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPictogramPhraseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val pictogram = getItem(position)
        holder.bind(pictogram)
    }

    inner class ViewHolder(private val binding: ItemPictogramPhraseBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(pictogram: Pictogram) {
            if (pictogram.imageUrl.isNotBlank()) {
                Glide.with(binding.ivPhrasePictogramImage.context)
                    .load(pictogram.imageUrl)
                    .placeholder(R.drawable.ic_placeholder_image) // Crea un drawable placeholder
                    .error(R.drawable.ic_error_image) // Crea un drawable para errores
                    .into(binding.ivPhrasePictogramImage)
            } else {
                // Opcional: poner una imagen por defecto si no hay URL
                binding.ivPhrasePictogramImage.setImageResource(R.drawable.ic_placeholder_image)
            }
            // No se necesita texto ya que la imagen lo incluye
        }
    }

    class PhraseDiffCallback : DiffUtil.ItemCallback<Pictogram>() {
        override fun areItemsTheSame(oldItem: Pictogram, newItem: Pictogram): Boolean {
            return oldItem.pictogramId == newItem.pictogramId
        }

        override fun areContentsTheSame(oldItem: Pictogram, newItem: Pictogram): Boolean {
            return oldItem == newItem // O compara campos específicos si es necesario
        }
    }
}