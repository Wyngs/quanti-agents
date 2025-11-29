package com.quantiagents.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.quantiagents.app.Constants.constant;
import com.quantiagents.app.Services.GeoLocationService;
import com.quantiagents.app.Services.ImageService;
import com.quantiagents.app.Services.NotificationService;
import com.quantiagents.app.Services.QRCodeService;
import com.quantiagents.app.Services.ServiceLocator;
import com.quantiagents.app.models.GeoLocation;
import com.quantiagents.app.models.Image;
import com.quantiagents.app.models.Notification;
import com.quantiagents.app.models.QRCode;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(AndroidJUnit4.class)
public class AuxiliaryServicesInstrumentedTest {

    private ServiceLocator locator;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        locator = new ServiceLocator(context);
    }

    @Test
    public void geoLocationValidationAndStorage() {
        GeoLocationService service = locator.geoLocationService();

        GeoLocation valid = new GeoLocation(53.5461, -113.4938, "user_geo", "event_geo");
        saveGeoSync(service, valid);
        GeoLocation fetched = service.getGeoLocationByUserIdAndEventId("user_geo", "event_geo");
        assertNotNull(fetched);
        assertEquals(53.5461, fetched.getLatitude(), 0.001);

        GeoLocation invalidLat = new GeoLocation(91.0, 0.0, "user_bad", "event_bad");
        AtomicBoolean failed = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);
        service.saveGeoLocation(invalidLat, s -> latch.countDown(), e -> {
            failed.set(true);
            latch.countDown();
        });
        awaitLatch(latch);
        assertTrue("Should reject invalid latitude", failed.get());
    }

    @Test
    public void notificationLifecycle() {
        NotificationService service = locator.notificationService();
        int recipientId = 9999;

        Notification note = new Notification(0, constant.NotificationType.GOOD, recipientId, 1, 100, "", "");
        saveNotificationSync(service, note);

        List<Notification> unread = service.getUnreadNotificationsByRecipientId(recipientId);
        assertTrue("Should have unread notification", unread.size() > 0);
        int noteId = unread.get(0).getNotificationId();

        markReadSync(service, noteId);

        Notification updated = service.getNotificationById(noteId);
        assertTrue("Should be marked read", updated.isHasRead());
    }

    @Test
    public void qrCodePersistence() {
        QRCodeService service = locator.qrCodeService();
        String qrVal = "test_qr_code_content";
        String eventId = "event_qr_1";

        QRCode code = new QRCode(0, qrVal, eventId);
        saveQRSync(service, code);

        List<QRCode> list = service.getQRCodesByEventId(eventId);
        assertFalse(list.isEmpty());
        assertEquals(qrVal, list.get(0).getQrCodeValue());
    }

    @Test
    public void imageMetadataStorage() {
        ImageService service = locator.imageService();
        Image img = new Image(null, "event_img_1", "https://example.com/pic.jpg");

        CountDownLatch latch = new CountDownLatch(1);
        service.saveImage(img, id -> latch.countDown(), e -> latch.countDown());
        awaitLatch(latch);

        service.deleteImagesByEventId("event_img_1");
        List<Image> images = service.getAllImages();

        assertNotNull(images);
    }

    private void saveGeoSync(GeoLocationService s, GeoLocation g) {
        CountDownLatch l = new CountDownLatch(1);
        s.saveGeoLocation(g, id -> l.countDown(), e -> l.countDown());
        awaitLatch(l);
    }

    private void saveNotificationSync(NotificationService s, Notification n) {
        CountDownLatch l = new CountDownLatch(1);
        s.saveNotification(n, v -> l.countDown(), e -> l.countDown());
        awaitLatch(l);
    }

    private void markReadSync(NotificationService s, int id) {
        CountDownLatch l = new CountDownLatch(1);
        s.markNotificationAsRead(id, v -> l.countDown(), e -> l.countDown());
        awaitLatch(l);
    }

    private void saveQRSync(QRCodeService s, QRCode q) {
        CountDownLatch l = new CountDownLatch(1);
        s.saveQRCode(q, v -> l.countDown(), e -> l.countDown());
        awaitLatch(l);
    }

    private void awaitLatch(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}