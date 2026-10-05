package pe.edu.upeu.pharmamobil.domain.error

sealed interface ErrorApi {
    data class Validacion(val porCampo: Map<String, String>) : ErrorApi
    data object NoEncontrado : ErrorApi
    data class Conflicto(val mensaje: String) : ErrorApi
    data object NoAutorizado : ErrorApi
    data object Servidor : ErrorApi
    data object SinConexion : ErrorApi
    data object TiempoAgotado : ErrorApi
    data class Desconocido(val mensaje: String) : ErrorApi
}

class ErrorApiException(
    val error: ErrorApi
) : RuntimeException(error.mensajeLegible())

fun ErrorApi.mensajeLegible(): String {
    return when (this) {
        is ErrorApi.Validacion -> "Revisa los datos del formulario"
        ErrorApi.NoEncontrado -> "El recurso ya no existe"
        is ErrorApi.Conflicto -> mensaje
        ErrorApi.NoAutorizado -> "No autorizado"
        ErrorApi.Servidor -> "El servidor no pudo procesar la solicitud"
        ErrorApi.SinConexion -> "No hay conexion con el servidor"
        ErrorApi.TiempoAgotado -> "La solicitud tardo demasiado"
        is ErrorApi.Desconocido -> mensaje
    }
}
