package com.example.aicollect.presentation

import android.view.View
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.example.aicollect.R
import com.google.android.material.snackbar.Snackbar

/**
 * `MainActivity`'s bottom nav bar is a custom `<include>` inside the same `CoordinatorLayout` as
 * the Fragment content — not a `BottomNavigationView`, so it has none of Material's built-in
 * Snackbar-avoidance behavior. A plain `Snackbar.make(...)` renders underneath it, unreadable
 * (2026-08-24 user feedback). Anchoring explicitly above it — when it's actually visible, i.e. on
 * Home/My Vault/Detalle, not the full-screen destinations where it's `GONE` — fixes that.
 */
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
