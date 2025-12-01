package com.quantiagents.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.service.notification.StatusBarNotification;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.quantiagents.app.Services.BadgeService;
import com.quantiagents.app.R;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class BadgeServiceInstrumentedTest {

    private Context context;
    private BadgeService badgeService;
    private NotificationManager notificationManager;
    private static final int BADGE_NOTIFICATION_ID = 8888;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        badgeService = new BadgeService(context);
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // CHECK before granting: Only run the shell command if permission is missing.
        // This prevents the app from being killed/restarted if you already have permission.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(
                        "pm grant " + context.getPackageName() + " android.permission.POST_NOTIFICATIONS");
                try {
                    // Only sleep if we actually ran the command
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {}
            }
        }

        // Ensure clean state before starting
        badgeService.clearBadge();
    }

    @After
    public void tearDown() {
        // Always clean up the notification after the test finishes so it doesn't linger on your phone
        if (badgeService != null) {
            badgeService.clearBadge();
        }
    }

    @Test
    public void testSetBadgeCountPostsCorrectNotificationForSamsung() throws InterruptedException {
        int testCount = 5;

        // 1. Trigger the badge update
        badgeService.setBadgeCount(testCount);

        // Allow some time for the system to post the notification
        Thread.sleep(1000);

        // 2. Retrieve active notifications from the system
        StatusBarNotification[] activeNotifications = notificationManager.getActiveNotifications();
        StatusBarNotification badgeNotification = null;

        for (StatusBarNotification sbn : activeNotifications) {
            if (sbn.getId() == BADGE_NOTIFICATION_ID) {
                badgeNotification = sbn;
                break;
            }
        }

        assertNotNull("Badge notification should be posted to the system tray", badgeNotification);
        Notification notification = badgeNotification.getNotification();

        // 3. Verify 'number' is set (Critical for Samsung Badges)
        assertEquals("Notification .number should match badge count", testCount, notification.number);

        // 4. Verify Small Icon is the vector drawable (Critical for Status Bar visibility)
        // Note: Icon comparison relies on ResId matching
        assertEquals("Small icon should be the vector 'ic_logo_ticket'",
                R.drawable.ic_logo_ticket,
                notification.getSmallIcon().getResId());

        // 5. Verify notification is NOT Ongoing (Must be dismissible to count as a badge on some launchers)
        // CRITICAL FIX: Ongoing must be FALSE
        boolean isOngoing = (notification.flags & Notification.FLAG_ONGOING_EVENT) != 0;
        assertFalse("Notification should NOT be flagged as Ongoing", isOngoing);

        // 6. Verify Auto-Cancel is set (Standard behavior for touch dismissal)
        boolean isAutoCancel = (notification.flags & Notification.FLAG_AUTO_CANCEL) != 0;
        assertTrue("Notification should have FLAG_AUTO_CANCEL set", isAutoCancel);

        // 7. Verify Content Text contains the count
        CharSequence text = notification.extras.getCharSequence(Notification.EXTRA_TEXT);
        assertNotNull(text);
        assertTrue("Notification text should contain the count", text.toString().contains(testCount + " unread items"));
    }

    @Test
    public void testClearBadgeRemovesNotification() throws InterruptedException {
        // First post it
        badgeService.setBadgeCount(3);
        Thread.sleep(500);

        // Check it exists
        boolean existsBefore = false;
        for (StatusBarNotification sbn : notificationManager.getActiveNotifications()) {
            if (sbn.getId() == BADGE_NOTIFICATION_ID) {
                existsBefore = true;
                break;
            }
        }
        assertTrue("Notification should exist before clear", existsBefore);

        // Then clear it
        badgeService.clearBadge();
        Thread.sleep(500);

        // Verify removal
        boolean existsAfter = false;
        for (StatusBarNotification sbn : notificationManager.getActiveNotifications()) {
            if (sbn.getId() == BADGE_NOTIFICATION_ID) {
                existsAfter = true;
                break;
            }
        }

        assertFalse("Badge notification should be removed from system tray after clear", existsAfter);
    }
}