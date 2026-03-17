package ru.ibakaidov.distypepro

import android.os.SystemClock
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ibakaidov.distypepro.screens.AuthActivity

@RunWith(AndroidJUnit4::class)
class BankHostParityTest {

    @get:Rule
    val scenarioRule = ActivityScenarioRule(AuthActivity::class.java)

    @Test
    fun inlineAndSeparateHosts_shareSameBankActions() {
        UiTestSupport.loginAndWaitForMainScreen()
        assertCategoryState(toolbarId = R.id.bank_inline_toolbar)

        val categoryName = "Parity Category ${SystemClock.elapsedRealtime()}"
        UiTestSupport.clickToolbarAction(
            toolbarId = R.id.bank_inline_toolbar,
            actionId = R.id.action_add_category,
            titleRes = R.string.bank_add_category
        )
        UiTestSupport.replaceDialogPrompt(categoryName)
        onView(withText(R.string.ok)).perform(click())
        UiTestSupport.waitForViewOnScreen(withText(categoryName), 10_000)
        onView(withText(categoryName)).perform(click())

        assertStatementsState(toolbarId = R.id.bank_inline_toolbar)
        pressBack()
        UiTestSupport.waitForViewOnScreen(withText(categoryName), 10_000)

        UiTestSupport.clickToolbarAction(
            toolbarId = R.id.toolbar,
            actionId = R.id.settings_menu_item,
            titleRes = R.string.settings
        )
        UiTestSupport.selectExposedDropdownOption(
            R.id.bankPlacementDropdown,
            InstrumentationRegistry.getInstrumentation().targetContext
                .getString(R.string.settings_bank_placement_separate)
        )
        pressBack()

        UiTestSupport.waitForViewOnScreen(withId(R.id.open_bank_card), 10_000)
        onView(withId(R.id.open_bank_card)).perform(click())
        UiTestSupport.waitForViewOnScreen(withId(R.id.bank_group), 10_000)

        assertCategoryState(toolbarId = R.id.toolbar)
        onView(withText(categoryName)).perform(click())
        assertStatementsState(toolbarId = R.id.toolbar)
    }

    private fun assertCategoryState(toolbarId: Int) {
        UiTestSupport.assertToolbarActionAvailable(
            toolbarId = toolbarId,
            actionId = R.id.action_add_category,
            titleRes = R.string.bank_add_category
        )
        onView(withId(R.id.action_add_statement)).check(doesNotExist())
        UiTestSupport.openToolbarOverflow(toolbarId)
        onView(withText(R.string.global_import_title)).check(matches(isDisplayed()))
        pressBack()
    }

    private fun assertStatementsState(toolbarId: Int) {
        UiTestSupport.assertToolbarActionAvailable(
            toolbarId = toolbarId,
            actionId = R.id.action_add_statement,
            titleRes = R.string.bank_add_phrase
        )
        onView(withId(R.id.action_add_category)).check(doesNotExist())
        onView(withText(R.string.global_import_title)).check(doesNotExist())
    }
}
