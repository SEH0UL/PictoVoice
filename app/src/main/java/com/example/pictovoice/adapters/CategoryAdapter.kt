package com.example.pictovoice.adapters // Asegúrate que este es el paquete correcto

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.pictovoice.Data.model.Category
import com.example.pictovoice.databinding.ItemCategoryFolderBinding


/**
 * Adaptador para mostrar una lista de carpetas de categorías ([Category]) en un RecyclerView.
 * Se utiliza en [com.example.pictovoice.ui.home.HomeActivity] para la navegación entre
 * categorías de pictogramas dinámicas.
 *
 * Utiliza [ListAdapter] para un manejo eficiente de las actualizaciones de la lista.
 *
 * @param onCategoryClick Lambda que se ejecuta cuando una carpeta de categoría es seleccionada.
 * Recibe el objeto [Category] seleccionado como parámetro.
 */
class CategoryAdapter(
    private val onCategoryClick: (Category) -> Unit
) : ListAdapter<Category, CategoryAdapter.ViewHolder>(CategoryDiffCallback()) {

    /**
     * Crea nuevas vistas (invocado por el layout manager).
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategoryFolderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    /**
     * Reemplaza el contenido de una vista (invocado por el layout manager).
     * Obtiene la [Category] en la [position] dada y vincula los datos al [ViewHolder].
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = getItem(position)
        holder.bind(category) // Ya no se pasa onCategoryClick aquí
    }

    /**
     * ViewHolder para cada item de categoría (carpeta).
     * Muestra el nombre de la categoría en un botón.
     *
     * @param binding El ViewBinding para el layout del item (`item_category_folder.xml`).
     */
    inner class ViewHolder(private val binding: ItemCategoryFolderBinding) :
        RecyclerView.ViewHolder(binding.root) { // itemView es binding.root

        init {
            // Configurar el listener de clic en el botón (o en itemView si se prefiere para toda la carpeta)
            binding.btnCategoryFolder.setOnClickListener {
                val position = layoutPosition // Usar bindingAdapterPosition o layoutPosition
                if (position != RecyclerView.NO_POSITION) {
                    val category = getItem(position)
                    // Log.d("CategoryAdapter", "Carpeta de categoría pulsada: ${category.name}") // Log eliminado para versión final
                    onCategoryClick(category) // Llama directamente a la lambda del adaptador
                }
            }
            // Si el clic debe ser en toda la carpeta y no solo el botón:
            // itemView.setOnClickListener { ... }
        }

        /**
         * Vincula los datos de la [category] a las vistas del item.
         * @param category El objeto [Category] a mostrar.
         */
        fun bind(category: Category) {
            binding.btnCategoryFolder.text = category.name
            // Si tuvieras un ImageView en item_category_folder.xml para el icono:
            // if (category.iconResourceId != 0) {
            //    binding.ivCategoryFolderIcon.setImageResource(category.iconResourceId)
            //    binding.ivCategoryFolderIcon.visibility = View.VISIBLE
            // } else {
            //    binding.ivCategoryFolderIcon.visibility = View.GONE // O un placeholder
            // }
        }
    }

    /**
     * Callback para calcular la diferencia entre dos listas de [Category] para [ListAdapter].
     */
    private class CategoryDiffCallback : DiffUtil.ItemCallback<Category>() {
        override fun areItemsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem.categoryId == newItem.categoryId
        }

        override fun areContentsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem == newItem
        }
    }
}