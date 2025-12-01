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

/**
 * Service layer for RegistrationHistory operations.
 * Handles business logic for saving, updating, and deleting event registration histories.
 */
public class RegistrationHistoryService {

    private final RegistrationHistoryRepository repository;

    /**
     * Constructor that initializes the RegistrationHistoryService with required dependencies.
     * RegistrationHistoryService instantiates its own repositories internally.
     *
     * @param context The Android context (currently unused but kept for consistency)
     */
    public RegistrationHistoryService(Context context) {
        // RegistrationHistoryService instantiates its own repositories internally
        FireBaseRepository fireBaseRepository = new FireBaseRepository();
        this.repository = new RegistrationHistoryRepository(fireBaseRepository);
    }

    /**
     * Retrieves a registration history by event ID and user ID synchronously.
     *
     * @param eventId The unique identifier of the event
     * @param userId The unique identifier of the user
     * @return The RegistrationHistory object, or null if not found
     */
    public RegistrationHistory getRegistrationHistoryByEventIdAndUserId(String eventId, String userId) {
        return repository.getRegistrationHistoryByEventIdAndUserId(eventId, userId);
    }

    /**
     * Retrieves all registration histories synchronously.
     *
     * @return List of all registration histories
     */
    public List<RegistrationHistory> getAllRegistrationHistories() {
        return repository.getAllRegistrationHistories();
    }

    /**
     * Retrieves all registration histories for a specific event synchronously.
     *
     * @param eventId The unique identifier of the event
     * @return List of registration histories for the event
     */
    public List<RegistrationHistory> getRegistrationHistoriesByEventId(String eventId) {
        return repository.getRegistrationHistoriesByEventId(eventId);
    }

    /**
     * Retrieves all registration histories for a specific user synchronously.
     *
     * @param userId The unique identifier of the user
     * @return List of registration histories for the user
     */
    public List<RegistrationHistory> getRegistrationHistoriesByUserId(String userId) {
        return repository.getRegistrationHistoriesByUserId(userId);
    }

    /**
     * Validates and saves a new registration history.
     *
     * @param history The registration history to save
     * @param onSuccess Callback invoked on successful save
     * @param onFailure Callback receiving validation or database errors
     */
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

    /**
     * Validates and updates an existing registration history.
     *
     * @param history The registration history to update (must have valid status)
     * @param onSuccess Callback invoked on successful update
     * @param onFailure Callback invoked if update fails or status is missing
     */
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

    /**
     * Deletes a registration history by event ID and user ID asynchronously.
     *
     * @param eventId The unique identifier of the event
     * @param userId The unique identifier of the user
     * @param onSuccess Callback invoked on successful deletion
     * @param onFailure Callback invoked if deletion fails
     */
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

    /**
     * Deletes a registration history by event ID and user ID synchronously.
     *
     * @param eventId The unique identifier of the event
     * @param userId The unique identifier of the user
     * @return True if registration history was successfully deleted, false otherwise
     */
    public boolean deleteRegistrationHistory(String eventId, String userId) {
        return repository.deleteRegistrationHistoryByEventIdAndUserId(eventId, userId);
    }
}
