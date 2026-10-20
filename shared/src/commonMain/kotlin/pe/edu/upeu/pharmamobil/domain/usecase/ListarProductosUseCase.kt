package pe.edu.upeu.pharmamobil.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

class ListarProductosUseCase(
    private val productoRepository: ProductoRepository
) {

    fun observar(): Flow<List<Producto>> = productoRepository.observar()

    suspend operator fun invoke(): Result<List<Producto>> = resultadoDe {
        productoRepository.observar().first()
    }

    suspend fun sincronizar(): Result<Unit> = resultadoDe {
        productoRepository.sincronizar()
    }
}
