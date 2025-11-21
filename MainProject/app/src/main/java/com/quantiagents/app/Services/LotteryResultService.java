package com.quantiagents.app.Services;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.quantiagents.app.Constants.constant;
import com.quantiagents.app.Repository.FireBaseRepository;
import com.quantiagents.app.Repository.LotteryResultRepository;
import com.quantiagents.app.models.Event;
import com.quantiagents.app.models.LotteryResult;
import com.quantiagents.app.models.RegistrationHistory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class LotteryResultService {

    private final LotteryResultRepository repository;
    private final RegistrationHistoryService registrationHistoryService;
    private final EventService eventService;

    public LotteryResultService(Context context) {
        FireBaseRepository fireBaseRepository = new FireBaseRepository();
        this.repository = new LotteryResultRepository(fireBaseRepository);
        this.registrationHistoryService = new RegistrationHistoryService(context);
        this.eventService = new EventService(context);
    }

    public LotteryResult getLotteryResultByTimestampAndEventId(Date timestamp, String eventId) {
        return repository.getLotteryResultByTimestampAndEventId(timestamp, eventId);
    }

    public List<LotteryResult> getLotteryResultsByEventId(String eventId) {
        return repository.getLotteryResultsByEventId(eventId);
    }

    public List<LotteryResult> getAllLotteryResults() {
        return repository.getAllLotteryResults();
    }

    public void saveLotteryResult(LotteryResult result, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        if (result == null) {
            onFailure.onFailure(new IllegalArgumentException("Result cannot be null"));
            return;
        }
        repository.saveLotteryResult(result, onSuccess, onFailure);
    }

    public void deleteLotteryResult(Date timestamp, String eventId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        repository.deleteLotteryResultByTimestampAndEventId(timestamp, eventId, onSuccess, onFailure);
    }

    /**
     * Runs a lottery drawing from the WAITING list.
     * 1. Checks event exists.
     * 2. Filters registrations for WAITLIST status.
     * 3. Randomly selects 'numberOfEntrants'.
     * 4. Updates their status to SELECTED.
     * 5. Saves the LotteryResult.
     */
    public void runLottery(String eventId, int numberOfEntrants,
                           OnSuccessListener<LotteryResult> onSuccess,
                           OnFailureListener onFailure) {
        // Validate inputs
        if (eventId == null || eventId.trim().isEmpty()) {
            onFailure.onFailure(new IllegalArgumentException("Event ID is required"));
            return;
        }
        if (numberOfEntrants <= 0) {
            onFailure.onFailure(new IllegalArgumentException("Number of entrants must be positive"));
            return;
        }

        // 1. Verify event
        Event event = eventService.getEventById(eventId);
        if (event == null) {
            onFailure.onFailure(new IllegalArgumentException("Event not found with ID: " + eventId));
            return;
        }

        // 2. Get Waiting List
        List<RegistrationHistory> allRegistrations = registrationHistoryService.getRegistrationHistoriesByEventId(eventId);
        List<RegistrationHistory> waitingList = new ArrayList<>();
        for (RegistrationHistory reg : allRegistrations) {
            if (reg.getEventRegistrationStatus() == constant.EventRegistrationStatus.WAITLIST) {
                waitingList.add(reg);
            }
        }

        if (waitingList.isEmpty()) {
            onFailure.onFailure(new IllegalStateException("No entrants on the waiting list."));
            return;
        }

        // Cap the number of entrants if waitlist is smaller than requested
        int drawCount = Math.min(numberOfEntrants, waitingList.size());

        // 3. Random Selection
        Collections.shuffle(waitingList, new Random());
        List<RegistrationHistory> winners = waitingList.subList(0, drawCount);
        List<String> winnerIds = new ArrayList<>();

        // 4. Update Status to SELECTED
        for (RegistrationHistory winner : winners) {
            winner.setEventRegistrationStatus(constant.EventRegistrationStatus.SELECTED);
            winnerIds.add(winner.getUserId());
            // We perform individual updates here. In a production app, a batch write would be better.
            registrationHistoryService.updateRegistrationHistory(winner,
                    aVoid -> Log.d("Lottery", "User " + winner.getUserId() + " selected"),
                    e -> Log.e("Lottery", "Failed to update user status", e));
        }

        // 5. Save Result
        LotteryResult result = new LotteryResult(eventId, winnerIds);
        repository.saveLotteryResult(result,
                aVoid -> {
                    Log.d("App", "Lottery completed for event: " + eventId);
                    onSuccess.onSuccess(result);
                },
                onFailure);
    }

    /**
     * Refills canceled slots by drawing new winners from the waiting list.
     * Calculates open slots based on WaitingListLimit - (Selected + Confirmed).
     */
    public void refillCanceledSlots(String eventId,
                                    OnSuccessListener<LotteryResult> onSuccess,
                                    OnFailureListener onFailure) {
        Event event = eventService.getEventById(eventId);
        if (event == null) {
            onFailure.onFailure(new IllegalArgumentException("Event not found"));
            return;
        }

        List<RegistrationHistory> allRegs = registrationHistoryService.getRegistrationHistoriesByEventId(eventId);
        int occupiedCount = 0;
        for (RegistrationHistory reg : allRegs) {
            if (reg.getEventRegistrationStatus() == constant.EventRegistrationStatus.SELECTED ||
                    reg.getEventRegistrationStatus() == constant.EventRegistrationStatus.CONFIRMED) {
                occupiedCount++;
            }
        }

        // Assuming waitingListLimit acts as the total capacity for the event roster
        double limit = event.getWaitingListLimit();
        // If limit is 0 (unlimited) or very large, we might default to capacity or just draw 1.
        // For safety here, we'll use eventCapacity if limit is 0.
        if (limit <= 0) limit = event.getEventCapacity();

        int openSlots = (int) limit - occupiedCount;

        if (openSlots <= 0) {
            onFailure.onFailure(new IllegalStateException("No open slots to refill (Limit reached)."));
            return;
        }

        // Reuse the runLottery logic to draw 'openSlots' new people
        runLottery(eventId, openSlots, onSuccess, onFailure);
    }
}