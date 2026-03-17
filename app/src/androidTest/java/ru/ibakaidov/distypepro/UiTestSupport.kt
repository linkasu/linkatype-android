package ru.ibakaidov.distypepro

import android.app.Activity
import android.graphics.Rect
import android.os.SystemClock
import android.view.View
import android.widget.AutoCompleteTextView
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.util.TreeIterables
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import org.hamcrest.Matcher

object UiTestSupport {

    fun loginAndWaitForMainScreen(timeoutMs: Long = 30_000) {
        val args = InstrumentationRegistry.getArguments()
        val email = args.getString("email") ?: error("Missing instrumentation arg: email")
        val password = args.getString("password") ?: error("Missing instrumentation arg: password")

        waitForViewOnScreen(withId(R.id.emailInput), 10_000)
        onView(withId(R.id.emailInput))
            .perform(scrollTo(), replaceText(email), closeSoftKeyboard())
        waitForViewOnScreen(withId(R.id.passwordInput), 10_000)
        onView(withId(R.id.passwordInput))
            .perform(scrollTo(), replaceText(password), closeSoftKeyboard())
        onView(withId(R.id.authPrimaryButton))
            .perform(scrollTo(), click())

        waitForAuthResult(timeoutMs)
    }

    fun waitForAuthResult(timeoutMs: Long) {
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

    fun waitForViewOnScreen(viewMatcher: Matcher<View>, timeoutMs: Long) {
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

    fun waitForViewToDisappear(viewMatcher: Matcher<View>, timeoutMs: Long) {
        val startTime = SystemClock.elapsedRealtime()
        val endTime = startTime + timeoutMs
        do {
            val activity = getResumedActivity()
            val rootView = activity?.window?.decorView
            if (rootView != null) {
                val matched = TreeIterables.breadthFirstViewTraversal(rootView)
                    .any { viewMatcher.matches(it) && it.isShown }
                if (!matched) return
            }
            SystemClock.sleep(50)
        } while (SystemClock.elapsedRealtime() < endTime)

        throw AssertionError("View still shown after $timeoutMs ms: $viewMatcher")
    }

    fun replaceDialogPrompt(text: String) {
        onView(withId(R.id.input_prompt))
            .inRoot(isDialog())
            .perform(replaceText(text), closeSoftKeyboard())
    }

    fun selectExposedDropdownOption(@IdRes viewId: Int, optionText: String) {
        onView(withId(viewId)).perform(scrollTo(), click())
        val activity = getResumedActivity() ?: error("No resumed activity for dropdown selection")
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val dropdown = activity.findViewById<AutoCompleteTextView>(viewId)
                ?: error("Dropdown view not found: $viewId")
            val adapter = dropdown.adapter ?: error("Dropdown adapter not found: $viewId")
            val position = (0 until adapter.count)
                .firstOrNull { adapter.getItem(it)?.toString() == optionText }
                ?: error("Dropdown option not found: $optionText")
            dropdown.setText(optionText, false)
            dropdown.onItemClickListener?.onItemClick(null, null, position, adapter.getItemId(position))
        }
    }

    fun clickToolbarAction(toolbarId: Int, @IdRes actionId: Int, @StringRes titleRes: Int) {
        if (isViewVisible(withId(actionId))) {
            onView(withId(actionId)).perform(click())
            return
        }
        openToolbarOverflow(toolbarId)
        onView(withText(titleRes)).perform(click())
    }

    fun assertToolbarActionAvailable(toolbarId: Int, @IdRes actionId: Int, @StringRes titleRes: Int) {
        if (isViewVisible(withId(actionId))) {
            onView(withId(actionId)).check(matches(isDisplayed()))
            return
        }
        openToolbarOverflow(toolbarId)
        onView(withText(titleRes)).check(matches(isDisplayed()))
        pressBack()
    }

    fun openToolbarOverflow(toolbarId: Int) {
        val overflowDescription = androidx.appcompat.R.string.abc_action_menu_overflow_description
        val overflowMatcher = org.hamcrest.Matchers.allOf(
            withContentDescription(overflowDescription),
            isDescendantOfA(withId(toolbarId))
        )
        if (isViewVisible(overflowMatcher)) {
            onView(overflowMatcher).perform(click())
            return
        }

        val activity = getResumedActivity() ?: error("No resumed activity for toolbar overflow")
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val toolbar = activity.findViewById<Toolbar>(toolbarId)
                ?: error("Toolbar view not found: $toolbarId")
            check(toolbar.showOverflowMenu()) { "Overflow menu is not available for toolbar: $toolbarId" }
        }
    }

    private fun isViewVisible(viewMatcher: Matcher<View>): Boolean {
        val activity = getResumedActivity()
        val rootView = activity?.window?.decorView ?: return false
        val visibleBounds = Rect()
        return TreeIterables.breadthFirstViewTraversal(rootView)
            .any { viewMatcher.matches(it) && it.isShown && it.getGlobalVisibleRect(visibleBounds) }
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
}
