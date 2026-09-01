# SmartCollect — Backend (Cloud Functions)

Backend serverless de [SmartCollect](..) sobre Firebase Cloud Functions. Se usa solo para dos cosas
que el cliente Android no puede resolver por sí solo:

- **`recognizeItem`** — identifica un objeto a partir de una foto (Google Cloud Vision + Google
  Gemini) para autorrellenar una publicación nueva.
- **`searchValuation`** / **`refreshValuation`** — calculan una valoración de mercado orientativa
  a partir de anuncios activos reales en eBay (eBay Browse API), a partir del texto identificado
  por Gemini.

El resto de la aplicación (colección, perfil, autenticación) usa directamente los SDK de
Firebase (Auth/Firestore/Storage) desde el cliente, sin pasar por este backend.

## Requisitos

- Node.js 24.
- npm.
- Firebase CLI, autenticada contra una cuenta de Google con acceso al proyecto Firebase.

## Estructura

```
functions/
  src/
    index.ts            # exporta recognizeItem, searchValuation, refreshValuation
    recognizeItem.ts
    refreshValuation.ts  # searchValuation y refreshValuation
```

## Despliegue

1. Da de alta el secreto `GEMINI_API_KEY` en Secret Manager y concede acceso a la cuenta de
   servicio de Cloud Functions
   (`<PROJECT_NUMBER>-compute@developer.gserviceaccount.com`) mediante el rol
   `roles/secretmanager.secretAccessor`. El propio flujo de `firebase deploy` detecta si el
   secreto existe y solicita este permiso automáticamente si aún no se ha concedido.
2. Despliega, desde esta carpeta (`backend/`):

   ```sh
   cd functions && npm install && npm run build
   cd .. && firebase deploy --only functions:recognizeItem
   ```

   sustituyendo el nombre de la función según corresponda para el resto
   (`searchValuation`, `refreshValuation`). Todas se despliegan en la región `europe-west1`.

El primer despliegue de una función activa automáticamente, si no lo están ya, varias APIs de
Google Cloud necesarias: `run.googleapis.com`, `eventarc.googleapis.com`,
`pubsub.googleapis.com`, `storage.googleapis.com`, `secretmanager.googleapis.com` y
`artifactregistry.googleapis.com`. El asistente de línea de comandos de Firebase las habilita de
forma interactiva la primera vez que se detectan como necesarias y ausentes.

En el primer despliegue, Firebase preguntará si se desea configurar una política de limpieza de
las imágenes de contenedor generadas en Artifact Registry (para evitar acumulación de facturación
por builds antiguos); en este proyecto se usa borrado automático de imágenes con más de un día de
antigüedad.

**Verificación:** una vez completado, la consola de Firebase confirma el estado *Deploy
complete!* y permite consultar los registros de ejecución de cada función desde *Functions*.

## Scripts disponibles (`functions/package.json`)

| Script | Descripción |
| --- | --- |
| `npm run build` | Compila TypeScript a `lib/`. |
| `npm run build:watch` | Compila en modo watch. |
| `npm run serve` | Compila y arranca el emulador local de Functions. |
| `npm run shell` | Compila y abre el shell interactivo de Firebase Functions. |
| `npm run deploy` | Compila y despliega todas las funciones. |
| `npm run logs` | Muestra los logs de ejecución en producción. |

## Variables y secretos sensibles no incluidos en el repositorio

El valor del secreto `GEMINI_API_KEY` se gestiona en Secret Manager y no forma parte del
repositorio. Quien desee reproducir el proyecto desde cero deberá generarlo con sus propias
credenciales de la API de Gemini.
