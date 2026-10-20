package pe.edu.upeu.pharmamobil.data.repository

import kotlinx.coroutines.flow.Flow
import pe.edu.upeu.pharmamobil.data.local.ProductoLocal
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

class ProductoRepositorioOffline(
    private val local: ProductoLocal,
    private val remoto: ProductoRepositorioRemoto
) : ProductoRepository {

    override fun observar(): Flow<List<Producto>> = local.observar()

    override suspend fun registrar(producto: Producto): Producto {
        val guardado = local.guardarPendiente(producto)

        try {
            sincronizar()
        } catch (fallo: ErrorApiException) {
            if (!fallo.error.esFalloTemporal()) {
                local.borrar(guardado.id)
                throw fallo
            }
            // El producto permanece pendiente para el próximo intento.
        }

        return guardado
    }

    override suspend fun obtener(id: Long): Producto {
        return local.buscar(id) ?: remoto.obtener(id).also(local::guardarSincronizado)
    }

    override suspend fun actualizar(producto: Producto): Producto {
        if (producto.id < 0L) {
            local.actualizarPendiente(producto)
            intentarSincronizar()
            return producto
        }

        val actualizado = remoto.actualizar(producto)
        local.guardarSincronizado(actualizado)
        return actualizado
    }

    override suspend fun eliminar(id: Long) {
        if (id >= 0L) {
            remoto.eliminar(id)
        }
        local.borrar(id)
    }

    override suspend fun sincronizar() {
        subirPendientes()
        local.reemplazarSincronizados(remoto.listar())
    }

    private suspend fun subirPendientes() {
        local.pendientes().forEach { pendiente ->
            val guardadoRemoto = remoto.registrar(pendiente)
            local.confirmarSincronizacion(pendiente.id, guardadoRemoto)
        }
    }

    private suspend fun intentarSincronizar() {
        try {
            sincronizar()
        } catch (fallo: ErrorApiException) {
            if (!fallo.error.esFalloTemporal()) {
                throw fallo
            }
        }
    }

    private fun ErrorApi.esFalloTemporal(): Boolean =
        this == ErrorApi.SinConexion ||
            this == ErrorApi.TiempoAgotado ||
            this == ErrorApi.Servidor
}
