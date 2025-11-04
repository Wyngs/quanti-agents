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
import com.quantiagents.app.models.LotteryResult;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class LotteryResultRepository {

    private final CollectionReference context;

    public LotteryResultRepository(FireBaseRepository fireBaseRepository) {
        this.context = fireBaseRepository.getLotteryCollectionRef();
    }

    /**
     * Helper method to create document ID from timestamp and eventId
     * Format: timestamp_eventId (e.g., "20250101120000_123")
     */
    private String createDocumentId(LocalDateTime timestamp, int eventId) {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
        // Format timestamp as yyyyMMddHHmmss (year, month, day, hour, minute, second)
        String timestampStr = timestamp.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return timestampStr + "_" + eventId;
    }

    public LotteryResult getLotteryResultByTimestampAndEventId(LocalDateTime timestamp, int eventId) {
        try {
            String docId = createDocumentId(timestamp, eventId);
            DocumentSnapshot snapshot = Tasks.await(context.document(docId).get());
            if (snapshot.exists()) {
                return snapshot.toObject(LotteryResult.class);
            } else {
                Log.d("Firestore", "No lottery result found for timestamp: " + timestamp + ", event ID: " + eventId);
                return null;
            }
        } catch (ExecutionException | InterruptedException e) {
            Log.e("Firestore", "Error getting lottery result", e);
            return null;
        }
    }

    public List<LotteryResult> getLotteryResultsByEventId(int eventId) {
        try {
            // Query by eventId field since document ID contains timestamp
            QuerySnapshot snapshot = Tasks.await(context.whereEqualTo("eventId", eventId).get());
            List<LotteryResult> results = new ArrayList<>();
            for (QueryDocumentSnapshot document : snapshot) {
                LotteryResult result = document.toObject(LotteryResult.class);
                if (result != null) {
                    results.add(result);
                }
            }
            return results;
        } catch (ExecutionException | InterruptedException e) {
            Log.e("Firestore", "Error getting lottery results by event ID", e);
            return new ArrayList<>();
        }
    }

    public List<LotteryResult> getAllLotteryResults() {
        try {
            QuerySnapshot snapshot = Tasks.await(context.get());
            List<LotteryResult> results = new ArrayList<>();
            for (QueryDocumentSnapshot document : snapshot) {
                LotteryResult result = document.toObject(LotteryResult.class);
                if (result != null) {
                    results.add(result);
                }
            }
            return results;
        } catch (ExecutionException | InterruptedException e) {
            Log.e("Firestore", "Error getting all lottery results", e);
            return new ArrayList<>();
        }
    }

    public void saveLotteryResult(LotteryResult result, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        if (result == null) {
            onFailure.onFailure(new IllegalArgumentException("Lottery result cannot be null"));
            return;
        }
        if (result.getTimeStamp() == null) {
            result.setTimeStamp(LocalDateTime.now());
        }
        // Use composite key: timestamp_eventId
        String docId = createDocumentId(result.getTimeStamp(), result.getEventId());
        context.document(docId)
                .set(result)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Lottery result saved with ID: " + docId);
                    onSuccess.onSuccess(aVoid);
                })
                .addOnFailureListener(onFailure);
    }

    public void updateLotteryResult(@NonNull LotteryResult result,
                                   @NonNull OnSuccessListener<Void> onSuccess,
                                   @NonNull OnFailureListener onFailure) {
        if (result.getTimeStamp() == null) {
            onFailure.onFailure(new IllegalArgumentException("Timestamp is required for update"));
            return;
        }
        // Use composite key: timestamp_eventId
        String docId = createDocumentId(result.getTimeStamp(), result.getEventId());
        context.document(docId)
                .set(result, SetOptions.merge()) // merge only changed fields
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Lottery result updated with ID: " + docId);
                    onSuccess.onSuccess(aVoid);
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error updating lottery result", e);
                    onFailure.onFailure(e);
                });
    }

    public void deleteLotteryResultByTimestampAndEventId(LocalDateTime timestamp, int eventId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        String docId = createDocumentId(timestamp, eventId);
        context.document(docId)
                .delete()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public boolean deleteLotteryResultByTimestampAndEventId(LocalDateTime timestamp, int eventId) {
        try {
            String docId = createDocumentId(timestamp, eventId);
            Tasks.await(context.document(docId).delete());
            Log.d("Firestore", "Lottery result deleted with ID: " + docId);
            return true;
        } catch (ExecutionException | InterruptedException e) {
            Log.e("Firestore", "Error deleting lottery result", e);
            return false;
        }
    }
}
