package com.quantiagents.app.eventTests;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.os.Bundle;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.quantiagents.app.R;
import com.quantiagents.app.ui.myevents.MyEventFragment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Verifies: loads data, tab counts update, switching tabs refreshes list.
 * We inject fakes directly into the fragment via reflection to avoid the app ServiceLocator.
 */
@RunWith(AndroidJUnit4.class)
public class MyEventFragmentTest {

    @Before
    public void setup() {
        // Build fake graph in-memory
        TestDataSetup.seedData(); // keeps your existing fakes ready to reuse
    }

    @Test
    public void loadsCountsAndSwapsOnTab() throws Exception {

        FragmentScenario<MyEventFragment> scenario =
                FragmentScenario.launchInContainer(MyEventFragment.class, new Bundle(), R.style.Theme_QATest);

        scenario.onFragment(f -> {
            try {
                // Inject fakes directly: userService, regService, eventService
                Field userF = f.getClass().getDeclaredField("userService");
                Field regF  = f.getClass().getDeclaredField("regService");
                Field evtF  = f.getClass().getDeclaredField("eventService");
                userF.setAccessible(true);
                regF.setAccessible(true);
                evtF.setAccessible(true);
                userF.set(f, new TestDataSetup.FakeUserService());
                regF.set(f,  new TestDataSetup.FakeRegService());
                evtF.set(f,  new TestDataSetup.FakeEventService());

                // Call the private reload method so UI uses the fakes
                Method reload = f.getClass().getDeclaredMethod("reloadDataForCurrentTab", boolean.class);
                reload.setAccessible(true);
                reload.invoke(f, true);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        });

        // Waiting tab default. Expect "Waiting (2)"
        onView(withId(R.id.tab_waiting)).check(matches(withText("Waiting (2)")));
        onView(withId(R.id.tab_selected)).check(matches(withText("Selected (1)")));
        onView(withId(R.id.tab_confirmed)).check(matches(withText("Confirmed (1)")));
        onView(withId(R.id.tab_past)).check(matches(withText("Past (2)"))); // cancelled + closed

        // Switch to Selected
        onView(withId(R.id.tab_selected)).perform(click());
        onView(withId(R.id.tab_selected)).check(matches(withText("Selected (1)")));

        // Switch to Confirmed
        onView(withId(R.id.tab_confirmed)).perform(click());
        onView(withId(R.id.tab_confirmed)).check(matches(withText("Confirmed (1)")));

        // Switch to Past
        onView(withId(R.id.tab_past)).perform(click());
        onView(withId(R.id.tab_past)).check(matches(withText("Past (2)")));
    }
}
