# SmartCollect

SmartCollect es una aplicación Android para coleccionistas de objetos deportivos. Permite
catalogar artículos fotografiándolos —con reconocimiento automático asistido por IA (Google Cloud
Vision y Google Gemini) para sugerir candidatos— y consultar una valoración de mercado orientativa
por artículo: la valoración se obtiene mediante Gemini con grounding de búsqueda web (se descartó
la integración con eBay). Este repositorio contiene el cliente Android (Kotlin, MVVM, Hilt, Room); el backend serverless
(Cloud Functions) vive en [backend-TFG](https://github.com/ARPmaster/backend-TFG).

## Requisitos del entorno de desarrollo

- Android Studio, con soporte para AGP 9.3.1 / Gradle 9.5.0 (usa la versión estable más reciente
  de Android Studio disponible).
- JDK 11.
- SDK de Android con `compileSdk`/`targetSdk` en 36 y `minSdk` en 29 (Android 10).
- Cuenta de Google con acceso (o permiso para crear) un proyecto de Firebase.

## Instalación del cliente Android

1. Clona el repositorio.
2. Coloca el fichero `google-services.json` del proyecto Firebase en la carpeta `app/` (no se
   incluye en el repositorio por seguridad). Para obtenerlo: consola de Firebase
   (console.firebase.google.com) → *Project settings → General → Your apps → Add app → Android*,
   registra el paquete `com.example.aicollect` y descarga el fichero.
3. Compila e instala mediante:

   ```sh
   ./gradlew :app:assembleDebug
   ```

   o abre el proyecto en Android Studio y pulsa **Run**.

## Configuración de Firebase

Desde la [consola de Firebase](https://console.firebase.google.com), crea un proyecto y habilita:

- **Authentication**, con el proveedor de email y contraseña.
- **Cloud Firestore**, en modo producción, con reglas de seguridad que restrinjan el acceso al
  propio usuario autenticado.
- **Cloud Storage**, para las fotografías de las piezas y de perfil.

Las Cloud Functions (`recognizeItem`, `searchValuation`, `refreshValuation`) se despliegan desde
[backend-TFG](https://github.com/ARPmaster/backend-TFG), con su propio README y la configuración
del secreto `GEMINI_API_KEY`.

## Tests

```sh
./gradlew :app:testDebugUnitTest          # unitarios (JVM)
./gradlew :app:connectedDebugAndroidTest  # instrumentación (requiere emulador o dispositivo)
```

## Estructura del proyecto

```
app/src/main/java/com/example/aicollect/
  application/      # Modelos de dominio y contratos de repositorio
  data/              # Implementaciones: Firestore, Room (caché local), Cloud Functions
  presentation/       # Fragments, ViewModels, vistas custom (MVVM)
```

## Variables y secretos sensibles no incluidos en el repositorio

Por motivos de seguridad, este repositorio no incluye `app/google-services.json`. Quien desee
reproducir el proyecto desde cero deberá generarlo con sus propias credenciales de Firebase.
