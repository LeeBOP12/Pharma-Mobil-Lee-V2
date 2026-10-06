package pe.edu.upeu.pharmamobil.presentation.producto

import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.platform.formatearSoles


data class ProductoUi(
    val id: Long,
    val nombre: String,
    val precioValor: Double,
    val precio: String,
    val stockValor: Int,
    val stock: String,
    val requiereReposicion: Boolean
)

fun Producto.aUi(): ProductoUi = ProductoUi(
    id = id,
    nombre = nombre,
    precioValor = precio,
    precio = formatearSoles(precio),
    stockValor = stock,
    stock = "$stock u.",
    requiereReposicion = requiereReposicion
)
