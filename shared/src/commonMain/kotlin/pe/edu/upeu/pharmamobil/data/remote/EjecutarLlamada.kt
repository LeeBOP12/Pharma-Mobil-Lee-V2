package pe.edu.upeu.pharmamobil.data.remote

import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.errors.IOException
import kotlin.coroutines.cancellation.CancellationException
import pe.edu.upeu.pharmamobil.data.remote.dto.ErrorResponseDto
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException

suspend fun <T> ejecutarLlamadaRemota(bloque: suspend () -> T): T {
    return try {
        bloque()
    } catch (cancelacion: CancellationException) {
        throw cancelacion
    } catch (e: ClientRequestException) {
        throw traducir4xx(e)
    } catch (_: ServerResponseException) {
        throw ErrorApiException(ErrorApi.Servidor)
    } catch (_: HttpRequestTimeoutException) {
        throw ErrorApiException(ErrorApi.TiempoAgotado)
    } catch (_: IOException) {
        throw ErrorApiException(ErrorApi.SinConexion)
    }
}

private suspend fun traducir4xx(e: ClientRequestException): ErrorApiException {
    val cuerpo = runCatching { e.response.body<ErrorResponseDto>() }.getOrNull()
    val mensaje = cuerpo?.message?.takeIf { it.isNotBlank() }
        ?: e.response.status.description

    val error = when (e.response.status) {
        HttpStatusCode.BadRequest -> ErrorApi.Validacion(cuerpo?.validationErrors.orEmpty())
        HttpStatusCode.Unauthorized -> ErrorApi.NoAutorizado
        HttpStatusCode.NotFound -> ErrorApi.NoEncontrado
        HttpStatusCode.Conflict -> ErrorApi.Conflicto(mensaje)
        else -> ErrorApi.Desconocido(mensaje)
    }

    return ErrorApiException(error)
}
