package com.quantiagents.app;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.quantiagents.app.Repository.EventRepository;
import com.quantiagents.app.models.Event;

import com.quantiagents.app.Services.EventService;
import com.quantiagents.app.Repository.EventRepository;
import com.quantiagents.app.models.Event;


import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Basic EventService tests using Mockito.
 * We inject a mocked EventRepository to isolate service logic.
 */
public class EventServiceTest {

    /** Reflection helper to set a private final field (for test-only injection). */
    static void forceSet(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            Field mods = Field.class.getDeclaredField("modifiers");
            mods.setAccessible(true);
            mods.setInt(f, f.getModifiers() & ~Modifier.FINAL);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Small box to capture async callback values. */
    static class Box<T> { T v; }

    // In EventServiceTest.java, replace mk(...) with this:
    private static Event mk(String id, String title) {
        // third arg is posterImageId; use any placeholder like "p"
        return new Event(id, title, "p");
    }


    /** Ensures saveEvent fails when event == null. */
    @Test
    public void saveEvent_null_fails() {
        EventService svc = new EventService(null);
        Box<Throwable> fail = new Box<>();

        svc.saveEvent(null, v -> fail("not expected"), e -> fail.v = e);

        assertNotNull(fail.v);
        assertTrue(fail.v.getMessage().toLowerCase().contains("event"));
    }

    /** Ensures saveEvent fails when title is blank. */
    @Test
    public void saveEvent_blankTitle_fails() {
        EventService svc = new EventService(null);
        Box<Throwable> fail = new Box<>();
        svc.saveEvent(mk("E1", "   "), v -> fail("not expected"), e -> fail.v = e);
        assertNotNull(fail.v);
        assertTrue(fail.v.getMessage().toLowerCase().contains("title"));
    }

    /** Ensures saveEvent calls success for a valid event. */
    @Test
    public void saveEvent_valid_callsSuccess() {
        EventService svc = new EventService(null);
        EventRepository repo = mock(EventRepository.class);
        // When service calls repo.saveEvent(event, ok, err), immediately trigger ok.onSuccess(null)
        doAnswer(inv -> { inv.<OnSuccessListener<Void>>getArgument(1).onSuccess(null); return null; })
                .when(repo).saveEvent(any(Event.class), any(), any());
        forceSet(svc, "repository", repo);

        Box<Boolean> ok = new Box<>(); ok.v = false;
        svc.saveEvent(mk("E2", "Hackathon"), v -> ok.v = true, e -> fail("not expected"));
        assertTrue(ok.v);
        verify(repo).saveEvent(any(Event.class), any(), any());
    }

    /** Ensures updateEvent fails when title is blank. */
    @Test
    public void updateEvent_blankTitle_fails() {
        EventService svc = new EventService(null);
        Box<Throwable> fail = new Box<>();
        svc.updateEvent(mk("E3", ""), v -> fail("not expected"), e -> fail.v = e);
        assertNotNull(fail.v);
        assertTrue(fail.v.getMessage().toLowerCase().contains("title"));
    }

    /** Ensures updateEvent calls success for valid input. */
    @Test
    public void updateEvent_valid_callsSuccess() {
        EventService svc = new EventService(null);
        EventRepository repo = mock(EventRepository.class);
        doAnswer(inv -> { inv.<OnSuccessListener<Void>>getArgument(1).onSuccess(null); return null; })
                .when(repo).updateEvent(any(Event.class), any(), any());
        forceSet(svc, "repository", repo);

        Box<Boolean> ok = new Box<>(); ok.v = false;
        svc.updateEvent(mk("E4", "Tech Talk"), v -> ok.v = true, e -> fail("not expected"));
        assertTrue(ok.v);
        verify(repo).updateEvent(any(Event.class), any(), any());
    }

    /** Ensures getEventById returns what the repo returns. */
    @Test
    public void getEventById_delegates() {
        EventService svc = new EventService(null);
        EventRepository repo = mock(EventRepository.class);
        when(repo.getEventById("E5")).thenReturn(mk("E5", "Career Fair"));
        forceSet(svc, "repository", repo);

        Event got = svc.getEventById("E5");
        assertNotNull(got);
        assertEquals("Career Fair", got.getTitle());
        verify(repo).getEventById("E5");
    }

    /** Ensures getAllEvents returns the repo list. */
    @Test
    public void getAllEvents_delegates() {
        EventService svc = new EventService(null);
        EventRepository repo = mock(EventRepository.class);
        when(repo.getAllEvents()).thenReturn(List.of(mk("A","A"), mk("B","B")));
        forceSet(svc, "repository", repo);

        List<Event> list = svc.getAllEvents();
        assertEquals(2, list.size());
        verify(repo).getAllEvents();
    }

    /** Ensures async delete calls success. */
    @Test
    public void delete_async_callsSuccess() {
        EventService svc = new EventService(null);
        EventRepository repo = mock(EventRepository.class);
        doAnswer(inv -> { inv.<OnSuccessListener<Void>>getArgument(1).onSuccess(null); return null; })
                .when(repo).deleteEventById(eq("E6"), any(), any());
        forceSet(svc, "repository", repo);

        Box<Boolean> ok = new Box<>(); ok.v = false;
        svc.deleteEvent("E6", v -> ok.v = true, e -> fail("not expected"));
        assertTrue(ok.v);
        verify(repo).deleteEventById(eq("E6"), any(), any());
    }

    /** Ensures boolean delete returns repo boolean. */
    @Test
    public void delete_boolean_returnsRepoValue() {
        EventService svc = new EventService(null);
        EventRepository repo = mock(EventRepository.class);
        forceSet(svc, "repository", repo);

        when(repo.deleteEventById("E7")).thenReturn(true);
        assertTrue(svc.deleteEvent("E7"));
        when(repo.deleteEventById("E7")).thenReturn(false);
        assertFalse(svc.deleteEvent("E7"));
        verify(repo, times(2)).deleteEventById("E7");
    }

}
