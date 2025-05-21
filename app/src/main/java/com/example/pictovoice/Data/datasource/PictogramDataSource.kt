package com.example.pictovoice.Data.datasource

import com.example.pictovoice.Data.model.Category
import com.example.pictovoice.Data.model.Pictogram
import com.example.pictovoice.R

/**
 * Singleton que actúa como fuente de datos local para las definiciones de
 * categorías de pictogramas y todos los pictogramas de la aplicación.
 *
 * Centraliza la creación de estos datos estáticos para asegurar consistencia
 * y facilitar el mantenimiento.
 */
object PictogramDataSource {

    // --- Constantes para IDs de Categorías ---
    const val PRONOUNS_CATEGORY_ID = "local_pronombres"
    const val FIXED_VERBS_CATEGORY_ID = "local_verbos"

    // Dinámicas (seleccionables como carpetas)
    const val CATEGORY_ID_COMIDA = "local_comida"
    const val CATEGORY_ID_ANIMALES = "local_animales"
    const val CATEGORY_ID_SENTIMIENTOS = "local_sentimientos"
    const val CATEGORY_ID_AFICIONES = "local_aficiones"
    const val CATEGORY_ID_VERBOS_2 = "local_verbos_2"
    const val CATEGORY_ID_CHARLA_RAPIDA = "local_charla_rapida"
    const val CATEGORY_ID_LUGARES = "local_lugares"
    const val CATEGORY_ID_FRASES_HECHAS = "local_frases_hechas"
    const val CATEGORY_ID_NUMEROS = "local_numeros"
    const val CATEGORY_ID_OBJETOS = "local_objetos"
    const val CATEGORY_ID_ACCIONES = "local_acciones"

    // Lista de todas las categorías dinámicas definidas que pueden aparecer como carpetas
    fun getAllDynamicCategories(): List<Category> {
        return listOf(
            Category(categoryId = CATEGORY_ID_COMIDA, name = "Comida", displayOrder = 0 /*, iconResourceId = R.drawable.ic_folder_food*/),
            Category(categoryId = CATEGORY_ID_ANIMALES, name = "Animales", displayOrder = 1 /*, iconResourceId = R.drawable.ic_folder_animals*/),
            Category(categoryId = CATEGORY_ID_SENTIMIENTOS, name = "Sentimientos", displayOrder = 2 /*, iconResourceId = R.drawable.ic_folder_sentimientos */),
            Category(categoryId = CATEGORY_ID_NUMEROS, name = "Números", displayOrder = 3 /*, iconResourceId = R.drawable.ic_folder_numeros */),
            Category(categoryId = CATEGORY_ID_AFICIONES, name = "Aficiones", displayOrder = 4 /*, iconResourceId = R.drawable.ic_folder_aficiones */),
            Category(categoryId = CATEGORY_ID_LUGARES, name = "Lugares", displayOrder = 5 /*, iconResourceId = R.drawable.ic_folder_lugares */),
            Category(categoryId = CATEGORY_ID_VERBOS_2, name = "Verbos II", displayOrder = 6 /*, iconResourceId = R.drawable.ic_folder_verbos2 */),
            Category(categoryId = CATEGORY_ID_CHARLA_RAPIDA, name = "Charla Rápida", displayOrder = 7 /*, iconResourceId = R.drawable.ic_folder_charla_rapida */),
            Category(categoryId = CATEGORY_ID_FRASES_HECHAS, name = "Frases Hechas", displayOrder = 8 /*, iconResourceId = R.drawable.ic_folder_frases_hechas */),
            Category(categoryId = CATEGORY_ID_OBJETOS, name = "Objetos", displayOrder = 9 /*, iconResourceId = R.drawable.ic_folder_objetos*/), // Si aún la quieres
            Category(categoryId = CATEGORY_ID_ACCIONES, name = "Acciones", displayOrder = 10 /*, iconResourceId = R.drawable.ic_folder_acciones*/) // Si aún la quieres
        )
    }

