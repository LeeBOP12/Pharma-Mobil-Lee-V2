package pe.edu.upeu.pharmamobil.data.remote

import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.errors.IOException
import kotlin.coroutines.cancellation.CancellationException
import pe.edu.upeu.pharmamobil.data.remote.dto.ErrorResponseDto
import pe.edu.upeu.pharmamobil.domain.model.ErrorSesion
import pe.edu.upeu.pharmamobil.domain.model.ErrorSesionException

suspend fun <T> ejecutarLlamadaSesion(bloque: suspend () -> T): T {
    return try {
        bloque()
    } catch (cancelacion: CancellationException) {
        throw cancelacion
    } catch (fallo: ClientRequestException) {
        val cuerpo = runCatching { fallo.response.body<ErrorResponseDto>() }.getOrNull()
        throw ErrorSesionException(
            mapearErrorSesion(
                estado = fallo.response.status,
                mensaje = cuerpo?.message
            )
        )
    } catch (_: ServerResponseException) {
        throw ErrorSesionException(ErrorSesion.SinConexion)
    } catch (_: HttpRequestTimeoutException) {
        throw ErrorSesionException(ErrorSesion.SinConexion)
    } catch (_: IOException) {
        throw ErrorSesionException(ErrorSesion.SinConexion)
    }
}

private fun mapearErrorSesion(
    estado: HttpStatusCode,
    mensaje: String?
): ErrorSesion {
    return when (estado) {
        HttpStatusCode.Unauthorized -> {
            if (mensaje?.contains("expir", ignoreCase = true) == true) {
                ErrorSesion.SesionExpirada
            } else {
                ErrorSesion.CredencialesInvalidas
            }
        }

        HttpStatusCode.Forbidden -> ErrorSesion.SinPermiso
        else -> ErrorSesion.SinConexion
    }
}
