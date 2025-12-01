package com.quantiagents.app.Services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.quantiagents.app.Repository.FireBaseRepository;
import com.quantiagents.app.Repository.NotificationRepository;
import com.quantiagents.app.models.Notification;
import com.quantiagents.app.models.User;
import com.quantiagents.app.ui.main.MainActivity;
import com.quantiagents.app.R;

import java.util.List;

/**
 * Service layer for Notification operations.
 * Handles business logic for creating, updating, and managing notifications.
 * Also updates app icon badge counts when notifications are saved.
 */
public class NotificationService {

    private final NotificationRepository repository;
    private final Context context;
    private static final String CHANNEL_ID = "event_updates_channel";

    /**
     * Constructor that initializes the NotificationService with required dependencies.
     * NotificationService instantiates its own repositories internally.
     *
     * @param context The Android context used for badge updates
     */
    public NotificationService(Context context) {
        FireBaseRepository fireBaseRepository = new FireBaseRepository();
        this.repository = new NotificationRepository(fireBaseRepository);
        this.context = context;
        createNotificationChannel();
    }

    /**
     * Retrieves a notification by its ID synchronously.
     *
     * @param notificationId The unique identifier of the notification
     * @return The Notification object, or null if not found
     */
    public Notification getNotificationById(int notificationId) {
        return repository.getNotificationById(notificationId);
    }

    /**
     * Retrieves a notification by its ID asynchronously.
     *
     * @param notificationId The unique identifier of the notification
     * @param onSuccess Callback receiving the Notification object, or null if not found
     * @param onFailure Callback receiving any error exception
     */
    public void getNotificationById(int notificationId, 
                                    OnSuccessListener<Notification> onSuccess, 
                                    OnFailureListener onFailure) {
        repository.getNotificationById(notificationId, onSuccess, onFailure);
    }

    /**
     * Retrieves all notifications synchronously.
     *
     * @return List of all notifications
     */
    public List<Notification> getAllNotifications() {
        return repository.getAllNotifications();
    }

    /**
     * Retrieves all notifications for a specific recipient synchronously.
     *
     * @param recipientId The user ID of the notification recipient
     * @return List of notifications for the recipient
     */
    public List<Notification> getNotificationsByRecipientId(int recipientId) {
        return repository.getNotificationsByRecipientId(recipientId);
    }

    /**
     * Retrieves all unread notifications for a specific recipient synchronously.
     *
     * @param recipientId The user ID of the notification recipient
     * @return List of unread notifications for the recipient
     */
    public List<Notification> getUnreadNotificationsByRecipientId(int recipientId) {
        return repository.getUnreadNotificationsByRecipientId(recipientId);
    }

    /**
     * Validates and saves a new notification.
     * If notificationId is 0 or negative, Firebase will auto-generate an ID.
     * Updates the app icon badge count after saving.
     * Only saves notifications for users who have notifications enabled.
     *
     * @param notification The notification to save
     * @param onSuccess Callback invoked on successful save
     * @param onFailure Callback receiving validation or database errors
     */
    public void saveNotification(Notification notification, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        if (notification == null || notification.getType() == null) {
            if (onFailure != null) onFailure.onFailure(new IllegalArgumentException("Invalid notification"));
            return;
        }

        // Check if recipient has notifications enabled before saving
        UserService userService = new UserService(context);
        int recipientId = notification.getRecipientId();
        
        // Find the recipient user by matching their userId hash to recipientId
        userService.getAllUsers(
                users -> {
                    User recipient = null;
                    for (User user : users) {
                        if (user != null && user.getUserId() != null) {
                            int userIdHash = Math.abs(user.getUserId().hashCode());
                            if (userIdHash == recipientId) {
                                recipient = user;
                                break;
                            }
                        }
                    }

                    // Only save notification if recipient has notifications enabled
                    if (recipient == null) {
                        Log.d("App", "Recipient not found for notification, skipping save");
                        // Silently skip if recipient not found (user may have been deleted)
                        if (onSuccess != null) onSuccess.onSuccess(null);
                        return;
                    }

                    if (!recipient.hasNotificationsOn()) {
                        Log.d("App", "Recipient has notifications disabled, skipping save for notification: " + notification.getNotificationId());
                        // Silently skip if notifications are disabled
                        if (onSuccess != null) onSuccess.onSuccess(null);
                        return;
                    }

                    // Recipient has notifications enabled, proceed with saving
                    repository.saveNotification(notification,
                            aVoid -> {
                                Log.d("App", "Notification saved: " + notification.getNotificationId());

                                // 1. Update BadgeService (Legacy/ShortcutBadger support)
                                BadgeService badgeService = new BadgeService(context);
                                badgeService.updateBadgeCount();

                                // 2. SHOW SYSTEM NOTIFICATION (Required for Samsung/Android 8+ badges)
                                userService.getCurrentUser(user -> {
                                    if (user != null) {
                                        // Calculate new unread count asynchronously then post notification
                                        badgeService.getUnreadNotificationCount(user.getUserId(), count -> {
                                            showSystemNotification(notification, count);
                                        });
                                    }
                                }, e -> Log.e("NotifService", "Failed to get user for system notification"));

                                if (onSuccess != null) onSuccess.onSuccess(aVoid);
                            },
                            e -> {
                                Log.e("App", "Failed to save notification", e);
                                if (onFailure != null) onFailure.onFailure(e);
                            });
                },
                e -> {
                    Log.e("App", "Failed to fetch users to check notification preference", e);
                    // If we can't check, we'll save anyway to avoid breaking functionality
                    // But log the error for debugging
                    repository.saveNotification(notification,
                            aVoid -> {
                                Log.d("App", "Notification saved (preference check failed): " + notification.getNotificationId());
                                BadgeService badgeService = new BadgeService(context);
                                badgeService.updateBadgeCount();
                                userService.getCurrentUser(user -> {
                                    if (user != null) {
                                        badgeService.getUnreadNotificationCount(user.getUserId(), count -> {
                                            showSystemNotification(notification, count);
                                        });
                                    }
                                }, e2 -> Log.e("NotifService", "Failed to get user for system notification"));
                                if (onSuccess != null) onSuccess.onSuccess(aVoid);
                            },
                            e2 -> {
                                Log.e("App", "Failed to save notification", e2);
                                if (onFailure != null) onFailure.onFailure(e2);
                            });
                }
        );
    }


