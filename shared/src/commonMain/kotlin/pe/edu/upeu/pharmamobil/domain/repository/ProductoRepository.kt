package pe.edu.upeu.pharmamobil.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.edu.upeu.pharmamobil.domain.model.Producto

interface ProductoRepository {

    /** Fuente de verdad que la interfaz observa durante toda su vida. */
    fun observar(): Flow<List<Producto>>

    /** Guarda primero en local y conserva el dato si no hay conexión. */
    suspend fun registrar(producto: Producto): Producto

    suspend fun obtener(id: Long): Producto

    suspend fun actualizar(producto: Producto): Producto

    suspend fun eliminar(id: Long)

    /** Sube lo pendiente antes de descargar la versión del servidor. */
    suspend fun sincronizar()
}
