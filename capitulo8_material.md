# Material extraído del código para el Capítulo 8 (Diseño del software)

> Extracción cruda, sin redacción ni diagramas. Todo cita el fichero exacto de donde sale.
> Paquete raíz real: `com.example.aicollect`.
> Proyecto Gradle de **un solo módulo** (`:app`) — ver `settings.gradle.kts` (`include(":app")`, único módulo declarado).

---

## 1. Estructura de paquetes

Listado completo de paquetes bajo `com.example.aicollect` (obtenido recorriendo
`app/src/main/java/com/example/aicollect`):

| Paquete | Contenido |
|---|---|
| `com.example.aicollect` (raíz) | `AIcollectApplication.kt` (bootstrap de la app, `Application` con Hilt) |
| `application` | Interfaz `AuthRepository` (Kotlin puro) |
| `application.items` | Modelo de dominio de ítems: `Item`, `PricePoint`, `ValuationSearch`, `ValuationResult` (todos en `Item.kt`), interfaz `ItemRepository`, `CollectionPriceFilter`, `ItemSortOption` (+ extensión `sortedByOption`), `PortfolioAnalytics` |
| `application.recognition` | `RankedCandidate` (modelo) e interfaz `RecognitionRepository` |
| `data` | `DarkModePreferences.kt`, `FilterPreferences.kt` (preferencias locales, `SharedPreferences`) |
| `data.auth` | `FirebaseAuthRepository` (implementación de `AuthRepository`) |
| `data.di` | Módulos Hilt: `DatabaseModule.kt`, `FirebaseModule.kt`, `RepositoryModule.kt` |
| `data.items` | `FirestoreItemRepository` (implementación de `ItemRepository`) |
| `data.items.local` | Caché local Room: `AppDatabase.kt`, `Converters.kt`, `ItemDao.kt`, `ItemEntity.kt` |
| `data.recognition` | `FirebaseRecognitionRepository` (implementación de `RecognitionRepository`) |
| `presentation` | `MainActivity.kt`, `MainViewModel.kt`, `SnackbarUtils.kt` |
| `presentation.auth` | `LoginFragment`/`LoginViewModel`, `RegisterFragment`/`RegisterViewModel`, `ForgotPasswordBottomSheetFragment`/`ForgotPasswordViewModel` |
| `presentation.collection` | `HomeFragment`/`HomeViewModel`, `MyVaultFragment`/`MyVaultViewModel`, `ItemDetailFragment`/`ItemDetailViewModel`, `FilterBottomSheetFragment`, `CollectionFeedAdapter`, `CollectionFeedItem`, `ItemFormatting`, `PortfolioLineChartView`, `SportDonutChartView` |
| `presentation.newpost` | `NewPostFragment`/`NewPostViewModel`, `NewPostDisambiguationFragment` |
| `presentation.edititem` | `EditItemFragment`/`EditItemViewModel` |
| `presentation.settings` | `EditProfileFragment`/`EditProfileViewModel`, `SecurityFragment`/`SecurityViewModel`, `HelpFragment`, `AboutFragment`, `FaqAccordion.kt` |

**Sobre la regla "`application/` no puede importar `android.*` ni `com.google.firebase.*`":**

- **No existe ninguna herramienta que la fuerce.** Se buscó en el repositorio cualquier configuración de lint/detekt/ktlint (`find … -iname "detekt*" -o -iname "lint.xml" …`) y no hay ningún fichero de ese tipo. `app/build.gradle.kts` no tiene bloque `lint {}` con reglas custom ni plugins de detekt/ktlint.
- El proyecto es **un único módulo Gradle** (`:app`, ver `settings.gradle.kts`), así que tampoco hay una frontera de módulo (con `implementation`/`api` restringido) que impida técnicamente que una clase en `application/` importe `android.*` o Firebase.
- Verificación empírica: `grep -rln "^import android\.\|^import com\.google\.firebase" app/src/main/java/com/example/aicollect/application/` no devuelve ningún fichero — hoy el paquete `application/` **de hecho** está limpio de esos imports, pero eso es disciplina del autor, no una regla forzada por herramienta o por Gradle.
- La única referencia a esta regla es documental: aparece en la memoria de sesión del asistente y en el brief técnico (`/home/king/Descargas/brief_completo_claude_code.md`), no en ningún fichero versionado dentro del propio repositorio del proyecto.

