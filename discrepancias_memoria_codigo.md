# Discrepancias entre `Version 0.3 TFG.docx` (memoria) y el código real

Comparación hecha leyendo el texto extraído de `/home/king/Escritorio/Version 0.3 TFG.docx`
(867 KB, modificado 2026-08-27) contra el código fuente actual del repositorio
`/home/king/Escritorio/AiCollect`. El Capítulo 8 ("Diseño del software") está vacío en el
docx — es solo un encabezado, sin contenido — así que estas discrepancias importan sobre todo
porque los diagramas de ese capítulo se van a basar en lo que ya describen los Capítulos 5 y 7.

---

## 1. Valoración de mercado: la memoria la describe asíncrona/no bloqueante, el código es síncrona/bloqueante

### Qué dice la memoria

**Sección 5.4, "Valoración automática de mercado":**
> "A diferencia del pipeline de identificación, que se ejecuta antes de que la pieza exista en
> el inventario, la valoración de mercado se dispara automáticamente **después** de que el
> usuario confirma y publica la pieza [...]. Esta llamada es **asíncrona y no bloqueante: la
> pieza queda disponible de inmediato en el inventario del usuario**, y la estimación de valor
> [...] se actualiza en la ficha una vez el guardián responde, **sin que el usuario tenga que
> esperar** ni introducir ningún dato manualmente."

**Sección 5.6, "Vista lógica: flujo de identificación y valoración de una pieza"** (orden de
pasos):
> 5. Persistencia. Al confirmar, la pieza se escribe en Firestore, quedando disponible de forma
>    inmediata en el inventario del usuario.
> 6. Valoración automática. **De forma asíncrona**, se dispara el guardián de valoración [...];
>    la estimación de precio se incorpora a la ficha en cuanto Gemini responde.

**RF-24** (Sección 7.1.4):
> "El sistema deberá solicitar automáticamente al backend la valoración de mercado del objeto
> **tras** su publicación, **sin bloquear la interacción del usuario** mientras se calcula."

**RNF-02** (Sección 7.2.1):
> "El sistema deberá devolver la valoración de mercado en un tiempo aproximado de 6 a 7 segundos
> **tras la publicación** del objeto."

**CU-06, flujo principal:**
> 7. El sistema comprueba que no falte ningún campo obligatorio y **guarda la pieza** en la
>    colección (RF-23).
> 8. El sistema dispara, **sin intervención del usuario**, la obtención de la valoración de
>    mercado **de forma asíncrona**, que se incorpora a la ficha en 6 a 7 segundos (RNF-02).
>
> Postcondiciones (éxito): "la pieza queda guardada y visible de inmediato en Home y en My
> Vault; su valoración de mercado se incorpora a la ficha unos segundos después **sin que el
> usuario tenga que hacer nada más**."

O sea: **publicar primero (inmediato) → valorar después (en segundo plano, sin bloquear)**.

### Qué hace el código realmente

`app/src/main/java/com/example/aicollect/presentation/newpost/NewPostViewModel.kt`, función
`publish()`:

```kotlin
private suspend fun publish(request: PendingPublish) {
    _saveState.value = SaveItemUiState.Loading

    // Búsqueda de precio ANTES de crear el item (2026-08-24, pedido explícito): así se escribe
    // en Firestore en una sola operación, ya con el precio puesto, en vez de crear vacío y
    // actualizar después (dejaba un hueco visible en "Sin valorar"). Si la búsqueda falla, no
    // se bloquea la publicación...
    val valuation = itemRepository
        .searchValuation(request.nombre, request.marca, request.modelo, request.edicion)
        .getOrElse { ValuationResult(precio = null, min = null, max = null, moneda = DEFAULT_CURRENCY, fuentes = emptyList()) }

    val newItem = Item(/* ...incluye ya valuation.precio/min/max/fuentes... */)

    val photosSnapshot = _photos.toList()
    itemRepository.createItem(newItem, photosSnapshot)
        .onSuccess { _saveState.value = SaveItemUiState.Success }
        .onFailure { _saveState.value = SaveItemUiState.Error(...) }
}
```

Es decir: **primero se busca la valoración (`searchValuation`), y solo cuando esa llamada
termina se crea el ítem (`createItem`)**. Ambas operaciones están cubiertas por el mismo
`SaveItemUiState.Loading`, que en `NewPostFragment.kt` deshabilita el botón "Publicar" y muestra
un overlay de pantalla completa (`overlayLoading`, con el texto del string
`new_post_publishing_valuation`) durante toda la espera. El propio kdoc de `SaveItemUiState.Loading`
en el mismo fichero lo confirma:

> "Covers both the market-price lookup and the actual Firestore write — from the user's point of
> view it's one action ('Publicar'), 2026-08-24 pedido explícito: antes eran dos pasos (crear
> vacío, luego `refreshValuation` en segundo plano) y el precio tardaba en aparecer."

Y `PROJECT_CONTEXT.md` (línea 618) documenta el cambio de arquitectura explícitamente:

> "`FirestoreItemRepository.createItem()` ahora escribe el item ya con el precio si
> `searchValuation` tuvo éxito, en una sola llamada a Firestore — **ya no hay `refreshValuation`
> posterior fire-and-forget**."

### Resumen de la discrepancia

