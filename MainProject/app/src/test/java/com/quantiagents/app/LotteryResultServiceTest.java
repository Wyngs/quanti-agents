package com.quantiagents.app;

import com.quantiagents.app.Repository.LotteryResultRepository;
import com.quantiagents.app.Services.LotteryResultService;
import com.quantiagents.app.Services.RegistrationHistoryService;
import com.quantiagents.app.models.LotteryResult;
import com.quantiagents.app.models.RegistrationHistory;
import com.google.android.gms.tasks.OnSuccessListener;
import com.quantiagents.app.Services.EventService;
import com.quantiagents.app.Repository.EventRepository;
import com.quantiagents.app.models.Event;


import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Basic LotteryResultService tests with Mockito.
 * We mock LotteryResultRepository, RegistrationHistoryService, and EventService.
 */
public class LotteryResultServiceTest {

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

    static class Box<T> { T v; }

    private static RegistrationHistory rh(String userId, long when) {
        RegistrationHistory r = new RegistrationHistory();
        r.setUserId(userId);
        r.setRegisteredAt(new Date(when));
        return r;
    }

    /** save: null -> failure */
    @Test
    public void save_null_fails() {
        LotteryResultService svc = new LotteryResultService(null);
        Box<Throwable> fail = new Box<>();
        svc.saveLotteryResult(null, v -> fail("x"), e -> fail.v = e);
        assertNotNull(fail.v);
    }

    /** save: blank eventId -> failure */
    @Test
    public void save_blankEventId_fails() {
        LotteryResultService svc = new LotteryResultService(null);
        LotteryResult bad = new LotteryResult();
        bad.setEventId("   ");
        bad.setEntrantIds(List.of("U1"));
        Box<Throwable> fail = new Box<>();
        svc.saveLotteryResult(bad, v -> fail("x"), e -> fail.v = e);
        assertNotNull(fail.v);
        assertTrue(fail.v.getMessage().toLowerCase().contains("event id"));
    }

    /** save: empty entrants -> failure */
    @Test
    public void save_emptyEntrants_fails() {
        LotteryResultService svc = new LotteryResultService(null);
        LotteryResult bad = new LotteryResult();
        bad.setEventId("E1");
        bad.setEntrantIds(List.of());
        Box<Throwable> fail = new Box<>();
        svc.saveLotteryResult(bad, v -> fail("x"), e -> fail.v = e);
        assertNotNull(fail.v);
        assertTrue(fail.v.getMessage().toLowerCase().contains("entrant"));
    }

    /** drawLottery: count <= 0 -> empty result */
    @Test
    public void drawLottery_zero_returnsEmpty() {
        LotteryResultService svc = new LotteryResultService(null);
        Box<LotteryResult> got = new Box<>();
        svc.drawLottery("E2", 0, r -> got.v = r, e -> fail("x"));
        assertNotNull(got.v);
        assertEquals("E2", got.v.getEventId());
        assertTrue(got.v.getEntrantIds().isEmpty());
    }

    /** drawLottery: empty waiting -> empty result */
    @Test
    public void drawLottery_emptyWaiting_returnsEmpty() {
        LotteryResultService svc = new LotteryResultService(null);

        LotteryResultRepository lotRepo = mock(LotteryResultRepository.class);
        RegistrationHistoryService regSvc = mock(RegistrationHistoryService.class);
        forceSet(svc, "repository", lotRepo);
        forceSet(svc, "registrationHistoryService", regSvc);

        // return empty waiting list
        doAnswer(inv -> { inv.<OnSuccessListener<List<RegistrationHistory>>>getArgument(2).onSuccess(List.of()); return null; })
                .when(regSvc).getByEventAndStatus(eq("E3"), anyString(), any(), any());

        Box<LotteryResult> got = new Box<>();
        svc.drawLottery("E3", 5, r -> got.v = r, e -> fail("x"));
        assertNotNull(got.v);
        assertTrue(got.v.getEntrantIds().isEmpty());
        verify(regSvc).getByEventAndStatus(eq("E3"), anyString(), any(), any());
    }

    /** drawLottery: picks up to 'count' and triggers bulk update + save. */
    @Test
    public void drawLottery_picksUpToCount() {
        LotteryResultService svc = new LotteryResultService(null);

        LotteryResultRepository lotRepo = mock(LotteryResultRepository.class);
        RegistrationHistoryService regSvc = mock(RegistrationHistoryService.class);
        forceSet(svc, "repository", lotRepo);
        forceSet(svc, "registrationHistoryService", regSvc);

        // Waiting list of 3 users
        List<RegistrationHistory> waiting = List.of(rh("U1",1), rh("U2",2), rh("U3",3));
        doAnswer(inv -> { inv.<OnSuccessListener<List<RegistrationHistory>>>getArgument(2).onSuccess(waiting); return null; })
                .when(regSvc).getByEventAndStatus(eq("E4"), anyString(), any(), any());

        // bulkUpdateStatus returns number of ids updated
        doAnswer(inv -> { List<String> ids = inv.getArgument(1);
            inv.<OnSuccessListener<Integer>>getArgument(3).onSuccess(ids.size()); return null; })
                .when(regSvc).bulkUpdateStatus(eq("E4"), anyList(), anyString(), any(), any());

        // saveLotteryResult succeeds
        doAnswer(inv -> { inv.<OnSuccessListener<Void>>getArgument(1).onSuccess(null); return null; })
                .when(lotRepo).saveLotteryResult(any(LotteryResult.class), any(), any());

        Box<LotteryResult> got = new Box<>();
        svc.drawLottery("E4", 2, r -> got.v = r, e -> fail("x"));

        assertNotNull(got.v);
        assertEquals("E4", got.v.getEventId());
        assertEquals(2, got.v.getEntrantIds().size());
        verify(regSvc).bulkUpdateStatus(eq("E4"), anyList(), anyString(), any(), any());
        verify(lotRepo).saveLotteryResult(any(LotteryResult.class), any(), any());
    }

