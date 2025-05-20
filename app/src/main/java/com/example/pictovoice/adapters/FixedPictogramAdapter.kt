package com.example.pictovoice.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.pictovoice.R
import com.example.pictovoice.data.model.Pictogram
import com.example.pictovoice.databinding.ItemPictogramFixedBinding

/**
 * Adaptador para mostrar una lista de pictogramas fijos ([Pictogram]) en un RecyclerView,
 * como los pronombres o verbos base en [com.example.pictovoice.ui.home.HomeActivity].
 *
 * Muestra principalmente la imagen del pictograma y maneja la selección del mismo.
 * Utiliza [ListAdapter] para un manejo eficiente de las actualizaciones de la lista.
 *
 * @param onItemClick Lambda que se ejecuta cuando un pictograma de la lista es seleccionado.
 * Recibe el objeto [Pictogram] seleccionado como parámetro.
 */
class FixedPictogramAdapter(
    private val onItemClick: (Pictogram) -> Unit
) : ListAdapter<Pictogram, FixedPictogramAdapter.ViewHolder>(FixedDiffCallback()) {

    /**
     * Crea nuevas vistas (invocado por el layout manager).
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPictogramFixedBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    /**
     * Reemplaza el contenido de una vista (invocado por el layout manager).
     * Obtiene el [Pictogram] en la [position] dada y vincula los datos al [ViewHolder].
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val pictogram = getItem(position)
        holder.bind(pictogram)
    }

    /**
     * ViewHolder para cada item de pictograma fijo.
     * Muestra la imagen del pictograma.
     *
     * @param binding El ViewBinding para el layout del item (`item_pictogram_fixed.xml`).
     */
    inner class ViewHolder(private val binding: ItemPictogramFixedBinding) :
        RecyclerView.ViewHolder(binding.root) { // itemView es binding.root

        init {
            // Configurar el listener de clic para todo el item (la imagen del pictograma)
            binding.root.setOnClickListener {
                val position = layoutPosition
                if (position != RecyclerView.NO_POSITION) {
                    val pictogram = getItem(position)
                    onItemClick(pictogram) // Llama a la lambda del adaptador
                }
            }
        }

        /**
         * Vincula los datos del [pictogram] a las vistas del item.
         * @param pictogram El objeto [Pictogram] a mostrar.
         */
        fun bind(pictogram: Pictogram) {
            if (pictogram.imageResourceId != 0) {
                binding.ivFixedPictogramImage.setImageResource(pictogram.imageResourceId)
            } else {
                // Si no hay un imageResourceId válido, se muestra un placeholder.
                binding.ivFixedPictogramImage.setImageResource(R.drawable.ic_placeholder_image)
            }
            // El layout item_pictogram_fixed.xml (asumido por ItemPictogramFixedBinding)
            // probablemente solo contiene el ImageView. Si tuviera un TextView para el nombre,
            // se establecería aquí: binding.tvPictogramName.text = pictogram.name
        }
    }

    /**
     * Callback para calcular la diferencia entre dos listas de [Pictogram] para [ListAdapter].
     */
    private class FixedDiffCallback : DiffUtil.ItemCallback<Pictogram>() {
        override fun areItemsTheSame(oldItem: Pictogram, newItem: Pictogram): Boolean {
            return oldItem.pictogramId == newItem.pictogramId
        }

        override fun areContentsTheSame(oldItem: Pictogram, newItem: Pictogram): Boolean {
            // El data class Pictogram (simplificado) ya implementa equals()
            // basado en todas sus propiedades relevantes para el contenido.
            return oldItem == newItem
        }
    }
}