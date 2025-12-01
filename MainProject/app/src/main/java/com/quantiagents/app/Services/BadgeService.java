package com.quantiagents.app.Services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.quantiagents.app.R;
import com.quantiagents.app.models.Notification;
import com.quantiagents.app.ui.main.MainActivity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import me.leolin.shortcutbadger.ShortcutBadger;

/**
 * Service to manage app icon badge counts for unread notifications and messages.
 */
public class BadgeService {

    private static final String TAG = "BadgeService";
    // Changed ID to prevent conflict with other channels
    private static final String BADGE_CHANNEL_ID = "badge_counter_channel_v2";
    private static final int BADGE_NOTIFICATION_ID = 8888;

    private final Context context;
    private final NotificationService notificationService;
    private final UserService userService;
    private final ChatService chatService;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public BadgeService(Context context) {
        // Use application context to avoid memory leaks
        this.context = context.getApplicationContext() != null ? context.getApplicationContext() : context;

        this.notificationService = new NotificationService(this.context);
        this.userService = new UserService(this.context);
        this.chatService = new ChatService(this.context);

        createBadgeChannel();
    }

    /**
     * Updates the app icon badge with the current unread notification and message count.
     */
    public void updateBadgeCount() {
        try {
            userService.getCurrentUser(
                    user -> {
                        if (user == null) {
                            clearBadge();
                            return;
                        }

                        executor.execute(() -> {
                            try {
                                int recipientId = Math.abs(user.getUserId().hashCode());
                                int unreadNotificationCount = getUnreadNotificationCount(recipientId);
                                int unreadMessageCount = chatService.getTotalUnreadMessageCount(user.getUserId());

                                int totalUnreadCount = unreadNotificationCount + unreadMessageCount;

                                if (totalUnreadCount > 0) {
                                    setBadgeCount(totalUnreadCount);
                                } else {
                                    clearBadge();
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error calculating badge count", e);
                                clearBadge();
                            }
                        });
                    },
                    e -> {
                        Log.e(TAG, "Failed to get current user for badge update", e);
                        clearBadge();
                    }
            );
        } catch (Exception e) {
            Log.e(TAG, "Error initiating badge update", e);
        }
    }

    /**
     * Sets the badge count using both ShortcutBadger and System Notification.
     */
    public void setBadgeCount(int count) {
        if (count <= 0) {
            clearBadge();
            return;
        }

        // 1. Attempt ShortcutBadger
        try {
            ShortcutBadger.applyCount(context, count);
        } catch (Throwable t) {
            Log.e(TAG, "ShortcutBadger failed (expected on some devices)", t);
        }

        // 2. Post System Notification
        try {
            postSystemBadgeNotification(count);
        } catch (Throwable t) {
            Log.e(TAG, "Error posting system badge notification", t);
        }
    }

    /**
     * Clears the badge and removes the system notification.
     */
    public void clearBadge() {
        try {
            ShortcutBadger.removeCount(context);
        } catch (Throwable t) {
            Log.e(TAG, "Error clearing ShortcutBadger", t);
        }

        try {
            NotificationManagerCompat.from(context).cancel(BADGE_NOTIFICATION_ID);
        } catch (Throwable t) {
            Log.e(TAG, "Error canceling system notification", t);
        }
    }

    // --- Helper Methods ---

    private int getUnreadNotificationCount(int recipientId) {
        try {
            List<Notification> unreadNotifications = notificationService.getUnreadNotificationsByRecipientId(recipientId);
            return unreadNotifications != null ? unreadNotifications.size() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private void createBadgeChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    BADGE_CHANNEL_ID,
                    "Badge Counter",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Maintains the unread badge count");
            channel.setShowBadge(true);
            channel.enableVibration(false);
            channel.setSound(null, null); // Silent but "important" enough to show badge

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void postSystemBadgeNotification(int count) {
        // Permission check for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // Add extra to tell MainActivity to open Notifications tab
        intent.putExtra("navigate_to_notifications", true);

        // FLAG_UPDATE_CURRENT is important to ensure the extra is actually attached if PendingIntent already existed
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        // Use a standard notification configuration to ensure visibility on Samsung launchers
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, BADGE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_logo_ticket) // Use vector icon for status bar compatibility
                .setContentTitle("EqualEntry")
                .setContentText(count + " unread items")
                .setNumber(count) // Required for Samsung to display the number on the badge
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true) // Allows the user to dismiss it
                .setOnlyAlertOnce(true);

        NotificationManagerCompat.from(context).notify(BADGE_NOTIFICATION_ID, builder.build());
    }

    // --- Asynchronous Count Getters ---

    public void getUnreadNotificationCount(String userId, OnCountReady onCountReady) {
        if (userId == null || userId.isEmpty()) {
            if (onCountReady != null) onCountReady.onCount(0);
            return;
        }
        executor.execute(() -> {
            try {
                int count = getUnreadNotificationCount(Math.abs(userId.hashCode()));
                if (onCountReady != null) onCountReady.onCount(count);
            } catch (Exception e) {
                if (onCountReady != null) onCountReady.onCount(0);
            }
        });
    }

    public void getUnreadMessageCount(String userId, OnCountReady onCountReady) {
        if (userId == null || userId.isEmpty()) {
            if (onCountReady != null) onCountReady.onCount(0);
            return;
        }
        executor.execute(() -> {
            try {
                int count = chatService.getTotalUnreadMessageCount(userId);
                if (onCountReady != null) onCountReady.onCount(count);
            } catch (Exception e) {
                if (onCountReady != null) onCountReady.onCount(0);
            }
        });
    }

    public interface OnCountReady {
        void onCount(int count);
    }
}