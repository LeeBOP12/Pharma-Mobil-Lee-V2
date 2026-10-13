package pe.edu.upeu.pharmamobil.di

import android.content.pm.ApplicationInfo
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.core.module.Module
import org.koin.dsl.module
import pe.edu.upeu.pharmamobil.domain.platform.Compartidor
import pe.edu.upeu.pharmamobil.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobil.data.local.AlmacenSeguroAndroid
import pe.edu.upeu.pharmamobil.platform.CompartidorAndroid

actual val platformModule: Module = module {
    single<Boolean>(qualifier = named("esDepuracion")) {
        (androidContext().applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }
    single<AlmacenSeguro> { AlmacenSeguroAndroid(androidContext()) }
    single<Compartidor> { CompartidorAndroid(androidContext()) }
}
