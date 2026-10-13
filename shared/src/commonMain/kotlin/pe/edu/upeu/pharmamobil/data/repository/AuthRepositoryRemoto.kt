package pe.edu.upeu.pharmamobil.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.clearAuthTokens
import pe.edu.upeu.pharmamobil.data.mapper.toDomain
import pe.edu.upeu.pharmamobil.data.remote.AuthApi
import pe.edu.upeu.pharmamobil.data.remote.dto.LoginRequestDto
import pe.edu.upeu.pharmamobil.data.remote.ejecutarLlamadaSesion
import pe.edu.upeu.pharmamobil.domain.model.Credenciales
import pe.edu.upeu.pharmamobil.domain.model.ErrorSesion
import pe.edu.upeu.pharmamobil.domain.model.ErrorSesionException
import pe.edu.upeu.pharmamobil.domain.model.Sesion
import pe.edu.upeu.pharmamobil.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobil.domain.repository.AuthRepository
import pe.edu.upeu.pharmamobil.domain.session.GestorSesion

class AuthRepositoryRemoto(
    private val authApi: AuthApi,
    private val almacenSeguro: AlmacenSeguro,
    private val client: HttpClient,
    private val gestorSesion: GestorSesion
) : AuthRepository {

    override suspend fun iniciarSesion(credenciales: Credenciales): Sesion {
        return ejecutarLlamadaSesion {
            val respuesta = authApi.iniciarSesion(
                LoginRequestDto(
                    username = credenciales.usuario,
                    password = credenciales.clave
                )
            )

            almacenSeguro.guardarToken(respuesta.token)
            client.clearAuthTokens()

            respuesta.toDomain().also(gestorSesion::abrir)
        }
    }

    override suspend fun sesionGuardada(): Sesion? {
        val token = almacenSeguro.leerToken()?.takeIf { it.isNotBlank() }
            ?: return null

        client.clearAuthTokens()

        return try {
            ejecutarLlamadaSesion {
                authApi.usuarioActual().toDomain(token).also(gestorSesion::abrir)
            }
        } catch (fallo: ErrorSesionException) {
            if (
                fallo.error == ErrorSesion.CredencialesInvalidas ||
                fallo.error == ErrorSesion.SesionExpirada
            ) {
                almacenSeguro.borrarToken()
                client.clearAuthTokens()
                gestorSesion.cerrar(
                    "Tu sesión venció o ya no es válida. Inicia sesión nuevamente."
                )
                throw ErrorSesionException(ErrorSesion.SesionExpirada)
            } else {
                throw fallo
            }
        }
    }

    override suspend fun cerrarSesion() {
        almacenSeguro.borrarToken()
        client.clearAuthTokens()
        gestorSesion.cerrar()
    }
}
