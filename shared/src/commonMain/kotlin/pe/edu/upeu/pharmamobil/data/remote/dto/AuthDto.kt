package pe.edu.upeu.pharmamobil.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDto(
    val username: String,
    val password: String
)

@Serializable
data class LoginResponseDto(
    val token: String,
    val tipo: String,
    val expiraEnSegundos: Long,
    val username: String,
    val nombreCompleto: String,
    val roles: List<String>,
    val permisos: List<String>
)

@Serializable
data class UsuarioResponseDto(
    val id: Long,
    val username: String,
    val empleadoId: Long,
    val empleadoNombre: String,
    val cargo: String,
    val roles: List<String>,
    val permisos: List<String>,
    val estado: Boolean,
    val fechaCreacion: String
)
