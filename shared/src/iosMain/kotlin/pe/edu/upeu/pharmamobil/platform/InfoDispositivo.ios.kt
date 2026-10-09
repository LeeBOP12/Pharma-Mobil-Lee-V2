package pe.edu.upeu.pharmamobil.platform

import platform.UIKit.UIDevice

actual class InfoDispositivo actual constructor() {
    private val dispositivo = UIDevice.currentDevice

    actual val sistema: String = dispositivo.systemName
    actual val version: String = dispositivo.systemVersion
}
