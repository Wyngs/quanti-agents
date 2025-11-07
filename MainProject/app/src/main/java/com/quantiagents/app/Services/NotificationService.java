package com.quantiagents.app.Services;

import android.content.Context;
import androidx.annotation.NonNull;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.QuerySnapshot;
import com.quantiagents.app.Repository.FireBaseRepository;
import com.quantiagents.app.Repository.NotificationRepository;
import com.quantiagents.app.models.Notification;

public class NotificationService {

    private final NotificationRepository repository;

    public NotificationService(Context context) {
        FireBaseRepository fireBaseRepository = new FireBaseRepository();
        this.repository = new NotificationRepository(fireBaseRepository);
    }

    public Task<QuerySnapshot> getAllNotifications() {
        return repository.getAllNotifications();
    }

    public void saveNotification(Notification notification, com.google.android.gms.tasks.OnSuccessListener<String> onSuccess, com.google.android.gms.tasks.OnFailureListener onFailure) {
        if (notification == null) {
            onFailure.onFailure(new IllegalArgumentException("Notification cannot be null"));
            return;
        }
        if (notification.getType() == null) {
            onFailure.onFailure(new IllegalArgumentException("Notification type is required"));
            return;
        }
        repository.saveNotification(notification, onSuccess, onFailure);
    }

    public Task<Void> deleteNotification(String notificationId) {
        return repository.deleteNotificationById(notificationId);
    }

    public Task<QuerySnapshot> getNotificationsByRecipientId(String recipientId) {
        return repository.getNotificationsByRecipientId(recipientId);
    }
}