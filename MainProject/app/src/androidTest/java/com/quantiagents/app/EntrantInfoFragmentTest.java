package com.quantiagents.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.quantiagents.app.ui.entrantinfo.EntrantInfoFragment;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented UI smoke tests for EntrantInfoFragment.
 *
 * What we verify:
 *  - The TabLayout and action buttons render (basic visibility).
 *  - Entering a number and clicking "Draw" is possible (no crashes).
 *
 * Note: These tests avoid asserting backend effects. For deeper checks, inject a
 * fake service via a custom TestApplication or ServiceLocator and assert callbacks.
 */
@RunWith(AndroidJUnit4.class)
public class EntrantInfoFragmentTest {

    @Test
    public void tabsRender_andDrawButtonVisible() {
        // Launch the fragment inside a container Activity with a theme.
        FragmentScenario.launchInContainer(
                EntrantInfoFragment.class,
                EntrantInfoFragment.newInstance("evt_demo").getArguments(),
                R.style.Theme_Material3_DayNight_NoActionBar,
                null
        );

        // Simple visibility checks
        onView(withId(R.id.tabs)).check(matches(isDisplayed()));
        onView(withId(R.id.btnDraw)).check(matches(isDisplayed()));
        onView(withId(R.id.btnRedrawCanceled)).check(matches(isDisplayed()));
    }

    @Test
    public void enteringNumber_thenDraw_isClickable() {
        FragmentScenario.launchInContainer(
                EntrantInfoFragment.class,
                EntrantInfoFragment.newInstance("evt_demo").getArguments(),
                R.style.Theme_Material3_DayNight_NoActionBar,
                null
        );

        // Type "2" in the N input and click Draw. If anything is miswired, this would fail.
        onView(withId(R.id.inputCount)).perform(replaceText("2"), closeSoftKeyboard());
        onView(withId(R.id.btnDraw)).perform(click());

        // For a real state assertion, swap the service with a fake and verify the toast/callback.
    }
}
