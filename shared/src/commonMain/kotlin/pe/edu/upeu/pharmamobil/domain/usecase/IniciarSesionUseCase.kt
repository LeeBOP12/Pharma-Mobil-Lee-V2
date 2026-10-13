package pe.edu.upeu.pharmamobil.domain.usecase

import pe.edu.upeu.pharmamobil.domain.model.Credenciales
import pe.edu.upeu.pharmamobil.domain.model.Sesion
import pe.edu.upeu.pharmamobil.domain.repository.AuthRepository

class IniciarSesionUseCase(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(usuario: String, clave: String): Result<Sesion> = resultadoDe {
        repository.iniciarSesion(
            Credenciales(
                usuario = usuario.trim(),
                clave = clave
            )
        )
    }
}
