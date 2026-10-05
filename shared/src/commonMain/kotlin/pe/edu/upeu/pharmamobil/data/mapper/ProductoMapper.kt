package pe.edu.upeu.pharmamobil.data.mapper

import pe.edu.upeu.pharmamobil.data.remote.dto.ProductoRequestDto
import pe.edu.upeu.pharmamobil.data.remote.dto.ProductoResponseDto
import pe.edu.upeu.pharmamobil.domain.model.Producto

fun ProductoResponseDto.toDomain(): Producto {
    return Producto(
        id = id,
        nombre = nombre,
        precio = precio,
        stock = stock
    )
}

fun Producto.toRequestDto(categoriaId: Long): ProductoRequestDto {
    return ProductoRequestDto(
        nombre = nombre,
        precio = precio,
        stock = stock,
        categoriaId = categoriaId
    )
}
