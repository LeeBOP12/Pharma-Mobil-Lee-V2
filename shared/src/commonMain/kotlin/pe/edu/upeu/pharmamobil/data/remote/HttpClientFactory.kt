package pe.edu.upeu.pharmamobil.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import pe.edu.upeu.pharmamobil.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobil.domain.session.GestorSesion

expect fun motorHttp(): HttpClientEngineFactory<*>

expect val baseUrlBackend: String

fun crearHttpClient(
    almacenSeguro: AlmacenSeguro,
    gestorSesion: GestorSesion,
    esDepuracion: Boolean
): HttpClient {
    return HttpClient(motorHttp()) {
        expectSuccess = true

        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = true
                }
            )
        }

        install(Logging) {
            level = if (esDepuracion) LogLevel.HEADERS else LogLevel.NONE
            sanitizeHeader { cabecera ->
                cabecera.equals(HttpHeaders.Authorization, ignoreCase = true)
            }
        }

        install(Auth) {
            bearer {
                loadTokens {
                    almacenSeguro.leerToken()
                        ?.takeIf { it.isNotBlank() }
                        ?.let { BearerTokens(accessToken = it, refreshToken = "") }
                }

                sendWithoutRequest { request ->
                    !request.url.build().encodedPath.endsWith("/auth/login")
                }
            }
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 15000
            connectTimeoutMillis = 10000
        }

        defaultRequest {
            url(baseUrlBackend)
            contentType(ContentType.Application.Json)
        }

        HttpResponseValidator {
            handleResponseExceptionWithRequest { causa, request ->
                val excepcion = causa as? ClientRequestException
                val esLogin = request.url.encodedPath.endsWith("/auth/login")

                if (
                    excepcion?.response?.status == HttpStatusCode.Unauthorized &&
                    !esLogin
                ) {
                    almacenSeguro.borrarToken()
                    gestorSesion.cerrar(
                        "Tu sesión venció o ya no es válida. Inicia sesión nuevamente."
                    )
                }
            }
        }
    }
}
