package com.quantiagents.app;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.quantiagents.app.Repository.EventRepository;
import com.quantiagents.app.Services.EventService;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;

/**
 * Unit tests for EventService quota helpers.
 *
 * These tests prove that:
 *  - getSelectionQuota(...) returns whatever the repository provides via callback.
 *  - setSelectionQuota(...) forwards to the repository and surfaces the success callback.
 *
 * No Android/emulator required.
 */
public class EventServiceQuotaTest {

    private EventService service;
    private EventRepository repo;

    @Before
    public void setUp() throws Exception {
        service = new EventService(null);
        repo = mock(EventRepository.class);

        // Inject mocked repo into the service.
        Field f = EventService.class.getDeclaredField("repository");
        f.setAccessible(true);
        f.set(service, repo);
    }

    @Test
    public void getSelectionQuota_asyncYieldsRepoValue() {
        // Arrange: simulate repo returning 25
        doAnswer(inv -> {
            OnSuccessListener<Integer> ok = inv.getArgument(1);
            ok.onSuccess(25);
            return null;
        }).when(repo).getSelectionQuota(eq("evt1"), any(), any());

        // Act: capture value through a small box (array)
        final int[] box = {-1};
        service.getSelectionQuota("evt1", v -> box[0] = v, e -> fail("should not fail"));

        // Assert
        assertEquals(25, box[0]);
    }

    @Test
    public void setSelectionQuota_forwardsToRepo() {
        OnSuccessListener<Void> ok = mock(OnSuccessListener.class);
        OnFailureListener err = mock(OnFailureListener.class);

        // Arrange: immediately call success on repo.setSelectionQuota
        doAnswer(inv -> {
            inv.<OnSuccessListener<Void>>getArgument(2).onSuccess(null);
            return null;
        }).when(repo).setSelectionQuota(eq("evt1"), eq(40), any(), any());

        // Act
        service.setSelectionQuota("evt1", 40, ok, err);

        // Assert
        verify(ok).onSuccess(null);
        verify(err, never()).onFailure(any());
    }
}
