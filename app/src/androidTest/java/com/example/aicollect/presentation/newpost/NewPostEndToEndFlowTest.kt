// Test de integración end-to-end del flujo estrella de la app: seleccionar foto (fake) →
// reconocimiento (candidatos) → elegir uno → publicar, comprobando que el ítem publicado incluye
// la valoración de mercado cuando searchValuation tiene éxito, y que la publicación se completa
// igualmente (sin valoración) cuando searchValuation falla (RF-24).
package com.example.aicollect.presentation.newpost

import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.aicollect.HiltTestActivity
import com.example.aicollect.R
import com.example.aicollect.application.items.ValuationResult
import com.example.aicollect.application.items.ValuationSearch
import com.example.aicollect.application.recognition.RankedCandidate
import com.example.aicollect.testutil.FakeItemRepository
import com.example.aicollect.testutil.FakeRecognitionRepository
import com.example.aicollect.testutil.NoOpNavigator
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class NewPostEndToEndFlowTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var fakeRecognitionRepository: FakeRecognitionRepository

    @Inject
    lateinit var fakeItemRepository: FakeItemRepository

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    private val bestMatch = RankedCandidate(
        nombre = "Nike Air Force 1",
        marca = "Nike",
        modelo = "Air Force 1",
        edicion = null,
        procedencia = null,
        confianza = 0.92,
        numeroFuentes = 4,
        score = 0.9,
    )
    private val secondCandidate = RankedCandidate(
        nombre = "Adidas Superstar",
        marca = "Adidas",
        modelo = "Superstar",
        edicion = null,
        procedencia = null,
        confianza = 0.4,
        numeroFuentes = 1,
        score = 0.3,
    )

    private lateinit var scenario: ActivityScenario<HiltTestActivity>
    private lateinit var viewModel: NewPostViewModel
    private lateinit var navController: TestNavHostController

    /** Simula "seleccionar foto (fake) → reconocimiento": puebla los candidatos vía el fake
     * repository y muestra la pantalla de desambiguación, dentro de la misma Activity/ViewModel
     * que usará después NewPostFragment (activityViewModels compartido). */
    private fun launchWithRecognizedCandidates(candidates: List<RankedCandidate>) {
        scenario = ActivityScenario.launch(HiltTestActivity::class.java)
        scenario.onActivity { activity ->
            viewModel = ViewModelProvider(activity)[NewPostViewModel::class.java]
            fakeRecognitionRepository.nextResult = Result.success(candidates)
            viewModel.addPhoto(ByteArray(4))
            viewModel.recognize(ByteArray(4))
            // En la app real esto lo hace NewPostFragment.render() al recibir el Success, justo
            // antes de navegar a la desambiguación. Aquí saltamos directamente a esa pantalla,
            // así que hay que reconocer el resultado a mano para que recognitionState no se
            // quede en Success (si no, al volver a NewPostFragment su propio collector vería
            // Success otra vez e intentaría renavegar antes de tener NavController asignado).
            viewModel.acknowledgeRecognitionResult()

            val fragment = NewPostDisambiguationFragment()
            activity.supportFragmentManager.beginTransaction()
                .add(HiltTestActivity.CONTAINER_ID, fragment, "disambiguation")
                .commitNow()

            navController = TestNavHostController(InstrumentationRegistry.getInstrumentation().targetContext)
            navController.navigatorProvider.addNavigator("fragment", NoOpNavigator())
            navController.setGraph(R.navigation.nav_graph)
            navController.setCurrentDestination(R.id.newPostFragment)
            navController.navigate(R.id.newPostDisambiguationFragment)
            Navigation.setViewNavController(requireNotNull(fragment.view), navController)
        }
    }

    private fun replaceWithNewPostFragment() {
        scenario.onActivity { activity ->
            val fragment = NewPostFragment()
            activity.supportFragmentManager.beginTransaction()
                .replace(HiltTestActivity.CONTAINER_ID, fragment, "new-post")
                .commitNow()
            Navigation.setViewNavController(requireNotNull(fragment.view), navController)
        }
    }

    private fun fillSportAndConditionThenPublish() {
        onView(withId(R.id.btn_sport)).perform(click())
        onView(withText("Baloncesto")).inRoot(isPlatformPopup()).perform(click())
        onView(withId(R.id.btn_condition)).perform(click())
        onView(withText("Nuevo")).inRoot(isPlatformPopup()).perform(click())
        onView(withId(R.id.btn_publish)).perform(scrollTo(), click())
    }

    @Test
    fun fullFlow_recognitionToPublish_includesValuationWhenSearchValuationSucceeds() {
        fakeItemRepository.searchValuationResult = Result.success(
            ValuationResult(
                precio = 120.0,
                min = 100.0,
                max = 140.0,
                moneda = "EUR",
                fuentes = listOf(ValuationSearch("Nike oficial", "https://nike.com")),
            ),
        )

        launchWithRecognizedCandidates(listOf(bestMatch, secondCandidate))
        onView(withText("Nike Air Force 1")).check(matches(isDisplayed()))
        onView(withText("Nike Air Force 1")).perform(click())

        assertEquals(bestMatch, viewModel.selectedCandidate)
        assertEquals(R.id.newPostFragment, navController.currentDestination?.id)

        replaceWithNewPostFragment()
        // El nombre y la descripción ya vienen precargados del candidato elegido (prefillFromCandidate).
        onView(withId(R.id.et_name)).check(matches(withText("Nike Air Force 1")))
        fillSportAndConditionThenPublish()

        assertEquals(1, fakeItemRepository.createItemCallCount)
        val published = fakeItemRepository.lastCreatedItem
        assertEquals("Nike Air Force 1", published?.nombre)
        assertEquals("Nike", published?.marca)
        assertEquals("Air Force 1", published?.modelo)
        assertEquals("Baloncesto", published?.deporte)
        assertEquals("Nuevo", published?.estado)
        assertEquals(120.0, published?.valoracionActual)
        assertEquals("gemini_grounded_search", published?.fuenteValoracion)
        assertEquals(SaveItemUiState.Success, viewModel.saveState.value)
        assertEquals(R.id.homeFragment, navController.currentDestination?.id)
    }

    @Test
    fun fullFlow_publishCompletesWithoutValuation_whenSearchValuationFails() {
        fakeItemRepository.searchValuationResult = Result.failure(Exception("Sin conexión"))

        launchWithRecognizedCandidates(listOf(bestMatch))
        onView(withText("Nike Air Force 1")).perform(click())

        replaceWithNewPostFragment()
        fillSportAndConditionThenPublish()

        assertEquals(1, fakeItemRepository.createItemCallCount)
        val published = fakeItemRepository.lastCreatedItem
        assertEquals("Nike Air Force 1", published?.nombre)
        assertNull(published?.valoracionActual)
        assertNull(published?.fuenteValoracion)
        assertEquals(SaveItemUiState.Success, viewModel.saveState.value)
        assertEquals(R.id.homeFragment, navController.currentDestination?.id)
    }
}
