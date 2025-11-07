package com.quantiagents.app.Repository;

import androidx.annotation.NonNull;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.quantiagents.app.models.Notification;

public class NotificationRepository {

    private final CollectionReference context;

    public NotificationRepository(FireBaseRepository fireBaseRepository) {
        this.context = fireBaseRepository.getNotificationCollectionRef();
    }

    public Task<QuerySnapshot> getAllNotifications() {
        return context.get();
    }

    public void saveNotification(Notification notification, com.google.android.gms.tasks.OnSuccessListener<String> onSuccess, com.google.android.gms.tasks.OnFailureListener onFailure) {
        if (notification.getNotificationId() == null || notification.getNotificationId().trim().isEmpty()) {
            Task<DocumentReference> addTask = context.add(notification);
            addTask.addOnSuccessListener(documentReference -> {
                String docId = documentReference.getId();
                notification.setNotificationId(docId);
                context.document(docId).update("notificationId", docId)
                        .addOnSuccessListener(aVoid -> onSuccess.onSuccess(docId))
                        .addOnFailureListener(e -> onSuccess.onSuccess(docId));
            }).addOnFailureListener(onFailure);
        } else {
            String notificationId = notification.getNotificationId();
            DocumentReference docRef = context.document(notificationId);
            docRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    docRef.set(notification, SetOptions.merge())
                            .addOnSuccessListener(aVoid -> onSuccess.onSuccess(notificationId))
                            .addOnFailureListener(onFailure);
                } else {
                    docRef.set(notification)
                            .addOnSuccessListener(aVoid -> onSuccess.onSuccess(notificationId))
                            .addOnFailureListener(onFailure);
                }
            }).addOnFailureListener(onFailure);
        }
    }

    public Task<Void> deleteNotificationById(String notificationId) {
        return context.document(notificationId).delete();
    }

    public Task<QuerySnapshot> getNotificationsByRecipientId(String recipientId) {
        return context.whereEqualTo("recipientId", recipientId).get();
    }
}