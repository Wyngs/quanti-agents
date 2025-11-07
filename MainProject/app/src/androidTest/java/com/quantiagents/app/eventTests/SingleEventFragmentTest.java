package com.quantiagents.app.eventTests;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.quantiagents.app.Constants.constant;
import com.quantiagents.app.R;
import com.quantiagents.app.ui.myevents.SingleEventFragment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Renders a SELECTED card and Accept -> becomes Confirmed.
 * Inject fakes directly via reflection to avoid ServiceLocator path.
 */
@RunWith(AndroidJUnit4.class)
public class SingleEventFragmentTest {

    @Before
    public void setup() {
        TestDataSetup.seedData();
    }

    @Test
    public void rendersAndAcceptsSelection() throws Exception {
        Bundle args = new Bundle();
        args.putString("eventId", "e_sel_1");
        args.putString("registrationStatus", constant.EventRegistrationStatus.SELECTED.name());
        args.putBoolean("assignable", true);

        FragmentScenario<SingleEventFragment> scenario =
                FragmentScenario.launchInContainer(SingleEventFragment.class, args, R.style.Theme_QATest);

        scenario.onFragment(f -> {
            try {
                // Inject fakes directly
                Field userF = f.getClass().getDeclaredField("userService");
                Field regF  = f.getClass().getDeclaredField("regService");
                Field evtF  = f.getClass().getDeclaredField("eventService");
                userF.setAccessible(true);
                regF.setAccessible(true);
                evtF.setAccessible(true);
                userF.set(f, new TestDataSetup.FakeUserService());
                regF.set(f,  new TestDataSetup.FakeRegService());
                evtF.set(f,  new TestDataSetup.FakeEventService());

                // Kick the private loadAll() so UI paints from fakes
                Method loadAll = f.getClass().getDeclaredMethod("loadAll");
                loadAll.setAccessible(true);
                loadAll.invoke(f);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        });

        onView(withId(R.id.single_title)).check(matches(withText("C")));
        onView(withId(R.id.single_status_chip)).check(matches(withText("Selected")));

        onView(withId(R.id.single_action_primary)).check(matches(withText("Accept"))).perform(click());

        onView(withId(R.id.single_status_chip)).check(matches(withText("Confirmed")));
    }

    public static SingleEventFragment newInstance(String eventId, @Nullable String registrationStatus,
                                                  boolean assignable) {
        SingleEventFragment f = new SingleEventFragment();
        Bundle b = new Bundle();
        b.putString("eventId", eventId);
        b.putString("registrationStatus", registrationStatus);
        b.putBoolean("assignable", assignable);
        f.setArguments(b);
        return f;
    }
}
