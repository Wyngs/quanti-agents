package com.quantiagents.app.controllertests;

import com.quantiagents.app.controller.EventController;
import com.quantiagents.app.domain.Event;
import com.quantiagents.app.domain.gateway.PersistenceGateway;
import com.quantiagents.app.domain.services.LotteryService;

import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests for EventController using a concrete FakeGateway.
 */
public final class EventControllerTest {

    private FakeGateway gateway;
    private FakeLotteryService lottery;
    private EventController controller;
    private final Instant now = Instant.now();
    private static final String USER = "u1";

    // Simple fake LotteryService
    private static final class FakeLotteryService implements LotteryService {
        String custom;
        String def;
        @Override public String getSelectionCriteria() { return custom; }
        @Override public String getDefaultCriteria()   { return def;    }
    }

    @Before
    public void setUp() {
        gateway = new FakeGateway();
        lottery = new FakeLotteryService();
        controller = new EventController(gateway, lottery);
    }

    @Test
    public void getJoinableEvents_invalidInput_returnsEmpty() {
        assertTrue(controller.getJoinableEvents(null, now).isEmpty());
        assertTrue(controller.getJoinableEvents("   ", now).isEmpty());
        assertTrue(controller.getJoinableEvents(USER, null).isEmpty());
    }

    @Test
    public void getJoinableEvents_filtersOutWaitlistedAndRegistered() {
        // Arrange open events
        List<Event> open = new ArrayList<>(Arrays.asList(
                new Event("e1", "A", null),
                new Event("e2", "B", null),
                new Event("e3", "C", null)
        ));
        gateway.setOpenEvents(open);

        // USER already on waitlist for e1
        gateway.addToWaitlist("e1", USER);
        // USER already has a registration for e2
        gateway.createOrUpsertRegistration("e2", USER,
                com.quantiagents.app.domain.Registration.Status.PENDING);

        // Act
        List<Event> joinable = controller.getJoinableEvents(USER, now);

        // Assert: only e3 remains
        assertEquals(1, joinable.size());
        assertEquals("e3", joinable.get(0).getEventId());
    }

    @Test
    public void getJoinableEvents_noOpenEvents_returnsEmpty() {
        gateway.setOpenEvents(new ArrayList<>());
        assertTrue(controller.getJoinableEvents(USER, now).isEmpty());
    }

    @Test
    public void getWaitlistCount_passesThroughAndClampsAtLeastZero() {
        assertEquals(0, controller.getWaitlistCount(null));
        assertEquals(0, controller.getWaitlistCount("   "));

        gateway.addToWaitlist("e1", "x");
        gateway.addToWaitlist("e1", "y");
        assertEquals(2, controller.getWaitlistCount("e1"));
    }

    @Test
    public void getLotteryCriteria_returnsCustomWhenPresent_elseDefault_elseEmpty() {
        // custom wins
        lottery.custom = "custom";
        lottery.def = "default";
        assertEquals("custom", controller.getLotteryCriteriaMarkdown());

        // fallback to default
        lottery.custom = " ";
        assertEquals("default", controller.getLotteryCriteriaMarkdown());

        // empty when both blank
        lottery.custom = null;
        lottery.def = null;
        assertEquals("", controller.getLotteryCriteriaMarkdown());
    }
}
