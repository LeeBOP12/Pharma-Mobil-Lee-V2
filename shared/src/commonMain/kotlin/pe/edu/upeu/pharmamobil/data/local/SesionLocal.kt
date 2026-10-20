package pe.edu.upeu.pharmamobil.data.local

import pe.edu.upeu.pharmamobil.db.PharmaDb
import pe.edu.upeu.pharmamobil.domain.model.Sesion

class SesionLocal(base: PharmaDb) {

    private val consultas = base.sesionLocalQueries

    fun guardar(sesion: Sesion) {
        consultas.guardarSesion(
            usuario = sesion.usuario,
            nombre_completo = sesion.nombreCompleto,
            permisos = sesion.permisos.sorted().joinToString(SEPARADOR)
        )
    }

    fun leer(token: String): Sesion? =
        consultas.leerSesion().executeAsOneOrNull()?.let { fila ->
            Sesion(
                token = token,
                usuario = fila.usuario,
                nombreCompleto = fila.nombre_completo,
                permisos = fila.permisos
                    .split(SEPARADOR)
                    .filter { it.isNotBlank() }
                    .toSet()
            )
        }

    fun borrar() {
        consultas.borrarSesion()
    }

    private companion object {
        const val SEPARADOR = ","
    }
}
