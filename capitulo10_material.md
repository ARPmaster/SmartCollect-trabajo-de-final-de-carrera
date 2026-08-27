# Material para el Capítulo 10 (Pruebas realizadas)

Estado real de la cobertura de tests del proyecto tras esta sesión, verificado ejecutando
`./gradlew :app:testDebugUnitTest` (pasa, 115/115) y `./gradlew :app:assembleDebugAndroidTest`
(compila y empaqueta, 8 tests de instrumentación — **no ejecutados en dispositivo/emulador real**,
ver limitación explícita más abajo).

---

## 1. ViewModels — tests unitarios JVM (MockK + `kotlinx-coroutines-test`)

De los 10 ViewModels que identificamos como relevantes, **los 10 ya tenían test antes de empezar
esta tarea** (4 los añadí yo en la sesión anterior — Home/ItemDetail/MyVault/ForgotPassword —, los
otros 6 ya existían de sesiones previas al TFG). No hizo falta completar ninguno desde cero; sí
comprobé uno por uno que cumplen el mínimo pedido (camino feliz + error de repositorio +
validación de campos obligatorios) leyéndolos enteros — los 10 lo cumplen y en varios casos lo
superan (casos de negocio concretos: nombre de usuario duplicado, reautenticación antes de borrar
cuenta, aviso de duplicado al publicar, condición de carrera en el refresco de valoración).

| ViewModel | Fichero de test | Nº tests | Qué cubre |
|---|---|---|---|
| `NewPostViewModel` | `NewPostViewModelTest.kt` | 15 | Reconocimiento (éxito/error), gestión de hasta 3 fotos, validación de nombre/deporte/estado, publicación feliz con y sin candidato, fallo de `searchValuation` (no bloquea), fallo de `createItem`, aviso y confirmación/cancelación de duplicado |
| `HomeViewModel` | `HomeViewModelTest.kt` | 7 | Colección vacía, filtro por precio/deporte/estado, `hasNoFilterResults` vs. `isCollectionEmpty`, orden por `ItemSortOption`, fallo de `observeItems()` |
| `ItemDetailViewModel` | `ItemDetailViewModelTest.kt` | 10 | Carga feliz/error, refresco silencioso de valoración (dispara si no hay historial o si tiene &gt;30 días, no dispara si es reciente, error silencioso no rompe el estado, condición de carrera con una `load()` más reciente), eliminar feliz/error/sin ítem cargado |
| `MyVaultViewModel` | `MyVaultViewModelTest.kt` | 5 | Estado `Empty`, agregados sobre toda la colección, que el chip de deporte filtre solo top-valorados/condición sin tocar distribución total, deporte sin artículos, fallo de `observeItems()` |
| `EditItemViewModel` | `EditItemViewModelTest.kt` | 9 | Carga feliz/error, validación de nombre/deporte/estado, guardar antes de cargar, guardado feliz (solo 4 campos editables cambian, el resto viaja intacto), descripción en blanco → `null`, error de guardado |
| `LoginViewModel` | `LoginViewModelTest.kt` | 4 | Email/contraseña en blanco, inicio de sesión feliz, error del repositorio |
| `RegisterViewModel` | `RegisterViewModelTest.kt` | 9 | Todas las validaciones (email, contraseña, longitud/coincidencia), registro feliz, error genérico, nombre de usuario ya en uso (`UsernameTakenException`, estado distinto de `Error`) |
| `ForgotPasswordViewModel` | `ForgotPasswordViewModelTest.kt` | 3 | Email en blanco, envío feliz, error del repositorio |
| `EditProfileViewModel` | `EditProfileViewModelTest.kt` | 9 | Validación de longitud de nombre, guardado feliz, nombre ya en uso, error genérico, subida de foto feliz/error, getters delegados |
| `SecurityViewModel` | `SecurityViewModelTest.kt` | 12 | Sin cambios, validación de contraseña nueva, cambio de email/contraseña por separado y combinados, fallo detiene la cadena, eliminar cuenta (contraseña en blanco, error real de `reauthenticate` en vez de uno inventado, éxito reautentica y luego borra) |