---

## 2. Modelo de dominio

Fichero: `app/src/main/java/com/example/aicollect/application/items/Item.kt` — contiene **las cuatro** clases de dominio de ítems (no están repartidas en ficheros separados):

```kotlin
package com.example.aicollect.application.items
/**Uno de los valores de precios historicos
 * @param fecha es la fecha en la que se obtuvo ese precio, en milisegundos
 * @param precio es el precio objenido*/
data class PricePoint(val fecha: Long, val precio: Double)

/** Devuelve de donde se ha sacado un precio de objeto
 * @param label es lo buscado
 * @param url es lo encontrado*/
data class ValuationSearch(val label: String, val url: String)

/** Es la clase que devuelve el precio de un articulo, se usa en el momento de publicar. */
data class ValuationResult(
    val precio: Double?,
    val min: Double?,
    val max: Double?,
    val moneda: String,
    val fuentes: List<ValuationSearch>,
)

/** Estructura de datos que se usa en Firestore para un dato de item.
 *
 * Clase única de dominio: antes había `NewItem` (creación) e `ItemEdits` (edición) separadas de
 * esta, colapsadas aquí el 2026-08-24 tras revisión formal — decisión mía, para un CRUD de una
 * sola entidad no compensaba mantener 3 clases. [id]/[imageUrls]/[historialPrecios]/[createdAt]/
 * [updatedAt] tienen valor por defecto porque al publicar un item nuevo todavía no existen —
 * [ItemRepository.createItem] ignora lo que traigan y genera los valores reales al escribir en
 * Firestore.
 *
 * Al editar ([ItemRepository.updateItem]), la regla "el precio nunca se edita a mano" ya no la
 * garantiza el compilador (antes `ItemEdits` ni siquiera tenía esos campos) — la garantiza
 * `FirestoreItemRepository.updateItem()` a mano, leyendo solo nombre/descripcion/deporte/estado
 * del [Item] que recibe y descartando el resto explícitamente. Si se toca esa función en el
 * futuro, hay que mantener esa lista corta de campos sin ayuda del tipo. */
data class Item(
    val id: String = "",
    val nombre: String,
    val descripcion: String?,
    val marca: String,
    val modelo: String,
    val edicion: String?,
    val procedencia: String?,
    val deporte: String,
    val estado: String,
    val imageUrls: List<String> = emptyList(),
    val valoracionActual: Double?,
    val valoracionMin: Double?,
    val valoracionMax: Double?,
    val valoracionMoneda: String,
    val fuenteValoracion: String?,
    val valoracionBusquedas: List<ValuationSearch> = emptyList(),
    val historialPrecios: List<PricePoint> = emptyList(),
    val confianzaIdentificacion: Double?,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)
```

**Nota**: no existe una clase `ValuationSearch` en un fichero propio, ni una clase separada
para el candidato de reconocimiento dentro de este mismo paquete — ese modelo (`RankedCandidate`)
vive en otro paquete y fichero, ver más abajo.

Modelo relacionado del pipeline de reconocimiento — fichero
`app/src/main/java/com/example/aicollect/application/recognition/RecognitionRepository.kt`:

```kotlin
package com.example.aicollect.application.recognition

/**
 * One candidate returned by the `recognizeItem` Cloud Function (europe-west1), already ranked.
 * Mirrors `RankedCandidate` in the deployed `recognizeItem.ts` exactly — [score] is computed
 * server-side (0.4·visión + 0.35·consenso de fuentes + 0.25·confianza de Gemini) and must never
 * be recalculated on the client.
 */
data class RankedCandidate(
    val nombre: String,
    val marca: String?,
    val modelo: String?,
    val edicion: String?,
    val procedencia: String?,
    /** Gemini's own reported confidence, 0.0–1.0 — distinct from [score]. */
    val confianza: Double,
    val numeroFuentes: Int,
    val score: Double,
)
```

Clases de dominio auxiliares en el mismo paquete `application.items` (ficheros separados):