    /** async delete -> success */
    @Test
    public void delete_async_callsSuccess() {
        LotteryResultService svc = new LotteryResultService(null);
        LotteryResultRepository lotRepo = mock(LotteryResultRepository.class);
        forceSet(svc, "repository", lotRepo);

        doAnswer(inv -> { inv.<OnSuccessListener<Void>>getArgument(2).onSuccess(null); return null; })
                .when(lotRepo).deleteLotteryResultByTimestampAndEventId(any(Date.class), eq("E5"), any(), any());

        Box<Boolean> hit = new Box<>(); hit.v = false;
        svc.deleteLotteryResult(new Date(0), "E5", v -> hit.v = true, e -> fail("x"));
        assertTrue(hit.v);
        verify(lotRepo).deleteLotteryResultByTimestampAndEventId(any(Date.class), eq("E5"), any(), any());
    }

    /** boolean delete -> passthrough */
    @Test
    public void delete_boolean_passthrough() {
        LotteryResultService svc = new LotteryResultService(null);
        LotteryResultRepository lotRepo = mock(LotteryResultRepository.class);
        forceSet(svc, "repository", lotRepo);

        when(lotRepo.deleteLotteryResultByTimestampAndEventId(any(Date.class), eq("E6"))).thenReturn(true, false);
        assertTrue(svc.deleteLotteryResult(new Date(0), "E6"));
        assertFalse(svc.deleteLotteryResult(new Date(0), "E6"));
        verify(lotRepo, times(2)).deleteLotteryResultByTimestampAndEventId(any(Date.class), eq("E6"));
    }

    /** runLottery delegates to drawLottery; count 0 => empty. */
    @Test
    public void runLottery_delegates() {
        LotteryResultService svc = new LotteryResultService(null);
        // repo/regSvc not needed because count=0 path returns early
        Box<LotteryResult> got = new Box<>();
        svc.runLottery("E7", 0, r -> got.v = r, e -> fail("x"));
        assertNotNull(got.v);
        assertTrue(got.v.getEntrantIds().isEmpty());
    }

    /** refillCanceledSlots: quota <= selected+confirmed => empty result. */
    @Test
    public void refill_noOpenSlots_returnsEmpty() {
        LotteryResultService svc = new LotteryResultService(null);

        LotteryResultRepository lotRepo = mock(LotteryResultRepository.class);
        RegistrationHistoryService regSvc = mock(RegistrationHistoryService.class);
        EventService evtSvc = mock(EventService.class);
        forceSet(svc, "repository", lotRepo);
        forceSet(svc, "registrationHistoryService", regSvc);
        forceSet(svc, "eventService", evtSvc);

        // Return an Event with waitingListLimit = 0
        Event ev = new Event("E8", "t", "p");
        ev.setWaitingListLimit(0); // int or double depending on your model
        when(evtSvc.getEventById("E8")).thenReturn(ev);

        // selected = 0, confirmed = 0
        doAnswer(inv -> { inv.<OnSuccessListener<Integer>>getArgument(2).onSuccess(0); return null; })
                .when(regSvc).countByStatus(eq("E8"), anyString(), any(), any());

        Box<LotteryResult> got = new Box<>();
        svc.refillCanceledSlots("E8", r -> got.v = r, e -> fail("x"));
        assertNotNull(got.v);
        assertTrue(got.v.getEntrantIds().isEmpty());
    }


    /** cancelNonResponders: two stale users -> bulk update size 2. */
    @Test
    public void cancelNonResponders_movesStale() {
        LotteryResultService svc = new LotteryResultService(null);

        LotteryResultRepository lotRepo = mock(LotteryResultRepository.class);
        RegistrationHistoryService regSvc = mock(RegistrationHistoryService.class);
        forceSet(svc, "repository", lotRepo);
        forceSet(svc, "registrationHistoryService", regSvc);

        // selected list with old timestamps
        List<RegistrationHistory> selected = List.of(rh("U1", 1_000), rh("U2", 2_000));
        doAnswer(inv -> { inv.<OnSuccessListener<List<RegistrationHistory>>>getArgument(2).onSuccess(selected); return null; })
                .when(regSvc).getByEventAndStatus(eq("E9"), anyString(), any(), any());

        // bulk update returns number of ids
        doAnswer(inv -> { List<String> ids = inv.getArgument(1);
            inv.<OnSuccessListener<Integer>>getArgument(3).onSuccess(ids.size()); return null; })
                .when(regSvc).bulkUpdateStatus(eq("E9"), anyList(), anyString(), any(), any());

        Box<Integer> got = new Box<>();
        long deadline = 10_000L;
        svc.cancelNonResponders("E9", deadline, n -> got.v = n, e -> fail("x"));
        assertEquals(2, got.v);
    }
}
