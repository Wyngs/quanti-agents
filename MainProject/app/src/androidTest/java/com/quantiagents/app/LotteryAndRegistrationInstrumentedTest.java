package com.quantiagents.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.quantiagents.app.Constants.constant;
import com.quantiagents.app.Services.EventService;
import com.quantiagents.app.Services.LotteryResultService;
import com.quantiagents.app.Services.RegistrationHistoryService;
import com.quantiagents.app.Services.ServiceLocator;
import com.quantiagents.app.models.Event;
import com.quantiagents.app.models.LotteryResult;
import com.quantiagents.app.models.RegistrationHistory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Covers Registration History logic and Lottery Execution.
 */
@RunWith(AndroidJUnit4.class)
public class LotteryAndRegistrationInstrumentedTest {

    private Context context;
    private RegistrationHistoryService regService;
    private LotteryResultService lotteryService;
    private EventService eventService;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        ServiceLocator locator = new ServiceLocator(context);
        regService = locator.registrationHistoryService();
        lotteryService = locator.lotteryResultService();
        eventService = locator.eventService();
    }

    /**
     * Verifies a user can be added to the waitlist and retrieved.
     */
    @Test
    public void joinWaitlistAndRetrieve() {
        String eventId = "evt_test_reg";
        String userId = "user_test_reg";

        RegistrationHistory history = new RegistrationHistory(
                eventId, userId, constant.EventRegistrationStatus.WAITLIST, new Date()
        );

        saveRegistrationSync(history);

        RegistrationHistory fetched = regService.getRegistrationHistoryByEventIdAndUserId(eventId, userId);
        assertNotNull(fetched);
        assertEquals(constant.EventRegistrationStatus.WAITLIST, fetched.getEventRegistrationStatus());
    }

    /**
     * Verifies status updates.
     */
    @Test
    public void updateRegistrationStatus() {
        String eventId = "evt_status_update";
        String userId = "user_status_update";

        RegistrationHistory history = new RegistrationHistory(
                eventId, userId, constant.EventRegistrationStatus.WAITLIST, new Date()
        );
        saveRegistrationSync(history);

        history.setEventRegistrationStatus(constant.EventRegistrationStatus.SELECTED);
        updateRegistrationSync(history);

        RegistrationHistory updated = regService.getRegistrationHistoryByEventIdAndUserId(eventId, userId);
        assertEquals(constant.EventRegistrationStatus.SELECTED, updated.getEventRegistrationStatus());
    }

    /**
     * Tests that the lottery service correctly picks a subset of entrants.
     */
    @Test
    public void runLotterySelectsEntrants() {
        // 1. Create a real event so validation passes
        Event event = new Event();
        event.setTitle("Lottery Event");
        String eventId = saveEventSync(event);

        // 2. Create multiple waiting registrations
        // Updated: Must be WAITLIST for the lottery logic to pick them up.
        createRegistration(eventId, "user_1", constant.EventRegistrationStatus.WAITLIST);
        createRegistration(eventId, "user_2", constant.EventRegistrationStatus.WAITLIST);
        createRegistration(eventId, "user_3", constant.EventRegistrationStatus.WAITLIST);

        // 3. Run lottery to pick 2 winners
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<LotteryResult> resultRef = new AtomicReference<>();
        AtomicReference<Exception> errorRef = new AtomicReference<>();

        lotteryService.runLottery(eventId, 2,
                result -> {
                    resultRef.set(result);
                    latch.countDown();
                },
                e -> {
                    errorRef.set(e);
                    latch.countDown();
                });
        awaitLatch(latch);

        if (errorRef.get() != null) {
            fail("Lottery failed: " + errorRef.get().getMessage());
        }

        LotteryResult result = resultRef.get();
        assertNotNull("Lottery result should exist", result);
        assertEquals(2, result.getEntrantIds().size());
        assertFalse(result.getEntrantIds().contains("user_4")); // Non-existent user
    }

    // Helpers

    private void saveRegistrationSync(RegistrationHistory h) {
        CountDownLatch latch = new CountDownLatch(1);
        regService.saveRegistrationHistory(h, v -> latch.countDown(), e -> latch.countDown());
        awaitLatch(latch);
    }

    private void updateRegistrationSync(RegistrationHistory h) {
        CountDownLatch latch = new CountDownLatch(1);
        regService.updateRegistrationHistory(h, v -> latch.countDown(), e -> latch.countDown());
        awaitLatch(latch);
    }

    private void createRegistration(String eid, String uid, constant.EventRegistrationStatus status) {
        saveRegistrationSync(new RegistrationHistory(eid, uid, status, new Date()));
    }

    private String saveEventSync(Event e) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> id = new AtomicReference<>();
        eventService.saveEvent(e, s -> {
            id.set(s);
            latch.countDown();
        }, err -> latch.countDown());
        awaitLatch(latch);
        return id.get();
    }

    private void awaitLatch(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}