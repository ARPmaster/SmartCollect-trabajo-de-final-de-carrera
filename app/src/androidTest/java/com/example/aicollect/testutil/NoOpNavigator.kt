// Navegador de prueba que sustituye al navegador de fragmentos real: solo actualiza la pila de navegación sin instanciar ninguna pantalla de destino de verdad.
package com.example.aicollect.testutil

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.Navigator

@Navigator.Name("fragment")
class NoOpNavigator : Navigator<NavDestination>() {

    override fun createDestination(): NavDestination = NavDestination(this)

    override fun navigate(
        entries: List<NavBackStackEntry>,
        navOptions: NavOptions?,
        navigatorExtras: Extras?,
    ) {
        entries.forEach { entry -> state.push(entry) }
    }

    override fun popBackStack(popUpTo: NavBackStackEntry, savedState: Boolean) {
        state.pop(popUpTo, savedState)
    }
}
