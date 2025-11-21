package com.quantiagents.app;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.quantiagents.app.Constants.constant;
import com.quantiagents.app.Services.EventService;
import com.quantiagents.app.Services.RegistrationHistoryService;
import com.quantiagents.app.Services.ServiceLocator;
import com.quantiagents.app.Services.UserService;
import com.quantiagents.app.models.Event;
import com.quantiagents.app.models.RegistrationHistory;
import com.quantiagents.app.models.User;
import com.quantiagents.app.ui.main.MainActivity;
import com.quantiagents.app.ui.myevents.MyEventFragment;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * UI Integration Test for MyEventFragment.
 * <p>
 * This test walks through the four tabs (Waiting, Selected, Confirmed, Past)
 * verifying that the UI correctly filters and displays registrations based on their status.
 * It mocks the underlying services to provide immediate, deterministic data.
 * <p>
 * <b>Note:</b> Uses manual instrumentation (ActivityScenario + View traversal) instead of Espresso
 * to avoid InputManager compatibility issues on newer Android APIs (31+) with older test dependencies.
 */
@RunWith(AndroidJUnit4.class)
public class MyEventFlowInstrumentedTest {

    private App app;
    private MockUserService mockUserService;
    private MockEventService mockEventService;
    private MockRegistrationHistoryService mockRegService;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        app = (App) context;

        // 1. Create Mocks
        mockUserService = new MockUserService(context);
        mockEventService = new MockEventService(context);
        mockRegService = new MockRegistrationHistoryService(context);

        // 2. Prepare Service Locator with Mocks
        ServiceLocator testLocator = new ServiceLocator(context);
        testLocator.replaceUserService(mockUserService);
        testLocator.replaceEventService(mockEventService);
        testLocator.replaceRegistrationHistoryService(mockRegService);

        // 3. Inject into App
        app.setTestLocator(testLocator);

