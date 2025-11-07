package com.quantiagents.app.eventTests;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.scrollToPosition;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.quantiagents.app.App;
import com.quantiagents.app.Constants.constant;
import com.quantiagents.app.R;
import com.quantiagents.app.Services.EventService;
import com.quantiagents.app.Services.ServiceLocator;
import com.quantiagents.app.models.Event;
import com.quantiagents.app.ui.myevents.BrowseEventsFragment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/** UI tests for BrowseEventsFragment without depending on any Activity class. */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class BrowseEventsFragmentTest {

    /** Stub service returns predictable data: index 0 OPEN, index 1 CLOSED. */
    static final class StubEventService extends EventService {
        private final List<Event> data;
        public StubEventService(Context ctx, List<Event> data) { super(ctx); this.data = data; }
        @Override public List<Event> getAllEvents() { return data; }
        @Override public Event getEventById(String id) {
            for (Event e : data) if (e != null && id.equals(e.getEventId())) return e;
            return null;
        }
    }

    @Before
    public void setUp() {
        Context ctx = ApplicationProvider.getApplicationContext();
        App app = (App) ctx;
        ServiceLocator locator = app.locator();

        Event open = mkEvent("e1", "Beginner Swimming Lessons", constant.EventStatus.OPEN);
        Event closed = mkEvent("e2", "Interpretive Dance Class", constant.EventStatus.CLOSED);

        // Inject stub into ServiceLocator (you already added setters)
        locator.setEventService(new StubEventService(ctx, Arrays.asList(open, closed)));

        // Launch the fragment directly in a container with a stable theme
        FragmentScenario.launchInContainer(
                BrowseEventsFragment.class,
                null,
                androidx.appcompat.R.style.Theme_AppCompat // safe default theme
        );
    }

    private static @NonNull Event mkEvent(String id, String title, constant.EventStatus status) {
        Event e = new Event();
        e.setEventId(id);
        e.setTitle(title);
        e.setStatus(status);
        e.setEventStartDate(new Date());
        e.setEventEndDate(new Date());
        e.setRegistrationStartDate(new Date());
        e.setRegistrationEndDate(new Date());
        e.setEventCapacity(20);
        return e;
    }

    @Test
    public void showsAllEvents_andClosedLabelVisibility() {
        // Ensure row 1 (the CLOSED event) is bound
        onView(withId(R.id.recycler)).perform(scrollToPosition(1));

        // Assert the "Event closed" label within row 1 only (no ambiguous matcher)
        onView(new RecyclerViewMatcher(R.id.recycler)
                .atPositionOnView(1, R.id.text_closed))
                .check(matches(withText(R.string.event_closed)))
                .check(matches(isDisplayed()));
    }
}
