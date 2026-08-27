// Funciones de extensión para mostrar Snackbars en cualquier Fragment, ancladas por encima de la barra de navegación inferior cuando esta está visible.
package com.example.aicollect.presentation

import android.view.View
import androidx.annotation.StringRes
import androidx.core.view.isVisible
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
    val bottomNavBar = activity?.findViewById<View>(R.id.bottom_nav_bar)
    if (bottomNavBar != null && bottomNavBar.isVisible) {
        snackbar.anchorView = bottomNavBar
    }
}
