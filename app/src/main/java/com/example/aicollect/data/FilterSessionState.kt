// Estado del filtro de Home en memoria: vive solo mientras el usuario permanece en esa pantalla
// (sobrevive a reabrir el bottom sheet y a cambios de configuración, ya que no depende de la
// vista) y se reinicia al navegar a cualquier otro destino — ver el listener de navegación en
// MainActivity. A propósito no usa SharedPreferences: el filtro no debe recordarse entre
// sesiones ni al cambiar de pantalla dentro de la misma sesión.
package com.example.aicollect.data

object FilterSessionState {
    const val DEFAULT_MIN_PRICE = 0
    const val DEFAULT_MAX_PRICE = 1000

    data class SavedFilters(
        val minPrice: Int,
        val maxPrice: Int,
        val sport: String?,
        val condition: String?,
        val sortOrdinal: Int,
    )

    private val defaults = SavedFilters(
        minPrice = DEFAULT_MIN_PRICE,
        maxPrice = DEFAULT_MAX_PRICE,
        sport = null,
        condition = null,
        sortOrdinal = 0,
    )

    var current: SavedFilters = defaults
        private set

    fun update(filters: SavedFilters) {
        current = filters
    }

    fun reset() {
        current = defaults
    }
}
