package com.quantiagents.app.Services;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.quantiagents.app.Repository.FireBaseRepository;
import com.quantiagents.app.Repository.RegistrationHistoryRepository;
import com.quantiagents.app.models.RegistrationHistory;

import java.util.List;

public class RegistrationHistoryService {

    private final RegistrationHistoryRepository repository;

    public RegistrationHistoryService(Context context) {
        // RegistrationHistoryService instantiates its own repositories internally
        FireBaseRepository fireBaseRepository = new FireBaseRepository();
        this.repository = new RegistrationHistoryRepository(fireBaseRepository);
    }

    public RegistrationHistory getRegistrationHistoryByEventIdAndUserId(String eventId, String userId) {
        return repository.getRegistrationHistoryByEventIdAndUserId(eventId, userId);
    }

    public List<RegistrationHistory> getAllRegistrationHistories() {
        return repository.getAllRegistrationHistories();
    }

    public List<RegistrationHistory> getRegistrationHistoriesByEventId(String eventId) {
        return repository.getRegistrationHistoriesByEventId(eventId);
    }

    public List<RegistrationHistory> getRegistrationHistoriesByUserId(String userId) {
        return repository.getRegistrationHistoriesByUserId(userId);
    }

    public void saveRegistrationHistory(RegistrationHistory history, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        // Validate registration history before saving
        if (history == null) {
            onFailure.onFailure(new IllegalArgumentException("Registration history cannot be null"));
            return;
        }
        if (history.getEventRegistrationStatus() == null) {
            onFailure.onFailure(new IllegalArgumentException("Registration status is required"));
            return;
        }
        if (history.getRegisteredAt() == null) {
            onFailure.onFailure(new IllegalArgumentException("Registration date is required"));
            return;
        }
        
        repository.saveRegistrationHistory(history,
                aVoid -> {
                    Log.d("App", "Registration history saved: eventId=" + history.getEventId() + ", userId=" + history.getUserId());
                    onSuccess.onSuccess(aVoid);
                },
                e -> {
                    Log.e("App", "Failed to save registration history", e);
                    onFailure.onFailure(e);
                });
    }

    public void updateRegistrationHistory(@NonNull RegistrationHistory history,
                                         @NonNull OnSuccessListener<Void> onSuccess,
                                         @NonNull OnFailureListener onFailure) {
        // Validate registration history before updating
        if (history.getEventRegistrationStatus() == null) {
            onFailure.onFailure(new IllegalArgumentException("Registration status is required"));
            return;
        }
        
        repository.updateRegistrationHistory(history,
                aVoid -> {
                    Log.d("App", "Registration history updated: eventId=" + history.getEventId() + ", userId=" + history.getUserId());
                    onSuccess.onSuccess(aVoid);
                },
                e -> {
                    Log.e("App", "Failed to update registration history", e);
                    onFailure.onFailure(e);
                });
    }

    public void deleteRegistrationHistory(String eventId, String userId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        repository.deleteRegistrationHistoryByEventIdAndUserId(eventId, userId,
                aVoid -> {
                    Log.d("App", "Registration history deleted: eventId=" + eventId + ", userId=" + userId);
                    onSuccess.onSuccess(aVoid);
                },
                e -> {
                    Log.e("App", "Failed to delete registration history", e);
                    onFailure.onFailure(e);
                });
    }

    public boolean deleteRegistrationHistory(String eventId, String userId) {
        return repository.deleteRegistrationHistoryByEventIdAndUserId(eventId, userId);
    }

    /**
     * Asynchronously fetches all registrations for an event with the given status.
     *
     * @param eventId event id
     * @param status  status filter (use enumName: WAITING/SELECTED/CONFIRMED/CANCELED)
     * @param ok      receives list (possibly empty)
     * @param err     receives error
     */
    public void getByEventAndStatus(@NonNull String eventId,
                                    @NonNull String status,
                                    @NonNull OnSuccessListener<List<RegistrationHistory>> ok,
                                    @NonNull OnFailureListener err) {
        repository.getByEventAndStatus(eventId, status, ok, err);
    }

    /**
     * Asynchronously counts registrations for an event in a given status.
     *
     * @param eventId event id
     * @param status  status to count
     * @param ok      receives count
     * @param err     receives error
     */
    public void countByStatus(@NonNull String eventId,
                              @NonNull String status,
                              @NonNull OnSuccessListener<Integer> ok,
                              @NonNull OnFailureListener err) {
        repository.countByEventAndStatus(eventId, status, ok, err);
    }

    /**
     * Asynchronously updates {@code eventRegStatus} for a batch of users in an event.
     *
     * @param eventId   event id
     * @param userIds   target users
     * @param newStatus new status (enumName string)
     * @param ok        receives number of attempted updates
     * @param err       receives error
     */
    public void bulkUpdateStatus(@NonNull String eventId,
                                 @NonNull List<String> userIds,
                                 @NonNull String newStatus,
                                 @NonNull OnSuccessListener<Integer> ok,
                                 @NonNull OnFailureListener err) {
        repository.bulkUpdateStatus(eventId, userIds, newStatus, ok, err);
    }

}
