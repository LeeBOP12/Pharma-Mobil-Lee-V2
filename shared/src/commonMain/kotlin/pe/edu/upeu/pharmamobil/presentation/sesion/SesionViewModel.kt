package pe.edu.upeu.pharmamobil.presentation.sesion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
import pe.edu.upeu.pharmamobil.domain.error.mensajeLegible
import pe.edu.upeu.pharmamobil.domain.model.ErrorSesionException
import pe.edu.upeu.pharmamobil.domain.session.GestorSesion
import pe.edu.upeu.pharmamobil.domain.usecase.CerrarSesionUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.IniciarSesionUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.RecuperarSesionUseCase

class SesionViewModel(
    private val iniciarSesion: IniciarSesionUseCase,
    private val recuperarSesion: RecuperarSesionUseCase,
    private val cerrarSesion: CerrarSesionUseCase,
    private val gestorSesion: GestorSesion
) : ViewModel() {

    private val _uiState = MutableStateFlow(SesionUiState())
    val uiState: StateFlow<SesionUiState> = _uiState.asStateFlow()

    init {
        observarSesion()
        restaurarSesion()
    }

    fun onUsuarioChange(usuario: String) {
        gestorSesion.consumirAviso()
        _uiState.update { it.copy(usuario = usuario, error = null) }
    }

    fun onClaveChange(clave: String) {
        gestorSesion.consumirAviso()
        _uiState.update { it.copy(clave = clave, error = null) }
    }

    fun ingresar() {
        val estado = _uiState.value
        if (estado.cargando) return

        val validacion = when {
            estado.usuario.isBlank() -> "Ingresa tu usuario"
            estado.clave.isBlank() -> "Ingresa tu contraseña"
            else -> null
        }

        if (validacion != null) {
            _uiState.update { it.copy(error = validacion) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }

            iniciarSesion(estado.usuario, estado.clave).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            cargando = false,
                            usuario = "",
                            clave = "",
                            error = null
                        )
                    }
                },
                onFailure = { fallo ->
                    _uiState.update {
                        it.copy(
                            cargando = false,
                            error = mensajeDe(fallo, duranteLogin = true)
                        )
                    }
                }
            )
        }
    }

    fun salir() {
        viewModelScope.launch {
            cerrarSesion().onFailure { fallo ->
                _uiState.update {
                    it.copy(error = mensajeDe(fallo, duranteLogin = false))
                }
            }
        }
    }

    private fun restaurarSesion() {
        viewModelScope.launch {
            recuperarSesion().fold(
                onSuccess = {
                    _uiState.update { estado ->
                        estado.copy(verificandoSesion = false)
                    }
                },
                onFailure = { fallo ->
                    _uiState.update {
                        it.copy(
                            verificandoSesion = false,
                            error = mensajeDe(fallo, duranteLogin = false)
                        )
                    }
                }
            )
        }
    }

    private fun observarSesion() {
        viewModelScope.launch {
            gestorSesion.estado.collect { estado ->
                _uiState.update {
                    it.copy(
                        sesion = estado.sesion,
                        error = estado.aviso ?: it.error
                    )
                }
            }
        }
    }

    private fun mensajeDe(fallo: Throwable, duranteLogin: Boolean): String {
        if (
            duranteLogin &&
            fallo is ErrorApiException &&
            fallo.error == ErrorApi.NoAutorizado
        ) {
            return "Usuario o contraseña incorrectos"
        }

        return when (fallo) {
            is ErrorSesionException ->
                fallo.message ?: "No se pudo completar la operación"
            is ErrorApiException -> fallo.error.mensajeLegible()
            else -> fallo.message ?: "No se pudo completar la operación"
        }
    }
}