- `app/src/main/java/com/example/aicollect/application/items/CollectionPriceFilter.kt` — `object CollectionPriceFilter { fun isWithinRange(price: Double, minPrice: Int, maxPrice: Int): Boolean }`
- `app/src/main/java/com/example/aicollect/application/items/ItemSortOption.kt` — `enum class ItemSortOption { DEFAULT, ALPHABETICAL, PRICE_DESC, PRICE_ASC, OLDEST_FIRST }` + `fun List<Item>.sortedByOption(option: ItemSortOption): List<Item>`
- `app/src/main/java/com/example/aicollect/application/items/PortfolioAnalytics.kt` — `object PortfolioAnalytics` con `totalValue`, `monthlyEvolution`, `monthLabels`, `changePercent`, `distributionBy`, `topValued` (firmas completas en la sección 3, se usan desde varios ViewModels).

---

## 3. ViewModels relevantes

### Flujo "Nueva Publicación"

**`NewPostViewModel`** — fichero `app/src/main/java/com/example/aicollect/presentation/newpost/NewPostViewModel.kt`.
`@HiltViewModel`, constructor recibe `RecognitionRepository` e `ItemRepository`. Es **activity-scoped**
(`by activityViewModels()` en los Fragments), único ViewModel para todo el flujo captura→identificación→candidatos→formulario→publicación.

Estados expuestos:
```kotlin
sealed interface RecognitionUiState {
    data object Idle : RecognitionUiState
    data object Loading : RecognitionUiState
    data class Success(val candidates: List<RankedCandidate>) : RecognitionUiState
    data class Error(val message: String) : RecognitionUiState
}

sealed interface SaveItemUiState {
    data object Idle : SaveItemUiState
    data object Loading : SaveItemUiState
    data object Success : SaveItemUiState
    data class Error(val message: String) : SaveItemUiState
    data class ValidationError(val field: RequiredField) : SaveItemUiState
    data class DuplicateWarning(val existingItemName: String) : SaveItemUiState
}

enum class RequiredField { NAME, SPORT, CONDITION }
```

Funciones/propiedades públicas:
```kotlin
val recognitionState: StateFlow<RecognitionUiState>
val saveState: StateFlow<SaveItemUiState>
val photos: List<ByteArray>                       // getter, hasta 3 fotos
val canAddMorePhotos: Boolean
var candidates: List<RankedCandidate>              // set privado
var selectedCandidate: RankedCandidate?            // público, mutable

fun reset()
fun acknowledgeRecognitionResult()
fun addPhoto(imageBytes: ByteArray): Boolean
fun removePhotoAt(index: Int)
fun recognize(imageBytes: ByteArray)
fun saveItem(name: String, description: String?, sport: String?, condition: String?)
fun confirmPublishDespiteDuplicate()
fun dismissDuplicateWarning()
```
(`findSimilarExistingItem`, `publish`, `productKey`, `normalizeKey` son privados.)

### Flujo "Consultar colección"

**`HomeViewModel`** — fichero `app/src/main/java/com/example/aicollect/presentation/collection/HomeViewModel.kt`.
`@HiltViewModel`, constructor recibe `ItemRepository`.

```kotlin
sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Content(
        val isCollectionEmpty: Boolean,
        val hasNoFilterResults: Boolean,
        val visibleItems: List<Item>,
        val summary: CollectionSummary,
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

val uiState: StateFlow<HomeUiState>
fun setFilters(minPrice: Int, maxPrice: Int, sport: String?, condition: String?, sort: ItemSortOption)
```
(`summaryFor` es privada.) `uiState` se construye combinando `itemRepository.observeItems()` con un `MutableStateFlow<ActiveFilters>` interno vía `combine`.

**`ItemDetailViewModel`** — fichero `app/src/main/java/com/example/aicollect/presentation/collection/ItemDetailViewModel.kt`.
`@HiltViewModel`, constructor recibe `ItemRepository`.

```kotlin
sealed interface ItemDetailUiState {
    data object Loading : ItemDetailUiState
    data class Content(
        val imageUrl: String?,
        val nombre: String,
        val priceLabel: String,
        val estado: String,
        val deporte: String,
        val sportEmoji: String,
        val evolution: List<Float>,
        val monthLabels: List<String>,
        val valuationRangeLabel: String?,
    ) : ItemDetailUiState
    data class Error(val message: String) : ItemDetailUiState
}

sealed interface DeleteItemUiState {
    data object Idle : DeleteItemUiState
    data object Deleting : DeleteItemUiState
    data object Success : DeleteItemUiState
    data class Error(val message: String) : DeleteItemUiState
}

val uiState: StateFlow<ItemDetailUiState>
val deleteState: StateFlow<DeleteItemUiState>
fun load(itemId: String)
fun deleteItem()
```
(`refreshValuationSilently`, `needsValuationRefresh`, `toContent`, `valuationRangeLabel`, `sportEmoji` son privadas/de extensión privada.)

