package pe.edu.upeu.pharmamobil.domain.usecase

import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

class ActualizarProductoUseCase(
    private val productoRepository: ProductoRepository
) {

    suspend operator fun invoke(
        id: Long,
        nombre: String,
        precio: String,
        stock: String
    ): Result<Producto> {
        val errores = ErroresDeProducto(
            nombre = validarNombre(nombre),
            precio = validarPrecio(precio),
            stock = validarStock(stock)
        )

        if (errores.hayErrores) {
            return Result.failure(ProductoInvalidoException(errores))
        }

        return resultadoDe {
            productoRepository.actualizar(
                Producto(
                    id = id,
                    nombre = nombre.trim(),
                    precio = precio.toDouble(),
                    stock = stock.toInt()
                )
            )
        }
    }

    private fun validarNombre(nombre: String): String? {
        return if (nombre.isBlank()) "El nombre es obligatorio" else null
    }

    private fun validarPrecio(precio: String): String? {
        val precioValor = precio.toDoubleOrNull()
        return when {
            precio.isBlank() -> "El precio es obligatorio"
            precioValor == null || !precioValor.isFinite() -> "El precio debe ser un numero valido"
            precioValor <= 0 -> "El precio debe ser mayor a 0"
            else -> null
        }
    }

    private fun validarStock(stock: String): String? {
        val stockValor = stock.toIntOrNull()
        return when {
            stock.isBlank() -> "El stock es obligatorio"
            stockValor == null -> "El stock debe ser un numero entero"
            stockValor < 0 -> "El stock no puede ser negativo"
            else -> null
        }
    }
}
