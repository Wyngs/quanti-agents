package com.quantiagents.app.Services;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.quantiagents.app.Constants.constant;
import com.quantiagents.app.Repository.FireBaseRepository;
import com.quantiagents.app.Repository.LotteryResultRepository;
import com.quantiagents.app.models.LotteryResult;
import com.quantiagents.app.models.RegistrationHistory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class LotteryResultService {

    private final LotteryResultRepository repository;
    private final RegistrationHistoryService registrationHistoryService;
    private final EventService eventService;

    public LotteryResultService(Context context) {
        // LotteryResultService instantiates its own repositories internally
        FireBaseRepository fireBaseRepository = new FireBaseRepository();
        this.repository = new LotteryResultRepository(fireBaseRepository);
        this.registrationHistoryService = new RegistrationHistoryService(context);
        this.eventService = new EventService(context);
    }

    public LotteryResult getLotteryResultByTimestampAndEventId(LocalDateTime timestamp, int eventId) {
        return repository.getLotteryResultByTimestampAndEventId(timestamp, eventId);
    }

    public List<LotteryResult> getLotteryResultsByEventId(int eventId) {
        return repository.getLotteryResultsByEventId(eventId);
    }

    public List<LotteryResult> getAllLotteryResults() {
        return repository.getAllLotteryResults();
    }

    public void saveLotteryResult(LotteryResult result, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        // Validate lottery result before saving
        if (result == null) {
            onFailure.onFailure(new IllegalArgumentException("Lottery result cannot be null"));
            return;
        }
        if (result.getEventId() <= 0) {
            onFailure.onFailure(new IllegalArgumentException("Event ID is required"));
            return;
        }
        if (result.getEntrantIds() == null || result.getEntrantIds().isEmpty()) {
            onFailure.onFailure(new IllegalArgumentException("Entrant IDs cannot be null or empty"));
            return;
        }
        
        repository.saveLotteryResult(result,
                aVoid -> {
                    Log.d("App", "Lottery result saved for event: " + result.getEventId());
                    onSuccess.onSuccess(aVoid);
                },
                e -> {
                    Log.e("App", "Failed to save lottery result", e);
                    onFailure.onFailure(e);
                });
    }

    public void updateLotteryResult(@NonNull LotteryResult result,
                                   @NonNull OnSuccessListener<Void> onSuccess,
                                   @NonNull OnFailureListener onFailure) {
        // Validate lottery result before updating
        if (result.getEventId() <= 0) {
            onFailure.onFailure(new IllegalArgumentException("Event ID is required"));
            return;
        }
        if (result.getEntrantIds() == null || result.getEntrantIds().isEmpty()) {
            onFailure.onFailure(new IllegalArgumentException("Entrant IDs cannot be null or empty"));
            return;
        }
        
        repository.updateLotteryResult(result,
                aVoid -> {
                    Log.d("App", "Lottery result updated for event: " + result.getEventId());
                    onSuccess.onSuccess(aVoid);
                },
                e -> {
                    Log.e("App", "Failed to update lottery result", e);
                    onFailure.onFailure(e);
                });
    }

    public void deleteLotteryResult(LocalDateTime timestamp, int eventId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        repository.deleteLotteryResultByTimestampAndEventId(timestamp, eventId,
                aVoid -> {
                    Log.d("App", "Lottery result deleted: timestamp=" + timestamp + ", eventId=" + eventId);
                    onSuccess.onSuccess(aVoid);
                },
                e -> {
                    Log.e("App", "Failed to delete lottery result", e);
                    onFailure.onFailure(e);
                });
    }

    public boolean deleteLotteryResult(LocalDateTime timestamp, int eventId) {
        return repository.deleteLotteryResultByTimestampAndEventId(timestamp, eventId);
    }

    /**
     * Runs a lottery for a given event, randomly selecting the specified number of entrants
     * from confirmed registrations for that event.
     * 
     * @param eventId The ID of the event to run the lottery for
     * @param numberOfEntrants The number of entrants to select
     * @param onSuccess Callback for successful lottery execution
     * @param onFailure Callback for failed lottery execution
     */
    public void runLottery(int eventId, int numberOfEntrants,
                          OnSuccessListener<LotteryResult> onSuccess,
                          OnFailureListener onFailure) {
        // Validate inputs
        if (eventId <= 0) {
            onFailure.onFailure(new IllegalArgumentException("Event ID must be positive"));
            return;
        }
        if (numberOfEntrants <= 0) {
            onFailure.onFailure(new IllegalArgumentException("Number of entrants must be positive"));
            return;
        }

        // Verify event exists
        if (eventService.getEventById(eventId) == null) {
            onFailure.onFailure(new IllegalArgumentException("Event not found with ID: " + eventId));
            return;
        }

        // Get all registration histories for this event
        List<RegistrationHistory> registrations = registrationHistoryService.getRegistrationHistoriesByEventId(eventId);
        
        // Filter for confirmed registrations only
        List<Integer> confirmedUserIds = new ArrayList<>();
        for (RegistrationHistory registration : registrations) {
            if (registration.getEventRegistrationStatus() == constant.EventRegistrationStatus.CONFIRMED) {
                confirmedUserIds.add(registration.getUserId());
            }
        }

        // Check if we have enough confirmed registrations
        if (confirmedUserIds.isEmpty()) {
            onFailure.onFailure(new IllegalArgumentException("No confirmed registrations found for event: " + eventId));
            return;
        }

        if (confirmedUserIds.size() < numberOfEntrants) {
            Log.w("App", "Requested " + numberOfEntrants + " entrants, but only " + confirmedUserIds.size() + " confirmed registrations available. Selecting all available.");
            numberOfEntrants = confirmedUserIds.size();
        }

        // Randomly select entrants
        List<Integer> selectedEntrants = new ArrayList<>(confirmedUserIds);
        Collections.shuffle(selectedEntrants, new Random());
        selectedEntrants = selectedEntrants.subList(0, numberOfEntrants);

        // Create lottery result
        LotteryResult result = new LotteryResult(eventId, selectedEntrants);

        // Save lottery result
        repository.saveLotteryResult(result,
                aVoid -> {
                    Log.d("App", "Lottery completed for event: " + eventId + " with " + selectedEntrants.size() + " entrants");
                    onSuccess.onSuccess(result);
                },
                e -> {
                    Log.e("App", "Failed to save lottery result", e);
                    onFailure.onFailure(e);
                });
    }
}
