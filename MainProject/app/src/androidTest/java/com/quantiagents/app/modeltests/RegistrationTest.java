package com.quantiagents.app.modeltests;

import org.junit.Test;

import static org.junit.Assert.*;

import com.quantiagents.app.domain.Registration;

/**
 * Tests for Registration model.
 */
public final class RegistrationTest {

    @Test
    public void constructorAndGettersWork() {
        Registration r = new Registration("e1", "u1", Registration.Status.SELECTED);
        assertEquals("e1", r.getEventId());
        assertEquals("u1", r.getUserId());
        assertEquals(Registration.Status.SELECTED, r.getStatus());
    }

    @Test
    public void settersUpdateValues() {
        Registration r = new Registration();
        r.setEventId("ex");
        r.setUserId("ux");
        r.setStatus(Registration.Status.PENDING);

        assertEquals("ex", r.getEventId());
        assertEquals("ux", r.getUserId());
        assertEquals(Registration.Status.PENDING, r.getStatus());
    }
}
