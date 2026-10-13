package pe.edu.upeu.pharmamobil.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import pe.edu.upeu.pharmamobil.data.remote.dto.LoginRequestDto
import pe.edu.upeu.pharmamobil.data.remote.dto.LoginResponseDto
import pe.edu.upeu.pharmamobil.data.remote.dto.UsuarioResponseDto

class AuthApi(
    private val client: HttpClient
) {
    suspend fun iniciarSesion(request: LoginRequestDto): LoginResponseDto {
        return client.post("auth/login") {
            setBody(request)
        }.body()
    }

    suspend fun usuarioActual(): UsuarioResponseDto {
        return client.get("auth/me").body()
    }
}
