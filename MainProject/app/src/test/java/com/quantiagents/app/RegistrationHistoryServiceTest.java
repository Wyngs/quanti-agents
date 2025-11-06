package com.quantiagents.app;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.quantiagents.app.Repository.RegistrationHistoryRepository;
import com.quantiagents.app.Services.RegistrationHistoryService;
import com.quantiagents.app.models.RegistrationHistory;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

/**
 * Unit tests for RegistrationHistoryService.
 *
 * Strategy:
 *  - We DON'T change production constructors. Instead we inject a mocked repository
 *    using reflection so tests can run on the JVM (no Android/Firestore required).
 *  - We verify that the service delegates correctly to the repository and propagates
 *    values/callbacks without adding hidden business logic.
 */
public class RegistrationHistoryServiceTest {

    private RegistrationHistoryService service;
    private RegistrationHistoryRepository repo;

    @Before
    public void setUp() throws Exception {
        // Create the service normally. Context is unused in these methods.
        service = new RegistrationHistoryService(null);

        // Mock the repository dependency.
        repo = Mockito.mock(RegistrationHistoryRepository.class);

        // Replace the private final field 'repository' with our mock.
        Field f = RegistrationHistoryService.class.getDeclaredField("repository");
        f.setAccessible(true);
        f.set(service, repo);
    }

    @Test
    public void getByEventAndStatusSync_returnsListFromRepo() {
        // Arrange: repo returns two items
        RegistrationHistory a = new RegistrationHistory();
        RegistrationHistory b = new RegistrationHistory();
        when(repo.getByEventAndStatus("evt1", "WAITING")).thenReturn(Arrays.asList(a, b));

        // Act
        List<RegistrationHistory> out = service.getByEventAndStatus("evt1", "WAITING");

        // Assert: same size as repo; verify delegation
        assertEquals(2, out.size());
        verify(repo).getByEventAndStatus("evt1", "WAITING");
    }

    @Test
    public void countByStatusSync_delegatesToRepo() {
        // Arrange
        when(repo.countByEventAndStatusSync("evt1", "SELECTED")).thenReturn(7);

        // Act
        int n = service.countByStatusSync("evt1", "SELECTED");

        // Assert
        assertEquals(7, n);
    }

    @Test
    public void bulkUpdateStatus_callsRepoAndBubblesResult() {
        // We'll capture the success callback and simulate a "3 docs updated" result.
        OnSuccessListener<Integer> ok = mock(OnSuccessListener.class);
        OnFailureListener err = mock(OnFailureListener.class);

        // Arrange: when bulkUpdateStatus is called, immediately call success with 3.
        doAnswer(inv -> {
            OnSuccessListener<Integer> cb = inv.getArgument(4);
            cb.onSuccess(3);
            return null;
        }).when(repo).bulkUpdateStatus(eq("evt1"), anyList(), eq("SELECTED"), any(), any());

        // Act
        service.bulkUpdateStatus("evt1", Arrays.asList("u1","u2","u3"), "SELECTED", ok, err);

        // Assert: success fired, no error
        verify(ok).onSuccess(3);
        verify(err, never()).onFailure(any());
    }
}
