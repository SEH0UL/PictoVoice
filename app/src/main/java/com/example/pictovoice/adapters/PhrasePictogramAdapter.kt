package com.example.pictovoice.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.pictovoice.R // Para el placeholder R.drawable.ic_placeholder_image
import com.example.pictovoice.Data.model.Pictogram // Asegúrate que el path a Pictogram es correcto
import com.example.pictovoice.databinding.ItemPictogramPhraseBinding

/**
 * Adaptador para mostrar la lista de pictogramas ([Pictogram]) que componen la frase
 * que el alumno está construyendo en [com.example.pictovoice.ui.home.HomeActivity].
 *
 * Este adaptador es principalmente para visualización y no maneja interacciones de clic en sus items.
 * Utiliza [ListAdapter] para un manejo eficiente de las actualizaciones de la lista.
 */
class PhrasePictogramAdapter :
    ListAdapter<Pictogram, PhrasePictogramAdapter.ViewHolder>(PhraseDiffCallback()) {

    /**
     * Crea nuevas vistas (invocado por el layout manager).
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPictogramPhraseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    /**
     * Reemplaza el contenido de una vista (invocado por el layout manager).
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val pictogram = getItem(position)
        holder.bind(pictogram)
    }

    /**
     * ViewHolder para cada pictograma en la barra de frase.
     * Muestra la imagen del pictograma.
     *
     * @param binding El ViewBinding para el layout del item (`item_pictogram_phrase.xml`).
     */
    inner class ViewHolder(private val binding: ItemPictogramPhraseBinding) :
        RecyclerView.ViewHolder(binding.root) { // itemView es binding.root

        /**
         * Vincula los datos del [pictogram] a la vista del item.
         * @param pictogram El objeto [Pictogram] a mostrar.
         */
        fun bind(pictogram: Pictogram) {
            if (pictogram.imageResourceId != 0) { // 0 se considera un ID de recurso inválido
                binding.ivPhrasePictogramImage.setImageResource(pictogram.imageResourceId)
            } else {
                // Si no hay imageResourceId, se muestra una imagen placeholder.
                // La lógica para pictogram.imageUrl se ha eliminado ya que el modelo Pictogram
                // fue simplificado para priorizar recursos locales (imageResourceId).
                binding.ivPhrasePictogramImage.setImageResource(R.drawable.ic_placeholder_image)
            }
            // El nombre del pictograma no se suele mostrar en esta barra, solo la imagen.
        }
    }

    /**
     * Callback para calcular la diferencia entre dos listas de [Pictogram] para [ListAdapter].
     */
    private class PhraseDiffCallback : DiffUtil.ItemCallback<Pictogram>() {
        override fun areItemsTheSame(oldItem: Pictogram, newItem: Pictogram): Boolean {
            // Asumimos que pictogramId es un identificador único y estable para cada pictograma.
            return oldItem.pictogramId == newItem.pictogramId
        }

        override fun areContentsTheSame(oldItem: Pictogram, newItem: Pictogram): Boolean {
            // El Data class Pictogram (simplificado) ya implementa equals()
            // basado en todas sus propiedades relevantes para el contenido visual.
            return oldItem == newItem
        }
    }
}