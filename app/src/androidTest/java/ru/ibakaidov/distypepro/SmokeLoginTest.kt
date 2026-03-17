package ru.ibakaidov.distypepro

import android.os.SystemClock
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.pressImeActionButton
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Matchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ibakaidov.distypepro.screens.AuthActivity

@RunWith(AndroidJUnit4::class)
class SmokeLoginTest {

    @get:Rule
    val scenarioRule = ActivityScenarioRule(AuthActivity::class.java)

    @Test
    fun loginShowsMainScreenWithInlineBank() {
        UiTestSupport.loginAndWaitForMainScreen()
        UiTestSupport.waitForViewOnScreen(withId(R.id.input_group), 10_000)
        UiTestSupport.waitForViewOnScreen(withId(R.id.bank_inline_section), 10_000)
        UiTestSupport.waitForViewOnScreen(withId(R.id.bank_inline_toolbar), 10_000)
        onView(withId(R.id.open_bank_card)).check(matches(not(isDisplayed())))

        openSpotlight()
        UiTestSupport.waitForViewOnScreen(withId(R.id.fullscreen_content), 10_000)
        pressBack()

        UiTestSupport.clickToolbarAction(
            toolbarId = R.id.toolbar,
            actionId = R.id.settings_menu_item,
            titleRes = R.string.settings
        )
        UiTestSupport.waitForViewOnScreen(withId(R.id.ttsSectionTitle), 10_000)
        pressBack()

        onView(withId(R.id.dialog_menu_item)).perform(click())
        UiTestSupport.waitForViewOnScreen(withId(R.id.messages_recycler), 10_000)
        pressBack()

        UiTestSupport.assertToolbarActionAvailable(
            toolbarId = R.id.bank_inline_toolbar,
            actionId = R.id.action_add_category,
            titleRes = R.string.bank_add_category
        )
        onView(withId(R.id.action_add_statement)).check(doesNotExist())

        UiTestSupport.openToolbarOverflow(R.id.bank_inline_toolbar)
        onView(withText(R.string.global_import_title)).perform(click())
        UiTestSupport.waitForViewOnScreen(withId(R.id.global_import_list), 10_000)
        pressBack()

        val categoryName = "Smoke Category ${SystemClock.elapsedRealtime()}"
        UiTestSupport.clickToolbarAction(
            toolbarId = R.id.bank_inline_toolbar,
            actionId = R.id.action_add_category,
            titleRes = R.string.bank_add_category
        )
        UiTestSupport.replaceDialogPrompt(categoryName)
        onView(withText(R.string.ok)).perform(click())
        UiTestSupport.waitForViewOnScreen(withText(categoryName), 10_000)
        onView(withText(categoryName)).perform(click())

        UiTestSupport.assertToolbarActionAvailable(
            toolbarId = R.id.bank_inline_toolbar,
            actionId = R.id.action_add_statement,
            titleRes = R.string.bank_add_phrase
        )
        onView(withId(R.id.action_add_category)).check(doesNotExist())

        val phraseText = "Smoke Phrase ${SystemClock.elapsedRealtime()}"
        UiTestSupport.clickToolbarAction(
            toolbarId = R.id.bank_inline_toolbar,
            actionId = R.id.action_add_statement,
            titleRes = R.string.bank_add_phrase
        )
        UiTestSupport.replaceDialogPrompt(phraseText)
        onView(withText(R.string.ok)).perform(click())
        UiTestSupport.waitForViewOnScreen(withText(phraseText), 10_000)

        pressBack()
        UiTestSupport.waitForViewOnScreen(withText(categoryName), 10_000)

        onView(withId(R.id.text_to_speech_edittext))
            .perform(click(), replaceText("Inline layout check"), pressImeActionButton())
        onView(withId(R.id.say_button)).check(matches(isDisplayed()))
        onView(withId(R.id.bank_inline_toolbar)).check(matches(isDisplayed()))
    }
    private fun openSpotlight() {
        onView(withId(R.id.spotlight_menu_item)).perform(click())
    }
}
