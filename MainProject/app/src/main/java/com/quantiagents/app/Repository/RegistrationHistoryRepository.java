package com.quantiagents.app.Repository;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.quantiagents.app.models.RegistrationHistory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class RegistrationHistoryRepository {

    private final CollectionReference context;

    public RegistrationHistoryRepository(FireBaseRepository fireBaseRepository) {
        this.context = fireBaseRepository.getRegistrationHistoryCollectionRef();
    }

    public RegistrationHistory getRegistrationHistoryByEventIdAndUserId(String eventId, String userId) {
        try {
            String docId = eventId + "_" + userId;
            DocumentSnapshot snapshot = Tasks.await(context.document(docId).get());
            if (snapshot.exists()) {
                return snapshot.toObject(RegistrationHistory.class);
            } else {
                Log.d("Firestore", "No registration history found for eventId: " + eventId + ", userId: " + userId);
                return null;
            }
        } catch (ExecutionException | InterruptedException e) {
            Log.e("Firestore", "Error getting registration history", e);
            return null;
        }
    }

    public List<RegistrationHistory> getAllRegistrationHistories() {
        try {
            QuerySnapshot snapshot = Tasks.await(context.get());
            List<RegistrationHistory> histories = new ArrayList<>();
            for (QueryDocumentSnapshot document : snapshot) {
                RegistrationHistory history = document.toObject(RegistrationHistory.class);
                if (history != null) {
                    histories.add(history);
                }
            }
            return histories;
        } catch (ExecutionException | InterruptedException e) {
            Log.e("Firestore", "Error getting all registration histories", e);
            return new ArrayList<>();
        }
    }

    public List<RegistrationHistory> getRegistrationHistoriesByEventId(String eventId) {
        try {
            QuerySnapshot snapshot = Tasks.await(context.whereEqualTo("eventId", eventId).get());
            List<RegistrationHistory> histories = new ArrayList<>();
            for (QueryDocumentSnapshot document : snapshot) {
                RegistrationHistory history = document.toObject(RegistrationHistory.class);
                if (history != null) {
                    histories.add(history);
                }
            }
            return histories;
        } catch (ExecutionException | InterruptedException e) {
            Log.e("Firestore", "Error getting registration histories by event ID", e);
            return new ArrayList<>();
        }
    }

    public List<RegistrationHistory> getRegistrationHistoriesByUserId(String userId) {
        try {
            QuerySnapshot snapshot = Tasks.await(context.whereEqualTo("userId", userId).get());
            List<RegistrationHistory> histories = new ArrayList<>();
            for (QueryDocumentSnapshot document : snapshot) {
                RegistrationHistory history = document.toObject(RegistrationHistory.class);
                if (history != null) {
                    histories.add(history);
                }
            }
            return histories;
        } catch (ExecutionException | InterruptedException e) {
            Log.e("Firestore", "Error getting registration histories by user ID", e);
            return new ArrayList<>();
        }
    }

    public void saveRegistrationHistory(RegistrationHistory history, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        if (history == null) {
            onFailure.onFailure(new IllegalArgumentException("Registration history cannot be null"));
            return;
        }
        // Use composite key: eventId_userId
        String docId = history.getEventId() + "_" + history.getUserId();
        context.document(docId)
                .set(history)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public void updateRegistrationHistory(@NonNull RegistrationHistory history,
                                         @NonNull OnSuccessListener<Void> onSuccess,
                                         @NonNull OnFailureListener onFailure) {
        // Use composite key: eventId_userId
        String docId = history.getEventId() + "_" + history.getUserId();
        context.document(docId)
                .set(history, SetOptions.merge()) // merge only changed fields
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Registration history updated: eventId=" + history.getEventId() + ", userId=" + history.getUserId());
                    onSuccess.onSuccess(aVoid);
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error updating registration history", e);
                    onFailure.onFailure(e);
                });
    }

    public void deleteRegistrationHistoryByEventIdAndUserId(String eventId, String userId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        String docId = eventId + "_" + userId;
        context.document(docId)
                .delete()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public boolean deleteRegistrationHistoryByEventIdAndUserId(String eventId, String userId) {
        try {
            String docId = eventId + "_" + userId;
            Tasks.await(context.document(docId).delete());
            Log.d("Firestore", "Registration history deleted: eventId=" + eventId + ", userId=" + userId);
            return true;
        } catch (ExecutionException | InterruptedException e) {
            Log.e("Firestore", "Error deleting registration history", e);
            return false;
        }
    }
}
