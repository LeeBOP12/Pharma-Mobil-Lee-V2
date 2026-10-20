package pe.edu.upeu.pharmamobil.data.repository

import pe.edu.upeu.pharmamobil.data.mapper.toDomain
import pe.edu.upeu.pharmamobil.data.mapper.toRequestDto
import pe.edu.upeu.pharmamobil.data.remote.ProductoApi
import pe.edu.upeu.pharmamobil.data.remote.ejecutarLlamadaRemota
import pe.edu.upeu.pharmamobil.domain.model.Producto

class ProductoRepositorioRemoto(
    private val productoApi: ProductoApi
) {

    suspend fun registrar(producto: Producto): Producto {
        return ejecutarLlamadaRemota {
            val categoriaId = obtenerCategoriaActivaId()
            productoApi.crear(producto.toRequestDto(categoriaId)).toDomain()
        }
    }

    suspend fun listar(): List<Producto> {
        return ejecutarLlamadaRemota {
            productoApi.listar().contenido
                .filter { it.estado }
                .map { it.toDomain() }
        }
    }

    suspend fun obtener(id: Long): Producto {
        return ejecutarLlamadaRemota {
            productoApi.obtener(id).toDomain()
        }
    }

    suspend fun actualizar(producto: Producto): Producto {
        return ejecutarLlamadaRemota {
            val categoriaId = obtenerCategoriaActivaId()
            productoApi.actualizar(
                id = producto.id,
                producto = producto.toRequestDto(categoriaId)
            ).toDomain()
        }
    }

    suspend fun eliminar(id: Long) {
        ejecutarLlamadaRemota {
            productoApi.eliminar(id)
        }
    }

    private suspend fun obtenerCategoriaActivaId(): Long {
        return productoApi.listarCategorias()
            .firstOrNull { it.estado }
            ?.id
            ?: error("No hay categorias activas en el backend para registrar productos")
    }
}
