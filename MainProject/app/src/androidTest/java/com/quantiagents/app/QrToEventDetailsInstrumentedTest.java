package com.quantiagents.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.quantiagents.app.Services.EventService;
import com.quantiagents.app.Services.QRCodeService;
import com.quantiagents.app.Services.ServiceLocator;
import com.quantiagents.app.models.Event;
import com.quantiagents.app.models.QRCode;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class QrToEventDetailsInstrumentedTest {

    private EventService eventService;
    private QRCodeService qrCodeService;
    private String createdEventId;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        ServiceLocator locator = new ServiceLocator(context);
        eventService = locator.eventService();
        qrCodeService = locator.qrCodeService();
    }

    @After
    public void tearDown() {
        if (createdEventId != null) {
            CountDownLatch latch = new CountDownLatch(1);
            eventService.deleteEvent(createdEventId,
                    aVoid -> latch.countDown(),
                    e -> latch.countDown());
            awaitLatch(latch);
        }
    }

    @Test
    public void qrCodeLinksToSavedEvent() {
        Event event = new Event();
        event.setTitle("QR Linked Event");
        createdEventId = saveEventSync(event);

        String qrValue = "qr_test_value_" + System.currentTimeMillis();
        QRCode code = new QRCode(0, qrValue, createdEventId);
        saveQrSync(code);

        List<QRCode> qrCodes = qrCodeService.getQRCodesByEventId(createdEventId);
        assertNotNull(qrCodes);
        assertFalse(qrCodes.isEmpty());
        assertEquals(qrValue, qrCodes.get(0).getQrCodeValue());

        Event fetched = eventService.getEventById(createdEventId);
        assertNotNull(fetched);
        assertEquals("QR Linked Event", fetched.getTitle());
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

    private void saveQrSync(QRCode code) {
        CountDownLatch latch = new CountDownLatch(1);
        qrCodeService.saveQRCode(code,
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
