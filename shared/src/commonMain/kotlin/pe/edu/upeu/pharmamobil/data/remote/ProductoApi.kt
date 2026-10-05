package pe.edu.upeu.pharmamobil.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.delete
import io.ktor.client.request.setBody
import pe.edu.upeu.pharmamobil.data.remote.dto.CategoriaResponseDto
import pe.edu.upeu.pharmamobil.data.remote.dto.PaginaResponseDto
import pe.edu.upeu.pharmamobil.data.remote.dto.ProductoRequestDto
import pe.edu.upeu.pharmamobil.data.remote.dto.ProductoResponseDto

class ProductoApi(
    private val client: HttpClient
) {

    suspend fun listar(
        pagina: Int = 0,
        tamanio: Int = 20
    ): PaginaResponseDto<ProductoResponseDto> {
        return client.get("productos") {
            parameter("pagina", pagina)
            parameter("tamanio", tamanio)
            parameter("ordenarPor", "id")
            parameter("direccion", "asc")
        }.body()
    }

    suspend fun crear(producto: ProductoRequestDto): ProductoResponseDto {
        return client.post("productos") {
            setBody(producto)
        }.body()
    }

    suspend fun obtener(id: Long): ProductoResponseDto {
        return client.get("productos/$id").body()
    }

    suspend fun actualizar(id: Long, producto: ProductoRequestDto): ProductoResponseDto {
        return client.put("productos/$id") {
            setBody(producto)
        }.body()
    }

    suspend fun eliminar(id: Long) {
        client.delete("productos/$id")
    }

    suspend fun listarCategorias(): List<CategoriaResponseDto> {
        return client.get("categorias").body()
    }
}
