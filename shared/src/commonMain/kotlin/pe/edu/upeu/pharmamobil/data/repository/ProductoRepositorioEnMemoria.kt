package pe.edu.upeu.pharmamobil.data.repository

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

/**
 * Almacenamiento en memoria de productos. El id lo asigna el repositorio,
 * no la pantalla, para evitar identificadores duplicados.
 *
 * El delay simula la latencia que traera el backend REST, de modo que el
 * estado de carga de la pantalla sea visible desde ahora.
 *
 * Koin lo registra como single, asi que es un objeto compartido y sus metodos
 * son suspend: nada garantiza que dos llamadas no se crucen. El [Mutex]
 * protege la lista y el contador de ids de esa carrera.
 *
 * XD Lee
 */
class ProductoRepositorioEnMemoria : ProductoRepository {

    private val candado = Mutex()
    private val productos = MutableStateFlow<List<Producto>>(emptyList())
    private var siguienteId = 1L

    override fun observar(): Flow<List<Producto>> = productos

    override suspend fun registrar(producto: Producto): Producto {
        delay(RETARDO_REGISTRO_MS)
        return candado.withLock {
            val guardado = producto.copy(id = siguienteId++)
            productos.value = productos.value + guardado
            guardado
        }
    }

    override suspend fun obtener(id: Long): Producto {
        delay(RETARDO_LISTADO_MS)
        return candado.withLock {
            productos.value.firstOrNull { it.id == id }
                ?: error("Producto no encontrado")
        }
    }

    override suspend fun actualizar(producto: Producto): Producto {
        delay(RETARDO_REGISTRO_MS)
        return candado.withLock {
            val actuales = productos.value.toMutableList()
            val indice = actuales.indexOfFirst { it.id == producto.id }
            if (indice < 0) error("Producto no encontrado")
            actuales[indice] = producto
            productos.value = actuales
            producto
        }
    }

    override suspend fun eliminar(id: Long) {
        delay(RETARDO_REGISTRO_MS)
        candado.withLock {
            productos.value = productos.value.filterNot { it.id == id }
        }
    }

    override suspend fun sincronizar() = Unit

    private companion object {
        const val RETARDO_REGISTRO_MS = 5000L
        const val RETARDO_LISTADO_MS = 5000L
    }
}
