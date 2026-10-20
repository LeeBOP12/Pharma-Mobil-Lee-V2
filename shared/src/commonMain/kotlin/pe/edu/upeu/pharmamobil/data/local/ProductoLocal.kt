package pe.edu.upeu.pharmamobil.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.edu.upeu.pharmamobil.db.PharmaDb
import pe.edu.upeu.pharmamobil.domain.model.Producto

/**
 * Fuente local del inventario. Esta clase solo conoce SQLDelight y el modelo
 * de dominio; no depende de Ktor ni de los DTO del backend.
 */
class ProductoLocal(base: PharmaDb) {

    private val consultas = base.productoQueries

    fun observar(): Flow<List<Producto>> =
        consultas.listarTodos()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { filas ->
                filas.map { fila ->
                    Producto(
                        id = fila.id,
                        nombre = fila.nombre,
                        precio = fila.precio,
                        stock = fila.stock.toInt()
                    )
                }
            }

    fun pendientes(): List<Producto> =
        consultas.listarPendientes()
            .executeAsList()
            .map { fila ->
                Producto(
                    id = fila.id,
                    nombre = fila.nombre,
                    precio = fila.precio,
                    stock = fila.stock.toInt()
                )
            }

    fun buscar(id: Long): Producto? =
        consultas.buscarPorId(id)
            .executeAsOneOrNull()
            ?.let { fila ->
                Producto(
                    id = fila.id,
                    nombre = fila.nombre,
                    precio = fila.precio,
                    stock = fila.stock.toInt()
                )
            }

    fun guardarPendiente(producto: Producto): Producto {
        val menorId = consultas.siguienteIdLocal().executeAsOne()
        val local = producto.copy(id = minOf(menorId, 0L) - 1L)
        guardar(local, pendiente = true)
        return local
    }

    fun actualizarPendiente(producto: Producto): Producto {
        guardar(producto, pendiente = true)
        return producto
    }

    fun guardarSincronizado(producto: Producto) {
        guardar(producto, pendiente = false)
    }

    fun confirmarSincronizacion(idLocal: Long, productoRemoto: Producto) {
        consultas.transaction {
            consultas.borrarPorId(idLocal)
            guardar(productoRemoto, pendiente = false)
        }
    }

    fun reemplazarSincronizados(productos: List<Producto>) {
        consultas.transaction {
            consultas.borrarSincronizados()
            productos.forEach { producto ->
                guardar(producto, pendiente = false)
            }
        }
    }

    fun borrar(id: Long) {
        consultas.borrarPorId(id)
    }

    private fun guardar(producto: Producto, pendiente: Boolean) {
        consultas.insertar(
            id = producto.id,
            nombre = producto.nombre,
            precio = producto.precio,
            stock = producto.stock.toLong(),
            pendiente = if (pendiente) 1L else 0L
        )
    }
}
