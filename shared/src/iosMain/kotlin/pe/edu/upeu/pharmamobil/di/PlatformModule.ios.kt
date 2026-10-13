package pe.edu.upeu.pharmamobil.di

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform
import org.koin.core.qualifier.named
import org.koin.core.module.Module
import org.koin.dsl.module
import pe.edu.upeu.pharmamobil.domain.platform.Compartidor
import pe.edu.upeu.pharmamobil.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobil.data.local.AlmacenSeguroIos
import pe.edu.upeu.pharmamobil.platform.CompartidorIos

@OptIn(ExperimentalNativeApi::class)
actual val platformModule: Module = module {
    single<Boolean>(qualifier = named("esDepuracion")) {
        Platform.isDebugBinary
    }
    single<AlmacenSeguro> { AlmacenSeguroIos() }
    single<Compartidor> { CompartidorIos() }
}
