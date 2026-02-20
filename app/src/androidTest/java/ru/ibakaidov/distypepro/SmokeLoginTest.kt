package ru.ibakaidov.distypepro

import android.app.Activity
import android.os.SystemClock
import android.view.View
import android.widget.TextView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.util.TreeIterables
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ibakaidov.distypepro.screens.AuthActivity

@RunWith(AndroidJUnit4::class)
class SmokeLoginTest {

    @get:Rule
    val scenarioRule = ActivityScenarioRule(AuthActivity::class.java)

    @Test
    fun loginShowsMainScreen() {
        val args = InstrumentationRegistry.getArguments()
        val email = args.getString("email") ?: error("Missing instrumentation arg: email")
        val password = args.getString("password") ?: error("Missing instrumentation arg: password")

        onView(withId(R.id.emailInput))
            .perform(replaceText(email), closeSoftKeyboard())
        onView(withId(R.id.passwordInput))
            .perform(replaceText(password), closeSoftKeyboard())
        onView(withId(R.id.authPrimaryButton))
            .perform(click())

        waitForAuthResult(30_000)
        waitForViewOnScreen(withId(R.id.input_group), 10_000)
        waitForViewOnScreen(withId(R.id.open_bank_card), 10_000)

        openSpotlight()
        waitForViewOnScreen(withId(R.id.fullscreen_content), 10_000)
        pressBack()

        openMainOverflow()
        onView(withText(R.string.settings)).perform(click())
        waitForViewOnScreen(withId(R.id.ttsSectionTitle), 10_000)
        pressBack()

        onView(withId(R.id.dialog_menu_item)).perform(click())
        waitForViewOnScreen(withId(R.id.messages_recycler), 10_000)
        pressBack()

        onView(withId(R.id.open_bank_card)).perform(click())
        waitForViewOnScreen(withId(R.id.bank_group), 10_000)
        onView(withId(R.id.action_add_category)).check(matchesDisplayed())
        onView(withId(R.id.action_add_statement)).check(doesNotExist())

        openBankOverflow()
        onView(withText(R.string.global_import_title)).perform(click())
        waitForViewOnScreen(withId(R.id.global_import_list), 10_000)
        pressBack()

        val categoryName = "Smoke Category ${SystemClock.elapsedRealtime()}"
        onView(withId(R.id.action_add_category)).perform(click())
        waitForViewOnScreen(withId(R.id.input_prompt), 5_000)
        onView(withId(R.id.input_prompt))
            .perform(replaceText(categoryName), closeSoftKeyboard())
        onView(withText(R.string.ok)).perform(click())
        waitForViewOnScreen(withText(categoryName), 10_000)
        onView(withText(categoryName)).perform(click())

        onView(withId(R.id.action_add_statement)).check(matchesDisplayed())
        onView(withId(R.id.action_add_category)).check(doesNotExist())

        val phraseText = "Smoke Phrase ${SystemClock.elapsedRealtime()}"
        onView(withId(R.id.action_add_statement)).perform(click())
        waitForViewOnScreen(withId(R.id.input_prompt), 5_000)
        onView(withId(R.id.input_prompt))
            .perform(replaceText(phraseText), closeSoftKeyboard())
        onView(withText(R.string.ok)).perform(click())
        waitForViewOnScreen(withText(phraseText), 10_000)
    }

    private fun matchesDisplayed() = androidx.test.espresso.assertion.ViewAssertions.matches(isDisplayed())

    private fun waitForAuthResult(timeoutMs: Long) {
        val startTime = SystemClock.elapsedRealtime()
        val endTime = startTime + timeoutMs
        do {
            val activity = getResumedActivity()
            val rootView = activity?.window?.decorView
            if (rootView != null) {
                val mainView = rootView.findViewById<View>(R.id.input_group)
                if (mainView?.isShown == true) return
                val snackbarText = findSnackbarText(rootView)
                if (snackbarText != null) {
                    throw AssertionError("Login failed: $snackbarText")
                }
            }
            SystemClock.sleep(50)
        } while (SystemClock.elapsedRealtime() < endTime)

        throw AssertionError("Auth result not observed within $timeoutMs ms")
    }

    private fun waitForViewOnScreen(viewMatcher: Matcher<View>, timeoutMs: Long) {
        val startTime = SystemClock.elapsedRealtime()
        val endTime = startTime + timeoutMs
        do {
            val activity = getResumedActivity()
            val rootView = activity?.window?.decorView
            if (rootView != null) {
                val matched = TreeIterables.breadthFirstViewTraversal(rootView)
                    .any { viewMatcher.matches(it) && it.isShown }
                if (matched) return
            }
            SystemClock.sleep(50)
        } while (SystemClock.elapsedRealtime() < endTime)

        throw AssertionError("View not found within $timeoutMs ms: $viewMatcher")
    }

    private fun findSnackbarText(root: View): String? {
        val snackbarMatcher = withId(com.google.android.material.R.id.snackbar_text)
        return TreeIterables.breadthFirstViewTraversal(root)
            .firstOrNull { snackbarMatcher.matches(it) && it.isShown }
            ?.let { (it as? TextView)?.text?.toString() }
    }

    private fun getResumedActivity(): Activity? {
        var activity: Activity? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val resumed = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
            activity = resumed.firstOrNull()
        }
        return activity
    }

    private fun openMainOverflow() {
        val overflowDescription = androidx.appcompat.R.string.abc_action_menu_overflow_description
        onView(
            allOf(
                withContentDescription(overflowDescription),
                isDescendantOfA(withId(R.id.toolbar))
            )
        ).perform(click())
    }

    private fun openSpotlight() {
        onView(withId(R.id.spotlight_menu_item)).perform(click())
    }

    private fun openBankOverflow() {
        val overflowDescription = androidx.appcompat.R.string.abc_action_menu_overflow_description
        onView(
            allOf(
                withContentDescription(overflowDescription),
                isDescendantOfA(withId(R.id.toolbar))
            )
        ).perform(click())
    }
}
