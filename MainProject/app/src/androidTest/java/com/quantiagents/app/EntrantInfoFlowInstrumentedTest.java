package com.quantiagents.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;

import static org.hamcrest.Matchers.allOf;

import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.quantiagents.app.R;
import com.quantiagents.app.ui.entrantinfo.EntrantInfoHostActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class EntrantInfoFlowInstrumentedTest {

    @Rule
    public ActivityScenarioRule<EntrantInfoHostActivity> rule =
            new ActivityScenarioRule<>(
                    new Intent(
                            ApplicationProvider.getApplicationContext(),
                            EntrantInfoHostActivity.class
                    ).putExtra(EntrantInfoHostActivity.EXTRA_EVENT_ID, "demo-event-1")
            );

    @Test
    public void renders_basicChrome() {
        onView(withId(R.id.btnRedrawCanceled)).check(matches(isDisplayed()));
        onView(withId(R.id.tabs)).check(matches(isDisplayed()));
        onView(withId(R.id.pager)).check(matches(isDisplayed()));
        onView(withId(R.id.swipe)).check(matches(isDisplayed()));
        onView(withId(R.id.list)).check(matches(isDisplayed()));
    }

    @Test
    public void can_switch_tabs() {
        onView(allOf(withText("Selected"),  isDescendantOfA(withId(R.id.tabs)))).perform(click());
        onView(allOf(withText("Confirmed"), isDescendantOfA(withId(R.id.tabs)))).perform(click());
        onView(allOf(withText("Canceled"),  isDescendantOfA(withId(R.id.tabs)))).perform(click());
        onView(allOf(withText("Waiting"),   isDescendantOfA(withId(R.id.tabs)))).perform(click());
    }

    @Test
    public void can_type_and_click_buttons_without_crash() {
        onView(withId(R.id.btnRedrawCanceled)).perform(click());
        onView(withId(R.id.pager)).check(matches(isDisplayed()));
    }
}
