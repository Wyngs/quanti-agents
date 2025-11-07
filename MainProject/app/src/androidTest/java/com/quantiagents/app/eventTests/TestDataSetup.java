package com.quantiagents.app.eventTests;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import com.quantiagents.app.App;
import com.quantiagents.app.Constants.constant;
import com.quantiagents.app.Services.EventService;
import com.quantiagents.app.Services.RegistrationHistoryService;
import com.quantiagents.app.Services.ServiceLocator;
import com.quantiagents.app.Services.UserService;
import com.quantiagents.app.models.Event;
import com.quantiagents.app.models.RegistrationHistory;
import com.quantiagents.app.models.User;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Test-only seed that installs an isolated ServiceLocator with fakes. */
public final class TestDataSetup {

    private TestDataSetup() {}

    public static void seedData() {
        Application app = ApplicationProvider.getApplicationContext();
        if (!(app instanceof App)) return;
        App myApp = (App) app;

        ServiceLocator testLocator = new ServiceLocator(myApp);

        // swap in fakes
        testLocator.replaceUserService(new FakeUserService());
        testLocator.replaceEventService(new FakeEventService());
        testLocator.replaceRegistrationHistoryService(new FakeRegService());

        // tell the app to use this graph
        myApp.setTestLocator(testLocator);
    }

    // ---------- FAKE SERVICES (now pass a non-null Context) ----------

    static class FakeUserService extends UserService {
        private final User user;

        public FakeUserService() {
            super(ApplicationProvider.<Context>getApplicationContext(), null);
            user = new User();
            user.setUserId("u1");
            user.setName("Test User");
        }

        @Override
        public void getCurrentUser(@NonNull com.google.android.gms.tasks.OnSuccessListener<User> success,
                                   @NonNull com.google.android.gms.tasks.OnFailureListener fail) {
            success.onSuccess(user);
        }
    }

    static class FakeEventService extends EventService {
        private final List<Event> events = new ArrayList<>();

        public FakeEventService() {
            super(ApplicationProvider.<Context>getApplicationContext());
            events.add(build("e_wait_1", "A", 10, 20, constant.EventStatus.OPEN));
            events.add(build("e_wait_2", "B", 10, 20, constant.EventStatus.OPEN));
            events.add(build("e_sel_1",  "C", 10, 20, constant.EventStatus.OPEN));
            events.add(build("e_conf_1", "D", 10, 20, constant.EventStatus.OPEN));
            events.add(build("e_cancel_1","E", 10, 20, constant.EventStatus.CLOSED)); // Past
        }

        private Event build(String id, String title, double cost, int cap, constant.EventStatus st) {
            Event e = new Event();
            e.setEventId(id);
            e.setTitle(title);
            e.setCost(cost);
            e.setEventCapacity(cap);
            e.setStatus(st);
            e.setWaitingList(new ArrayList<>());
            e.setSelectedList(new ArrayList<>());
            e.setConfirmedList(new ArrayList<>());
            e.setCancelledList(new ArrayList<>());
            return e;
        }

        @Override public Event getEventById(String eventId) {
            for (Event e : events) if (e.getEventId().equals(eventId)) return e;
            return null;
        }

        @Override
        public void updateEvent(@NonNull Event event,
                                @NonNull com.google.android.gms.tasks.OnSuccessListener<Void> onSuccess,
                                @NonNull com.google.android.gms.tasks.OnFailureListener onFailure) {
            onSuccess.onSuccess(null); // in-memory no-op
        }
    }

    static class FakeRegService extends RegistrationHistoryService {
        private final List<RegistrationHistory> regs = new ArrayList<>();

        public FakeRegService() {
            super(ApplicationProvider.<Context>getApplicationContext());
            regs.add(reg("e_wait_1", "u1", constant.EventRegistrationStatus.WAITLIST));
            regs.add(reg("e_wait_2", "u1", constant.EventRegistrationStatus.WAITLIST));
            regs.add(reg("e_sel_1",  "u1", constant.EventRegistrationStatus.SELECTED));
            regs.add(reg("e_conf_1", "u1", constant.EventRegistrationStatus.CONFIRMED));
            regs.add(reg("e_cancel_1","u1", constant.EventRegistrationStatus.CANCELLED));
        }

        private RegistrationHistory reg(String e, String u, constant.EventRegistrationStatus s) {
            RegistrationHistory r = new RegistrationHistory();
            r.setEventId(e);
            r.setUserId(u);
            r.setEventRegistrationStatus(s);
            r.setRegisteredAt(new Date());
            return r;
        }

        @Override public List<RegistrationHistory> getRegistrationHistoriesByUserId(String userId) { return regs; }

        @Override public RegistrationHistory getRegistrationHistoryByEventIdAndUserId(String eventId, String userId) {
            for (RegistrationHistory r : regs)
                if (r.getEventId().equals(eventId) && r.getUserId().equals(userId)) return r;
            return null;
        }

        @Override
        public void updateRegistrationHistory(@NonNull RegistrationHistory history,
                                              @NonNull com.google.android.gms.tasks.OnSuccessListener<Void> onSuccess,
                                              @NonNull com.google.android.gms.tasks.OnFailureListener onFailure) {
            for (int i = 0; i < regs.size(); i++) {
                RegistrationHistory r = regs.get(i);
                if (r.getEventId().equals(history.getEventId()) && r.getUserId().equals(history.getUserId())) {
                    regs.set(i, history);
                    onSuccess.onSuccess(null);
                    return;
                }
            }
            regs.add(history);
            onSuccess.onSuccess(null);
        }
    }
}
