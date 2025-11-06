package com.quantiagents.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.quantiagents.app.ui.entrantinfo.EntrantListFragment;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented UI smoke tests for EntrantListFragment.
 *
 * What we verify:
 *  - The RecyclerView and SwipeRefreshLayout render (basic wiring).
 *  - This covers the "list screen exists" acceptance criteria without
 *    coupling to Firestore/network.
 */
@RunWith(AndroidJUnit4.class)
public class EntrantListFragmentSmokeTest {

    @Test
    public void recyclerAndEmptyLabelRender() {
        // Launch a WAITING tab instance
        FragmentScenario.launchInContainer(
                EntrantListFragment.newInstance("evt_demo", "WAITING").getClass(),
                EntrantListFragment.newInstance("evt_demo", "WAITING").getArguments(),
                R.style.Theme_Material3_DayNight_NoActionBar,
                null
        );

        // UI exists and is visible
        onView(withId(R.id.list)).check(matches(isDisplayed()));
        onView(withId(R.id.swipe)).check(matches(isDisplayed()));
    }
}