### ViewModels adicionales del CRUD de ítems (no pedidos explícitamente, pero comparten `ItemRepository` y son relevantes para el diagrama de clases/paquetes)

**`MyVaultViewModel`** — `app/src/main/java/com/example/aicollect/presentation/collection/MyVaultViewModel.kt`, `@HiltViewModel(ItemRepository)`:
```kotlin
val uiState: StateFlow<MyVaultUiState>
fun selectSport(sport: String?)
```

**`EditItemViewModel`** — `app/src/main/java/com/example/aicollect/presentation/edititem/EditItemViewModel.kt`, `@HiltViewModel(ItemRepository, SavedStateHandle)`:
```kotlin
val uiState: StateFlow<EditItemUiState>
val saveState: StateFlow<SaveEditUiState>
fun save(name: String, description: String?, sport: String?, condition: String?)
```

---

## 4. Repositorios

**Interfaz de dominio**: `ItemRepository` — fichero `app/src/main/java/com/example/aicollect/application/items/ItemRepository.kt`.

```kotlin
interface ItemRepository {
    suspend fun createItem(item: Item, imageBytes: List<ByteArray>): Result<String>
    suspend fun updateItem(itemId: String, item: Item): Result<Unit>
    suspend fun deleteItem(itemId: String): Result<Unit>
    suspend fun getItem(itemId: String): Result<Item>
    fun observeItems(): Flow<List<Item>>
    suspend fun refreshValuation(itemId: String): Result<Item>
    suspend fun searchValuation(nombre: String, marca: String, modelo: String, edicion: String?): Result<ValuationResult>
}
```

**Implementación**: `FirestoreItemRepository` — fichero `app/src/main/java/com/example/aicollect/data/items/FirestoreItemRepository.kt`. Implementa los 7 métodos de la interfaz. Constructor:
```kotlin
class FirestoreItemRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseStorage: FirebaseStorage,
    private val firebaseAuth: FirebaseAuth,
    private val firebaseFunctions: FirebaseFunctions,
    private val itemDao: ItemDao,
) : ItemRepository
```
`ItemDao` es la caché local Room (`data/items/local/ItemDao.kt`) — según el kdoc del fichero, **Room es la fuente que la UI observa** (`observeItems()` devuelve una query de Room, no el snapshot de Firestore directo); Firestore sigue siendo la fuente de escritura y su listener (`addSnapshotListener` en `startFirestoreSync`) es lo que mantiene Room sincronizado en segundo plano.

`refreshValuation`/`searchValuation` llaman a Cloud Functions llamadas `refreshValuation` y `searchValuation` respectivamente (constantes `REFRESH_VALUATION_FUNCTION`/`SEARCH_VALUATION_FUNCTION` al final del fichero), vía `firebaseFunctions.getHttpsCallable(...)`.

**Binding Hilt** (`app/src/main/java/com/example/aicollect/data/di/RepositoryModule.kt`):
```kotlin
@Binds @Singleton abstract fun bindAuthRepository(impl: FirebaseAuthRepository): AuthRepository
@Binds @Singleton abstract fun bindItemRepository(impl: FirestoreItemRepository): ItemRepository
@Binds @Singleton abstract fun bindRecognitionRepository(impl: FirebaseRecognitionRepository): RecognitionRepository
```

**¿ViewModel → Repositorio directo, o hay capa de casos de uso intermedia?**

No existe ninguna capa de casos de uso (`UseCase`) en el proyecto — se buscó cualquier fichero/clase
con el sufijo `UseCase` o `Interactor` en todo `app/src/main/java` y no aparece ninguno. Todos los
ViewModels (`NewPostViewModel`, `HomeViewModel`, `ItemDetailViewModel`, `MyVaultViewModel`,
`EditItemViewModel`) reciben `ItemRepository` (y, en el caso de `NewPostViewModel`, también
`RecognitionRepository`) **directamente por constructor vía Hilt** y llaman a sus métodos sin
intermediarios (p. ej. `itemRepository.createItem(...)`, `itemRepository.observeItems()`,
`recognitionRepository.recognizeItem(...)`).

Repositorio de reconocimiento, para completar el cuadro:

