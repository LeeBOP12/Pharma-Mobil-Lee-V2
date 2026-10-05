package pe.edu.upeu.pharmamobil.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CategoriaResponseDto(
    val id: Long,
    val nombre: String,
    val descripcion: String? = null,
    val estado: Boolean = true,
    val fechaCreacion: String? = null,
    val fechaModificacion: String? = null
)