**Además**, lógica de dominio pura sin ViewModel (no pedida explícitamente, ya cubierta de la
sesión anterior): `PortfolioAnalyticsTest` (14), `ItemSortOptionTest` (7), `ItemFormattingTest` (8),
`CollectionPriceFilterTest` (3).

**Total tests unitarios JVM: 115, en 14 clases, 0 fallos.**

---

## 2. Tests de instrumentación (`androidTest`) — nuevos en esta sesión

**No existía ningún test de instrumentación en el proyecto** (la carpeta `androidTest` no existía
siquiera). Se ha montado la infraestructura completa desde cero:

- **Librerías añadidas** (`gradle/libs.versions.toml` + `app/build.gradle.kts`): `hilt-android-testing`
  (+ `kspAndroidTest`), `androidx.fragment:fragment-testing`, `androidx.navigation:navigation-testing`,
  `androidx.test:core`. Se reutiliza Espresso, que ya estaba declarado (aunque sin usar).
- `CustomTestRunner` (sustituye la `Application` real por `HiltTestApplication`) — cambia
  `testInstrumentationRunner` en `app/build.gradle.kts`.
- `HiltTestActivity` — Activity vacía `@AndroidEntryPoint` que aloja los Fragments bajo test
  (necesaria porque usan `by activityViewModels()`/`by viewModels()` con `@HiltViewModel`).
- `TestRepositoryModule` (`@TestInstallIn`, sustituye `RepositoryModule` real) + `FakeItemRepository`,
  `FakeRecognitionRepository`, `FakeAuthRepository` — dobles en memoria, **ningún test toca
  Firebase/Firestore/Storage real**.
- `NoOpNavigator` + `launchFragmentWithNavController(...)` — arranca el Fragment con un
  `TestNavHostController` cargado con el `nav_graph.xml` real pero con el navegador de fragmentos
  sustituido por uno que no ejecuta transacciones reales, así que verificar una navegación (p. ej.
  "Publicar" → Home) no arrastra tener que renderizar también la pantalla de destino.

| Fragment | Fichero de test | Nº tests | Qué cubre |
|---|---|---|---|
| `NewPostFragment` | `NewPostFragmentTest.kt` | 2 | Publicar sin campos obligatorios no llama al repositorio y muestra el aviso; rellenar nombre+deporte+estado publica y navega a Home |
| `NewPostDisambiguationFragment` | `NewPostDisambiguationFragmentTest.kt` | 4 | Los candidatos se muestran con el primero marcado "Mejor coincidencia"; lista vacía muestra el estado vacío; "Ninguno de estos" limpia la selección y hace `popBackStack()` real; tocar un candidato lo guarda como seleccionado y también vuelve atrás |
| `LoginFragment` (3ª pantalla, no pedida explícitamente, coste marginal bajo reutilizando la infraestructura de arriba) | `LoginFragmentTest.kt` | 2 | Credenciales en blanco no llaman a `signIn`; credenciales válidas inician sesión y navegan a Home |

**Total: 8 tests de instrumentación en 3 clases.**

### ⚠️ Limitación honesta — no ejecutados en dispositivo/emulador real

Este entorno de trabajo no tiene `adb` ni ningún emulador/dispositivo Android conectado (verificado:
`adb` ni siquiera está instalado aquí). Se ha verificado que:
- `./gradlew :app:assembleDebugAndroidTest` **compila, procesa Hilt (KSP) y empaqueta el APK de
  test sin errores** — esto confirma que el grafo de dependencias de Hilt es coherente (los dobles
  sustituyen correctamente a `RepositoryModule`) y que no hay errores de tipos/API.
