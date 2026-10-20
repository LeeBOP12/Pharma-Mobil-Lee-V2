package pe.edu.upeu.pharmamobil.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import pe.edu.upeu.pharmamobil.data.local.ProductoLocal
import pe.edu.upeu.pharmamobil.data.remote.ProductoApi
import pe.edu.upeu.pharmamobil.db.PharmaDb
import pe.edu.upeu.pharmamobil.domain.model.Producto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProductoRepositorioOfflineTest {

    @Test
    fun sincronizarConRedGuardaLaRespuestaYElFlowLaEmite() = runTest {
        val cliente = clienteCon(
            MockEngine {
                responderJson(paginaConProducto(id = 7L, nombre = "Paracetamol"))
            }
        )
        val repositorio = repositorio(cliente)

        repositorio.sincronizar()

        val productos = repositorio.observar().first()
        assertEquals(1, productos.size)
        assertEquals(7L, productos.single().id)
        assertEquals("Paracetamol", productos.single().nombre)
        cliente.close()
    }

    @Test
    fun sinRedElProductoQuedaPendienteYLaPantallaLoVe() = runTest {
        val base = baseEnMemoria()
        val local = ProductoLocal(base)
        val cliente = clienteCon(MockEngine { throw IOException("Sin conexión") })
        val repositorio = repositorio(cliente, local)

        repositorio.registrar(
            Producto(id = 0L, nombre = "Ibuprofeno", precio = 4.5, stock = 20)
        )

        assertEquals(1, local.pendientes().size)
        assertEquals("Ibuprofeno", repositorio.observar().first().single().nombre)
        assertTrue(repositorio.observar().first().single().id < 0L)
        cliente.close()
    }

    @Test
    fun alReconectarSubeLoPendienteAntesDeDescargar() = runTest {
        var conectado = false
        val solicitudes = mutableListOf<Pair<HttpMethod, String>>()
        val base = baseEnMemoria()
        val local = ProductoLocal(base)
        val motor = MockEngine { solicitud ->
            if (!conectado) throw IOException("Sin conexión")

            solicitudes += solicitud.method to solicitud.url.encodedPath
            when {
                solicitud.url.encodedPath.endsWith("/categorias") ->
                    responderJson("""[{"id":1,"nombre":"Medicamentos","estado":true}]""")

                solicitud.method == HttpMethod.Post ->
                    responderJson(productoJson(id = 41L, nombre = "Amoxicilina"), HttpStatusCode.Created)

                else -> responderJson(paginaConProducto(id = 41L, nombre = "Amoxicilina"))
            }
        }
        val cliente = clienteCon(motor)
        val repositorio = repositorio(cliente, local)

        repositorio.registrar(
            Producto(id = 0L, nombre = "Amoxicilina", precio = 8.0, stock = 12)
        )
        assertEquals(1, local.pendientes().size)

        conectado = true
        repositorio.sincronizar()

        assertTrue(solicitudes[0].second.endsWith("/categorias"))
        assertEquals(HttpMethod.Post, solicitudes[1].first)
        assertEquals(HttpMethod.Get, solicitudes[2].first)
        assertTrue(local.pendientes().isEmpty())
        assertEquals(41L, repositorio.observar().first().single().id)
        cliente.close()
    }

    private fun repositorio(
        cliente: HttpClient,
        local: ProductoLocal = ProductoLocal(baseEnMemoria())
    ): ProductoRepositorioOffline = ProductoRepositorioOffline(
        local = local,
        remoto = ProductoRepositorioRemoto(ProductoApi(cliente))
    )

    private fun baseEnMemoria(): PharmaDb {
        val driver = crearDriverEnMemoria()
        return PharmaDb(driver)
    }

    private fun clienteCon(motor: MockEngine): HttpClient = HttpClient(motor) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        defaultRequest {
            url("http://localhost/api/v1/")
            contentType(ContentType.Application.Json)
        }
    }

    private fun MockRequestHandleScope.responderJson(
        contenido: String,
        estado: HttpStatusCode = HttpStatusCode.OK
    ) = respond(
        content = contenido,
        status = estado,
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    )

    private fun paginaConProducto(id: Long, nombre: String): String = """
        {
          "contenido": [${productoJson(id, nombre)}],
          "pagina": 0,
          "tamanio": 20,
          "totalElementos": 1,
          "totalPaginas": 1,
          "ultima": true
        }
    """.trimIndent()

    private fun productoJson(id: Long, nombre: String): String = """
        {
          "id": $id,
          "nombre": "$nombre",
          "precio": 8.0,
          "stock": 12,
          "estado": true,
          "categoriaId": 1,
          "categoriaNombre": "Medicamentos"
        }
    """.trimIndent()
}
