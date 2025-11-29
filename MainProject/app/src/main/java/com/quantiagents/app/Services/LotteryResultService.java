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
import com.quantiagents.app.models.Notification;
import com.quantiagents.app.models.RegistrationHistory;
import com.quantiagents.app.models.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service layer for LotteryResult operations and running lotteries.
 */
public class LotteryResultService {

    private final LotteryResultRepository repository;
    private final RegistrationHistoryService registrationHistoryService;
    private final EventService eventService;
    private final NotificationService notificationService;
    private final UserService userService;

    public LotteryResultService(Context context) {
        FireBaseRepository fireBaseRepository = new FireBaseRepository();
        this.repository = new LotteryResultRepository(fireBaseRepository);
        this.registrationHistoryService = new RegistrationHistoryService(context);
        this.eventService = new EventService(context);
        this.notificationService = new NotificationService(context);
        this.userService = new UserService(context);
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
     * 4. Updates their status to SELECTED (and waits for all updates to complete).
     * 5. Syncs Event lists.
     * 6. Saves the LotteryResult and triggers onSuccess.
     *
     * This method must be called OFF the main thread because it uses
     * synchronous Firestore reads via EventService / RegistrationHistoryService.
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

        // 1. Verify event (synchronous read; caller must be off main thread)
        Event event = eventService.getEventById(eventId);
        if (event == null) {
            onFailure.onFailure(new IllegalArgumentException("Event not found with ID: " + eventId));
            return;
        }

        // 2. Get Waiting List
        List<RegistrationHistory> allRegistrations =
                registrationHistoryService.getRegistrationHistoriesByEventId(eventId);
        List<RegistrationHistory> waitingList = new ArrayList<>();
        if (allRegistrations != null) {
            for (RegistrationHistory reg : allRegistrations) {
                if (reg != null &&
                        reg.getEventRegistrationStatus() == constant.EventRegistrationStatus.WAITLIST) {
                    waitingList.add(reg);
                }
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

        // 4. Update Status to SELECTED, but wait for all async updates
        AtomicInteger pending = new AtomicInteger(winners.size());
        List<Exception> errors = new ArrayList<>();

        for (RegistrationHistory winner : winners) {
            winner.setEventRegistrationStatus(constant.EventRegistrationStatus.SELECTED);
            winnerIds.add(winner.getUserId());

            registrationHistoryService.updateRegistrationHistory(
                    winner,
                    aVoid -> {
                        Log.d("Lottery", "User " + winner.getUserId() + " selected");
                        if (pending.decrementAndGet() == 0) {
                            // All updates done
                            if (!errors.isEmpty()) {
                                onFailure.onFailure(errors.get(0));
                            } else {
                                finalizeLottery(event, eventId, winnerIds, onSuccess, onFailure);
                            }
                        }
                    },
                    e -> {
                        Log.e("Lottery", "Failed to update user status", e);
                        errors.add(e);
                        if (pending.decrementAndGet() == 0) {
                            if (!errors.isEmpty()) {
                                onFailure.onFailure(errors.get(0));
                            } else {
                                finalizeLottery(event, eventId, winnerIds, onSuccess, onFailure);
                            }
                        }
                    }
            );
        }
    }

    /**
     * After all RegistrationHistory updates are complete, sync the Event lists,
     * update the Event document, then save the LotteryResult and finally
     * call onSuccess.
     */
    private void finalizeLottery(Event event,
                                 String eventId,
                                 List<String> winnerIds,
                                 OnSuccessListener<LotteryResult> onSuccess,
                                 OnFailureListener onFailure) {

        // Sync Event lists (remove winners from waiting, add to selected)
        List<String> evWaiting = event.getWaitingList();
        if (evWaiting == null) evWaiting = new ArrayList<>();

        List<String> evSelected = event.getSelectedList();
        if (evSelected == null) evSelected = new ArrayList<>();

        for (String winnerId : winnerIds) {
            evWaiting.remove(winnerId);
            if (!evSelected.contains(winnerId)) {
                evSelected.add(winnerId);
            }
        }
        event.setWaitingList(evWaiting);
        event.setSelectedList(evSelected);
        event.setFirstLotteryDone(true); // Mark as done

        // First update the event document
        eventService.updateEvent(
                event,
                aVoid -> {
                    Log.d("Lottery", "Event lists synced for event: " + eventId);

                    // Then save the LotteryResult
                    LotteryResult result = new LotteryResult(eventId, winnerIds);
                    repository.saveLotteryResult(
                            result,
                            v -> {
                                Log.d("App", "Lottery completed for event: " + eventId);
                                // Send notifications to all winners
                                sendLotteryWinNotifications(event, winnerIds);
                                onSuccess.onSuccess(result);
                            },
                            onFailure
                    );
                },
                e -> {
                    Log.e("Lottery", "Failed to sync event lists", e);
                    onFailure.onFailure(e);
                }
        );
    }

    /**
     * Refills canceled slots by drawing new winners from the waiting list.
     * Calculates open slots based on WaitingListLimit - (Selected + Confirmed).
     *
     * This method should also be called off the main thread, because it uses
     * EventService.getEventById synchronously.
     */
    public void refillCanceledSlots(String eventId,
                                    OnSuccessListener<LotteryResult> onSuccess,
                                    OnFailureListener onFailure) {
        Event event = eventService.getEventById(eventId);
        if (event == null) {
            onFailure.onFailure(new IllegalArgumentException("Event not found"));
            return;
        }

        List<RegistrationHistory> allRegs =
                registrationHistoryService.getRegistrationHistoriesByEventId(eventId);
        int occupiedCount = 0;
        if (allRegs != null) {
            for (RegistrationHistory reg : allRegs) {
                if (reg.getEventRegistrationStatus() == constant.EventRegistrationStatus.SELECTED ||
                        reg.getEventRegistrationStatus() == constant.EventRegistrationStatus.CONFIRMED) {
                    occupiedCount++;
                }
            }
        }

        // Assuming waitingListLimit acts as the total capacity for the event roster
        double limit = event.getWaitingListLimit();
        // If limit is 0 (unlimited) or very large, we might default to capacity.
        if (limit <= 0) limit = event.getEventCapacity();

        int openSlots = (int) limit - occupiedCount;

        if (openSlots <= 0) {
            onFailure.onFailure(new IllegalStateException("No open slots to refill (Limit reached)."));
            return;
        }

        // Reuse the runLottery logic to draw 'openSlots' new people
        runLottery(eventId, openSlots, onSuccess, onFailure);
    }

    /**
     * Sends lottery win notifications to all winners.
     */
    private void sendLotteryWinNotifications(Event event, List<String> winnerIds) {
        if (event == null || winnerIds == null || winnerIds.isEmpty()) return;

        String eventId = event.getEventId();
        String organizerId = event.getOrganizerId();
        String eventName = event.getTitle() != null ? event.getTitle() : "Event";

        if (eventId == null || organizerId == null) return;

        int eventIdInt = Math.abs(eventId.hashCode());
        int organizerIdInt = Math.abs(organizerId.hashCode());

        for (String winnerId : winnerIds) {
            if (winnerId == null || winnerId.trim().isEmpty()) continue;

            int winnerIdInt = Math.abs(winnerId.hashCode());

            // Notification for the winner (GOOD type)
            String status = "User Won Lottery";
            String details = "Congratulations you have won the lottery for Event : " + eventName + ". Please accept or decline the invitation.";
            Notification winnerNotification = new Notification(
                    0, // Auto-generate ID
                    constant.NotificationType.GOOD,
                    winnerIdInt,
                    organizerIdInt, // senderId = eventOrganizerId
                    eventIdInt,
                    status,
                    details
            );

            notificationService.saveNotification(winnerNotification,
                    aVoid -> Log.d("Lottery", "Notification sent to winner: " + winnerId),
                    e -> Log.e("Lottery", "Failed to send notification to winner: " + winnerId, e)
            );
        }
    }
}