        // 4. Seed Data
        seedTestData();
    }

    @After
    public void tearDown() {
        app.setTestLocator(null); // Reset to real services
    }

    /**
     * Seeds the mock services with 4 events and 4 registrations, one for each status type.
     */
    private void seedTestData() {
        // User
        User user = new User("test_user", "device_123", "Test Entrant", "test@example.com", "555-5555", "hash");
        mockUserService.setActiveUser(user);

        // Create 4 Events
        createEventAndReg("evt_wait", "Waiting Event", constant.EventRegistrationStatus.WAITLIST, user.getUserId());
        createEventAndReg("evt_sel", "Selected Event", constant.EventRegistrationStatus.SELECTED, user.getUserId());
        createEventAndReg("evt_conf", "Confirmed Event", constant.EventRegistrationStatus.CONFIRMED, user.getUserId());
        createEventAndReg("evt_canc", "Past Event", constant.EventRegistrationStatus.CANCELLED, user.getUserId());
    }

    private void createEventAndReg(String id, String title, constant.EventRegistrationStatus status, String userId) {
        // Event
        Event e = new Event();
        e.setEventId(id);
        e.setTitle(title);
        e.setDescription("Description for " + title);
        e.setEventStartDate(new Date());
        e.setEventCapacity(100);
        e.setOrganizerId("org_1");
        e.setStatus(constant.EventStatus.OPEN);
        mockEventService.addEvent(e);

        // Registration
        RegistrationHistory reg = new RegistrationHistory(id, userId, status, new Date());
        mockRegService.addHistory(reg);
    }

    @Test
    public void testTabNavigationAndContentDisplay() {
        // Launch MainActivity. The mocked UserService will ensure we bypass login and land in the app.
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {

            // Manually swap the displayed fragment to MyEventFragment for testing
            scenario.onActivity(activity -> {
                activity.getSupportFragmentManager().beginTransaction()
                        .replace(R.id.content_container, MyEventFragment.newInstance())
                        .commitNow();
            });

            // ----------------------------------------------------------------
            // 1. Verify 'Waiting' Tab (Default)
            // ----------------------------------------------------------------
            // Expectation: "Waiting Event" is visible
            waitForTextInRecycler(scenario, R.id.my_events_recycler, "Waiting Event", 3000);
            waitForTextInRecycler(scenario, R.id.my_events_recycler, "Leave Waiting List", 1000);

            // ----------------------------------------------------------------
            // 2. Verify 'Selected' Tab
            // ----------------------------------------------------------------
            // Click tab manually on UI thread
            scenario.onActivity(activity -> {
                activity.findViewById(R.id.tab_selected).performClick();
            });

            // Wait and verify "Selected Event" and "Accept" button
            waitForTextInRecycler(scenario, R.id.my_events_recycler, "Selected Event", 3000);
            waitForTextInRecycler(scenario, R.id.my_events_recycler, "Accept", 1000);

            // Verify Waiting event is NO LONGER visible in this list
            verifyTextNotInRecycler(scenario, R.id.my_events_recycler, "Waiting Event");

            // ----------------------------------------------------------------
            // 3. Verify 'Confirmed' Tab
            // ----------------------------------------------------------------
            scenario.onActivity(activity -> {
                activity.findViewById(R.id.tab_confirmed).performClick();
            });

            waitForTextInRecycler(scenario, R.id.my_events_recycler, "Confirmed Event", 3000);
            waitForTextInRecycler(scenario, R.id.my_events_recycler, "Confirmed", 1000); // Status chip text

            // ----------------------------------------------------------------
            // 4. Verify 'Past' (Cancelled) Tab
            // ----------------------------------------------------------------
            scenario.onActivity(activity -> {
                activity.findViewById(R.id.tab_past).performClick();
            });

            waitForTextInRecycler(scenario, R.id.my_events_recycler, "Past Event", 3000);

            // ----------------------------------------------------------------
            // 5. Verify Empty State Logic
            // ----------------------------------------------------------------
            // Clear data
            mockRegService.clear();

            // Trigger a reload by refreshing the fragment manually
            scenario.onActivity(activity -> {
                MyEventFragment fragment = (MyEventFragment) activity.getSupportFragmentManager()
                        .findFragmentById(R.id.content_container);
                if (fragment != null) {
                    fragment.refreshData();
                }
            });

            waitForViewVisibility(scenario, R.id.my_events_empty, View.VISIBLE, 3000);
            waitForTextInView(scenario, R.id.my_events_empty, "Nothing here yet", 1000); // Check R.string.my_events_empty_generic value
        }
    }

    // --- Helper Methods for Manual Instrumentation ---

    private void waitForTextInRecycler(ActivityScenario<MainActivity> scenario, int recyclerId, String text, long timeout) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeout) {
            AtomicBoolean found = new AtomicBoolean(false);
            scenario.onActivity(activity -> {
                RecyclerView rv = activity.findViewById(recyclerId);
                if (rv != null && rv.getVisibility() == View.VISIBLE) {
                    found.set(recursiveFindText(rv, text));
                }
            });
            if (found.get()) return;
            SystemClock.sleep(200);
        }
        fail("Timed out waiting for text '" + text + "' in recycler");
    }

    private void verifyTextNotInRecycler(ActivityScenario<MainActivity> scenario, int recyclerId, String text) {
        AtomicBoolean found = new AtomicBoolean(false);
        scenario.onActivity(activity -> {
            RecyclerView rv = activity.findViewById(recyclerId);
            if (rv != null) {
                found.set(recursiveFindText(rv, text));
            }
        });
        if (found.get()) {
            fail("Text '" + text + "' should NOT be visible");
        }
    }

    private void waitForViewVisibility(ActivityScenario<MainActivity> scenario, int viewId, int visibility, long timeout) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeout) {
            AtomicBoolean match = new AtomicBoolean(false);
            scenario.onActivity(activity -> {
                View v = activity.findViewById(viewId);
                if (v != null && v.getVisibility() == visibility) {
                    match.set(true);
                }
            });
            if (match.get()) return;
            SystemClock.sleep(200);
        }
        fail("Timed out waiting for view visibility");
    }

    private void waitForTextInView(ActivityScenario<MainActivity> scenario, int viewId, String text, long timeout) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeout) {
            AtomicBoolean match = new AtomicBoolean(false);
            scenario.onActivity(activity -> {
                View v = activity.findViewById(viewId);
                if (recursiveFindText(v, text)) {
                    match.set(true);
                }
            });
            if (match.get()) return;
            SystemClock.sleep(200);
        }
        fail("Timed out waiting for text in view");
    }

    private boolean recursiveFindText(View v, String text) {
        if (v == null) return false;
        if (v.getVisibility() != View.VISIBLE) return false;

        if (v instanceof TextView) {
            CharSequence txt = ((TextView) v).getText();
            if (txt != null && txt.toString().contains(text)) return true;
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                if (recursiveFindText(g.getChildAt(i), text)) return true;
            }
        }
        return false;
    }

    // ============================================================================================
    // MOCK SERVICES
    // ============================================================================================

    static class MockUserService extends UserService {
        private User activeUser;

        public MockUserService(Context context) { super(context); }

        public void setActiveUser(User u) { this.activeUser = u; }

        @Override
        public void getCurrentUser(OnSuccessListener<User> onSuccess, OnFailureListener onFailure) {
            if (activeUser != null) onSuccess.onSuccess(activeUser);
            else onFailure.onFailure(new Exception("No user"));
        }

        @Override
        public User getUserById(String userId) {
            // For organizer name lookup
            return new User(userId, "dev", "Organizer Name", "email", "phone", "hash");
        }
    }

    static class MockEventService extends EventService {
        private final List<Event> db = new ArrayList<>();

        public MockEventService(Context context) { super(context); }

        public void addEvent(Event e) { db.add(e); }

        @Override
        public Event getEventById(String eventId) {
            for (Event e : db) {
                if (e.getEventId().equals(eventId)) return e;
            }
            return null;
        }
    }

    static class MockRegistrationHistoryService extends RegistrationHistoryService {
        private final List<RegistrationHistory> db = new ArrayList<>();

        public MockRegistrationHistoryService(Context context) { super(context); }

        public void addHistory(RegistrationHistory h) { db.add(h); }

        public void clear() { db.clear(); }

        @Override
        public List<RegistrationHistory> getRegistrationHistoriesByUserId(String userId) {
            List<RegistrationHistory> result = new ArrayList<>();
            for (RegistrationHistory h : db) {
                if (h.getUserId().equals(userId)) result.add(h);
            }
            return result;
        }
    }
}