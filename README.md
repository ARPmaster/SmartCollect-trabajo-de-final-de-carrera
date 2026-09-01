# AICollect

AICollect es una aplicación Android para coleccionistas de objetos deportivos. Permite catalogar
artículos fotografiándolos —con reconocimiento automático asistido por IA (Google Cloud Vision y
Google Gemini) para sugerir candidatos— y consultar una valoración de mercado orientativa por
artículo, calculada a partir de anuncios reales de eBay. El cliente Android (Kotlin, MVVM, Hilt,
Room) vive en `app/`; el backend serverless (Cloud Functions) vive en `backend/`.

## Requisitos del entorno de desarrollo

- Android Studio, con soporte para AGP 9.3.1 / Gradle 9.5.0 (usa la versión estable más reciente
  de Android Studio disponible).
- JDK 11.
- SDK de Android con `compileSdk`/`targetSdk` en 36 y `minSdk` en 29 (Android 10).
- Cuenta de Google con acceso (o permiso para crear) un proyecto de Firebase.
- Para el backend: Node.js 24, npm y Firebase CLI — ver `backend/README.md`.

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
`backend/` — ver `backend/README.md` para el procedimiento completo, incluida la configuración
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
backend/
  functions/src/     # Cloud Functions (recognizeItem, searchValuation, refreshValuation)
```

## Variables y secretos sensibles no incluidos en el repositorio

Por motivos de seguridad, este repositorio no incluye `app/google-services.json`. Quien desee
reproducir el proyecto desde cero deberá generarlo con sus propias credenciales de Firebase.
