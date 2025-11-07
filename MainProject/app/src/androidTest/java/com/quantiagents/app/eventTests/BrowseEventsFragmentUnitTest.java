package com.quantiagents.app.eventTests;

import static org.junit.Assert.*;

import com.quantiagents.app.Constants.constant;
import com.quantiagents.app.models.Event;
import com.quantiagents.app.ui.myevents.BrowseEventsFragment;

import org.junit.Test;

/** Pure JVM test for the reflection-based isOpen() helper. */
public class BrowseEventsFragmentUnitTest {

    @Test
    public void isOpen_true_whenGetterReturnsOPEN() {
        Event e = new TestEvent("e1", "Title", null, constant.EventStatus.OPEN);
        assertTrue(BrowseEventsFragment.isOpen(e));
    }

    @Test
    public void isOpen_false_whenGetterReturnsCLOSED() {
        Event e = new TestEvent("e2", "Title", null, constant.EventStatus.CLOSED);
        assertFalse(BrowseEventsFragment.isOpen(e));
    }

    @Test
    public void isOpen_false_whenNoGetterPresent() {
        Event e = new Event("e3", "Plain Event", null);
        assertFalse(BrowseEventsFragment.isOpen(e));
    }

    // minimal test subclass to expose getStatus() for reflection
    static class TestEvent extends Event {
        private final constant.EventStatus status;
        TestEvent(String id, String title, String poster, constant.EventStatus status) {
            super(id, title, poster);
            this.status = status;
        }
        public constant.EventStatus getStatus() { return status; }
    }
}
