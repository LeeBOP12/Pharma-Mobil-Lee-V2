package pe.edu.upeu.pharmamobil.presentation.producto

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pe.edu.upeu.pharmamobil.data.repository.FakeProductoRepository
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.platform.Compartidor
import pe.edu.upeu.pharmamobil.domain.usecase.ActualizarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.EliminarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ListarProductosUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.RegistrarProductoUseCase
import pe.edu.upeu.pharmamobil.platform.formatearSoles
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * viewModelScope corre sobre Dispatchers.Main, que en una prueba no existe:
 * setMain lo sustituye por un dispatcher de prueba antes de cada caso.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProductoViewModelTest {

    @BeforeTest
    fun instalarMain() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun restaurarMain() {
        Dispatchers.resetMain()
    }

    private fun nuevoViewModel(
        repositorio: FakeProductoRepository = FakeProductoRepository()
    ) = ProductoViewModel(
        registrarProducto = RegistrarProductoUseCase(repositorio),
        listarProductos = ListarProductosUseCase(repositorio),
        actualizarProducto = ActualizarProductoUseCase(repositorio),
        eliminarProducto = EliminarProductoUseCase(repositorio),
        compartidor = object : Compartidor {
            override fun compartir(texto: String) = Unit
        }
    )

    @Test
    fun arrancaEnSinProductosCuandoElInventarioEstaVacio() = runTest {

        val viewModel = nuevoViewModel()

        assertEquals(ProductoUiState.Fase.SinProductos, viewModel.uiState.value.fase)
    }

    @Test
    fun transicionaACargandoMientrasConsultaElInventario() = runTest {

        val continuarConsulta = CompletableDeferred<Unit>()
        val repositorio = FakeProductoRepository(
            mutableListOf(
                Producto(id = 1L, nombre = "Paracetamol", precio = 12.5, stock = 5)
            )
        ).apply {
            antesDeListar = { continuarConsulta.await() }
        }
        val viewModel = nuevoViewModel(repositorio)

        assertEquals(ProductoUiState.Fase.Cargando, viewModel.uiState.value.fase)

        continuarConsulta.complete(Unit)
        advanceUntilIdle()

        val fase = assertIs<ProductoUiState.Fase.ConProductos>(
            viewModel.uiState.value.fase
        )
        assertEquals("Paracetamol", fase.productos.single().nombre)
        assertEquals(1, repositorio.llamadasAListar)
    }

    @Test
    fun muestraElInventarioConElPrecioYaFormateado() = runTest {

        val repositorio = FakeProductoRepository(
            mutableListOf(
                Producto(id = 1L, nombre = "Paracetamol", precio = 12.5, stock = 5),
                Producto(id = 2L, nombre = "Ibuprofeno", precio = 8.9, stock = 20),
                Producto(id = 3L, nombre = "Amoxicilina", precio = 18.0, stock = 12)
            )
        )

        val fase = assertIs<ProductoUiState.Fase.ConProductos>(
            nuevoViewModel(repositorio).uiState.value.fase
        )

        assertEquals(3, fase.productos.size)
        assertEquals(formatearSoles(12.5), fase.productos.first().precio)
        assertEquals("5 u.", fase.productos.first().stock)
        assertTrue(fase.productos.first().requiereReposicion)
    }

    @Test
    fun pasaAFaseErrorCuandoElRepositorioFalla() = runTest {

        val repositorio = FakeProductoRepository().apply {
            fallaAlListar = IllegalStateException("Sin conexión")
        }

        val fase = assertIs<ProductoUiState.Fase.Error>(
            nuevoViewModel(repositorio).uiState.value.fase
        )

        assertEquals("Sin conexión", fase.mensaje)
    }

    @Test
    fun precioCeroDejaErrorEnFormularioSinLlamarAlRepositorio() = runTest {

        val repositorio = FakeProductoRepository()
        val viewModel = nuevoViewModel(repositorio)

        viewModel.onNombreChange("Paracetamol")
        viewModel.onPrecioChange("0")
        viewModel.onStockChange("5")
        viewModel.registrar()

        val estado = viewModel.uiState.value

        assertNull(estado.formulario.nombreError)
        assertEquals("El precio debe ser mayor a 0", estado.formulario.precioError)
        assertNull(estado.formulario.stockError)
        assertEquals(ProductoUiState.Fase.SinProductos, estado.fase)
        assertNull(estado.mensajeExito)
        assertEquals(0, repositorio.llamadasARegistrar)
    }

    @Test
    fun validacionDelServidorSeMuestraEnLosCamposDelFormulario() = runTest {

        val repositorio = FakeProductoRepository().apply {
            fallaAlRegistrar = ErrorApiException(
                ErrorApi.Validacion(
                    mapOf(
                        "nombre" to "El nombre ya existe",
                        "precio" to "El precio supera el maximo permitido",
                        "stock" to "El stock supera el maximo permitido"
                    )
                )
            )
        }
        val viewModel = nuevoViewModel(repositorio)

        viewModel.onNombreChange("Paracetamol")
        viewModel.onPrecioChange("12.50")
        viewModel.onStockChange("5")
        viewModel.registrar()

        val estado = viewModel.uiState.value
        val operacion = assertIs<Operacion.Fallida>(estado.operacion)

        assertEquals("Revisa los datos del formulario", operacion.mensaje)
        assertEquals("El nombre ya existe", estado.formulario.nombreError)
        assertEquals(
            "El precio supera el maximo permitido",
            estado.formulario.precioError
        )
        assertEquals(
            "El stock supera el maximo permitido",
            estado.formulario.stockError
        )
        assertNull(estado.mensajeExito)
        assertEquals(1, repositorio.llamadasARegistrar)
    }

    @Test
    fun registrarLimpiaElFormularioYRecargaElInventario() = runTest {

        val viewModel = nuevoViewModel()

        viewModel.onNombreChange("Paracetamol")
        viewModel.onPrecioChange("12.50")
        viewModel.onStockChange("5")
        viewModel.registrar()

        val estado = viewModel.uiState.value
        val fase = assertIs<ProductoUiState.Fase.ConProductos>(estado.fase)

        assertEquals("Paracetamol", fase.productos.single().nombre)
        assertEquals("", estado.formulario.nombre)
        assertEquals("", estado.formulario.precio)
        assertEquals("", estado.formulario.stock)
        assertEquals(
            "Producto \"Paracetamol\" registrado correctamente",
            estado.mensajeExito
        )
    }

    @Test
    fun eliminarRecargaElInventarioSinElProductoEliminado() = runTest {

        val repositorio = FakeProductoRepository(
            mutableListOf(
                Producto(id = 1L, nombre = "Paracetamol", precio = 12.5, stock = 5),
                Producto(id = 2L, nombre = "Ibuprofeno", precio = 8.9, stock = 20)
            )
        )
        val viewModel = nuevoViewModel(repositorio)
        val productoAEliminar = assertIs<ProductoUiState.Fase.ConProductos>(
            viewModel.uiState.value.fase
        ).productos.first()

        viewModel.eliminar(productoAEliminar)

        val estado = viewModel.uiState.value
        val fase = assertIs<ProductoUiState.Fase.ConProductos>(estado.fase)

        assertEquals(listOf("Ibuprofeno"), fase.productos.map { it.nombre })
        assertEquals(Operacion.Inactiva, estado.operacion)
        assertEquals("Producto \"Paracetamol\" eliminado", estado.mensajeExito)
        assertEquals(1, repositorio.llamadasAEliminar)
        assertEquals(2, repositorio.llamadasAListar)
    }
}
