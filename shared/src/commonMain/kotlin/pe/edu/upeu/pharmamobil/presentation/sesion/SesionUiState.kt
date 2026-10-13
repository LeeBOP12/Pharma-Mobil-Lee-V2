package pe.edu.upeu.pharmamobil.presentation.sesion

import pe.edu.upeu.pharmamobil.domain.model.Sesion

data class SesionUiState(
    val verificandoSesion: Boolean = true,
    val cargando: Boolean = false,
    val usuario: String = "",
    val clave: String = "",
    val sesion: Sesion? = null,
    val error: String? = null
)