| | Memoria (5.4 / 5.6 / RF-24 / RNF-02 / CU-06) | Código real |
|---|---|---|
| Orden | Publicar → valorar después | Valorar → publicar después |
| Bloqueo | No bloqueante, el ítem aparece de inmediato | Bloqueante: botón deshabilitado + overlay de pantalla completa hasta que ambas operaciones terminan |
| Modelo | Fire-and-forget (`viewModelScope.launch` sin esperar) | Secuencial, esperado (`.getOrElse` seguido de `createItem`) |

Es un cambio de diseño real, hecho a propósito el 2026-08-24 (mejor UX: evita el hueco visible de
"Sin valorar" en la ficha recién creada), pero la memoria nunca se actualizó para reflejarlo —
sigue describiendo el diseño anterior. Como el Capítulo 8 va a dibujar el diagrama de secuencia
de este mismo flujo, conviene decidir antes de dibujarlo si se corrige la memoria (RF-24, RNF-02,
5.4, 5.6, CU-06) para que describa el comportamiento bloqueante actual, o si se revierte el código
al diseño fire-and-forget que la memoria ya documenta.

---

## 2. Room y las escrituras: la memoria dice "Room no participa", el código sí escribe en Room directamente al editar/eliminar

### Qué dice la memoria

**Sección 9.4, "Capa local: Room":**
> "**Room no participa en las escrituras**: cuando un usuario publica o edita una pieza, la
> operación se dirige directamente a Firestore, y **solo se ve reflejado en Room una vez este es
> notificado del cambio por Firestore**."

**Sección 9.5, "Sincronización local–remoto":**
> "La sincronización se apoya en un listener de Firestore en tiempo real: cada vez que la
> colección de ítems del usuario cambia en el servidor, el cliente recibe la actualización y la
> escribe en la tabla local de Room."

Es decir: el único camino de escritura hacia Room es el listener de Firestore, tanto para crear
como para editar/eliminar.

### Qué hace el código realmente

Cierto para `createItem()` (no toca `itemDao` directamente, depende del listener). Pero
`FirestoreItemRepository.kt` — `updateItem()` y `deleteItem()` escriben en Room **de forma
directa e inmediata**, sin pasar por el listener:

```kotlin
override suspend fun updateItem(itemId: String, item: Item): Result<Unit> = runCatching {
    val uid = requireUid()
    val updatedAt = System.currentTimeMillis()
    itemsCollection(uid).document(itemId)
        .update(item.toUpdateMap(updatedAt = updatedAt))
        .await()
    // Sin esto, una relectura inmediata (getItem(), que mira Room primero) devolvería el item
    // anterior a la edición hasta que el listener de Firestore se pusiera al día...
    itemDao.upsertAll(listOf(item.copy(id = itemId, updatedAt = updatedAt).toEntity(uid)))
    Unit
}

override suspend fun deleteItem(itemId: String): Result<Unit> = runCatching {
    ...
    itemsCollection(uid).document(itemId).delete().await()
    // Igual que en updateItem: sin esto, el item borrado seguiría apareciendo brevemente...
    itemDao.deleteById(itemId)
    Unit
}
```

`refreshValuation()` hace lo mismo (relee el documento de Firestore tras llamar a la Cloud
Function y escribe el resultado en Room a mano).

### Resumen de la discrepancia

| Operación | Memoria (9.4/9.5) | Código real |
|---|---|---|
| Crear (`createItem`) | Solo vía listener de Firestore | Coincide: solo vía listener |
| Editar (`updateItem`) | Solo vía listener de Firestore | **Escribe en Room directamente**, además del listener |
| Eliminar (`deleteItem`) | Solo vía listener de Firestore | **Borra de Room directamente**, además del listener |
| Refrescar valoración (`refreshValuation`) | No mencionado como caso aparte | **Escribe en Room directamente** tras releer Firestore |

Es un matiz menor comparado con el punto 1, pero relevante para un diagrama de secuencia fiel:
"editar" y "eliminar" no siguen el mismo camino que "crear" hacia Room, y la razón (evitar que una
relectura inmediata tras editar/eliminar devuelva el dato viejo) está documentada en el propio
código pero no en la memoria.

---

## Lo que sí coincide (verificado, sin discrepancia)

- Esquema de `ItemEntity` (Sección 9.4, Tabla 22) — coincide campo a campo con
  `data/items/local/ItemEntity.kt`.
- RF-14 (eliminar ítem con confirmación previa) — `ItemDetailFragment.showDeleteConfirmationDialog()`.
- RF-29 (eliminar cuenta) — `AuthRepository.deleteAccount()` + `SecurityFragment.showDeleteAccountDialog()`
  con reautenticación por contraseña.
- RF-20/RF-21 (prellenar ficha desde el candidato salvo estado/deporte, que siempre son manuales) —
  `NewPostFragment.prefillFromCandidate()` solo rellena nombre/descripción.
- Evolución documentada de la fuente de valoración (StockX → catálogo manual → eBay Browse →
  Gemini con grounding) — Sección 6.6 de la memoria coincide con `PROJECT_CONTEXT.md` y con el
  código actual (`FirestoreItemRepository.searchValuation`/`refreshValuation`).
- Arquitectura MVVM, patrón UiState sellado, Fragments + Navigation Component + Activity única —
  todo coincide con lo verificado en `capitulo8_material.md`.
