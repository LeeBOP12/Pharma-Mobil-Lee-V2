package pe.edu.upeu.pharmamobil.data.repository

import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

/**
 * Doble del inventario para las pruebas: sin delay y capaz de fallar a
 * voluntad. Sin el, probar el camino de error del caso de uso o del ViewModel
 * era imposible, porque el repositorio en memoria nunca falla.
 */
class
FakeProductoRepository(
    private val productos: MutableList<Producto> = mutableListOf()
) : ProductoRepository {

    var fallaAlRegistrar: Throwable? = null
    var fallaAlListar: Throwable? = null
    var fallaAlActualizar: Throwable? = null
    var fallaAlEliminar: Throwable? = null
    var llamadasARegistrar: Int = 0
        private set
    var llamadasAListar: Int = 0
        private set
    var llamadasAActualizar: Int = 0
        private set
    var llamadasAEliminar: Int = 0
        private set

    private var siguienteId = 1L

    override suspend fun registrar(producto: Producto): Producto {

        llamadasARegistrar++
        fallaAlRegistrar?.let { throw it }

        val guardado = producto.copy(id = siguienteId++)
        productos.add(guardado)
        return guardado
    }

    override suspend fun listar(): List<Producto> {

        llamadasAListar++
        fallaAlListar?.let { throw it }

        return productos.toList()
    }

    override suspend fun obtener(id: Long): Producto {
        return productos.firstOrNull { it.id == id }
            ?: error("Producto no encontrado")
    }

    override suspend fun actualizar(producto: Producto): Producto {
        llamadasAActualizar++
        fallaAlActualizar?.let { throw it }

        val indice = productos.indexOfFirst { it.id == producto.id }
        if (indice < 0) error("Producto no encontrado")
        productos[indice] = producto
        return producto
    }

    override suspend fun eliminar(id: Long) {
        llamadasAEliminar++
        fallaAlEliminar?.let { throw it }
        productos.removeAll { it.id == id }
    }
}
