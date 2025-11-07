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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

/**
 * Manages functions related to lottery result
 */
public class LotteryResultRepository {

    private final CollectionReference context;

    public LotteryResultRepository(FireBaseRepository fireBaseRepository) {
        this.context = fireBaseRepository.getLotteryCollectionRef();
    }

    /**
     * Helper method to create document ID from timestamp and eventId
     * Format: timestamp_eventId (e.g., "20250101120000_event123")
     */
    private String createDocumentId(Date timestamp, String eventId) {
        if (timestamp == null) {
            timestamp = new Date();
        }
        // Format timestamp as yyyyMMddHHmmss (year, month, day, hour, minute, second)
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
        String timestampStr = formatter.format(timestamp);
        return timestampStr + "_" + eventId;
    }

    /**
     * Locates lottery result by timestamp and event id
     * @param timestamp
     * Timestamp to locate
     * @param eventId
     * Event id to locate
     * @return
     * Returns lottery result if exists, null otherwise
     */
    public LotteryResult getLotteryResultByTimestampAndEventId(Date timestamp, String eventId) {
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

    /**
     * Locates list of lottery results from event id
     * @param eventId
     * Event id to locate
     * @return
     * Returns list of lottery results
     */
    public List<LotteryResult> getLotteryResultsByEventId(String eventId) {
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

    /**
     * Gets all Lottery Results
     * @return
     * Returns list of lottery results
     */
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

    /**
     * Saves a result to the firebase
     * @param result
     * Lottery Result to save
     * @param onSuccess
     * Calls function on success
     * @param onFailure
     * Calls function on failure
     */
    public void saveLotteryResult(LotteryResult result, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        if (result == null) {
            onFailure.onFailure(new IllegalArgumentException("Lottery result cannot be null"));
            return;
        }
        if (result.getTimeStamp() == null) {
            result.setTimeStamp(new Date());
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

    /**
     * Updates a result in the firebase
     * @param result
     * Lottery Result to update
     * @param onSuccess
     * Calls function on success
     * @param onFailure
     * Calls function on failure
     */
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

    /**
     * Deletes a result via timestamp and event id to the firebase
     * @param timestamp
     * Timestamp to locate
     * @param eventId
     * Event id to locate
     * @param onSuccess
     * Calls function on success
     * @param onFailure
     * Calls function on failure
     */
    public void deleteLotteryResultByTimestampAndEventId(Date timestamp, String eventId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        String docId = createDocumentId(timestamp, eventId);
        context.document(docId)
                .delete()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    /**
     * Deletes a result via timestamp and event id to the firebase
     * @param timestamp
     * Timestamp to locate
     * @param eventId
     * Event id to locate
     * @return
     * Returns boolean if success
     */
    public boolean deleteLotteryResultByTimestampAndEventId(Date timestamp, String eventId) {
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
