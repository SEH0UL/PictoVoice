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

    // Lista de todas las categorías dinámicas definidas
    fun getAllDynamicCategories(): List<Category> {
        return listOf(
            Category(categoryId = CATEGORY_ID_COMIDA, name = "Comida", displayOrder = 0 ),
            Category(categoryId = CATEGORY_ID_ANIMALES, name = "Animales", displayOrder = 1 ),
            Category(categoryId = CATEGORY_ID_SENTIMIENTOS, name = "Sentimientos", displayOrder = 2 ),
            Category(categoryId = CATEGORY_ID_NUMEROS, name = "Números", displayOrder = 3),
            Category(categoryId = CATEGORY_ID_AFICIONES, name = "Aficiones", displayOrder = 4 ),
            Category(categoryId = CATEGORY_ID_LUGARES, name = "Lugares", displayOrder = 5 ),
            Category(categoryId = CATEGORY_ID_VERBOS_2, name = "Verbos II", displayOrder = 6 ),
            Category(categoryId = CATEGORY_ID_CHARLA_RAPIDA, name = "Charla Rápida", displayOrder = 7 ),
            Category(categoryId = CATEGORY_ID_OBJETOS, name = "Objetos", displayOrder = 8 ),
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
        pictos.add(Pictogram(pictogramId = "local_com_001", name = "Desayuno", category = CATEGORY_ID_COMIDA, imageResourceId = R.drawable.picto_desayuno, audioResourceId = R.raw.audio_desayuno, levelRequired = 1, baseExp = 20))
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

        // -- ANIMALES --
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

        // -- SENTIMIENTOS --
        pictos.add(Pictogram(pictogramId = "local_sen_001", name = "Aburrimiento", category = CATEGORY_ID_SENTIMIENTOS, imageResourceId = R.drawable.picto_aburrimiento, audioResourceId = R.raw.audio_aburrimiento, levelRequired = 3, baseExp = 30))
        pictos.add(Pictogram(pictogramId = "local_sen_002", name = "Amado", category = CATEGORY_ID_SENTIMIENTOS, imageResourceId = R.drawable.picto_amada, audioResourceId = R.raw.audio_amada, levelRequired = 3, baseExp = 30))
        pictos.add(Pictogram(pictogramId = "local_sen_003", name = "Calor", category = CATEGORY_ID_SENTIMIENTOS, imageResourceId = R.drawable.picto_calor, audioResourceId = R.raw.audio_calor, levelRequired = 3, baseExp = 30))
        pictos.add(Pictogram(pictogramId = "local_sen_004", name = "Dolor", category = CATEGORY_ID_SENTIMIENTOS, imageResourceId = R.drawable.picto_dolor, audioResourceId = R.raw.audio_dolor, levelRequired = 3, baseExp = 30))
        pictos.add(Pictogram(pictogramId = "local_sen_005", name = "Enfado", category = CATEGORY_ID_SENTIMIENTOS, imageResourceId = R.drawable.picto_enfado, audioResourceId = R.raw.audio_enfado, levelRequired = 3, baseExp = 30))
        pictos.add(Pictogram(pictogramId = "local_sen_006", name = "Frío", category = CATEGORY_ID_SENTIMIENTOS, imageResourceId = R.drawable.picto_frio, audioResourceId = R.raw.audio_frio, levelRequired = 3, baseExp = 30))
        pictos.add(Pictogram(pictogramId = "local_sen_007", name = "Hambre", category = CATEGORY_ID_SENTIMIENTOS, imageResourceId = R.drawable.picto_hambre, audioResourceId = R.raw.audio_hambre, levelRequired = 3, baseExp = 30))
        pictos.add(Pictogram(pictogramId = "local_sen_008", name = "Miedo", category = CATEGORY_ID_SENTIMIENTOS, imageResourceId = R.drawable.picto_miedo, audioResourceId = R.raw.audio_miedo, levelRequired = 3, baseExp = 30))
        pictos.add(Pictogram(pictogramId = "local_sen_009", name = "Nervioso", category = CATEGORY_ID_SENTIMIENTOS, imageResourceId = R.drawable.picto_nervioso, audioResourceId = R.raw.audio_nervioso, levelRequired = 3, baseExp = 30))
        pictos.add(Pictogram(pictogramId = "local_sen_010", name = "Orgulloso", category = CATEGORY_ID_SENTIMIENTOS, imageResourceId = R.drawable.picto_orgulloso, audioResourceId = R.raw.audio_orgulloso, levelRequired = 3, baseExp = 30))
        pictos.add(Pictogram(pictogramId = "local_sen_011", name = "Picar", category = CATEGORY_ID_SENTIMIENTOS, imageResourceId = R.drawable.picto_picar, audioResourceId = R.raw.audio_picar, levelRequired = 3, baseExp = 30))
        pictos.add(Pictogram(pictogramId = "local_sen_012", name = "Risa", category = CATEGORY_ID_SENTIMIENTOS, imageResourceId = R.drawable.picto_risa, audioResourceId = R.raw.audio_risa, levelRequired = 3, baseExp = 30))
        pictos.add(Pictogram(pictogramId = "local_sen_013", name = "Sed", category = CATEGORY_ID_SENTIMIENTOS, imageResourceId = R.drawable.picto_sed, audioResourceId = R.raw.audio_sed, levelRequired = 3, baseExp = 30))
        pictos.add(Pictogram(pictogramId = "local_sen_014", name = "Sorpresa", category = CATEGORY_ID_SENTIMIENTOS, imageResourceId = R.drawable.picto_sorpresa, audioResourceId = R.raw.audio_sorpresa, levelRequired = 3, baseExp = 30))
        pictos.add(Pictogram(pictogramId = "local_sen_015", name = "Sueño", category = CATEGORY_ID_SENTIMIENTOS, imageResourceId = R.drawable.picto_suenyo, audioResourceId = R.raw.audio_suenyo, levelRequired = 3, baseExp = 30))
        pictos.add(Pictogram(pictogramId = "local_sen_016", name = "Vergüenza", category = CATEGORY_ID_SENTIMIENTOS, imageResourceId = R.drawable.picto_verguenza, audioResourceId = R.raw.audio_verguenza, levelRequired = 3, baseExp = 30))

        // -- NUMEROS --
        pictos.add(Pictogram(pictogramId = "local_num_001", name = "Uno", category = CATEGORY_ID_NUMEROS, imageResourceId = R.drawable.picto_1, audioResourceId = R.raw.audio_1, levelRequired = 4, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_num_002", name = "Dos", category = CATEGORY_ID_NUMEROS, imageResourceId = R.drawable.picto_2, audioResourceId = R.raw.audio_2, levelRequired = 4, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_num_003", name = "Tres", category = CATEGORY_ID_NUMEROS, imageResourceId = R.drawable.picto_3, audioResourceId = R.raw.audio_3, levelRequired = 4, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_num_004", name = "Cuatro", category = CATEGORY_ID_NUMEROS, imageResourceId = R.drawable.picto_4, audioResourceId = R.raw.audio_4, levelRequired = 4, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_num_005", name = "Cinco", category = CATEGORY_ID_NUMEROS, imageResourceId = R.drawable.picto_5, audioResourceId = R.raw.audio_5, levelRequired = 4, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_num_006", name = "Seis", category = CATEGORY_ID_NUMEROS, imageResourceId = R.drawable.picto_6, audioResourceId = R.raw.audio_6, levelRequired = 4, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_num_007", name = "Siete", category = CATEGORY_ID_NUMEROS, imageResourceId = R.drawable.picto_7, audioResourceId = R.raw.audio_7, levelRequired = 4, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_num_008", name = "Ocho", category = CATEGORY_ID_NUMEROS, imageResourceId = R.drawable.picto_8, audioResourceId = R.raw.audio_8, levelRequired = 4, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_num_009", name = "Nueve", category = CATEGORY_ID_NUMEROS, imageResourceId = R.drawable.picto_9, audioResourceId = R.raw.audio_9, levelRequired = 4, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_num_010", name = "Diez", category = CATEGORY_ID_NUMEROS, imageResourceId = R.drawable.picto_10, audioResourceId = R.raw.audio_10, levelRequired = 4, baseExp = 25))

        // -- OBJETOS --
        pictos.add(Pictogram(pictogramId = "local_obj_001", name = "billete", category = CATEGORY_ID_OBJETOS, imageResourceId = R.drawable.picto_billetes, audioResourceId = R.raw.audio_billetes, levelRequired = 5, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_obj_002", name = "Libro", category = CATEGORY_ID_OBJETOS, imageResourceId = R.drawable.picto_libro, audioResourceId = R.raw.audio_libro, levelRequired = 5, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_obj_003", name = "caja", category = CATEGORY_ID_OBJETOS, imageResourceId = R.drawable.picto_caja, audioResourceId = R.raw.audio_caja, levelRequired = 5, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_obj_004", name = "calendario", category = CATEGORY_ID_OBJETOS, imageResourceId = R.drawable.picto_calendario, audioResourceId = R.raw.audio_calendario, levelRequired = 5, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_obj_005", name = "dibujo", category = CATEGORY_ID_OBJETOS, imageResourceId = R.drawable.picto_dibujo, audioResourceId = R.raw.audio_dibujo, levelRequired = 5, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_obj_006", name = "Lápiz", category = CATEGORY_ID_OBJETOS, imageResourceId = R.drawable.picto_lapiz, audioResourceId = R.raw.audio_lapiz, levelRequired = 5, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_obj_007", name = "fuego", category = CATEGORY_ID_OBJETOS, imageResourceId = R.drawable.picto_fuego, audioResourceId = R.raw.audio_fuego, levelRequired = 5, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_obj_008", name = "Llave", category = CATEGORY_ID_OBJETOS, imageResourceId = R.drawable.picto_llave, audioResourceId = R.raw.audio_llave, levelRequired = 5, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_obj_009", name = "Monedas", category = CATEGORY_ID_OBJETOS, imageResourceId = R.drawable.picto_monedas, audioResourceId = R.raw.audio_monedas, levelRequired = 5, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_obj_010", name = "Monedero", category = CATEGORY_ID_OBJETOS, imageResourceId = R.drawable.picto_monedero, audioResourceId = R.raw.audio_monedero, levelRequired = 5, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_obj_011", name = "Objeto", category = CATEGORY_ID_OBJETOS, imageResourceId = R.drawable.picto_objeto, audioResourceId = R.raw.audio_objeto, levelRequired = 5, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_obj_012", name = "Papel", category = CATEGORY_ID_OBJETOS, imageResourceId = R.drawable.picto_papel, audioResourceId = R.raw.audio_papel, levelRequired = 5, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_obj_013", name = "Pincel", category = CATEGORY_ID_OBJETOS, imageResourceId = R.drawable.picto_pincel, audioResourceId = R.raw.audio_pincel, levelRequired = 5, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_obj_014", name = "Pinturas", category = CATEGORY_ID_OBJETOS, imageResourceId = R.drawable.picto_pinturas, audioResourceId = R.raw.audio_pinturas, levelRequired = 5, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_obj_015", name = "Pizarra", category = CATEGORY_ID_OBJETOS, imageResourceId = R.drawable.picto_pizarra, audioResourceId = R.raw.audio_pizarra, levelRequired = 5, baseExp = 20))
        pictos.add(Pictogram(pictogramId = "local_obj_016", name = "Reloj", category = CATEGORY_ID_OBJETOS, imageResourceId = R.drawable.picto_reloj, audioResourceId = R.raw.audio_reloj, levelRequired = 5, baseExp = 20))

        // -- LUGARES --
        pictos.add(Pictogram(pictogramId = "local_lug_001", name = "Balancín", category = CATEGORY_ID_LUGARES, imageResourceId = R.drawable.picto_balancin, audioResourceId = R.raw.audio_balancin, levelRequired = 6, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_lug_002", name = "Banco", category = CATEGORY_ID_LUGARES, imageResourceId = R.drawable.picto_banco, audioResourceId = R.raw.audio_banco, levelRequired = 6, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_lug_003", name = "Cama elástica", category = CATEGORY_ID_LUGARES, imageResourceId = R.drawable.picto_cama_elastica, audioResourceId = R.raw.audio_cama_elastica, levelRequired = 6, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_lug_004", name = "Columpio", category = CATEGORY_ID_LUGARES, imageResourceId = R.drawable.picto_columpio, audioResourceId = R.raw.audio_columpio, levelRequired = 6, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_lug_005", name = "Floristería", category = CATEGORY_ID_LUGARES, imageResourceId = R.drawable.picto_floristeria, audioResourceId = R.raw.audio_floristeria, levelRequired = 6, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_lug_006", name = "Fuente", category = CATEGORY_ID_LUGARES, imageResourceId = R.drawable.picto_fuente, audioResourceId = R.raw.audio_fuente, levelRequired = 6, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_lug_007", name = "Heladería", category = CATEGORY_ID_LUGARES, imageResourceId = R.drawable.picto_heladeria, audioResourceId = R.raw.audio_heladeria, levelRequired = 6, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_lug_008", name = "Librería", category = CATEGORY_ID_LUGARES, imageResourceId = R.drawable.picto_libreria, audioResourceId = R.raw.audio_libreria, levelRequired = 6, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_lug_009", name = "Papelera", category = CATEGORY_ID_LUGARES, imageResourceId = R.drawable.picto_papelera, audioResourceId = R.raw.audio_papelera, levelRequired = 6, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_lug_010", name = "Tobogán", category = CATEGORY_ID_LUGARES, imageResourceId = R.drawable.picto_tobogan, audioResourceId = R.raw.audio_tobogan, levelRequired = 6, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_lug_011", name = "Calle", category = CATEGORY_ID_LUGARES, imageResourceId = R.drawable.picto_calle, audioResourceId = R.raw.audio_calle, levelRequired = 6, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_lug_012", name = "Campo", category = CATEGORY_ID_LUGARES, imageResourceId = R.drawable.picto_campo, audioResourceId = R.raw.audio_campo, levelRequired = 6, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_lug_013", name = "Casa", category = CATEGORY_ID_LUGARES, imageResourceId = R.drawable.picto_casa, audioResourceId = R.raw.audio_casa, levelRequired = 6, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_lug_014", name = "Cine", category = CATEGORY_ID_LUGARES, imageResourceId = R.drawable.picto_cine, audioResourceId = R.raw.audio_cine, levelRequired = 6, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_lug_015", name = "Circo", category = CATEGORY_ID_LUGARES, imageResourceId = R.drawable.picto_circo, audioResourceId = R.raw.audio_circo, levelRequired = 6, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_lug_016", name = "Ciudad", category = CATEGORY_ID_LUGARES, imageResourceId = R.drawable.picto_ciudad, audioResourceId = R.raw.audio_ciudad, levelRequired = 6, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_lug_017", name = "Colegio", category = CATEGORY_ID_LUGARES, imageResourceId = R.drawable.picto_colegio, audioResourceId = R.raw.audio_colegio, levelRequired = 6, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_lug_018", name = "Farmacia", category = CATEGORY_ID_LUGARES, imageResourceId = R.drawable.picto_farmacia, audioResourceId = R.raw.audio_farmacia, levelRequired = 6, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_lug_019", name = "Hospital", category = CATEGORY_ID_LUGARES, imageResourceId = R.drawable.picto_hospital, audioResourceId = R.raw.audio_hospital, levelRequired = 6, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_lug_020", name = "Mercado", category = CATEGORY_ID_LUGARES, imageResourceId = R.drawable.picto_mercado, audioResourceId = R.raw.audio_mercado, levelRequired = 6, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_lug_021", name = "Parque", category = CATEGORY_ID_LUGARES, imageResourceId = R.drawable.picto_parque, audioResourceId = R.raw.audio_parque, levelRequired = 6, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_lug_022", name = "Piscina", category = CATEGORY_ID_LUGARES, imageResourceId = R.drawable.picto_piscina, audioResourceId = R.raw.audio_piscina, levelRequired = 6, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_lug_023", name = "Playa", category = CATEGORY_ID_LUGARES, imageResourceId = R.drawable.picto_playa, audioResourceId = R.raw.audio_playa, levelRequired = 6, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_lug_024", name = "Plaza", category = CATEGORY_ID_LUGARES, imageResourceId = R.drawable.picto_plaza, audioResourceId = R.raw.audio_plaza, levelRequired = 6, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_lug_025", name = "Pueblo", category = CATEGORY_ID_LUGARES, imageResourceId = R.drawable.picto_pueblo, audioResourceId = R.raw.audio_pueblo, levelRequired = 6, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_lug_026", name = "Teatro", category = CATEGORY_ID_LUGARES, imageResourceId = R.drawable.picto_teatro, audioResourceId = R.raw.audio_teatro, levelRequired = 6, baseExp = 25))

        // -- VERBOS 2 --
        pictos.add(Pictogram(pictogramId = "local_v2_001", name = "Acompañar", category = CATEGORY_ID_VERBOS_2, imageResourceId = R.drawable.picto_acompanar, audioResourceId = R.raw.audio_acompanar, levelRequired = 7, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_v2_002", name = "Afirmar", category = CATEGORY_ID_VERBOS_2, imageResourceId = R.drawable.picto_afirmar, audioResourceId = R.raw.audio_afirmar, levelRequired = 7, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_v2_003", name = "Empujar", category = CATEGORY_ID_VERBOS_2, imageResourceId = R.drawable.picto_empujar, audioResourceId = R.raw.audio_empujar, levelRequired = 7, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_v2_004", name = "Esperar", category = CATEGORY_ID_VERBOS_2, imageResourceId = R.drawable.picto_esperar, audioResourceId = R.raw.audio_esperar, levelRequired = 7, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_v2_005", name = "Levantar la mano", category = CATEGORY_ID_VERBOS_2, imageResourceId = R.drawable.picto_levantar_mano, audioResourceId = R.raw.audio_levantar_mano, levelRequired = 7, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_v2_006", name = "Llevar", category = CATEGORY_ID_VERBOS_2, imageResourceId = R.drawable.picto_llevar, audioResourceId = R.raw.audio_llevar, levelRequired = 7, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_v2_007", name = "Negar", category = CATEGORY_ID_VERBOS_2, imageResourceId = R.drawable.picto_negar, audioResourceId = R.raw.audio_negar, levelRequired = 7, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_v2_008", name = "Pasear", category = CATEGORY_ID_VERBOS_2, imageResourceId = R.drawable.picto_pasear, audioResourceId = R.raw.audio_pasear, levelRequired = 7, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_v2_009", name = "Tocar", category = CATEGORY_ID_VERBOS_2, imageResourceId = R.drawable.picto_tocar, audioResourceId = R.raw.audio_tocar, levelRequired = 7, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_v2_010", name = "Traer", category = CATEGORY_ID_VERBOS_2, imageResourceId = R.drawable.picto_traer, audioResourceId = R.raw.audio_traer, levelRequired = 7, baseExp = 25))

        // -- CHARLA RÁPIDA --
        pictos.add(Pictogram(pictogramId = "local_cr_001", name = "Adiós", category = CATEGORY_ID_CHARLA_RAPIDA, imageResourceId = R.drawable.picto_adios, audioResourceId = R.raw.audio_adios, levelRequired = 8, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_cr_002", name = "Buenas noches", category = CATEGORY_ID_CHARLA_RAPIDA, imageResourceId = R.drawable.picto_buenas_noches, audioResourceId = R.raw.audio_buenas_noches, levelRequired = 8, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_cr_003", name = "Buenas tardes", category = CATEGORY_ID_CHARLA_RAPIDA, imageResourceId = R.drawable.picto_buenas_tardes, audioResourceId = R.raw.audio_buenas_tardes, levelRequired = 8, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_cr_004", name = "Buenos días", category = CATEGORY_ID_CHARLA_RAPIDA, imageResourceId = R.drawable.picto_buenos_dias, audioResourceId = R.raw.audio_buenos_dias, levelRequired = 8, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_cr_005", name = "Estoy bien", category = CATEGORY_ID_CHARLA_RAPIDA, imageResourceId = R.drawable.picto_estoy_bien, audioResourceId = R.raw.audio_estoy_bien, levelRequired = 8, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_cr_006", name = "Gracias", category = CATEGORY_ID_CHARLA_RAPIDA, imageResourceId = R.drawable.picto_gracias, audioResourceId = R.raw.audio_gracias, levelRequired = 8, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_cr_007", name = "Hola", category = CATEGORY_ID_CHARLA_RAPIDA, imageResourceId = R.drawable.picto_hola, audioResourceId = R.raw.audio_hola, levelRequired = 8, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_cr_008", name = "Lo siento", category = CATEGORY_ID_CHARLA_RAPIDA, imageResourceId = R.drawable.picto_lo_siento, audioResourceId = R.raw.audio_lo_siento, levelRequired = 8, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_cr_009", name = "Mal", category = CATEGORY_ID_CHARLA_RAPIDA, imageResourceId = R.drawable.picto_mal, audioResourceId = R.raw.audio_mal, levelRequired = 8, baseExp = 25)) 
        pictos.add(Pictogram(pictogramId = "local_cr_010", name = "Me gusta eso", category = CATEGORY_ID_CHARLA_RAPIDA, imageResourceId = R.drawable.picto_me_gusta_eso, audioResourceId = R.raw.audio_me_gusta_eso, levelRequired = 8, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_cr_011", name = "Muy guay", category = CATEGORY_ID_CHARLA_RAPIDA, imageResourceId = R.drawable.picto_muy_guay, audioResourceId = R.raw.audio_muy_guay, levelRequired = 8, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_cr_012", name = "No lo entiendo", category = CATEGORY_ID_CHARLA_RAPIDA, imageResourceId = R.drawable.picto_no_lo_entiendo, audioResourceId = R.raw.audio_no_lo_entiendo, levelRequired = 8, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_cr_013", name = "No me gusta eso", category = CATEGORY_ID_CHARLA_RAPIDA, imageResourceId = R.drawable.picto_no_me_gusta_eso, audioResourceId = R.raw.audio_no_me_gusta_eso, levelRequired = 8, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_cr_014", name = "Por favor", category = CATEGORY_ID_CHARLA_RAPIDA, imageResourceId = R.drawable.picto_por_favor, audioResourceId = R.raw.audio_por_favor, levelRequired = 8, baseExp = 25))
        pictos.add(Pictogram(pictogramId = "local_cr_015", name = "Yo no sé", category = CATEGORY_ID_CHARLA_RAPIDA, imageResourceId = R.drawable.picto_yo_no_se, audioResourceId = R.raw.audio_yo_no_se, levelRequired = 8, baseExp = 25))

        return pictos
    }
}