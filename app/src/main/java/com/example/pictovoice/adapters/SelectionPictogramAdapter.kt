package com.example.pictovoice.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.pictovoice.R
import com.example.pictovoice.data.model.Pictogram // Asegúrate que el path a Pictogram es correcto
import com.example.pictovoice.databinding.ItemPictogramSelectionBinding

/**
 * Adaptador para mostrar una cuadrícula de pictogramas ([Pictogram]) seleccionables por el alumno,
 * típicamente para las categorías dinámicas en [com.example.pictovoice.ui.home.HomeActivity].
 *
 * Muestra la imagen del pictograma y maneja la selección del mismo.
 * Utiliza [ListAdapter] para un manejo eficiente de las actualizaciones de la lista.
 *
 * @param onItemClick Lambda que se ejecuta cuando un pictograma de la lista es seleccionado.
 * Recibe el objeto [Pictogram] seleccionado como parámetro.
 */
class SelectionPictogramAdapter(
    private val onItemClick: (Pictogram) -> Unit
) : ListAdapter<Pictogram, SelectionPictogramAdapter.ViewHolder>(SelectionDiffCallback()) {

    /**
     * Crea nuevas vistas (invocado por el layout manager).
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPictogramSelectionBinding.inflate(
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
        holder.bind(pictogram) // El listener de clic ya está en el ViewHolder init
    }

    /**
     * ViewHolder para cada item de pictograma en la cuadrícula de selección.
     * Muestra la imagen del pictograma.
     *
     * @param binding El ViewBinding para el layout del item (`item_pictogram_selection.xml`).
     */
    inner class ViewHolder(private val binding: ItemPictogramSelectionBinding) :
        RecyclerView.ViewHolder(binding.root) { // itemView es binding.root

        init {
            // Configurar el listener de clic para todo el item (la imagen del pictograma)
            binding.root.setOnClickListener {
                // Usamos layoutPosition por consistencia con tu adaptación anterior que funcionó.
                // bindingAdapterPosition es generalmente preferido por su robustez ante
                // actualizaciones del adaptador pendientes de reflejarse en el layout.
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
                binding.ivSelectionPictogramImage.setImageResource(pictogram.imageResourceId)
            } else {
                // Si no hay un imageResourceId válido, se muestra un placeholder.
                binding.ivSelectionPictogramImage.setImageResource(R.drawable.ic_placeholder_image)
            }
            // Según un comentario en tu código original, el layout item_pictogram_selection.xml
            // solo contiene el ImageView y no un TextView para el nombre del pictograma,
            // lo cual es común para este tipo de cuadrícula de selección.
        }
    }

    /**
     * Callback para calcular la diferencia entre dos listas de [Pictogram] para [ListAdapter].
     */
    private class SelectionDiffCallback : DiffUtil.ItemCallback<Pictogram>() {
        override fun areItemsTheSame(oldItem: Pictogram, newItem: Pictogram): Boolean {
            return oldItem.pictogramId == newItem.pictogramId
        }

        override fun areContentsTheSame(oldItem: Pictogram, newItem: Pictogram): Boolean {
            return oldItem == newItem
        }
    }
}