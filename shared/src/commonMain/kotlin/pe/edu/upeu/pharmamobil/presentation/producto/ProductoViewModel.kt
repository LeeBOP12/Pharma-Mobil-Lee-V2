package pe.edu.upeu.pharmamobil.presentation.producto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
import pe.edu.upeu.pharmamobil.domain.error.mensajeLegible
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.platform.Compartidor
import pe.edu.upeu.pharmamobil.domain.usecase.ActualizarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.EliminarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ListarProductosUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ProductoInvalidoException
import pe.edu.upeu.pharmamobil.domain.usecase.RegistrarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.comoTextoParaCompartir


class ProductoViewModel(
    private val registrarProducto: RegistrarProductoUseCase,
    private val listarProductos: ListarProductosUseCase,
    private val actualizarProducto: ActualizarProductoUseCase,
    private val eliminarProducto: EliminarProductoUseCase,
    private val compartidor: Compartidor
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductoUiState())
    val uiState: StateFlow<ProductoUiState> = _uiState.asStateFlow()

    init {
        observarProductos()
        sincronizar()
    }

    private fun observarProductos() {
        viewModelScope.launch {
            listarProductos.observar()
                .catch { fallo ->
                    _uiState.update {
                        it.copy(
                            fase = ProductoUiState.Fase.Error(
                                fallo.message ?: "No se pudo leer el inventario guardado"
                            )
                        )
                    }
                }
                .collect { productos ->
                    _uiState.update {
                        it.copy(
                            fase = if (productos.isEmpty()) {
                                ProductoUiState.Fase.SinProductos
                            } else {
                                ProductoUiState.Fase.ConProductos(productos.map { producto -> producto.aUi() })
                            }
                        )
                    }
                }
        }
    }

    fun sincronizar() {
        if (_uiState.value.sincronizando) return

        viewModelScope.launch {
            _uiState.update { it.copy(sincronizando = true) }

            listarProductos.sincronizar().fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(sincronizando = false, avisoRed = null)
                    }
                },
                onFailure = { fallo ->
                    val aviso = when (fallo) {
                        is ErrorApiException -> when (fallo.error) {
                            ErrorApi.SinConexion, ErrorApi.TiempoAgotado ->
                                "Sin conexión: mostrando datos guardados"

                            ErrorApi.Servidor ->
                                "Servidor no disponible: mostrando datos guardados"

                            else -> fallo.error.mensajeLegible()
                        }

                        else -> fallo.message ?: "No se pudo sincronizar el inventario"
                    }
                    _uiState.update {
                        it.copy(sincronizando = false, avisoRed = aviso)
                    }
                }
            )
        }
    }

    fun onNombreChange(nombre: String) {
        _uiState.update {
            it.copy(
                formulario = it.formulario.copy(nombre = nombre, nombreError = null),
                operacion = Operacion.Inactiva,
                mensajeExito = null
            )
        }
    }

    fun onPrecioChange(precio: String) {
        _uiState.update {
            it.copy(
                formulario = it.formulario.copy(precio = precio, precioError = null),
                operacion = Operacion.Inactiva,
                mensajeExito = null
            )
        }
    }

    fun onStockChange(stock: String) {
        _uiState.update {
            it.copy(
                formulario = it.formulario.copy(stock = stock, stockError = null),
                operacion = Operacion.Inactiva,
                mensajeExito = null
            )
        }
    }

    fun registrar() {
        val formulario = _uiState.value.formulario
        if (formulario.estaEditando) {
            actualizar()
            return
        }

        if (_uiState.value.operacion is Operacion.EnCurso) return

        viewModelScope.launch {

            _uiState.update {
                it.copy(
                    operacion = Operacion.EnCurso(TipoOperacion.Crear),
                    formulario = it.formulario.sinErrores(),
                    mensajeExito = null
                )
            }

            registrarProducto(
                nombre = formulario.nombre,
                precio = formulario.precio,
                stock = formulario.stock
            ).fold(
                onSuccess = { producto ->
                    _uiState.update {
                        it.copy(
                            operacion = Operacion.Inactiva,
                            formulario = FormularioProducto(),
                            mensajeExito = "Producto \"${producto.nombre}\" registrado correctamente"
                        )
                    }
                },
                onFailure = { fallo ->
                    manejarFalloFormulario(fallo, TipoOperacion.Crear)
                }
            )
        }
    }

    fun seleccionarParaEditar(producto: ProductoUi) {
        _uiState.update {
            it.copy(
                formulario = FormularioProducto(
                    id = producto.id,
                    nombre = producto.nombre,
                    precio = producto.precioValor.toString(),
                    stock = producto.stockValor.toString()
                ),
                operacion = Operacion.Inactiva,
                mensajeExito = null
            )
        }
    }

    fun cancelarEdicion() {
        _uiState.update {
            it.copy(
                formulario = FormularioProducto(),
                operacion = Operacion.Inactiva,
                mensajeExito = null
            )
        }
    }

    fun compartir(producto: ProductoUi) {
        val productoDominio = Producto(
            id = producto.id,
            nombre = producto.nombre,
            precio = producto.precioValor,
            stock = producto.stockValor
        )
        compartidor.compartir(productoDominio.comoTextoParaCompartir())
    }

    private fun actualizar() {
        val formulario = _uiState.value.formulario
        val id = formulario.id ?: return

        if (_uiState.value.operacion is Operacion.EnCurso) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    operacion = Operacion.EnCurso(TipoOperacion.Actualizar),
                    formulario = it.formulario.sinErrores(),
                    mensajeExito = null
                )
            }

            actualizarProducto(
                id = id,
                nombre = formulario.nombre,
                precio = formulario.precio,
                stock = formulario.stock
            ).fold(
                onSuccess = { producto ->
                    _uiState.update {
                        it.copy(
                            operacion = Operacion.Inactiva,
                            formulario = FormularioProducto(),
                            mensajeExito = "Producto \"${producto.nombre}\" actualizado correctamente"
                        )
                    }
                },
                onFailure = { fallo ->
                    manejarFalloFormulario(fallo, TipoOperacion.Actualizar)
                }
            )
        }
    }

    fun eliminar(producto: ProductoUi) {
        if (_uiState.value.operacion is Operacion.EnCurso) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    operacion = Operacion.EnCurso(TipoOperacion.Eliminar),
                    mensajeExito = null
                )
            }

            eliminarProducto(producto.id).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            operacion = Operacion.Inactiva,
                            mensajeExito = "Producto \"${producto.nombre}\" eliminado"
                        )
                    }
                },
                onFailure = { fallo ->
                    _uiState.update {
                        it.copy(
                            operacion = Operacion.Fallida(mensajeDe(fallo))
                        )
                    }
                }
            )
        }
    }

    private fun manejarFalloFormulario(
        fallo: Throwable,
        tipo: TipoOperacion
    ) {
        when (fallo) {
            is ProductoInvalidoException -> _uiState.update {
                it.copy(
                    operacion = Operacion.Fallida(mensajeDe(fallo)),
                    formulario = it.formulario.copy(
                        nombreError = fallo.errores.nombre,
                        precioError = fallo.errores.precio,
                        stockError = fallo.errores.stock
                    )
                )
            }

            is ErrorApiException -> {
                val validacion = fallo.error as? ErrorApi.Validacion
                _uiState.update {
                    it.copy(
                        operacion = Operacion.Fallida(fallo.error.mensajeLegible()),
                        formulario = if (validacion == null) {
                            it.formulario
                        } else {
                            it.formulario.copy(
                                nombreError = validacion.porCampo["nombre"],
                                precioError = validacion.porCampo["precio"],
                                stockError = validacion.porCampo["stock"]
                            )
                        }
                    )
                }
            }

            else -> _uiState.update {
                it.copy(
                    operacion = Operacion.Fallida(
                        fallo.message ?: "No se pudo completar la operacion ${tipo.name.lowercase()}"
                    )
                )
            }
        }
    }

    private fun mensajeDe(fallo: Throwable): String {
        return when (fallo) {
            is ErrorApiException -> fallo.error.mensajeLegible()
            else -> fallo.message ?: "No se pudo completar la operacion"
        }
    }
}
