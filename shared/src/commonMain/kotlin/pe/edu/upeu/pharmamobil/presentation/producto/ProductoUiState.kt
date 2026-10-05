package pe.edu.upeu.pharmamobil.presentation.producto

data class ProductoUiState(
    val fase: Fase = Fase.Cargando,
    val formulario: FormularioProducto = FormularioProducto(),
    val operacion: Operacion = Operacion.Inactiva,
    val mensajeExito: String? = null
) {

    /** Fases excluyentes del inventario: solo una puede estar activa. */
    sealed interface Fase {

        data object Cargando : Fase

        data object SinProductos : Fase

        data class ConProductos(val productos: List<ProductoUi>) : Fase

        data class Error(val mensaje: String) : Fase
    }
}

sealed interface Operacion {
    data object Inactiva : Operacion
    data class EnCurso(val tipo: TipoOperacion) : Operacion
    data class Fallida(val mensaje: String) : Operacion
}

enum class TipoOperacion {
    Crear,
    Actualizar,
    Eliminar
}

data class FormularioProducto(
    val id: Long? = null,
    val nombre: String = "",
    val precio: String = "",
    val stock: String = "",
    val nombreError: String? = null,
    val precioError: String? = null,
    val stockError: String? = null
) {
    val estaEditando: Boolean
        get() = id != null

    fun sinErrores(): FormularioProducto {
        return copy(
            nombreError = null,
            precioError = null,
            stockError = null
        )
    }
}
