package com.quantiagents.app;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.quantiagents.app.Constants.constant;
import com.quantiagents.app.Services.NotificationService;
import com.quantiagents.app.Services.ServiceLocator;
import com.quantiagents.app.Services.UserService;
import com.quantiagents.app.models.Notification;
import com.quantiagents.app.models.User;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class NotificationPreferencesInstrumentedTest {

    private NotificationService notificationService;
    private UserService userService;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        ServiceLocator locator = new ServiceLocator(context);
        notificationService = locator.notificationService();
        userService = locator.userService();
    }

    @Test
    public void multipleUnreadNotificationsAreReturned() {
        int recipientId = createTestRecipientId();

        saveNotificationSync(new Notification(
                0,
                constant.NotificationType.GOOD,
                recipientId,
                1,
                100,
                "First status",
                "First details"));

        saveNotificationSync(new Notification(
                0,
                constant.NotificationType.GOOD,
                recipientId,
                1,
                101,
                "Second status",
                "Second details"));

        List<Notification> unread =
                notificationService.getUnreadNotificationsByRecipientId(recipientId);

        assertNotNull(unread);
        assertTrue("Expected at least two unread notifications",
                unread.size() >= 2);
    }

    @Test
    public void markingNotificationAsReadRemovesItFromUnreadList() {
        int recipientId = createTestRecipientId();

        saveNotificationSync(new Notification(
                0,
                constant.NotificationType.GOOD,
                recipientId,
                1,
                200,
                "Status",
                "Details"));

        List<Notification> unread =
                notificationService.getUnreadNotificationsByRecipientId(recipientId);
        assertNotNull(unread);
        assertFalse("Should have at least one unread", unread.isEmpty());

        int noteId = unread.get(0).getNotificationId();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean ok = new AtomicBoolean(false);

        notificationService.markNotificationAsRead(noteId,
                v -> {
                    ok.set(true);
                    latch.countDown();
                },
                e -> latch.countDown());
        awaitLatch(latch);
        assertTrue("markNotificationAsRead should succeed", ok.get());

        List<Notification> unreadAfter =
                notificationService.getUnreadNotificationsByRecipientId(recipientId);

        boolean stillThere = false;
        for (Notification n : unreadAfter) {
            if (n.getNotificationId() == noteId) {
                stillThere = true;
                break;
            }
        }
        assertFalse("Notification should no longer be in unread list", stillThere);
    }

    // --- helpers -----------------------------------------------------------

    private int createTestRecipientId() {
        String suffix = "_" + System.currentTimeMillis();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<User> createdRef = new AtomicReference<>();

        userService.createUser(
                "Notif Pref User" + suffix,
                "notifPrefUser" + suffix,
                "notifpref" + suffix + "@example.com",
                "5551112222",
                "pass123",
                user -> {
                    createdRef.set(user);
                    latch.countDown();
                },
                e -> latch.countDown());
        awaitLatch(latch);

        User user = createdRef.get();
        assertNotNull("Failed to create test user for notification prefs", user);

        return Math.abs(user.getUserId().hashCode());
    }

    private void saveNotificationSync(Notification n) {
        CountDownLatch latch = new CountDownLatch(1);
        notificationService.saveNotification(n,
                v -> latch.countDown(),
                e -> latch.countDown());
        awaitLatch(latch);
    }

    private void awaitLatch(CountDownLatch latch) {
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }
    }
}
