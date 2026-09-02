// Mensaje de UI que puede resolverse sin Context (los ViewModels lo construyen) y solo se
// convierte a String en el punto donde sí hay Context (el Fragment, vía asString()). Permite que
// los estados de error de los ViewModel mezclen mensajes fijos (recurso de strings.xml, con
// argumentos de formato si hace falta) y mensajes dinámicos (p. ej. el texto real de una
// excepción de red/Firebase, que no puede predefinirse como recurso) con el mismo tipo.
package com.example.aicollect.presentation

import android.content.Context
import androidx.annotation.StringRes

sealed interface UiText {
    data class DynamicString(val value: String) : UiText
    data class StringResource(@StringRes val resId: Int, val args: List<Any> = emptyList()) : UiText
}

fun UiText.asString(context: Context): String = when (this) {
    is UiText.DynamicString -> value
    is UiText.StringResource -> context.getString(resId, *args.toTypedArray())
}
