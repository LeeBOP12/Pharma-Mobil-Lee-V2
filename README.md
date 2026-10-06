This is a Kotlin Multiplatform project targeting Android, iOS.

* [/iosApp](./iosApp/iosApp) contains an iOS application. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/shared](./shared/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./shared/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./shared/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./shared/src/jvmMain/kotlin)
    folder is the appropriate location.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Android app: `./gradlew :androidApp:assembleDebug`
- iOS app: open the [/iosApp](./iosApp) directory in Xcode and run it from there.

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Android tests: `./gradlew :shared:testAndroidHostTest`
- iOS tests: `./gradlew :shared:iosSimulatorArm64Test`

### Conectividad REST

El proyecto consume el backend local de PharmaMobil mediante Ktor Client desde el modulo `shared`.

- URL base del backend local: `http://localhost:8080/api/v1/`
- URL base usada por Android Emulator: `http://10.0.2.2:8080/api/v1/`
- Endpoint consumido en la sesion 7: `GET /productos`
- Repositorio remoto: `ProductoRepositorioRemoto`
- Servicio remoto: `ProductoApi`
- DTO principales: `ProductoResponseDto`, `ProductoRequestDto`, `PaginaResponseDto` y `CategoriaResponseDto`
- Mapper principal: `ProductoResponseDto.toDomain()`

Campos principales de `ProductoResponseDto`:

| Campo JSON | Tipo Kotlin | Uso en dominio |
| --- | --- | --- |
| `id` | `Long` | `Producto.id` |
| `nombre` | `String` | `Producto.nombre` |
| `precio` | `Double` | `Producto.precio` |
| `stock` | `Int` | `Producto.stock` |
| `estado` | `Boolean` | No se usa en el modelo de dominio actual |
| `categoriaId` | `Long?` | Se conserva como dato remoto |
| `categoriaNombre` | `String?` | Se conserva como dato remoto |

### Manejo de errores

Las llamadas HTTP pasan por `ejecutarLlamadaRemota`, que convierte los fallos
de Ktor en errores de dominio (`ErrorApi`) antes de que lleguen a la interfaz:

- `400 Bad Request`: recupera `validationErrors` y los asocia con los campos
  `nombre`, `precio` y `stock`.
- `401 Unauthorized`, `404 Not Found` y `409 Conflict`: se traducen a errores
  de autorización, recurso inexistente y conflicto, respectivamente.
- Respuestas `5xx`, tiempo de espera agotado y fallos de conexión: producen
  mensajes legibles de servidor, tiempo agotado o ausencia de conexión.
- La cancelación de corrutinas se vuelve a lanzar para no tratarla como un
  error de negocio.

Los casos de uso exponen estos fallos mediante `Result`. `ProductoViewModel`
muestra los errores de validación del servidor junto a cada campo y representa
los demás fallos en `Operacion.Fallida` o `ProductoUiState.Fase.Error`. Después
de crear, actualizar o eliminar correctamente, vuelve a cargar el inventario
para mantener la pantalla sincronizada con el backend.

Las pruebas de `commonTest` usan `FakeProductoRepository` para comprobar las
transiciones de carga, la validación devuelta por el servidor y la recarga del
inventario después de eliminar.

### Capacidades nativas

La sesion 9 incorpora capacidades que conservan el contrato y la interfaz en
codigo comun, pero utilizan las API propias de cada sistema operativo:

- `formatearSoles` se declara con `expect` en `commonMain`. Android la resuelve
  con `NumberFormat` y la implementacion de iOS usa `NSNumberFormatter`.
- `Compartidor` se declara como interfaz en `domain`. `CompartidorAndroid`
  comparte texto con un `Intent`; `CompartidorIos` utiliza
  `UIActivityViewController`.
- Cada implementacion se registra en su `platformModule` y Koin entrega el
  contrato comun al `ProductoViewModel`.
- La pantalla de productos muestra el precio ya formateado y ofrece la accion
  Compartir sin importar paquetes de Android ni UIKit.

Archivos principales:

| Source set | Archivos |
| --- | --- |
| `commonMain` | `platform/Formato.kt`, `domain/platform/Compartidor.kt`, `domain/usecase/TextoParaCompartir.kt` |
| `androidMain` | `platform/Formato.android.kt`, `platform/CompartidorAndroid.kt`, `di/PlatformModule.android.kt` |
| `iosMain` | `platform/Formato.ios.kt`, `platform/CompartidorIos.kt`, `di/PlatformModule.ios.kt` |

Para ejecutar la prueba:

1. Iniciar el backend Spring Boot en IntelliJ IDEA.
2. Verificar `GET http://localhost:8080/api/v1/productos` desde Swagger o navegador.
3. Ejecutar la app Android desde Android Studio.
4. Confirmar que la pantalla de productos muestre los datos del backend.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
