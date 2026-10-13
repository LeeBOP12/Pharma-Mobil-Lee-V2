package pe.edu.upeu.pharmamobil.domain.usecase

import pe.edu.upeu.pharmamobil.domain.model.Sesion
import pe.edu.upeu.pharmamobil.domain.repository.AuthRepository

class RecuperarSesionUseCase(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(): Result<Sesion?> = resultadoDe {
        repository.sesionGuardada()
    }
}
