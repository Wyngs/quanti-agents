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

/**
 * Service layer for Notification operations.
 * Handles business logic for creating, updating, and managing notifications.
 * Also updates app icon badge counts when notifications are saved.
 */
public class NotificationService {

    private final NotificationRepository repository;
    private final Context context;

    /**
     * Constructor that initializes the NotificationService with required dependencies.
     * NotificationService instantiates its own repositories internally.
     *
     * @param context The Android context used for badge updates
     */
    public NotificationService(Context context) {
        // NotificationService instantiates its own repositories internally
        FireBaseRepository fireBaseRepository = new FireBaseRepository();
        this.repository = new NotificationRepository(fireBaseRepository);
        this.context = context;
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
     *
     * @param notification The notification to save
     * @param onSuccess Callback invoked on successful save
     * @param onFailure Callback receiving validation or database errors
     */
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
                    
                    // Update app icon badge
                    BadgeService badgeService = new BadgeService(context);
                    badgeService.updateBadgeCount();
                    
                    onSuccess.onSuccess(aVoid);
                },
                e -> {
                    Log.e("App", "Failed to save notification", e);
                    onFailure.onFailure(e);
                });
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
                    if (notification == null) {
                        onFailure.onFailure(new IllegalArgumentException("Notification not found"));
                        return;
                    }
                    
                    notification.setHasRead(true);
                    updateNotification(notification, onSuccess, onFailure);
                },
                e -> {
                    Log.e("App", "Failed to get notification", e);
                    onFailure.onFailure(e);
                });
    }

    /**
     * Deletes a notification asynchronously.
     *
     * @param notificationId The ID of the notification to delete
     * @param onSuccess Callback invoked on successful deletion
     * @param onFailure Callback invoked if deletion fails
     */
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

    /**
     * Deletes a notification synchronously.
     *
     * @param notificationId The ID of the notification to delete
     * @return True if notification was successfully deleted, false otherwise
     */
    public boolean deleteNotification(int notificationId) {
        return repository.deleteNotificationById(notificationId);
    }
}
