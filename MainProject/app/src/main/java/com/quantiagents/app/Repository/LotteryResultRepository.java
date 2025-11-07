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
     * Writes a lottery result document.
     * Uses composite id "yyyyMMddHHmmss_eventId". Assumes result has a timestamp.
     */
    public void saveLotteryResult(@NonNull LotteryResult result,
                                  @NonNull OnSuccessListener<Void> onSuccess,
                                  @NonNull OnFailureListener onFailure) {
        String docId = createDocumentId(result.getTimeStamp(), result.getEventId());
        context.document(docId)
                .set(result)
                .addOnSuccessListener(v -> {
                    android.util.Log.d("Firestore", "Lottery result saved: " + docId);
                    onSuccess.onSuccess(v);
                })
                .addOnFailureListener(onFailure);
    }

    /**
     * Merges fields into an existing lottery result document.
     * Uses the same composite id (timestamp + eventId). Assumes timestamp is present.
     */
    public void updateLotteryResult(@NonNull LotteryResult result,
                                    @NonNull OnSuccessListener<Void> onSuccess,
                                    @NonNull OnFailureListener onFailure) {
        String docId = createDocumentId(result.getTimeStamp(), result.getEventId());
        context.document(docId)
                .set(result, SetOptions.merge())
                .addOnSuccessListener(v -> {
                    android.util.Log.d("Firestore", "Lottery result updated: " + docId);
                    onSuccess.onSuccess(v);
                })
                .addOnFailureListener(onFailure);
    }

    public void deleteLotteryResultByTimestampAndEventId(Date timestamp, String eventId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        String docId = createDocumentId(timestamp, eventId);
        context.document(docId)
                .delete()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

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
