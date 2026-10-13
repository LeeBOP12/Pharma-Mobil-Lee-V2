package pe.edu.upeu.pharmamobil.domain.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import pe.edu.upeu.pharmamobil.domain.model.Sesion

data class EstadoSesion(
    val sesion: Sesion? = null,
    val aviso: String? = null
)

class GestorSesion {
    private val _estado = MutableStateFlow(EstadoSesion())
    val estado: StateFlow<EstadoSesion> = _estado.asStateFlow()

    fun abrir(sesion: Sesion) {
        _estado.value = EstadoSesion(sesion = sesion)
    }

    fun cerrar(aviso: String? = null) {
        _estado.value = EstadoSesion(aviso = aviso)
    }

    fun consumirAviso() {
        _estado.update { it.copy(aviso = null) }
    }
}
