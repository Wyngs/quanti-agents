package com.quantiagents.app;

import com.google.android.gms.tasks.OnSuccessListener;

import com.quantiagents.app.Constants.constant; // <-- for constant.EventRegistrationStatus
import com.quantiagents.app.Repository.RegistrationHistoryRepository;
import com.quantiagents.app.Services.RegistrationHistoryService;
import com.quantiagents.app.models.RegistrationHistory;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Basic RegistrationHistoryService tests with Mockito.
 */
public class RegistrationHistoryServiceTest {

    /** Test-only reflection helper to set a private final field. */
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

    /** Tiny box to capture async values. */
    static class Box<T> { T v; }

    /** Builder for a RegistrationHistory test object, using the REAL enum type. */
    private static RegistrationHistory rh(
            String e,
            String u,
            Date d,
            constant.EventRegistrationStatus status
    ) {
        RegistrationHistory r = new RegistrationHistory();
        r.setEventId(e);
        r.setUserId(u);
        r.setRegisteredAt(d);
        if (status != null) r.setEventRegistrationStatus(status);
        return r;
    }

    /** save: null history -> failure */
    @Test
    public void save_null_fails() {
        RegistrationHistoryService svc = new RegistrationHistoryService(null);
        Box<Throwable> fail = new Box<>();
        svc.saveRegistrationHistory(null, v -> fail("not expected"), e -> fail.v = e);
        assertNotNull(fail.v);
    }

    /** save: missing status -> failure */
    @Test
    public void save_missingStatus_fails() {
        RegistrationHistoryService svc = new RegistrationHistoryService(null);
        Box<Throwable> fail = new Box<>();
        svc.saveRegistrationHistory(rh("E1","U1", new Date(), null), v -> fail("x"), e -> fail.v = e);
        assertNotNull(fail.v);
        assertTrue(fail.v.getMessage().toLowerCase().contains("status"));
    }

    /** save: missing date -> failure */
    @Test
    public void save_missingDate_fails() {
        RegistrationHistoryService svc = new RegistrationHistoryService(null);
        Box<Throwable> fail = new Box<>();
        // pass a real enum constant
        svc.saveRegistrationHistory(
                rh("E1","U1", null, constant.EventRegistrationStatus.SELECTED),
                v -> fail("x"),
                e -> fail.v = e
        );
        assertNotNull(fail.v);
        assertTrue(fail.v.getMessage().toLowerCase().contains("date"));
    }

    /** save: valid -> success via mocked repo */
    @Test
    public void save_valid_callsSuccess() {
        RegistrationHistoryService svc = new RegistrationHistoryService(null);
        RegistrationHistoryRepository repo = mock(RegistrationHistoryRepository.class);
        doAnswer(inv -> { inv.<OnSuccessListener<Void>>getArgument(1).onSuccess(null); return null; })
                .when(repo).saveRegistrationHistory(any(), any(), any());
        forceSet(svc, "repository", repo);

        Box<Boolean> hit = new Box<>(); hit.v = false;
        svc.saveRegistrationHistory(
                rh("E2","U2", new Date(), constant.EventRegistrationStatus.SELECTED),
                v -> hit.v = true,
                e -> fail("x")
        );
        assertTrue(hit.v);
        verify(repo).saveRegistrationHistory(any(), any(), any());
    }

    /** update: missing status -> failure */
    @Test
    public void update_missingStatus_fails() {
        RegistrationHistoryService svc = new RegistrationHistoryService(null);
        Box<Throwable> fail = new Box<>();
        svc.updateRegistrationHistory(rh("E3","U3", new Date(), null), v -> fail("x"), e -> fail.v = e);
        assertNotNull(fail.v);
    }

    /** update: valid -> success */
    @Test
    public void update_valid_callsSuccess() {
        RegistrationHistoryService svc = new RegistrationHistoryService(null);
        RegistrationHistoryRepository repo = mock(RegistrationHistoryRepository.class);
        doAnswer(inv -> { inv.<OnSuccessListener<Void>>getArgument(1).onSuccess(null); return null; })
                .when(repo).updateRegistrationHistory(any(), any(), any());
        forceSet(svc, "repository", repo);

        Box<Boolean> hit = new Box<>(); hit.v = false;
        svc.updateRegistrationHistory(
                rh("E4","U4", new Date(), constant.EventRegistrationStatus.CONFIRMED),
                v -> hit.v = true,
                e -> fail("x")
        );
        assertTrue(hit.v);
        verify(repo).updateRegistrationHistory(any(), any(), any());
    }

