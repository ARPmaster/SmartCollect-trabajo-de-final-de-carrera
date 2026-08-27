// Test de instrumentación de LoginFragment: valida credenciales en blanco y el inicio de sesión con navegación a Home.
package com.example.aicollect.presentation.auth

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.aicollect.R
import com.example.aicollect.testutil.FakeAuthRepository
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
class LoginFragmentTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var fakeAuthRepository: FakeAuthRepository

    @Before
    fun setUp() {
        hiltRule.inject()
        fakeAuthRepository.fakeUserId = null
    }

    @Test
    fun blankCredentialsShowTheValidationMessageAndNeverCallSignIn() {
        launchFragmentWithNavController<LoginFragment>(R.id.loginFragment)

        onView(withId(R.id.btn_sign_in)).perform(click())

        onView(withText("Completa tu correo y contraseña.")).check(matches(isDisplayed()))
        assertEquals(0, fakeAuthRepository.signInCallCount)
    }

    @Test
    fun validCredentialsSignInAndNavigateToHome() {
        val launched = launchFragmentWithNavController<LoginFragment>(R.id.loginFragment)

        onView(withId(R.id.et_email)).perform(replaceText("user@example.com"), closeSoftKeyboard())
        onView(withId(R.id.et_password)).perform(replaceText("secret123"), closeSoftKeyboard())
        onView(withId(R.id.btn_sign_in)).perform(click())

        assertEquals(1, fakeAuthRepository.signInCallCount)
        assertEquals("user@example.com", fakeAuthRepository.lastSignInEmail)
        assertEquals(R.id.homeFragment, launched.navController.currentDestination?.id)
    }
}