    /**
     * Validates and updates an existing notification.
     *
     * @param notification The notification to update (must have valid type)
     * @param onSuccess Callback invoked on successful update
     * @param onFailure Callback invoked if update fails or notification type is missing
     */
    public void updateNotification(@NonNull Notification notification,
                                   @NonNull OnSuccessListener<Void> onSuccess,
                                   @NonNull OnFailureListener onFailure) {
        if (notification.getType() == null) {
            if (onFailure != null) onFailure.onFailure(new IllegalArgumentException("Notification type is required"));
            return;
        }

        repository.updateNotification(notification,
                aVoid -> {
                    if (onSuccess != null) onSuccess.onSuccess(aVoid);
                },
                e -> {
                    if (onFailure != null) onFailure.onFailure(e);
                });
    }

    /**
     * Marks a notification as read.
     *
     * @param notificationId The ID of the notification to mark as read
     * @param onSuccess Callback invoked on successful update
     * @param onFailure Callback invoked if notification is not found or update fails
     */
    public void markNotificationAsRead(int notificationId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        repository.getNotificationById(notificationId,
                notification -> {
                    if (notification != null) {
                        notification.setHasRead(true);
                        repository.updateNotification(notification,
                                aVoid -> {
                                    // Update badge after marking read
                                    BadgeService badgeService = new BadgeService(context);
                                    badgeService.updateBadgeCount();

                                    // Optional: Cancel the specific system notification if it exists
                                    try {
                                        NotificationManagerCompat.from(context).cancel(notificationId);
                                    } catch (Exception ignored) {}

                                    if (onSuccess != null) onSuccess.onSuccess(aVoid);
                                },
                                onFailure
                        );
                    } else {
                        if (onFailure != null) onFailure.onFailure(new IllegalArgumentException("Notification not found"));
                    }
                },
                onFailure);
    }

    /**
     * Deletes a notification asynchronously.
     *
     * @param notificationId The ID of the notification to delete
     * @param onSuccess Callback invoked on successful deletion
     * @param onFailure Callback invoked if deletion fails
     */
    public void deleteNotification(int notificationId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        repository.deleteNotificationById(notificationId, onSuccess, onFailure);
    }

    /**
     * Deletes a notification synchronously.
     *
     * @param notificationId The ID of the notification to delete
     * @return True if notification was successfully deleted, false otherwise
     */
    public boolean deleteNotification(int notificationId) {
        return repository.deleteNotificationById(notificationId);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Event Updates";
            String description = "Notifications for event status and messages";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.setShowBadge(true); // Explicitly enable badges for this channel

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void showSystemNotification(Notification notification, int badgeCount) {
        try {
            Intent intent = new Intent(context, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            // PendingIntent for when user taps the notification
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                    PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher) // Make sure this resource exists!
                    .setContentTitle(notification.getStatus())
                    .setContentText(notification.getDetails())
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setNumber(badgeCount); // <--- THIS SETS THE NUMBER ON SAMSUNG

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

            // Check permission for Android 13+
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {

                // Use notification ID as unique ID so we don't overwrite previous ones unless intended
                notificationManager.notify(notification.getNotificationId(), builder.build());
            }
        } catch (Exception e) {
            Log.e("NotifService", "Failed to show system notification", e);
        }
    }
}