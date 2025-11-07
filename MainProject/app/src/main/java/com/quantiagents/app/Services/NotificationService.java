package com.quantiagents.app.Services;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.quantiagents.app.Repository.FireBaseRepository;
import com.quantiagents.app.Repository.NotificationRepository;
import com.quantiagents.app.models.Notification;

import java.util.List;

public class NotificationService {

    private final NotificationRepository repository;

    public NotificationService(Context context) {
        // NotificationService instantiates its own repositories internally
        FireBaseRepository fireBaseRepository = new FireBaseRepository();
        this.repository = new NotificationRepository(fireBaseRepository);
    }

    public Notification getNotificationById(int notificationId) {
        return repository.getNotificationById(notificationId);
    }

    public List<Notification> getAllNotifications() {
        return repository.getAllNotifications();
    }

    public List<Notification> getNotificationsByRecipientId(int recipientId) {
        return repository.getNotificationsByRecipientId(recipientId);
    }

    public List<Notification> getUnreadNotificationsByRecipientId(int recipientId) {
        return repository.getUnreadNotificationsByRecipientId(recipientId);
    }

    public void saveNotification(Notification notification, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        // Validate notification before saving
        if (notification == null) {
            onFailure.onFailure(new IllegalArgumentException("Notification cannot be null"));
            return;
        }
        if (notification.getType() == null) {
            onFailure.onFailure(new IllegalArgumentException("Notification type is required"));
            return;
        }
        
        // If notificationId is 0 or negative, Firebase will auto-generate an ID
        // The notification object will be updated with the generated ID after saving
        repository.saveNotification(notification,
                aVoid -> {
                    Log.d("App", "Notification saved with ID: " + notification.getNotificationId());
                    onSuccess.onSuccess(aVoid);
                },
                e -> {
                    Log.e("App", "Failed to save notification", e);
                    onFailure.onFailure(e);
                });
    }

    public void updateNotification(@NonNull Notification notification,
                                   @NonNull OnSuccessListener<Void> onSuccess,
                                   @NonNull OnFailureListener onFailure) {
        // Validate notification before updating
        if (notification.getType() == null) {
            onFailure.onFailure(new IllegalArgumentException("Notification type is required"));
            return;
        }
        
        repository.updateNotification(notification,
                aVoid -> {
                    Log.d("App", "Notification updated: " + notification.getNotificationId());
                    onSuccess.onSuccess(aVoid);
                },
                e -> {
                    Log.e("App", "Failed to update notification", e);
                    onFailure.onFailure(e);
                });
    }

    public void markNotificationAsRead(int notificationId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        Notification notification = repository.getNotificationById(notificationId);
        if (notification == null) {
            onFailure.onFailure(new IllegalArgumentException("Notification not found"));
            return;
        }
        
        notification.setHasRead(true);
        updateNotification(notification, onSuccess, onFailure);
    }

    public void deleteNotification(int notificationId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        repository.deleteNotificationById(notificationId,
                aVoid -> {
                    Log.d("App", "Notification deleted: " + notificationId);
                    onSuccess.onSuccess(aVoid);
                },
                e -> {
                    Log.e("App", "Failed to delete notification", e);
                    onFailure.onFailure(e);
                });
    }

    public boolean deleteNotification(int notificationId) {
        return repository.deleteNotificationById(notificationId);
    }
}
