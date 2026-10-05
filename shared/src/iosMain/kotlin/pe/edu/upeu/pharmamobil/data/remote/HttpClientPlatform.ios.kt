package pe.edu.upeu.pharmamobil.data.remote

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin

actual fun motorHttp(): HttpClientEngineFactory<*> = Darwin

actual val baseUrlBackend: String = "http://localhost:8080/api/v1/"
