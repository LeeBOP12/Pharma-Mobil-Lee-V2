package pe.edu.upeu.pharmamobil.domain.model

sealed interface ErrorSesion {
    data object CredencialesInvalidas : ErrorSesion
    data object SesionExpirada : ErrorSesion
    data object SinPermiso : ErrorSesion
    data object SinConexion : ErrorSesion
}

class ErrorSesionException(
    val error: ErrorSesion
) : RuntimeException(error.mensajeLegible())

fun ErrorSesion.mensajeLegible(): String {
    return when (this) {
        ErrorSesion.CredencialesInvalidas -> "Usuario o contraseña incorrectos"
        ErrorSesion.SesionExpirada ->
            "Tu sesión venció o ya no es válida. Inicia sesión nuevamente."
        ErrorSesion.SinPermiso -> "No tienes permisos para realizar esta acción"
        ErrorSesion.SinConexion -> "No se pudo conectar con el servidor"
    }
}
