package ru.ibakaidov.distypepro

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
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
class BankPlacementSwitchTest {

    @get:Rule
    val scenarioRule = ActivityScenarioRule(AuthActivity::class.java)

    @Test
    fun switchingPlacementToSeparateScreen_movesBankOutOfMain() {
        UiTestSupport.loginAndWaitForMainScreen()
        UiTestSupport.waitForViewOnScreen(withId(R.id.bank_inline_section), 10_000)

        UiTestSupport.clickToolbarAction(
            toolbarId = R.id.toolbar,
            actionId = R.id.settings_menu_item,
            titleRes = R.string.settings
        )
        UiTestSupport.waitForViewOnScreen(withId(R.id.bankPlacementDropdown), 10_000)

        UiTestSupport.selectExposedDropdownOption(
            R.id.bankPlacementDropdown,
            InstrumentationRegistry.getInstrumentation().targetContext
                .getString(R.string.settings_bank_placement_separate)
        )
        pressBack()

        UiTestSupport.waitForViewOnScreen(withId(R.id.open_bank_card), 10_000)
        UiTestSupport.waitForViewToDisappear(withId(R.id.bank_inline_section), 10_000)
        onView(withId(R.id.open_bank_card)).check(matches(isDisplayed()))

        onView(withId(R.id.open_bank_card)).perform(click())
        UiTestSupport.waitForViewOnScreen(withId(R.id.bank_group), 10_000)
        UiTestSupport.assertToolbarActionAvailable(
            toolbarId = R.id.toolbar,
            actionId = R.id.action_add_category,
            titleRes = R.string.bank_add_category
        )
    }
}
