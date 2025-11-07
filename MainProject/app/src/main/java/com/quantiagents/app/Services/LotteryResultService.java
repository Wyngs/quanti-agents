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

public class LotteryResultService {

    private final LotteryResultRepository repository;
    private final RegistrationHistoryService registrationHistoryService;
    private final EventService eventService;

    // Region: internal status strings (use enum names to match Firestore)
    private static final String ST_WAITING    = constant.EventRegistrationStatus.WAITLIST.name();
    private static final String ST_SELECTED   = constant.EventRegistrationStatus.SELECTED.name();
    private static final String ST_CONFIRMED  = constant.EventRegistrationStatus.CONFIRMED.name();
    private static final String ST_CANCELLED  = constant.EventRegistrationStatus.CANCELLED.name();

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

    /**
     * Validates and persists a lottery result snapshot.
     */
    public void saveLotteryResult(@NonNull LotteryResult result,
                                  @NonNull OnSuccessListener<Void> onSuccess,
                                  @NonNull OnFailureListener onFailure) {
        if (result == null) { onFailure.onFailure(new IllegalArgumentException("Lottery result cannot be null")); return; }
        if (result.getEventId() == null || result.getEventId().trim().isEmpty()) {
            onFailure.onFailure(new IllegalArgumentException("Event ID is required")); return;
        }
        if (result.getEntrantIds() == null || result.getEntrantIds().isEmpty()) {
            onFailure.onFailure(new IllegalArgumentException("Entrant IDs cannot be null or empty")); return;
        }
        if (result.getTimeStamp() == null) {
            result.setTimeStamp(new java.util.Date());
        }

        repository.saveLotteryResult(result, aVoid -> {
            Log.d("App", "Lottery result saved for event: " + result.getEventId());
            onSuccess.onSuccess(aVoid);
        }, e -> {
            Log.e("App", "Failed to save lottery result", e);
            onFailure.onFailure(e);
        });
    }

    /**
     * Validates and updates an existing lottery result (merge write).
     */
    public void updateLotteryResult(@NonNull LotteryResult result,
                                    @NonNull OnSuccessListener<Void> onSuccess,
                                    @NonNull OnFailureListener onFailure) {
        if (result.getEventId() == null || result.getEventId().trim().isEmpty()) {
            onFailure.onFailure(new IllegalArgumentException("Event ID is required")); return;
        }
        if (result.getEntrantIds() == null || result.getEntrantIds().isEmpty()) {
            onFailure.onFailure(new IllegalArgumentException("Entrant IDs cannot be null or empty")); return;
        }
        if (result.getTimeStamp() == null) {
            onFailure.onFailure(new IllegalArgumentException("Timestamp is required for update")); return;
        }

        repository.updateLotteryResult(result, aVoid -> {
            Log.d("App", "Lottery result updated for event: " + result.getEventId());
            onSuccess.onSuccess(aVoid);
        }, e -> {
            Log.e("App", "Failed to update lottery result", e);
            onFailure.onFailure(e);
        });
    }