**Interfaz**: `RecognitionRepository` — `app/src/main/java/com/example/aicollect/application/recognition/RecognitionRepository.kt`:
```kotlin
interface RecognitionRepository {
    suspend fun recognizeItem(imageBase64: String): Result<List<RankedCandidate>>
}
```

**Implementación**: `FirebaseRecognitionRepository` — `app/src/main/java/com/example/aicollect/data/recognition/FirebaseRecognitionRepository.kt` (código completo en la Sección 6).

---

## 5. Fragments / pantallas del flujo de publicación

El flujo "Nueva Publicación" son **dos Fragments encadenados por Navigation Component**, no una única
pantalla con estados internos ni una cadena larga de N pantallas:

1. **`NewPostFragment`** — `app/src/main/java/com/example/aicollect/presentation/newpost/NewPostFragment.kt` (id de nav graph: `newPostFragment`, layout `fragment_new_post.xml`). Es **una única pantalla que combina** la caja de subida de foto (cámara o galería) **y el formulario** (nombre, descripción, deporte, estado, botón "Publicar") — según el kdoc del propio fichero, esto reemplazó un diseño anterior con un paso de "captura" separado, a petición explícita del usuario (feedback 2026-08-23: "el formulario debe verse desde el principio"). Al añadir la primera foto, un `MaterialAlertDialogBuilder` (`showAnalysisChoiceDialog`) deja elegir entre análisis automático (llama a `viewModel.recognize()` y navega a `NewPostDisambiguationFragment` cuando `RecognitionUiState.Success` llega) o relleno manual (se queda en la misma pantalla).
2. **`NewPostDisambiguationFragment`** — `app/src/main/java/com/example/aicollect/presentation/newpost/NewPostDisambiguationFragment.kt` (id de nav graph: `newPostDisambiguationFragment`, layout `fragment_new_post_disambiguation.xml`). Pinta la lista de `RankedCandidate` (`viewModel.candidates`, ya vienen ordenados por `score`) más una tarjeta "Ninguno de estos". Al tocar una tarjeta o "Ninguno de estos", solo hace `viewModel.selectedCandidate = candidate` (o `null`) y `findNavController().popBackStack()` — **no guarda nada aquí**, vuelve a `NewPostFragment`, que en `onViewStateRestored()` rellena `et_name`/`et_description` desde `viewModel.selectedCandidate` (función `prefillFromCandidate()`).

No hay una pantalla de "capturar" separada de "elegir análisis automático o manual": esa elección es
un `AlertDialog` dentro de `NewPostFragment`, no un destino de navegación propio.

**Grafo de navegación**: `app/src/main/res/navigation/nav_graph.xml`. Fragmento relevante (declaración
completa de los dos destinos del flujo):
```xml
<fragment
    android:id="@+id/newPostFragment"
    android:name="com.example.aicollect.presentation.newpost.NewPostFragment"
    android:label="NewPost"
    tools:layout="@layout/fragment_new_post" />

<fragment
    android:id="@+id/newPostDisambiguationFragment"
    android:name="com.example.aicollect.presentation.newpost.NewPostDisambiguationFragment"
    android:label="NewPostDisambiguation"
    tools:layout="@layout/fragment_new_post_disambiguation" />
```
No hay un `<action>` declarado entre ambos en el XML — la navegación se hace por id directo
(`findNavController().navigate(R.id.newPostDisambiguationFragment)` en `NewPostFragment.render()`)
y por `popBackStack()` de vuelta, no por `<action>`/`NavDirections` generados.

El estado compartido entre ambas pantallas vive en `NewPostViewModel`, obtenido en ambos Fragments
como `by activityViewModels()` — por eso sobrevive a la navegación entre los dos destinos sin pasar
argumentos por `Bundle`/`SavedStateHandle`.

---

## 6. Llamada real a `recognizeItem`

**Repositorio que hace la llamada** — `app/src/main/java/com/example/aicollect/data/recognition/FirebaseRecognitionRepository.kt` (fichero completo):

