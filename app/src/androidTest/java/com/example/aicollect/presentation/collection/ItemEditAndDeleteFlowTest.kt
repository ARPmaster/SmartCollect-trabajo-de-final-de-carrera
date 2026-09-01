// Test de integración end-to-end del flujo de detalle de un ítem ya publicado: editar (cambiar
// nombre y estado) → verificar persistencia en el fake, y eliminar con confirmación o cancelación
// del diálogo, verificando que solo se borra cuando se confirma.
package com.example.aicollect.presentation.collection

import androidx.core.os.bundleOf
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.aicollect.HiltTestActivity
import com.example.aicollect.R
import com.example.aicollect.application.items.Item
import com.example.aicollect.presentation.edititem.EditItemFragment
import com.example.aicollect.testutil.FakeItemRepository
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
class ItemEditAndDeleteFlowTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var fakeItemRepository: FakeItemRepository

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    private val itemId = "item-1"
    private val originalItem = Item(
        id = itemId,
        nombre = "Camiseta Lakers",
        descripcion = "Talla M",
        marca = "Nike",
        modelo = "Lakers Home",
        edicion = null,
        procedencia = null,
        deporte = "Baloncesto",
        estado = "Nuevo",
        valoracionActual = 60.0,
        valoracionMin = null,
        valoracionMax = null,
        valoracionMoneda = "EUR",
        fuenteValoracion = null,
        confianzaIdentificacion = null,
    )

    private lateinit var scenario: ActivityScenario<HiltTestActivity>
    private lateinit var navController: TestNavHostController

    private fun launchItemDetail() {
        fakeItemRepository.getItemResult = { Result.success(originalItem) }
        scenario = ActivityScenario.launch(HiltTestActivity::class.java)
        scenario.onActivity { activity ->
            val fragment = ItemDetailFragment().apply {
                arguments = bundleOf(ItemDetailFragment.ARG_ITEM_ID to itemId)
            }
            activity.supportFragmentManager.beginTransaction()
                .add(HiltTestActivity.CONTAINER_ID, fragment, "detail")
                .commitNow()

            navController = TestNavHostController(InstrumentationRegistry.getInstrumentation().targetContext)
            navController.navigatorProvider.addNavigator("fragment", NoOpNavigator())
            navController.setGraph(R.navigation.nav_graph)
            navController.setCurrentDestination(R.id.homeFragment)
            navController.navigate(R.id.itemDetailFragment, bundleOf("itemId" to itemId))
            Navigation.setViewNavController(requireNotNull(fragment.view), navController)
        }
    }

    private fun replaceWithEditItemFragment() {
        scenario.onActivity { activity ->
            val fragment = EditItemFragment().apply {
                arguments = bundleOf("itemId" to itemId)
            }
            activity.supportFragmentManager.beginTransaction()
                .replace(HiltTestActivity.CONTAINER_ID, fragment, "edit")
                .commitNow()
            Navigation.setViewNavController(requireNotNull(fragment.view), navController)
        }
    }

    @Test
    fun editItem_changesNameAndCondition_persistsThroughTheFake() {
        launchItemDetail()
        onView(withId(R.id.btn_edit_item)).perform(click())

        assertEquals(R.id.editItemFragment, navController.currentDestination?.id)
        replaceWithEditItemFragment()

        onView(withId(R.id.et_name)).check(matches(withText("Camiseta Lakers")))
        onView(withId(R.id.et_name)).perform(clearText(), typeText("Camiseta Lakers Retro"), closeSoftKeyboard())
        onView(withId(R.id.btn_condition)).perform(click())
        onView(withText("Buen estado")).inRoot(isPlatformPopup()).perform(click())
        onView(withId(R.id.btn_save)).perform(click())

        assertEquals(1, fakeItemRepository.updateItemCallCount)
        assertEquals(itemId, fakeItemRepository.lastUpdatedItemId)
        val updated = fakeItemRepository.lastUpdatedItem
        assertEquals("Camiseta Lakers Retro", updated?.nombre)
        assertEquals("Buen estado", updated?.estado)
        // Los campos no editables se conservan tal cual estaban.
        assertEquals("Nike", updated?.marca)
        assertEquals(R.id.itemDetailFragment, navController.currentDestination?.id)
    }

    @Test
    fun deleteItem_confirmed_deletesAndNavigatesBack() {
        launchItemDetail()
        onView(withId(R.id.btn_delete_item)).perform(click())

        onView(withText(R.string.item_detail_delete_confirm_button)).inRoot(isDialog()).perform(click())

        assertEquals(1, fakeItemRepository.deleteItemCallCount)
        assertEquals(itemId, fakeItemRepository.lastDeletedItemId)
        assertEquals(R.id.homeFragment, navController.currentDestination?.id)
    }

    @Test
    fun deleteItem_cancelled_doesNotDelete() {
        launchItemDetail()
        onView(withId(R.id.btn_delete_item)).perform(click())

        onView(withText(R.string.item_detail_delete_cancel_button)).inRoot(isDialog()).perform(click())

        assertEquals(0, fakeItemRepository.deleteItemCallCount)
        assertNull(fakeItemRepository.lastDeletedItemId)
        assertEquals(R.id.itemDetailFragment, navController.currentDestination?.id)
        onView(withId(R.id.tv_item_title)).check(matches(isDisplayed()))
    }
}
