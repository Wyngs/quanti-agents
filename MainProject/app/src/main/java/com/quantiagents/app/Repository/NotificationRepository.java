package com.quantiagents.app.Repository;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.quantiagents.app.models.Notification;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Manages locating and saving of notifications
 */
public class NotificationRepository {

    private final CollectionReference context;

    public NotificationRepository(FireBaseRepository fireBaseRepository) {
        this.context = fireBaseRepository.getNotificationCollectionRef();
    }

    /**
     * Find notification by id
     * @param notificationId
     * Notification id to locate
     * @return
     * Returns notification
     */
    public Notification getNotificationById(int notificationId) {
        try {
            DocumentSnapshot snapshot = Tasks.await(context.document(String.valueOf(notificationId)).get());
            if (snapshot.exists()) {
                return snapshot.toObject(Notification.class);
            } else {
                Log.d("Firestore", "No notification found for ID: " + notificationId);
                return null;
            }
        } catch (ExecutionException | InterruptedException e) {
            Log.e("Firestore", "Error getting notification", e);
            return null;
        }
    }

    /**
     * Gets a list of all notifications
     * @return
     * Returns a list of notifications
     */
    public List<Notification> getAllNotifications() {
        try {
            QuerySnapshot snapshot = Tasks.await(context.get());
            List<Notification> notifications = new ArrayList<>();
            for (QueryDocumentSnapshot document : snapshot) {
                Notification notification = document.toObject(Notification.class);
                if (notification != null) {
                    notifications.add(notification);
                }
            }
            return notifications;
        } catch (ExecutionException | InterruptedException e) {
            Log.e("Firestore", "Error getting all notifications", e);
            return new ArrayList<>();
        }
    }

    /**
     * Saves a notification to the firebase
     * @param notification
     * Notification to save
     * @param onSuccess
     * Calls a function on success
     * @param onFailure
     * Calls a function on failure
     */
    public void saveNotification(Notification notification, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        // If notificationId is 0 or negative, let Firebase auto-generate an ID
        if (notification.getNotificationId() <= 0) {
            // Use .add() to create a new document with auto-generated ID
            Task<DocumentReference> addTask = context.add(notification);
            addTask.addOnSuccessListener(documentReference -> {
                // Extract the auto-generated document ID
                String docId = documentReference.getId();
                
                // Generate a unique int ID from the Firestore document ID
                // Use hash code and ensure it's positive
                int generatedId = docId.hashCode();
                if (generatedId < 0) {
                    generatedId = Math.abs(generatedId);
                }
                // If somehow still 0, use a timestamp-based fallback
                if (generatedId == 0) {
                    generatedId = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
                }
                
                // Update the notification object with the generated ID
                notification.setNotificationId(generatedId);
                
                // Update the document with the generated notificationId field
                context.document(docId).update("notificationId", generatedId)
                        .addOnSuccessListener(aVoid -> {
                            Log.d("Firestore", "Notification created with auto-generated ID " + " (docId: " + docId + ")");
                            onSuccess.onSuccess(aVoid);
                        })
                        .addOnFailureListener(e -> {
                            Log.e("Firestore", "Error updating notification with generated ID", e);
                            // Still call success since document was created, just the ID update failed
                            onSuccess.onSuccess(null);
                        });
            }).addOnFailureListener(onFailure);
        } else {
            // Check if notification with this ID already exists
            DocumentReference docRef = context.document(String.valueOf(notification.getNotificationId()));
            docRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    // Document exists, update it
                    docRef.set(notification, SetOptions.merge())
                            .addOnSuccessListener(onSuccess)
                            .addOnFailureListener(onFailure);
                } else {
                    // Document doesn't exist, create it
                    docRef.set(notification)
                            .addOnSuccessListener(onSuccess)
                            .addOnFailureListener(onFailure);
                }
            }).addOnFailureListener(onFailure);
        }
    }

    /**
     * Updates a notification in the firebase
     * @param notification
     * Notification to update
     * @param onSuccess
     * Calls a function on success
     * @param onFailure
     * Calls a function on failure
     */
    public void updateNotification(@NonNull Notification notification,
                                   @NonNull OnSuccessListener<Void> onSuccess,
                                   @NonNull OnFailureListener onFailure) {
        context.document(String.valueOf(notification.getNotificationId()))
                .set(notification, SetOptions.merge()) // merge only changed fields
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Notification updated: " + notification.getNotificationId());
                    onSuccess.onSuccess(aVoid);
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error updating notification", e);
                    onFailure.onFailure(e);
                });
    }

    /**
     * Deletes a notification from the firebase
     * @param notificationId
     * Notification id to delete
     * @param onSuccess
     * Calls a function on success
     * @param onFailure
     * Calls a function on failure
     */
    public void deleteNotificationById(int notificationId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        context.document(String.valueOf(notificationId))
                .delete()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    /**
     * Deletes a notification via it's id from the firebase
     * @param notificationId
     * Notification id to delete
     * @return
     * Returns boolean if success
     */
    public boolean deleteNotificationById(int notificationId) {
        try {
            Tasks.await(context.document(String.valueOf(notificationId)).delete());
            Log.d("Firestore", "Notification deleted: " + notificationId);
            return true;
        } catch (ExecutionException | InterruptedException e) {
            Log.e("Firestore", "Error deleting notification", e);
            return false;
        }
    }

    /**
     * Gets a list of notifications from recipient id
     * @param recipientId
     * Recipient id to locate
     * @return
     * Returns list of notifications
     */
    public List<Notification> getNotificationsByRecipientId(int recipientId) {
        try {
            QuerySnapshot snapshot = Tasks.await(context.whereEqualTo("recipientId", recipientId).get());
            List<Notification> notifications = new ArrayList<>();
            for (QueryDocumentSnapshot document : snapshot) {
                Notification notification = document.toObject(Notification.class);
                if (notification != null) {
                    notifications.add(notification);
                }
            }
            return notifications;
        } catch (ExecutionException | InterruptedException e) {
            Log.e("Firestore", "Error getting notifications by recipient ID", e);
            return new ArrayList<>();
        }
    }

    /**
     * Gets list of unread notifications from recipient id
     * @param recipientId
     * Recipient id to locate
     * @return
     * Returns list of notifications
     */
    public List<Notification> getUnreadNotificationsByRecipientId(int recipientId) {
        try {
            QuerySnapshot snapshot = Tasks.await(context.whereEqualTo("recipientId", recipientId)
                    .whereEqualTo("hasRead", false).get());
            List<Notification> notifications = new ArrayList<>();
            for (QueryDocumentSnapshot document : snapshot) {
                Notification notification = document.toObject(Notification.class);
                if (notification != null) {
                    notifications.add(notification);
                }
            }
            return notifications;
        } catch (ExecutionException | InterruptedException e) {
            Log.e("Firestore", "Error getting unread notifications by recipient ID", e);
            return new ArrayList<>();
        }
    }
}