    public void deleteLotteryResult(Date timestamp, String eventId,
                                    OnSuccessListener<Void> onSuccess,
                                    OnFailureListener onFailure) {
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

    public boolean deleteLotteryResult(Date timestamp, String eventId) {
        return repository.deleteLotteryResultByTimestampAndEventId(timestamp, eventId);
    }

    /**
     * Draws {@code count} entrants from WAITING, promotes to SELECTED,
     * and persists a LotteryResult. Draw size is capped by:
     *   - remaining open seats = waitingListLimit - (SELECTED + CONFIRMED)
     *   - waiting pool size
     */
    public void drawLottery(@NonNull String eventId, int count,
                            @NonNull OnSuccessListener<LotteryResult> ok,
                            @NonNull OnFailureListener err) {
        if (count <= 0) {
            ok.onSuccess(new LotteryResult(eventId, new ArrayList<>()));
            return;
        }

        // Load event to get waitingListLimit (selection quota)
        Event ev = eventService.getEventById(eventId);
        if (ev == null) {
            err.onFailure(new IllegalStateException("Event not found: " + eventId));
            return;
        }
        final int quota = (int) Math.floor(ev.getWaitingListLimit());

        // Count SELECTED then CONFIRMED to compute remaining open seats
        registrationHistoryService.countByStatus(eventId, ST_SELECTED, selectedCount ->
                        registrationHistoryService.countByStatus(eventId, ST_CONFIRMED, confirmedCount -> {
                            int open = Math.max(0, quota - (selectedCount + confirmedCount));
                            if (open <= 0) {
                                ok.onSuccess(new LotteryResult(eventId, new ArrayList<>()));
                                return;
                            }

                            // Load WAITING registrations
                            registrationHistoryService.getByEventAndStatus(
                                    eventId, ST_WAITING,
                                    waitingList -> {
                                        if (waitingList.isEmpty()) {
                                            ok.onSuccess(new LotteryResult(eventId, new ArrayList<>()));
                                            return;
                                        }

                                        // Build waiting ID list
                                        List<String> waitingIds = new ArrayList<>();
                                        for (RegistrationHistory rh : waitingList) waitingIds.add(rh.getUserId());

                                        // Shuffle and pick up to min(requested, waiting size, open)
                                        Collections.shuffle(waitingIds);
                                        int pickCount = Math.min(Math.min(count, waitingIds.size()), open);
                                        List<String> picked = new ArrayList<>(waitingIds.subList(0, pickCount));

                                        // Promote to SELECTED in a batch
                                        registrationHistoryService.bulkUpdateStatus(
                                                eventId, picked, ST_SELECTED,
                                                updatedCount -> {
                                                    // Save a snapshot
                                                    LotteryResult result = new LotteryResult(eventId, picked);
                                                    repository.saveLotteryResult(result,
                                                            v -> ok.onSuccess(result),
                                                            err);
                                                },
                                                err);
                                    },
                                    err);
                        }, err)
                , err);
    }

    /**
     * Fills open seats computed as quota - (SELECTED + CONFIRMED) by drawing from WAITING.
     * Uses Event.waitingListLimit as the quota (no selectionQuota field).
     */
    public void refillCanceledSlots(@NonNull String eventId,
                                    @NonNull OnSuccessListener<LotteryResult> ok,
                                    @NonNull OnFailureListener err) {
        // We can delegate to drawLottery(open) after computing 'open'.
        Event ev = eventService.getEventById(eventId);
        if (ev == null) {
            err.onFailure(new IllegalStateException("Event not found: " + eventId));
            return;
        }
        final int quota = (int) Math.floor(ev.getWaitingListLimit());

        registrationHistoryService.countByStatus(eventId, ST_SELECTED, selectedCount ->
                        registrationHistoryService.countByStatus(eventId, ST_CONFIRMED, confirmedCount -> {
                            int open = Math.max(0, quota - (selectedCount + confirmedCount));
                            if (open <= 0) {
                                ok.onSuccess(new LotteryResult(eventId, new ArrayList<>()));
                            } else {
                                drawLottery(eventId, open, ok, err);
                            }
                        }, err)
                , err);
    }

    /**
     * Cancels SELECTED users who missed a response deadline.
     */
    public void cancelNonResponders(@NonNull String eventId, long deadlineEpochMs,
                                    @NonNull OnSuccessListener<Integer> okCanceledCount,
                                    @NonNull OnFailureListener err) {
        registrationHistoryService.getByEventAndStatus(
                eventId, ST_SELECTED,
                selectedList -> {
                    List<String> stale = new ArrayList<>();
                    for (RegistrationHistory rh : selectedList) {
                        if (rh.getRegisteredAt() != null &&
                                rh.getRegisteredAt().getTime() < deadlineEpochMs) {
                            stale.add(rh.getUserId());
                        }
                    }
                    if (stale.isEmpty()) { okCanceledCount.onSuccess(0); return; }

                    registrationHistoryService.bulkUpdateStatus(
                            eventId, stale, ST_CANCELLED, okCanceledCount, err);
                },
                err);
    }

    /**
     * Backwards-compatible wrapper.
     */
    public void runLottery(String eventId, int numberOfEntrants,
                           OnSuccessListener<LotteryResult> onSuccess,
                           OnFailureListener onFailure) {
        drawLottery(eventId, numberOfEntrants, onSuccess, onFailure);
    }
}
