package com.quantiagents.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class ManageEventInfoFlowInstrumentedTest {

    private EventService eventService;
    private RegistrationHistoryService regService;
    private LotteryResultService lotteryService;

    private String eventId;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        ServiceLocator locator = new ServiceLocator(context);
        eventService = locator.eventService();
        regService = locator.registrationHistoryService();
        lotteryService = locator.lotteryResultService();

        Event event = new Event();
        event.setTitle("Manage Info Event");
        eventId = saveEventSync(event);
    }

    @After
    public void tearDown() {
        if (eventId != null) {
            CountDownLatch latch = new CountDownLatch(1);
            eventService.deleteEvent(eventId,
                    aVoid -> latch.countDown(),
                    e -> latch.countDown());
            awaitLatch(latch);
        }
    }

    @Test
    public void registrationsWithDifferentStatusesArePersisted() {
        RegistrationHistory hWait = new RegistrationHistory(
                eventId, "user_wait", constant.EventRegistrationStatus.WAITLIST, new Date());
        RegistrationHistory hSel = new RegistrationHistory(
                eventId, "user_sel", constant.EventRegistrationStatus.SELECTED, new Date());
        RegistrationHistory hConf = new RegistrationHistory(
                eventId, "user_conf", constant.EventRegistrationStatus.CONFIRMED, new Date());

        saveRegistrationSync(hWait);
        saveRegistrationSync(hSel);
        saveRegistrationSync(hConf);

        RegistrationHistory fetchedWait =
                regService.getRegistrationHistoryByEventIdAndUserId(eventId, "user_wait");
        RegistrationHistory fetchedSel =
                regService.getRegistrationHistoryByEventIdAndUserId(eventId, "user_sel");
        RegistrationHistory fetchedConf =
                regService.getRegistrationHistoryByEventIdAndUserId(eventId, "user_conf");

        assertNotNull(fetchedWait);
        assertEquals(constant.EventRegistrationStatus.WAITLIST, fetchedWait.getEventRegistrationStatus());

        assertNotNull(fetchedSel);
        assertEquals(constant.EventRegistrationStatus.SELECTED, fetchedSel.getEventRegistrationStatus());

        assertNotNull(fetchedConf);
        assertEquals(constant.EventRegistrationStatus.CONFIRMED, fetchedConf.getEventRegistrationStatus());
    }

    @Test
    public void runningLotteryReturnsSubsetOfWaitlist() {
        String u1 = "lottery_user_1";
        String u2 = "lottery_user_2";
        String u3 = "lottery_user_3";

        saveRegistrationSync(new RegistrationHistory(
                eventId, u1, constant.EventRegistrationStatus.WAITLIST, new Date()));
        saveRegistrationSync(new RegistrationHistory(
                eventId, u2, constant.EventRegistrationStatus.WAITLIST, new Date()));
        saveRegistrationSync(new RegistrationHistory(
                eventId, u3, constant.EventRegistrationStatus.WAITLIST, new Date()));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<LotteryResult> resultRef = new AtomicReference<>();

        lotteryService.runLottery(eventId, 2,
                result -> {
                    resultRef.set(result);
                    latch.countDown();
                },
                e -> latch.countDown());
        awaitLatch(latch);

        LotteryResult result = resultRef.get();
        assertNotNull(result);
        assertEquals(2, result.getEntrantIds().size());

        Set<String> waitlistIds = new HashSet<>(Arrays.asList(u1, u2, u3));
        for (String id : result.getEntrantIds()) {
            // every selected entrant must have been on the original waitlist
            assertTrue(waitlistIds.contains(id));
        }
    }

    private String saveEventSync(Event event) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> idRef = new AtomicReference<>();
        eventService.saveEvent(event,
                id -> {
                    idRef.set(id);
                    latch.countDown();
                },
                e -> latch.countDown());
        awaitLatch(latch);
        return idRef.get();
    }

    private void saveRegistrationSync(RegistrationHistory history) {
        CountDownLatch latch = new CountDownLatch(1);
        regService.saveRegistrationHistory(history,
                v -> latch.countDown(),
                e -> latch.countDown());
        awaitLatch(latch);
    }

    private void awaitLatch(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }
    }
}
