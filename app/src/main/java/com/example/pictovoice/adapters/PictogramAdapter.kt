package com.example.pictovoice.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pictovoice.Data.Model.Pictogram
import com.example.pictovoice.databinding.ItemPictogramBinding

class PictogramAdapter(
    private val pictograms: List<Pictogram>,
    private val onItemClick: (Pictogram) -> Unit
) : RecyclerView.Adapter<PictogramAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemPictogramBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPictogramBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val pictogram = pictograms[position]
        holder.binding.tvPictogramName.text = pictogram.name
        // Aquí usarías Glide/Picasso para cargar imageUrl en un ImageView
        holder.itemView.setOnClickListener { onItemClick(pictogram) }
    }

    override fun getItemCount() = pictograms.size
}