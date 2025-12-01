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
import com.quantiagents.app.ui.main.MainActivity;
import com.quantiagents.app.R;

import java.util.List;

public class NotificationService {

    private final NotificationRepository repository;
    private final Context context;
    private static final String CHANNEL_ID = "event_updates_channel";

    public NotificationService(Context context) {
        FireBaseRepository fireBaseRepository = new FireBaseRepository();
        this.repository = new NotificationRepository(fireBaseRepository);
        this.context = context;
        createNotificationChannel();
    }

    // --- Added back the missing method causing the compile error ---
    public List<Notification> getAllNotifications() {
        return repository.getAllNotifications();
    }
    // -------------------------------------------------------------

    public Notification getNotificationById(int notificationId) {
        return repository.getNotificationById(notificationId);
    }

    public void getNotificationById(int notificationId,
                                    OnSuccessListener<Notification> onSuccess,
                                    OnFailureListener onFailure) {
        repository.getNotificationById(notificationId, onSuccess, onFailure);
    }

    public List<Notification> getNotificationsByRecipientId(int recipientId) {
        return repository.getNotificationsByRecipientId(recipientId);
    }

    public List<Notification> getUnreadNotificationsByRecipientId(int recipientId) {
        return repository.getUnreadNotificationsByRecipientId(recipientId);
    }

    public void saveNotification(Notification notification, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        if (notification == null || notification.getType() == null) {
            if (onFailure != null) onFailure.onFailure(new IllegalArgumentException("Invalid notification"));
            return;
        }

        repository.saveNotification(notification,
                aVoid -> {
                    Log.d("App", "Notification saved: " + notification.getNotificationId());

                    // 1. Update BadgeService (Legacy/ShortcutBadger support)
                    BadgeService badgeService = new BadgeService(context);
                    badgeService.updateBadgeCount();

                    // 2. SHOW SYSTEM NOTIFICATION (Required for Samsung/Android 8+ badges)
                    UserService userService = new UserService(context);
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
    }

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

    public void deleteNotification(int notificationId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        repository.deleteNotificationById(notificationId, onSuccess, onFailure);
    }

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