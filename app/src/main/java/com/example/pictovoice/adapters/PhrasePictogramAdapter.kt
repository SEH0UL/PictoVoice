package com.example.pictovoice.adapters // Asegúrate que el package es correcto

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
// import com.bumptech.glide.Glide // No necesitamos Glide si son recursos locales
import com.example.pictovoice.Data.Model.Pictogram
import com.example.pictovoice.R // Para el placeholder
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
            if (pictogram.imageResourceId != 0) { // 0 se considera un ID de recurso inválido
                binding.ivPhrasePictogramImage.setImageResource(pictogram.imageResourceId)
            } else if (pictogram.imageUrl != null && pictogram.imageUrl.isNotBlank()) {
                // Fallback a Glide si imageUrl existe y imageResourceId no (para un modelo híbrido)
                // Si ya no usas Glide en absoluto, puedes quitar esta parte.
                // Glide.with(binding.ivPhrasePictogramImage.context)
                //     .load(pictogram.imageUrl)
                //     .placeholder(R.drawable.ic_placeholder_image)
                //     .error(R.drawable.ic_error_image)
                //     .into(binding.ivPhrasePictogramImage)
                binding.ivPhrasePictogramImage.setImageResource(R.drawable.ic_placeholder_image) // O un placeholder si Glide no se usa
            }
            else {
                // Imagen por defecto si no hay imageResourceId ni imageUrl
                binding.ivPhrasePictogramImage.setImageResource(R.drawable.ic_placeholder_image) // Asegúrate de tener este drawable
            }
            // El nombre del pictograma no se muestra usualmente en la barra de frase, solo la imagen.
        }
    }

    class PhraseDiffCallback : DiffUtil.ItemCallback<Pictogram>() {
        override fun areItemsTheSame(oldItem: Pictogram, newItem: Pictogram): Boolean {
            // Usa un ID único. Si pictogramId es local y único, está bien.
            return oldItem.pictogramId == newItem.pictogramId
        }

        override fun areContentsTheSame(oldItem: Pictogram, newItem: Pictogram): Boolean {
            return oldItem == newItem // El data class compara todos los campos
        }
    }
}