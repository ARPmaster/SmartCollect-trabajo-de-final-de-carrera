// Test de instrumentación de NewPostFragment: validación de campos obligatorios al publicar y publicación feliz con navegación a Home.
package com.example.aicollect.presentation.newpost

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.aicollect.R
import com.example.aicollect.testutil.FakeItemRepository
import com.example.aicollect.testutil.launchFragmentWithNavController
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class NewPostFragmentTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var fakeItemRepository: FakeItemRepository

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun publishingWithoutRequiredFieldsShowsTheValidationMessageAndNeverCallsTheRepository() {
        launchFragmentWithNavController<NewPostFragment>(R.id.newPostFragment)

        onView(withId(R.id.btn_publish)).perform(scrollTo(), click())

        onView(withText(R.string.new_post_name_required_error)).check(matches(isDisplayed()))
        assertEquals(0, fakeItemRepository.createItemCallCount)
    }

    @Test
    fun fillingNameSportAndConditionThenPublishingCreatesTheItemAndNavigatesToHome() {
        val launched = launchFragmentWithNavController<NewPostFragment>(R.id.newPostFragment)

        onView(withId(R.id.et_name)).perform(replaceText("Nike Air Force 1"), closeSoftKeyboard())
        onView(withId(R.id.btn_sport)).perform(click())
        onView(withText("Baloncesto")).inRoot(isPlatformPopup()).perform(click())
        onView(withId(R.id.btn_condition)).perform(click())
        onView(withText("Nuevo")).inRoot(isPlatformPopup()).perform(click())

        onView(withId(R.id.btn_publish)).perform(scrollTo(), click())

        assertEquals(1, fakeItemRepository.createItemCallCount)
        assertEquals("Nike Air Force 1", fakeItemRepository.lastCreatedItem?.nombre)
        assertEquals("Baloncesto", fakeItemRepository.lastCreatedItem?.deporte)
        assertEquals("Nuevo", fakeItemRepository.lastCreatedItem?.estado)
        assertEquals(R.id.homeFragment, launched.navController.currentDestination?.id)
    }
}
