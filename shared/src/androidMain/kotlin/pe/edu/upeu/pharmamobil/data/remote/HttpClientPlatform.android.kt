package pe.edu.upeu.pharmamobil.data.remote

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.android.Android

actual fun motorHttp(): HttpClientEngineFactory<*> = Android

actual val baseUrlBackend: String = "http://10.0.2.2:8080/api/v1/"
