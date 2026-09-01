// Test de instrumentación de NewPostDisambiguationFragment: muestra los candidatos reconocidos y comprueba la selección o el descarte de todos ellos.
package com.example.aicollect.presentation.newpost

import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers.allOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.aicollect.HiltTestActivity
import com.example.aicollect.R
import com.example.aicollect.application.recognition.RankedCandidate
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
class NewPostDisambiguationFragmentTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var fakeRecognitionRepository: FakeRecognitionRepository

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

    private class Launched(
        val viewModel: NewPostViewModel,
        val navController: TestNavHostController,
    )

    private fun launchWithCandidates(candidates: List<RankedCandidate>): Launched {
        val scenario = ActivityScenario.launch(HiltTestActivity::class.java)
        lateinit var launched: Launched

        scenario.onActivity { activity ->
            val viewModel = ViewModelProvider(activity)[NewPostViewModel::class.java]
            fakeRecognitionRepository.nextResult = Result.success(candidates)
            viewModel.addPhoto(ByteArray(4))
            viewModel.recognize(ByteArray(4))

            val fragment = NewPostDisambiguationFragment()
            activity.supportFragmentManager.beginTransaction()
                .add(HiltTestActivity.CONTAINER_ID, fragment, "under-test")
                .commitNow()

            val navController = TestNavHostController(InstrumentationRegistry.getInstrumentation().targetContext)
            navController.navigatorProvider.addNavigator("fragment", NoOpNavigator())
            navController.setGraph(R.navigation.nav_graph)
            navController.setCurrentDestination(R.id.newPostFragment)
            navController.navigate(R.id.newPostDisambiguationFragment)
            Navigation.setViewNavController(requireNotNull(fragment.view), navController)

            launched = Launched(viewModel, navController)
        }

        return launched
    }

    @Test
    fun candidatesAreDisplayedWithTheFirstOneMarkedAsBestMatch() {
        launchWithCandidates(listOf(bestMatch, secondCandidate))

        onView(withText("Nike Air Force 1")).check(matches(isDisplayed()))
        onView(withText("Adidas Superstar")).check(matches(isDisplayed()))
        onView(allOf(withText(R.string.disambiguation_best_match_badge), isDisplayed()))
            .check(matches(isDisplayed()))
    }

    @Test
    fun emptyCandidateListShowsTheEmptyStateInsteadOfAnyCard() {
        launchWithCandidates(emptyList())

        onView(withId(R.id.tv_empty_state)).check(matches(isDisplayed()))
    }

    @Test
    fun tappingNoneOfTheseClearsTheSelectionAndPopsBackToNewPostFragment() {
        val launched = launchWithCandidates(listOf(bestMatch, secondCandidate))

        onView(withId(R.id.card_none_of_these)).perform(click())

        assertNull(launched.viewModel.selectedCandidate)
        assertEquals(R.id.newPostFragment, launched.navController.currentDestination?.id)
    }

    @Test
    fun tappingACandidateStoresItAsSelectedAndPopsBackToNewPostFragment() {
        val launched = launchWithCandidates(listOf(bestMatch, secondCandidate))

        onView(withText("Nike Air Force 1")).perform(click())

        assertEquals(bestMatch, launched.viewModel.selectedCandidate)
        assertEquals(R.id.newPostFragment, launched.navController.currentDestination?.id)
    }
}