    /** async delete -> success */
    @Test
    public void delete_async_callsSuccess() {
        RegistrationHistoryService svc = new RegistrationHistoryService(null);
        RegistrationHistoryRepository repo = mock(RegistrationHistoryRepository.class);
        doAnswer(inv -> { inv.<OnSuccessListener<Void>>getArgument(2).onSuccess(null); return null; })
                .when(repo).deleteRegistrationHistoryByEventIdAndUserId(eq("E5"), eq("U5"), any(), any());
        forceSet(svc, "repository", repo);

        Box<Boolean> hit = new Box<>(); hit.v = false;
        svc.deleteRegistrationHistory("E5","U5", v -> hit.v = true, e -> fail("x"));
        assertTrue(hit.v);
        verify(repo).deleteRegistrationHistoryByEventIdAndUserId(eq("E5"), eq("U5"), any(), any());
    }

    /** boolean delete -> passthrough */
    @Test
    public void delete_boolean_passthrough() {
        RegistrationHistoryService svc = new RegistrationHistoryService(null);
        RegistrationHistoryRepository repo = mock(RegistrationHistoryRepository.class);
        forceSet(svc, "repository", repo);

        when(repo.deleteRegistrationHistoryByEventIdAndUserId("E6","U6")).thenReturn(true, false);
        assertTrue(svc.deleteRegistrationHistory("E6","U6"));
        assertFalse(svc.deleteRegistrationHistory("E6","U6"));
        verify(repo, times(2)).deleteRegistrationHistoryByEventIdAndUserId("E6","U6");
    }

    /** getByEventAndStatus -> forwards list */
    @Test
    public void getByEventAndStatus_forwardsList() {
        RegistrationHistoryService svc = new RegistrationHistoryService(null);
        RegistrationHistoryRepository repo = mock(RegistrationHistoryRepository.class);
        doAnswer(inv -> {
            inv.<OnSuccessListener<List<RegistrationHistory>>>getArgument(2).onSuccess(List.of());
            return null;
        }).when(repo).getByEventAndStatus(eq("E7"), eq("WAITLIST"), any(), any()); // enum name as string
        forceSet(svc, "repository", repo);

        Box<List<RegistrationHistory>> got = new Box<>();
        svc.getByEventAndStatus("E7", "WAITLIST", l -> got.v = l, e -> fail("x"));
        assertNotNull(got.v);
        assertTrue(got.v.isEmpty());
    }

    /** countByStatus -> forwards integer */
    @Test
    public void countByStatus_forwardsCount() {
        RegistrationHistoryService svc = new RegistrationHistoryService(null);
        RegistrationHistoryRepository repo = mock(RegistrationHistoryRepository.class);
        doAnswer(inv -> { inv.<OnSuccessListener<Integer>>getArgument(2).onSuccess(42); return null; })
                .when(repo).countByEventAndStatus(eq("E8"), eq("SELECTED"), any(), any());
        forceSet(svc, "repository", repo);

        Box<Integer> got = new Box<>();
        svc.countByStatus("E8", "SELECTED", c -> got.v = c, e -> fail("x"));
        assertEquals(42, got.v);
    }

    /** bulkUpdateStatus -> forwards updated count */
    @Test
    public void bulkUpdateStatus_forwardsCount() {
        RegistrationHistoryService svc = new RegistrationHistoryService(null);
        RegistrationHistoryRepository repo = mock(RegistrationHistoryRepository.class);
        doAnswer(inv -> {
            List<String> ids = inv.getArgument(1);
            inv.<OnSuccessListener<Integer>>getArgument(3).onSuccess(ids.size());
            return null;
        }).when(repo).bulkUpdateStatus(eq("E9"), anyList(), eq("CONFIRMED"), any(), any());
        forceSet(svc, "repository", repo);

        Box<Integer> got = new Box<>();
        svc.bulkUpdateStatus("E9", List.of("U1","U2","U3"), "CONFIRMED", n -> got.v = n, e -> fail("x"));
        assertEquals(3, got.v);
    }
}
