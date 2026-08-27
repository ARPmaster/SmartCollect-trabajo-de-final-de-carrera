// Helper de pruebas de instrumentación: lanza un Fragment dentro de una Activity anfitriona con un NavController de prueba ya cargado con el grafo de navegación real.
package com.example.aicollect.testutil

import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.example.aicollect.HiltTestActivity
import com.example.aicollect.R

const val FRAGMENT_TAG = "fragment-under-test"

class LaunchedFragment<F : Fragment>(
    val scenario: ActivityScenario<HiltTestActivity>,
    val fragment: F,
    val navController: TestNavHostController,
)

inline fun <reified F : Fragment> launchFragmentWithNavController(startDestinationId: Int): LaunchedFragment<F> {
    val scenario = ActivityScenario.launch(HiltTestActivity::class.java)
    lateinit var fragmentRef: F
    lateinit var navControllerRef: TestNavHostController

    scenario.onActivity { activity ->
        val fragment = activity.supportFragmentManager.fragmentFactory.instantiate(
            requireNotNull(F::class.java.classLoader),
            F::class.java.name,
        )
        activity.supportFragmentManager.beginTransaction()
            .add(HiltTestActivity.CONTAINER_ID, fragment, FRAGMENT_TAG)
            .commitNow()

        val navController = TestNavHostController(InstrumentationRegistry.getInstrumentation().targetContext)
        navController.navigatorProvider.addNavigator("fragment", NoOpNavigator())
        navController.setGraph(R.navigation.nav_graph)
        navController.setCurrentDestination(startDestinationId)
        Navigation.setViewNavController(requireNotNull(fragment.view), navController)

        @Suppress("UNCHECKED_CAST")
        fragmentRef = fragment as F
        navControllerRef = navController
    }

    return LaunchedFragment(scenario, fragmentRef, navControllerRef)
}
