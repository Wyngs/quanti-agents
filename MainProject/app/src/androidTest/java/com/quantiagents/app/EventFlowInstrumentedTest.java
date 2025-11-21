package com.quantiagents.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.quantiagents.app.Constants.constant;
import com.quantiagents.app.Services.EventService;
import com.quantiagents.app.Services.ServiceLocator;
import com.quantiagents.app.models.Event;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Covers the lifecycle of an Event: Create, Read, Update, Delete.
 * Also tests validation logic and bulk retrieval.
 */
@RunWith(AndroidJUnit4.class)
public class EventFlowInstrumentedTest {

    private Context context;
    private ServiceLocator locator;
    private EventService eventService;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        locator = new ServiceLocator(context);
        eventService = locator.eventService();
        // Clean up potentially messy state before starting
        wipeEvents();
    }

    @After
    public void tearDown() {
        wipeEvents();
    }

    /**
     * Verifies that an event can be saved and retrieved by ID.
     */
    @Test
    public void createAndRetrieveEvent() {
        Event newEvent = new Event();
        newEvent.setTitle("Test Event");
        newEvent.setDescription("Unit Test Description");
        newEvent.setEventCapacity(100);
        newEvent.setCost(50.0);
        newEvent.setOrganizerId("test_organizer");

        Event saved = createEventSync(newEvent);
        assertNotNull("Event ID should be generated", saved.getEventId());

        Event fetched = eventService.getEventById(saved.getEventId());
        assertNotNull("Should retrieve event from DB", fetched);
        assertEquals("Test Event", fetched.getTitle());
        assertEquals(100, fetched.getEventCapacity(), 0.0);
    }

    /**
     * Verifies that updateEvent successfully persists changes to the database.
     */
    @Test
    public void updateEventDetails() {
        Event event = new Event();
        event.setTitle("Original Title");
        event = createEventSync(event);

        event.setTitle("Updated Title");
        event.setStatus(constant.EventStatus.CLOSED);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> errorRef = new AtomicReference<>();

        eventService.updateEvent(event,
                aVoid -> latch.countDown(),
                e -> {
                    errorRef.set(e);
                    latch.countDown();
                });
        awaitLatch(latch);
        assertNull("Update should not return error", errorRef.get());

        Event fetched = eventService.getEventById(event.getEventId());
        assertEquals("Updated Title", fetched.getTitle());
        assertEquals(constant.EventStatus.CLOSED, fetched.getStatus());
    }

    /**
     * Verifies that deleting an event removes it so it can no longer be retrieved.
     */
    @Test
    public void deleteEventRemovesIt() {
        Event event = new Event();
        event.setTitle("Delete Me");
        event = createEventSync(event);
        String id = event.getEventId();

        CountDownLatch latch = new CountDownLatch(1);
        eventService.deleteEvent(id,
                aVoid -> latch.countDown(),
                e -> latch.countDown());
        awaitLatch(latch);

        assertNull("Event should be null after delete", eventService.getEventById(id));
    }

    /**
     * Verifies that saving an event without a title fails validation.
     */
    @Test
    public void createEventRequiresTitle() {
        Event badEvent = new Event();
        badEvent.setTitle(""); // Empty title

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> errorRef = new AtomicReference<>();

        eventService.saveEvent(badEvent,
                s -> latch.countDown(),
                e -> {
                    errorRef.set(e);
                    latch.countDown();
                });
        awaitLatch(latch);

        assertNotNull("Should return error", errorRef.get());
        assertEquals(IllegalArgumentException.class, errorRef.get().getClass());
    }

    /**
     * Verifies that updating an event to have an empty title fails validation.
     */
    @Test
    public void updateEventRequiresTitle() {
        // 1. Create valid event
        Event event = new Event();
        event.setTitle("Valid Title");
        event = createEventSync(event);

        // 2. Modify to invalid state
        event.setTitle("");

        // 3. Attempt update
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> errorRef = new AtomicReference<>();

        eventService.updateEvent(event,
                aVoid -> latch.countDown(),
                e -> {
                    errorRef.set(e);
                    latch.countDown();
                });
        awaitLatch(latch);

        // 4. Assert failure
        assertNotNull("Update should fail with empty title", errorRef.get());
        assertEquals(IllegalArgumentException.class, errorRef.get().getClass());
    }

    /**
     * Verifies that getAllEvents retrieves all saved events.
     */
    @Test
    public void getAllEventsReturnsList() {
        Event e1 = new Event();
        e1.setTitle("Event One");
        createEventSync(e1);

        Event e2 = new Event();
        e2.setTitle("Event Two");
        createEventSync(e2);

        List<Event> all = eventService.getAllEvents();
        assertNotNull(all);
        assertTrue("Should find at least 2 events", all.size() >= 2);

        boolean found1 = false;
        boolean found2 = false;
        for(Event e : all) {
            if("Event One".equals(e.getTitle())) found1 = true;
            if("Event Two".equals(e.getTitle())) found2 = true;
        }
        assertTrue("Should contain Event One", found1);
        assertTrue("Should contain Event Two", found2);
    }

    /**
     * Verifies that fetching an event with a null or empty ID does not crash
     * and returns null.
     */
    @Test
    public void getEventHandlesInvalidIds() {
        assertNull(eventService.getEventById(null));
        assertNull(eventService.getEventById(""));
        assertNull(eventService.getEventById("   "));
    }

    // --- Helpers ---

    private Event createEventSync(Event event) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> idRef = new AtomicReference<>();
        eventService.saveEvent(event,
                id -> {
                    idRef.set(id);
                    latch.countDown();
                },
                e -> latch.countDown());
        awaitLatch(latch);
        if (idRef.get() != null) {
            event.setEventId(idRef.get());
            return event;
        }
        fail("Failed to create event sync");
        return null;
    }

    private void wipeEvents() {
        List<Event> events = eventService.getAllEvents();
        for (Event e : events) {
            eventService.deleteEvent(e.getEventId());
        }
    }

    private void awaitLatch(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}