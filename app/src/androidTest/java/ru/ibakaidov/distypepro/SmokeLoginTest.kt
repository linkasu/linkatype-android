package ru.ibakaidov.distypepro

import android.os.SystemClock
import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.PerformException
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.espresso.util.TreeIterables
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

        onView(isRoot()).perform(waitForView(withId(R.id.input_group), 15_000))
        onView(withId(R.id.input_group)).check(matchesDisplayed())
    }

    private fun matchesDisplayed() = androidx.test.espresso.assertion.ViewAssertions.matches(isDisplayed())

    private fun waitForView(viewMatcher: Matcher<View>, timeoutMs: Long): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> = isRoot()

            override fun getDescription(): String = "wait for view: $viewMatcher"

            override fun perform(uiController: androidx.test.espresso.UiController, view: View) {
                val startTime = SystemClock.elapsedRealtime()
                val endTime = startTime + timeoutMs
                do {
                    val matched = TreeIterables.breadthFirstViewTraversal(view)
                        .any { viewMatcher.matches(it) && it.isShown }
                    if (matched) return
                    uiController.loopMainThreadForAtLeast(50)
                } while (SystemClock.elapsedRealtime() < endTime)

                throw PerformException.Builder()
                    .withActionDescription(description)
                    .withViewDescription("View not found within $timeoutMs ms")
                    .build()
            }
        }
    }
}
