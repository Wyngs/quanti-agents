package com.quantiagents.app.controllertests;

import com.quantiagents.app.controller.JoinWaitlistController;

import org.junit.Before;
import org.junit.Test;

import java.time.Instant;

import static org.junit.Assert.*;

/**
 * Tests for JoinWaitlistController using FakeGateway.
 */
public final class JoinWaitlistControllerTest {

    private FakeGateway gateway;
    private JoinWaitlistController controller;
    private final Instant now = Instant.now();
    private static final String EVENT = "e1";
    private static final String USER  = "u1";

    @Before
    public void setUp() {
        gateway = new FakeGateway();
        controller = new JoinWaitlistController(gateway);
    }

    @Test
    public void joinWaitlist_invalidInputs_returnsFalse() {
        assertFalse(controller.joinWaitlist(null, USER, now));
        assertFalse(controller.joinWaitlist("   ", USER, now));
        assertFalse(controller.joinWaitlist(EVENT, null, now));
        assertFalse(controller.joinWaitlist(EVENT, " ", now));
        assertFalse(controller.joinWaitlist(EVENT, USER, null));
    }

    @Test
    public void joinWaitlist_closedWindow_returnsFalse() {
        gateway.open = false; // registration closed
        assertFalse(controller.joinWaitlist(EVENT, USER, now));
    }

    @Test
    public void joinWaitlist_happyPath_addsAndReturnsTrue() {
        assertTrue(controller.joinWaitlist(EVENT, USER, now));
        assertTrue(gateway.isUserOnWaitlist(EVENT, USER));
        assertEquals(1, gateway.getWaitlistCount(EVENT));
    }

    @Test
    public void joinWaitlist_idempotent_whenAlreadyOnList_returnsTrue() {
        assertTrue(controller.joinWaitlist(EVENT, USER, now));
        assertTrue(controller.joinWaitlist(EVENT, USER, now)); // still true
        assertEquals(1, gateway.getWaitlistCount(EVENT));       // no dupes
    }

    @Test
    public void leaveWaitlist_invalidInputs_returnsFalse() {
        assertFalse(controller.leaveWaitlist(null, USER));
        assertFalse(controller.leaveWaitlist(" ", USER));
        assertFalse(controller.leaveWaitlist(EVENT, null));
        assertFalse(controller.leaveWaitlist(EVENT, " "));
    }

    @Test
    public void leaveWaitlist_removesEntryAndReturnsTrue() {
        controller.joinWaitlist(EVENT, USER, now);
        assertTrue(controller.leaveWaitlist(EVENT, USER));
        assertFalse(gateway.isUserOnWaitlist(EVENT, USER));
        assertEquals(0, gateway.getWaitlistCount(EVENT));
    }

    @Test
    public void leaveWaitlist_whenNotOnList_returnsFalse() {
        assertFalse(controller.leaveWaitlist(EVENT, USER));
    }
}
