package com.quantiagents.app.Services;

import android.content.Context;
import android.util.Log;

import com.quantiagents.app.models.Notification;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import me.leolin.shortcutbadger.ShortcutBadger;

/**
 * Service to manage app icon badge counts for unread notifications and messages.
 */
public class BadgeService {

    private static final String TAG = "BadgeService";
    private final Context context;
    private final NotificationService notificationService;
    private final UserService userService;
    private final ChatService chatService;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * Constructor that initializes the BadgeService with required dependencies.
     *
     * @param context The Android context used for badge operations
     */
    public BadgeService(Context context) {
        this.context = context;
        this.notificationService = new NotificationService(context);
        this.userService = new UserService(context);
        this.chatService = new ChatService(context);
    }

    /**
     * Updates the app icon badge with the current unread notification and message count.
     * Should be called when:
     * - App starts
     * - New notification is received
     * - Notification is marked as read
     * - User views notifications
     * - New message is received in a chat
     * - Messages are marked as read
     */
    public void updateBadgeCount() {
        userService.getCurrentUser(
                user -> {
                    if (user == null) {
                        clearBadge();
                        return;
                    }

                    // Run badge count calculation on background thread
                    // because it uses synchronous Firestore calls (Tasks.await)
                    executor.execute(() -> {
                        try {
                            // Convert userId to recipientId (int) for notification lookup
                            int recipientId = Math.abs(user.getUserId().hashCode());

                            // Get unread notifications count (synchronous call - must be on background thread)
                            int unreadNotificationCount = getUnreadNotificationCount(recipientId);

                            // Get unread messages count from all chats (synchronous call - must be on background thread)
                            int unreadMessageCount = chatService.getTotalUnreadMessageCount(user.getUserId());

                            // Total unread count = notifications + messages
                            int totalUnreadCount = unreadNotificationCount + unreadMessageCount;

                            // Update badge on main thread (ShortcutBadger can be called from any thread, but being safe)
                            if (totalUnreadCount > 0) {
                                setBadgeCount(totalUnreadCount);
                            } else {
                                clearBadge();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error updating badge count", e);
                            clearBadge();
                        }
                    });
                },
                e -> {
                    Log.e(TAG, "Failed to get current user for badge update", e);
                    clearBadge();
                }
        );
    }

    /**
     * Sets the badge count on the app icon.
     *
     * @param count The number to display on the badge.
     */
    public void setBadgeCount(int count) {
        if (count <= 0) {
            clearBadge();
            return;
        }

        try {
            boolean success = ShortcutBadger.applyCount(context, count);
            if (success) {
                Log.d(TAG, "Badge count set to: " + count);
            } else {
                Log.w(TAG, "Failed to set badge count (launcher may not support badges)");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting badge count", e);
        }
    }

    /**
     * Clears the badge from the app icon.
     */
    public void clearBadge() {
        try {
            ShortcutBadger.removeCount(context);
            Log.d(TAG, "Badge cleared");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing badge", e);
        }
    }

    /**
     * Gets the count of unread notifications for a user.
     *
     * @param recipientId The recipient ID (int hash of userId).
     * @return The count of unread notifications.
     */
    private int getUnreadNotificationCount(int recipientId) {
        try {
            List<Notification> unreadNotifications = notificationService.getUnreadNotificationsByRecipientId(recipientId);
            return unreadNotifications != null ? unreadNotifications.size() : 0;
        } catch (Exception e) {
            Log.e(TAG, "Error getting unread notification count", e);
            return 0;
        }
    }

    /**
     * Gets the unread notification count for a user (asynchronous).
     * Calls the callback with the count on a background thread.
     *
     * @param userId The user ID.
     * @param onCountReady Callback with the unread notification count.
     */
    public void getUnreadNotificationCount(String userId, OnCountReady onCountReady) {
        if (userId == null || userId.isEmpty()) {
            if (onCountReady != null) {
                onCountReady.onCount(0);
            }
            return;
        }

        executor.execute(() -> {
            try {
                int recipientId = Math.abs(userId.hashCode());
                int count = getUnreadNotificationCount(recipientId);
                if (onCountReady != null) {
                    onCountReady.onCount(count);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting unread notification count", e);
                if (onCountReady != null) {
                    onCountReady.onCount(0);
                }
            }
        });
    }

    /**
     * Gets the unread message count for a user (asynchronous).
     * Calls the callback with the count on a background thread.
     *
     * @param userId The user ID.
     * @param onCountReady Callback with the unread message count.
     */
    public void getUnreadMessageCount(String userId, OnCountReady onCountReady) {
        if (userId == null || userId.isEmpty()) {
            if (onCountReady != null) {
                onCountReady.onCount(0);
            }
            return;
        }

        executor.execute(() -> {
            try {
                int count = chatService.getTotalUnreadMessageCount(userId);
                if (onCountReady != null) {
                    onCountReady.onCount(count);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting unread message count", e);
                if (onCountReady != null) {
                    onCountReady.onCount(0);
                }
            }
        });
    }

    /**
     * Callback interface for getting unread counts.
     */
    public interface OnCountReady {
        void onCount(int count);
    }
}

