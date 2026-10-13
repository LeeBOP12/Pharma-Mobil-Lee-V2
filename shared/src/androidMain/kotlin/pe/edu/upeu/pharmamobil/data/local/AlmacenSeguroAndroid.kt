package pe.edu.upeu.pharmamobil.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

@Suppress("DEPRECATION")
class AlmacenSeguroAndroid(
    context: Context
) : AlmacenSeguro {

    private val preferencias by lazy {
        val contexto = context.applicationContext
        val llaveMaestra = MasterKey.Builder(contexto)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            contexto,
            ARCHIVO_SEGURO,
            llaveMaestra,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override fun guardarToken(token: String) {
        preferencias.edit().putString(CLAVE_TOKEN, token).apply()
    }

    override fun leerToken(): String? {
        return preferencias.getString(CLAVE_TOKEN, null)
    }

    override fun borrarToken() {
        preferencias.edit().remove(CLAVE_TOKEN).apply()
    }

    private companion object {
        const val ARCHIVO_SEGURO = "pharmamobil_sesion_segura"
        const val CLAVE_TOKEN = "jwt"
    }
}
