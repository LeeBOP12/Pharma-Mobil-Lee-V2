package pe.edu.upeu.pharmamobil.data.local

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionarySetValue
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFStringCreateWithCString
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFStringEncodingUTF8
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

@OptIn(ExperimentalForeignApi::class)
class AlmacenSeguroIos : AlmacenSeguro {

    override fun guardarToken(token: String) {
        require(token.isNotBlank()) { "El token no puede estar vacío" }
        borrarToken()

        val bytes = token.encodeToByteArray()
        val datos = bytes.usePinned { fijado ->
            CFDataCreate(
                allocator = kCFAllocatorDefault,
                bytes = fijado.addressOf(0).reinterpret(),
                length = bytes.size.toLong()
            )
        } ?: error("No se pudo preparar el token para Keychain")

        val consulta = crearConsulta()
        CFDictionarySetValue(consulta, kSecValueData, datos)

        val estado = SecItemAdd(consulta, null)
        liberar(consulta)
        liberar(datos)

        check(estado == errSecSuccess) {
            "Keychain no pudo guardar el token (código $estado)"
        }
    }

    override fun leerToken(): String? = memScoped {
        val consulta = crearConsulta()
        CFDictionarySetValue(consulta, kSecReturnData, kCFBooleanTrue)
        CFDictionarySetValue(consulta, kSecMatchLimit, kSecMatchLimitOne)

        val resultado = alloc<CFTypeRefVar>()
        val estado = SecItemCopyMatching(consulta, resultado.ptr)
        liberar(consulta)

        if (estado == errSecItemNotFound) return@memScoped null
        check(estado == errSecSuccess) {
            "Keychain no pudo leer el token (código $estado)"
        }

        val datos: CFDataRef = resultado.value?.reinterpret()
            ?: return@memScoped null
        val longitud = CFDataGetLength(datos).toInt()
        val origen = CFDataGetBytePtr(datos)
        if (origen == null) {
            liberar(datos)
            return@memScoped null
        }
        val bytes = ByteArray(longitud) { indice -> origen[indice].toByte() }

        liberar(datos)
        bytes.decodeToString()
    }

    override fun borrarToken() {
        val consulta = crearConsulta()
        val estado = SecItemDelete(consulta)
        liberar(consulta)

        check(estado == errSecSuccess || estado == errSecItemNotFound) {
            "Keychain no pudo borrar el token (código $estado)"
        }
    }

    private fun crearConsulta(): CFMutableDictionaryRef {
        val consulta = CFDictionaryCreateMutable(
            allocator = kCFAllocatorDefault,
            capacity = 0,
            keyCallBacks = kCFTypeDictionaryKeyCallBacks.ptr,
            valueCallBacks = kCFTypeDictionaryValueCallBacks.ptr
        ) ?: error("No se pudo crear la consulta de Keychain")

        val servicio = crearTexto(SERVICIO)
        val cuenta = crearTexto(CUENTA)

        CFDictionarySetValue(consulta, kSecClass, kSecClassGenericPassword)
        CFDictionarySetValue(consulta, kSecAttrService, servicio)
        CFDictionarySetValue(consulta, kSecAttrAccount, cuenta)

        liberar(servicio)
        liberar(cuenta)
        return consulta
    }

    private fun crearTexto(valor: String): CFStringRef {
        return CFStringCreateWithCString(
            alloc = kCFAllocatorDefault,
            cStr = valor,
            encoding = kCFStringEncodingUTF8
        ) ?: error("No se pudo crear el identificador de Keychain")
    }

    private fun liberar(referencia: CFTypeRef?) {
        if (referencia != null) CFBridgingRelease(referencia)
    }

    private companion object {
        const val SERVICIO = "pe.edu.upeu.pharmamobil"
        const val CUENTA = "sesion-jwt"
    }
}
