package com.quantiagents.app.modeltests;

import com.quantiagents.app.domain.models.WaitingListEntry;
import org.junit.Test;

import java.time.Instant;

import static org.junit.Assert.*;

/**
 * Tests for WaitingListEntry model.
 */
public final class WaitingListEntryTest {

    @Test
    public void constructorAndGettersWork() {
        Instant t = Instant.now();
        WaitingListEntry w = new WaitingListEntry("e1", "u1", t);
        assertEquals("e1", w.getEventId());
        assertEquals("u1", w.getUserId());
        assertEquals(t, w.getJoinedAt());
    }

    @Test
    public void settersUpdateValues() {
        WaitingListEntry w = new WaitingListEntry();
        Instant t = Instant.now();

        w.setEventId("e2");
        w.setUserId("u2");
        w.setJoinedAt(t);

        assertEquals("e2", w.getEventId());
        assertEquals("u2", w.getUserId());
        assertEquals(t, w.getJoinedAt());
    }
}