```kotlin
package com.example.aicollect.data.recognition

import com.example.aicollect.application.recognition.RankedCandidate
import com.example.aicollect.application.recognition.RecognitionRepository
import com.google.firebase.functions.FirebaseFunctions
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

class FirebaseRecognitionRepository @Inject constructor(
    private val firebaseFunctions: FirebaseFunctions,
) : RecognitionRepository {

    override suspend fun recognizeItem(imageBase64: String): Result<List<RankedCandidate>> = runCatching {
        val response = firebaseFunctions
            .getHttpsCallable(RECOGNIZE_ITEM_FUNCTION)
            .call(mapOf("imageBase64" to imageBase64))
            .await()

        @Suppress("UNCHECKED_CAST")
        val body = response.data as? Map<String, Any?> ?: emptyMap()
        @Suppress("UNCHECKED_CAST")
        val rawCandidates = body["candidates"] as? List<Map<String, Any?>> ?: emptyList()
        rawCandidates.map { it.toRankedCandidate() }
    }

    private fun Map<String, Any?>.toRankedCandidate() = RankedCandidate(
        nombre = this["nombre"] as? String ?: "",
        marca = this["marca"] as? String,
        modelo = this["modelo"] as? String,
        edicion = this["edicion"] as? String,
        procedencia = this["procedencia"] as? String,
        confianza = (this["confianza"] as? Number)?.toDouble() ?: 0.0,
        numeroFuentes = (this["numeroFuentes"] as? Number)?.toInt() ?: 0,
        score = (this["score"] as? Number)?.toDouble() ?: 0.0,
    )

    private companion object {
        const val RECOGNIZE_ITEM_FUNCTION = "recognizeItem"
    }
}
```

Firma expuesta a la capa de dominio (interfaz, mismo fichero que `RankedCandidate`,
`app/src/main/java/com/example/aicollect/application/recognition/RecognitionRepository.kt`):
```kotlin
interface RecognitionRepository {
    suspend fun recognizeItem(imageBase64: String): Result<List<RankedCandidate>>
}
```

**Cómo se recibe y mapea la respuesta**: `FirebaseFunctions.getHttpsCallable("recognizeItem").call(mapOf("imageBase64" to imageBase64)).await()` devuelve un `HttpsCallableResult`; `response.data` se castea a `Map<String, Any?>`, se extrae la lista `candidates` (`List<Map<String, Any?>>`) y cada mapa se convierte a `RankedCandidate` con la función de extensión privada `toRankedCandidate()` de arriba. Todo el mapeo son casts seguros (`as?`) con valores por defecto (`""`, `0.0`, `0`) si el campo falta o tiene otro tipo — no hay una librería de serialización (Moshi/kotlinx.serialization) en este punto, es un mapeo manual campo a campo.

**Quién la invoca** — `NewPostViewModel.recognize()`, fichero `app/src/main/java/com/example/aicollect/presentation/newpost/NewPostViewModel.kt`:
```kotlin
fun recognize(imageBytes: ByteArray) {
    _recognitionState.value = RecognitionUiState.Loading
    viewModelScope.launch {
        val imageBase64 = Base64.getEncoder().encodeToString(imageBytes)
        recognitionRepository.recognizeItem(imageBase64)
            .onSuccess { result ->
                candidates = result
                _recognitionState.value = RecognitionUiState.Success(result)
            }
            .onFailure {
                _recognitionState.value = RecognitionUiState.Error(
                    it.message ?: "No se pudo analizar la imagen. Inténtalo de nuevo.",
                )
            }
    }
}
```
La imagen se codifica a base64 en el ViewModel antes de llamar al repositorio; el repositorio no
recibe nunca un `ByteArray` ni un `Uri`, solo el `String` base64. `recognize()` solo se llama para la
primera foto añadida (ver comentario en el propio fichero: "solo se analiza la primera en subir"),
desde `NewPostFragment.showAnalysisChoiceDialog()`.