    // Lista de TODOS los pictogramas definidos en la aplicación
    fun getAllPictograms(): List<Pictogram> {
        val pictos = mutableListOf<Pictogram>()

// --- PRONOMBRES (Categoría Fija) ---
        pictos.add(Pictogram(pictogramId = "local_pro_001", name = "Yo", category = PRONOUNS_CATEGORY_ID, imageResourceId = R.drawable.picto_yo, audioResourceId = R.raw.audio_yo, levelRequired = 1, baseExp = 10))
        pictos.add(Pictogram(pictogramId = "local_pro_002", name = "Tú", category = PRONOUNS_CATEGORY_ID, imageResourceId = R.drawable.picto_tu, audioResourceId = R.raw.audio_tu, levelRequired = 1, baseExp = 10))
        pictos.add(Pictogram(pictogramId = "local_pro_003", name = "Él", category = PRONOUNS_CATEGORY_ID, imageResourceId = R.drawable.picto_el, audioResourceId = R.raw.audio_el, levelRequired = 1, baseExp = 10))
        pictos.add(Pictogram(pictogramId = "local_pro_004", name = "Ella", category = PRONOUNS_CATEGORY_ID, imageResourceId = R.drawable.picto_ella, audioResourceId = R.raw.audio_ella, levelRequired = 1, baseExp = 10))
        pictos.add(Pictogram(pictogramId = "local_pro_005", name = "Nosotros", category = PRONOUNS_CATEGORY_ID, imageResourceId = R.drawable.picto_nosotros, audioResourceId = R.raw.audio_nosotros, levelRequired = 1, baseExp = 10))
        pictos.add(Pictogram(pictogramId = "local_pro_006", name = "Vosotros", category = PRONOUNS_CATEGORY_ID, imageResourceId = R.drawable.picto_vosotros, audioResourceId = R.raw.audio_vosotros, levelRequired = 1, baseExp = 10))
        pictos.add(Pictogram(pictogramId = "local_pro_007", name = "Ellos", category = PRONOUNS_CATEGORY_ID, imageResourceId = R.drawable.picto_ellos, audioResourceId = R.raw.audio_ellos, levelRequired = 1, baseExp = 10))

        // --- VERBOS FIJOS (Categoría Fija) ---
        pictos.add(Pictogram(pictogramId = "local_vrb_001", name = "Ser", category = FIXED_VERBS_CATEGORY_ID, imageResourceId = R.drawable.picto_ser, audioResourceId = R.raw.audio_ser, levelRequired = 1, baseExp = 15))
        pictos.add(Pictogram(pictogramId = "local_vrb_002", name = "Querer", category = FIXED_VERBS_CATEGORY_ID, imageResourceId = R.drawable.picto_querer, audioResourceId = R.raw.audio_querer, levelRequired = 1, baseExp = 15))
        pictos.add(Pictogram(pictogramId = "local_vrb_003", name = "Ir", category = FIXED_VERBS_CATEGORY_ID, imageResourceId = R.drawable.picto_ir, audioResourceId = R.raw.audio_ir, levelRequired = 1, baseExp = 15))
        pictos.add(Pictogram(pictogramId = "local_vrb_004", name = "Tener", category = FIXED_VERBS_CATEGORY_ID, imageResourceId = R.drawable.picto_tener, audioResourceId = R.raw.audio_tener, levelRequired = 1, baseExp = 15))
        pictos.add(Pictogram(pictogramId = "local_vrb_005", name = "Ver", category = FIXED_VERBS_CATEGORY_ID, imageResourceId = R.drawable.picto_ver, audioResourceId = R.raw.audio_ver, levelRequired = 1, baseExp = 15))
        pictos.add(Pictogram(pictogramId = "local_vrb_006", name = "Poder", category = FIXED_VERBS_CATEGORY_ID, imageResourceId = R.drawable.picto_poder, audioResourceId = R.raw.audio_poder, levelRequired = 1, baseExp = 15))
        pictos.add(Pictogram(pictogramId = "local_vrb_007", name = "Dar", category = FIXED_VERBS_CATEGORY_ID, imageResourceId = R.drawable.picto_dar, audioResourceId = R.raw.audio_dar, levelRequired = 1, baseExp = 15))
        pictos.add(Pictogram(pictogramId = "local_vrb_008", name = "Venir", category = FIXED_VERBS_CATEGORY_ID, imageResourceId = R.drawable.picto_venir, audioResourceId = R.raw.audio_venir, levelRequired = 1, baseExp = 15))
        pictos.add(Pictogram(pictogramId = "local_vrb_009", name = "Coger", category = FIXED_VERBS_CATEGORY_ID, imageResourceId = R.drawable.picto_coger, audioResourceId = R.raw.audio_coger, levelRequired = 1, baseExp = 15))
        pictos.add(Pictogram(pictogramId = "local_vrb_010", name = "Ayudar", category = FIXED_VERBS_CATEGORY_ID, imageResourceId = R.drawable.picto_ayudar, audioResourceId = R.raw.audio_ayudar, levelRequired = 1, baseExp = 15))

        // --- COMIDA ---
        pictos.add(Pictogram(pictogramId = "local_com_029_manzana", name = "Manzana", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_manzana, audioResourceId = R.raw.audio_manzana, levelRequired = 1, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_com_001", name = "Desayuno", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_desayuno, audioResourceId = R.raw.audio_desayuno, levelRequired = 1, baseExp = 20)) // Asumiendo baseExp
        pictos.add(Pictogram(pictogramId = "local_com_002", name = "Almuerzo", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_almuerzo, audioResourceId = R.raw.audio_almuerzo, levelRequired = 1, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_com_003", name = "Comida", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_comida, audioResourceId = R.raw.audio_comida, levelRequired = 1, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_com_004", name = "Merienda", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_merienda, audioResourceId = R.raw.audio_merienda, levelRequired = 1, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_com_005", name = "Cena", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_cena, audioResourceId = R.raw.audio_cena, levelRequired = 1, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_com_006", name = "Agua", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_agua, audioResourceId = R.raw.audio_agua, levelRequired = 1, baseExp = 5))
        pictos.add(Pictogram(pictogramId = "local_com_007", name = "Verdura", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_verdura, audioResourceId = R.raw.audio_verduras, levelRequired = 1, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_com_008", name = "Pasta", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_pasta, audioResourceId = R.raw.audio_pasta, levelRequired = 1, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_com_009", name = "Hortalizas", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_hortalizas, audioResourceId = R.raw.audio_hortalizas, levelRequired = 1, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_com_010", name = "Lácteos", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_lacteos, audioResourceId = R.raw.audio_lacteos, levelRequired = 1, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_com_011", name = "Frutas", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_frutas, audioResourceId = R.raw.audio_frutas, levelRequired = 1, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_com_012", name = "Dulces", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_dulces, audioResourceId = R.raw.audio_dulces, levelRequired = 1, baseExp = 10))
        pictos.add(Pictogram(pictogramId = "local_com_013", name = "Carne", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_carne, audioResourceId = R.raw.audio_carne, levelRequired = 1, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_com_014", name = "Sopa", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_sopa, audioResourceId = R.raw.audio_sopa, levelRequired = 1, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_com_015", name = "Pizza", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_pizza, audioResourceId = R.raw.audio_pizza, levelRequired = 1, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_com_016", name = "Pescado", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_pescado, audioResourceId = R.raw.audio_pescado, levelRequired = 1, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_com_017", name = "Paella", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_paella, audioResourceId = R.raw.audio_paella, levelRequired = 1, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_com_018", name = "Miel", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_miel, audioResourceId = R.raw.audio_miel, levelRequired = 1, baseExp = 10))
        pictos.add(Pictogram(pictogramId = "local_com_019", name = "Macarrones", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_macarrones, audioResourceId = R.raw.audio_macarrones, levelRequired = 1, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_com_020", name = "Lentejas", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_lentejas, audioResourceId = R.raw.audio_lentejas, levelRequired = 1, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_com_021", name = "Jamón", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_jamon, audioResourceId = R.raw.audio_jamon, levelRequired = 1, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_com_022", name = "Hamburguesa", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_hamburguesa, audioResourceId = R.raw.audio_hamburguesa, levelRequired = 1, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_com_023", name = "Guisantes", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_guisantes, audioResourceId = R.raw.audio_guisantes, levelRequired = 1, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_com_024", name = "Espaguetis", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_espaguetis, audioResourceId = R.raw.audio_espaguetis, levelRequired = 1, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_com_025", name = "Canelones", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_canelones, audioResourceId = R.raw.audio_canelones, levelRequired = 1, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_com_026", name = "Arroz con tomate", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_arroz_con_tomate, audioResourceId = R.raw.audio_arroz_con_tomate, levelRequired = 1, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_com_027", name = "Ensalada", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_ensalada, audioResourceId = R.raw.audio_ensalada, levelRequired = 1, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_com_028", name = "Chocolate", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_chocolate, audioResourceId = R.raw.audio_chocolate, levelRequired = 1, baseExp = 10))
        pictos.add(Pictogram(pictogramId = "local_com_029_manzana", name = "Manzana", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_manzana, audioResourceId = R.raw.audio_manzana, levelRequired = 1, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_com_030_galleta", name = "Galleta", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_galleta, audioResourceId = R.raw.audio_galleta, levelRequired = 1, baseExp = 10))
        pictos.add(Pictogram(pictogramId = "local_com_031_leche", name = "Leche", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_leche, audioResourceId = R.raw.audio_leche, levelRequired = 1, baseExp = 15))

        // Ejemplo para otra categoría
        pictos.add(Pictogram(pictogramId = "local_ani_001", name = "Perro", category = CATEGORY_ID_ANIMALES, imageResourceId = R.drawable.picto_perro, audioResourceId = R.raw.audio_perro, levelRequired = 2, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_ani_002", name = "Gato", category = CATEGORY_ID_ANIMALES, imageResourceId = R.drawable.picto_gato, audioResourceId = R.raw.audio_gato, levelRequired = 2, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_ani_003", name = "Vaca", category = CATEGORY_ID_ANIMALES, imageResourceId = R.drawable.picto_vaca, audioResourceId = R.raw.audio_vaca, levelRequired = 2, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_ani_004", name = "Delfín", category = CATEGORY_ID_ANIMALES, imageResourceId = R.drawable.picto_delfin, audioResourceId = R.raw.audio_delfin, levelRequired = 2, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_ani_005", name = "Tigre", category = CATEGORY_ID_ANIMALES, imageResourceId = R.drawable.picto_tigre, audioResourceId = R.raw.audio_tigre, levelRequired = 2, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_ani_006", name = "Zorro", category = CATEGORY_ID_ANIMALES, imageResourceId = R.drawable.picto_zorro, audioResourceId = R.raw.audio_zorro, levelRequired = 2, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_ani_007", name = "León", category = CATEGORY_ID_ANIMALES, imageResourceId = R.drawable.picto_leon, audioResourceId = R.raw.audio_leon, levelRequired = 2, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_ani_008", name = "Caballo", category = CATEGORY_ID_ANIMALES, imageResourceId = R.drawable.picto_caballo, audioResourceId = R.raw.audio_caballo, levelRequired = 2, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_ani_009", name = "Elefante", category = CATEGORY_ID_ANIMALES, imageResourceId = R.drawable.picto_elefante, audioResourceId = R.raw.audio_elefante, levelRequired = 2, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_ani_010", name = "Insecto", category = CATEGORY_ID_ANIMALES, imageResourceId = R.drawable.picto_insecto, audioResourceId = R.raw.audio_insecto, levelRequired = 2, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_ani_011", name = "Pingüino", category = CATEGORY_ID_ANIMALES, imageResourceId = R.drawable.picto_pinguino, audioResourceId = R.raw.audio_pinguino, levelRequired = 2, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_ani_012", name = "Serpiente", category = CATEGORY_ID_ANIMALES, imageResourceId = R.drawable.picto_serpiente, audioResourceId = R.raw.audio_serpiente, levelRequired = 2, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_ani_013", name = "Pez", category = CATEGORY_ID_ANIMALES, imageResourceId = R.drawable.picto_pez, audioResourceId = R.raw.audio_pez, levelRequired = 2, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_ani_014", name = "Rinoceronte", category = CATEGORY_ID_ANIMALES, imageResourceId = R.drawable.picto_rinoceronte, audioResourceId = R.raw.audio_rinoceronte, levelRequired = 2, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_ani_015", name = "Araña", category = CATEGORY_ID_ANIMALES, imageResourceId = R.drawable.picto_aranya, audioResourceId = R.raw.audio_aranya, levelRequired = 2, baseExp = 25))


        // **SENTIMIENTOS (CATEGORY_ID_SENTIMIENTOS = "local_sentimientos")**
        // Define levelRequired según tu mapeo (ej. Nivel 3)
        // EJEMPLO: pictos.add(Pictogram(pictogramId = "local_sen_feliz", name = "Feliz", category = CATEGORY_ID_SENTIMIENTOS, imageResourceId = R.drawable.picto_feliz, audioResourceId = R.raw.audio_feliz, levelRequired = 3, baseExp = 30))
        // ¡¡¡DEBES AÑADIR TUS PICTOGRAMAS AQUÍ!!!

        // **NÚMEROS (CATEGORY_ID_NUMEROS = "local_numeros")**
        // Define levelRequired (ej. Nivel 4)
        // ¡¡¡DEBES AÑADIR TUS PICTOGRAMAS AQUÍ!!!

        // **AFICIONES (CATEGORY_ID_AFICIONES = "local_aficiones")**
        // Define levelRequired (ej. Nivel 5)
        // ¡¡¡DEBES AÑADIR TUS PICTOGRAMAS AQUÍ!!!

        // **LUGARES (CATEGORY_ID_LUGARES = "local_lugares")**
        // Define levelRequired (ej. Nivel 6)
        // ¡¡¡DEBES AÑADIR TUS PICTOGRAMAS AQUÍ!!!

        // **VERBOS II (CATEGORY_ID_VERBOS_2 = "local_verbos_2")**
        // Define levelRequired (ej. Nivel 7)
        // ¡¡¡DEBES AÑADIR TUS PICTOGRAMAS AQUÍ!!!

        // **CHARLA RÁPIDA (CATEGORY_ID_CHARLA_RAPIDA = "local_charla_rapida")**
        // Define levelRequired (ej. Nivel 8)
        // ¡¡¡DEBES AÑADIR TUS PICTOGRAMAS AQUÍ!!!

        // **FRASES HECHAS (CATEGORY_ID_FRASES_HECHAS = "local_frases_hechas")**
        // Define levelRequired (ej. Nivel 9)
        // ¡¡¡DEBES AÑADIR TUS PICTOGRAMAS AQUÍ!!!

        // **OBJETOS (CATEGORY_ID_OBJETOS = "local_objetos")** - Si la usas
        // Define levelRequired
        // ¡¡¡DEBES AÑADIR TUS PICTOGRAMAS AQUÍ!!!

        // **ACCIONES (CATEGORY_ID_ACCIONES = "local_acciones")** - Si la usas
        // Define levelRequired
        // ¡¡¡DEBES AÑADIR TUS PICTOGRAMAS AQUÍ!!!

        return pictos
    }
}