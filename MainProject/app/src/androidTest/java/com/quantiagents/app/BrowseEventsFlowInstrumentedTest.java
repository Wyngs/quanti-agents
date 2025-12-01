package com.quantiagents.app;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import com.quantiagents.app.Services.UserService;
import com.quantiagents.app.models.User;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.quantiagents.app.Services.EventService;
import com.quantiagents.app.Services.ServiceLocator;
import com.quantiagents.app.models.Event;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class BrowseEventsFlowInstrumentedTest {

    private EventService eventService;
    private final List<String> createdEventIds = new ArrayList<>();

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        ServiceLocator locator = new ServiceLocator(context);
        eventService = locator.eventService();
    }

    @After
    public void tearDown() {
        for (String id : createdEventIds) {
            CountDownLatch latch = new CountDownLatch(1);
            eventService.deleteEvent(id,
                    aVoid -> latch.countDown(),
                    e -> latch.countDown());
            awaitLatch(latch);
        }
        createdEventIds.clear();
    }

    @Test
    public void savedEventsAppearInAllEventsList() {
        Event e1 = new Event();
        e1.setTitle("Browse Event One");
        Event e2 = new Event();
        e2.setTitle("Browse Event Two");

        createEventSync(e1);
        createEventSync(e2);

        List<Event> all = eventService.getAllEvents();
        assertNotNull(all);
        boolean found1 = false;
        boolean found2 = false;
        for (Event e : all) {
            if ("Browse Event One".equals(e.getTitle())) {
                found1 = true;
            }
            if ("Browse Event Two".equals(e.getTitle())) {
                found2 = true;
            }
        }
        assertTrue(found1);
        assertTrue(found2);
    }

    private void createEventSync(Event event) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> idRef = new AtomicReference<>();
        eventService.saveEvent(event,
                id -> {
                    idRef.set(id);
                    latch.countDown();
                },
                e -> latch.countDown());
        awaitLatch(latch);
        String id = idRef.get();
        if (id != null) {
            event.setEventId(id);
            createdEventIds.add(id);
        }
    }

    private void awaitLatch(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }
    }
}