**No existe el código fuente de la propia Cloud Function `recognizeItem.ts`** en este repositorio —
confirmado por la memoria de sesión ("el usuario tiene su propia versión de `recognizeItem.ts` y no la
compartirá") y por búsqueda en el repo: no hay ningún directorio `functions/` ni fichero `.ts` en todo
`/home/king/Escritorio/AiCollect`. Solo se puede documentar el lado cliente (arriba).

---

## 7. Discrepancias detectadas

**Aviso de alcance antes de esta sección**: no tengo acceso al texto real de la memoria del TFG (no
existe como fichero en este repositorio — la memoria de sesión del asistente indica que el Capítulo 5
"se pegó en conversación" en su momento y no se guardó como archivo). Por tanto **no puedo comparar
contra el texto exacto de las Secciones 5, 6, 7 y 9** que pides. Lo que sí existe en el entorno y he
usado como referencia son:
- `PROJECT_CONTEXT.md` (raíz del repo, log de estado real mantenido por el usuario), y
- `/home/king/Descargas/brief_completo_claude_code (1).md` (brief técnico más reciente, ago-21).

Comparando el código real contra esos dos documentos, encuentro estas discrepancias:

1. **Fuente de la valoración de mercado — automática por Cloud Function, no introducida por el usuario.**
   El brief más reciente (`brief_completo_claude_code (1).md`, líneas ~85-87 y ~286-351) documenta la
   decisión final como: *"El valor lo introduce el usuario, siempre. El campo `valoracionActual` es un
   campo de [entrada manual]... Gemini con grounding" actúa solo como "guardián de sentido común"* que
   valida un número ya introducido a mano, y que `StockX`/`SoldComps`/`eBay Browse API` fueron
   descartadas.
   **El código real hace justo lo contrario**: no hay ningún campo de precio en el formulario de
   publicación (`fragment_new_post.xml`, verificado con `grep -i "precio|price|valoracion"` → sin
   resultados) ni en el de edición (`fragment_edit_item.xml`, mismo resultado vacío). `NewPostViewModel.saveItem()`
   no recibe ningún parámetro de precio — la valoración se obtiene **automáticamente** llamando a
   `itemRepository.searchValuation(...)` (Cloud Function `searchValuation`) *antes* de crear el ítem
   (`NewPostViewModel.publish()`), y además `ItemDetailViewModel` la **refresca sola en segundo plano**
   sin ninguna acción del usuario (`refreshValuationSilently()`, se dispara si `needsValuationRefresh()`
   es cierto). El propio kdoc de `ItemDetailViewModel` lo dice explícitamente: *"añadida la valoración
   de mercado (Gemini + grounding en Google Search, sustituye a la idea original de eBay Browse
   API)... sin botón manual ni fuentes visibles"*.

2. **El "botón manual 'Actualizar valor' en el Detalle" ya no existe.**
   Tanto el brief como notas de sesión anteriores documentaban un botón manual "Actualizar valor" en
   la pantalla de detalle como forma de disparar `refreshValuation(itemId)`. El kdoc de
   `ItemDetailViewModel.kt` (líneas 42-53) dice explícitamente que ese botón **se quitó**: *"se decidió
   no mostrarlos y quitar también el botón 'Actualizar valor' para no prometer control manual sobre
   algo que igualmente se recalcula solo"*. Hoy el refresco es 100% automático y silencioso
   (`refreshValuationSilently()`), sin Snackbar ni UI de progreso.

3. **`products_cache` (mencionado en el brief como pieza central del pipeline de valoración) no tiene
   ninguna contraparte en el modelo de dominio del cliente.** Ni `Item`, ni `ValuationResult`, ni
   `ItemRepository` tienen ningún campo o método que referencie `products_cache` — toda esa lógica,
   si existe, vive en las Cloud Functions (`searchValuation`/`refreshValuation`), cuyo código fuente no
   está en este repositorio (no hay carpeta `functions/`). No puedo confirmar ni negar cómo se
   implementa `products_cache` desde el lado cliente porque el cliente no lo toca.

4. **Persistencia local con Room, no mencionada como pieza del pipeline de valoración/reconocimiento
   en el brief.** El brief describe la app como serverless puro (Firebase directo desde el cliente,
   sin más). El código real añade una capa de caché local Room (`data/items/local/AppDatabase.kt`,
   `ItemDao.kt`, `ItemEntity.kt`, `Converters.kt`) que **es la fuente que `observeItems()` expone a la
   UI** (Firestore alimenta Room en segundo plano vía listener, la UI lee de Room) — ver kdoc de
   `FirestoreItemRepository.kt` líneas 30-53. Esto ya está anotado como decisión consciente en la
   memoria de sesión del asistente (`project_aicollect_brief` → "Actualización 2026-08-24"), pero no
   aparece en el brief técnico ni en ningún documento versionado del repositorio.

5. **No hay capa de casos de uso (`UseCase`/`Interactor`).** Si la memoria del TFG describe una
   arquitectura con casos de uso explícitos entre ViewModel y Repository, el código no la tiene —
   ver Sección 4: los ViewModels llaman directo al repositorio.
