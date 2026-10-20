package pe.edu.upeu.pharmamobil.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

/**
 * Doble del inventario para las pruebas: sin delay y capaz de fallar a
 * voluntad. Sin el, probar el camino de error del caso de uso o del ViewModel
 * era imposible, porque el repositorio en memoria nunca falla.
 */
class
FakeProductoRepository(
    productosIniciales: MutableList<Producto> = mutableListOf()
) : ProductoRepository {

    private val productos = MutableStateFlow(productosIniciales.toList())

    var fallaAlRegistrar: Throwable? = null
    var fallaAlListar: Throwable? = null
    var fallaAlActualizar: Throwable? = null
    var fallaAlEliminar: Throwable? = null
    var fallaAlSincronizar: Throwable? = null
    var antesDeListar: suspend () -> Unit = {}
    var llamadasARegistrar: Int = 0
        private set
    var llamadasAListar: Int = 0
        private set
    var llamadasAActualizar: Int = 0
        private set
    var llamadasAEliminar: Int = 0
        private set
    var llamadasASincronizar: Int = 0
        private set

    private var siguienteId = 1L

    override fun observar(): Flow<List<Producto>> = flow {
        llamadasAListar++
        antesDeListar()
        fallaAlListar?.let { throw it }
        emitAll(productos)
    }

    override suspend fun registrar(producto: Producto): Producto {

        llamadasARegistrar++
        fallaAlRegistrar?.let { throw it }

        val guardado = producto.copy(id = siguienteId++)
        productos.value = productos.value + guardado
        return guardado
    }

    override suspend fun obtener(id: Long): Producto {
        return productos.value.firstOrNull { it.id == id }
            ?: error("Producto no encontrado")
    }

    override suspend fun actualizar(producto: Producto): Producto {
        llamadasAActualizar++
        fallaAlActualizar?.let { throw it }

        val actuales = productos.value.toMutableList()
        val indice = actuales.indexOfFirst { it.id == producto.id }
        if (indice < 0) error("Producto no encontrado")
        actuales[indice] = producto
        productos.value = actuales
        return producto
    }

    override suspend fun eliminar(id: Long) {
        llamadasAEliminar++
        fallaAlEliminar?.let { throw it }
        productos.value = productos.value.filterNot { it.id == id }
    }

    override suspend fun sincronizar() {
        llamadasASincronizar++
        fallaAlSincronizar?.let { throw it }
    }
}
