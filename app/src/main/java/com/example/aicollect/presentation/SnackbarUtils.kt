// Funciones de extensión para mostrar Snackbars en cualquier Fragment, ancladas por encima de la barra de navegación inferior en las pantallas que la tienen.
package com.example.aicollect.presentation

import android.view.View
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.example.aicollect.R
import com.google.android.material.snackbar.Snackbar

fun Fragment.showSnackbar(view: View, message: CharSequence, duration: Int = Snackbar.LENGTH_LONG): Snackbar {
    val snackbar = Snackbar.make(view, message, duration)
    anchorAboveBottomNavIfVisible(snackbar)
    return snackbar
}

fun Fragment.showSnackbar(view: View, @StringRes messageRes: Int, duration: Int = Snackbar.LENGTH_LONG): Snackbar {
    val snackbar = Snackbar.make(view, messageRes, duration)
    anchorAboveBottomNavIfVisible(snackbar)
    return snackbar
}

private fun Fragment.anchorAboveBottomNavIfVisible(snackbar: Snackbar) {
    // Se ancla siempre que la barra exista, sin mirar su visibilidad actual: si el Snackbar se
    // lanza justo antes de navegar a una pantalla que sí la muestra (p. ej. al volver a Home),
    // comprobar la visibilidad aquí capturaría el estado antiguo (GONE) y el Snackbar quedaría
    // sin ancla para siempre. El propio Snackbar sigue de forma dinámica los cambios de layout
    // del ancla, así que se reposiciona solo en cuanto la barra pasa a visible.
    activity?.findViewById<View>(R.id.bottom_nav_bar)?.let { bottomNavBar ->
        snackbar.anchorView = bottomNavBar
    }
}