- **No se ha podido ejecutar `./gradlew connectedAndroidTest`** contra un dispositivo real, así que
  **no puedo confirmar que los 8 tests pasen de verdad en tiempo de ejecución** (por ejemplo, si el
  `PopupMenu` de selección de deporte/estado responde exactamente como se espera a los matchers de
  Espresso, o si `TestNavHostController` se comporta como está previsto en este dispositivo/versión
  de Android concreta). Antes de citar "8 tests de instrumentación en verde" en la memoria como
  hecho verificado, ejecuta tú mismo `./gradlew connectedAndroidTest` con un emulador o un móvil
  conectado y confírmalo — si algo falla, dímelo y lo arreglo.

### Discrepancia encontrada con el enunciado de esta tarea

Pedías comprobar "que el botón Publicar esté deshabilitado si faltan estado/deporte, que se
habilite al completarlos". **El código real de `NewPostFragment` no hace eso**: `btn_publish` está
siempre habilitado (solo se deshabilita durante `SaveItemUiState.Loading`, mientras se publica) y
la validación ocurre al pulsarlo, mostrando un Snackbar con el campo que falta
(`NewPostViewModel.saveItem()` → `SaveItemUiState.ValidationError`). Los tests que escribí
verifican el comportamiento real (clic sin campos → Snackbar + repositorio no llamado), no el
descrito — si de verdad quieres deshabilitar el botón, es un cambio de UX que habría que hacer
aparte (no lo he tocado, según lo pedido de "no tocar la lógica de negocio existente").

---

## 3. Backend (`AiCollect-backend2`) — sigue sin control de versiones ni tests

Verificado en esta sesión:
- `AiCollect-backend2` **sigue sin ser un repositorio git** (`git status` falla: "no es un
  repositorio git").
- `functions/package.json` no tiene ningún script `test`, y sus `devDependencies` solo traen
  `firebase-functions-test` (declarada pero sin usar en ningún fichero — no hay ningún `.test.ts`
  ni `.spec.ts` en todo el repo).
- Las 3 Cloud Functions reales (`recognizeItem.ts`, `refreshValuation.ts`, `index.ts`) llaman a
  Cloud Vision API, al SDK de Gemini y al Admin SDK de Firestore — para testearlas de verdad con
  sentido (no un test vacío que no prueba nada) haría falta montar un framework de test (Jest o
  Vitest), mockear esos tres SDKs externos, y diseñar casos para el pipeline RAG de 2 fases y la
  fórmula de ranking ponderado. Esto **no es una tarea de menos de una hora** — no lo he improvisado,
  tal como pediste.

**Recomendación para la memoria**: documentar esto en el Capítulo 10 como limitación explícita y
consciente, no como un olvido — el propio Capítulo 6.7 ya lo admite ("No existen... pruebas
automatizadas del backend; la verificación dominante en la práctica ha sido la prueba manual
repetida"). Si al final queda tiempo tras el resto de la entrega, sería un trabajo aparte, con su
propio presupuesto de horas.

---

## 4. Resumen numérico

| Módulo | Tests | Estado |
|---|---|---|
| Unitarios JVM (`app/src/test`) | 115 | ✅ Verificado en verde (`./gradlew :app:testDebugUnitTest`) |
| Instrumentación (`app/src/androidTest`) | 8 | ⚠️ Compila y empaqueta; **no ejecutado en dispositivo/emulador** (ninguno disponible en este entorno) |
| Backend (`AiCollect-backend2`) | 0 | ❌ Sin tests ni control de versiones — limitación documentada, no abordada esta sesión |

No hay ninguna clase de las 10 ViewModels pedidas sin test. La cobertura instrumentada cubre 3
pantallas (2 pedidas + 1 extra) de las muchas que tiene la app — el resto de Fragments (Home,
My Vault, Detalle, Ajustes, Registro, etc.) sigue sin ningún test de instrumentación; si el Capítulo
10 necesita presentar un número de "pantallas cubiertas", que sea 3, no más.
