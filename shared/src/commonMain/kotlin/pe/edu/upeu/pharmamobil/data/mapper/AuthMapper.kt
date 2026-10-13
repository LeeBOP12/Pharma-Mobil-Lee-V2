package pe.edu.upeu.pharmamobil.data.mapper

import pe.edu.upeu.pharmamobil.data.remote.dto.LoginResponseDto
import pe.edu.upeu.pharmamobil.data.remote.dto.UsuarioResponseDto
import pe.edu.upeu.pharmamobil.domain.model.Sesion

fun LoginResponseDto.toDomain(): Sesion {
    return Sesion(
        token = token,
        usuario = username,
        nombreCompleto = nombreCompleto,
        permisos = permisos.toSet()
    )
}

fun UsuarioResponseDto.toDomain(token: String): Sesion {
    return Sesion(
        token = token,
        usuario = username,
        nombreCompleto = empleadoNombre,
        permisos = permisos.toSet()
    )
}
