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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class LotteryResultService {

    private final LotteryResultRepository repository;
    private final RegistrationHistoryService registrationHistoryService;
    private final EventService eventService;
    /// Region: internal status strings (use enum names to match Firestore)
    private static final String ST_WAITING   = constant.EventRegistrationStatus.WAITLIST.name();
    private static final String ST_SELECTED  = constant.EventRegistrationStatus.SELECTED.name();
    private static final String ST_CONFIRMED = constant.EventRegistrationStatus.CONFIRMED.name();
    private static final String ST_CANCELED  = constant.EventRegistrationStatus.CANCELLED.name();

    public LotteryResultService(Context context) {
        // LotteryResultService instantiates its own repositories internally
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
     * Ensures non-null result, non-empty eventId/entrantIds, and sets a timestamp if missing.
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
            result.setTimeStamp(new java.util.Date()); // default timestamp here (service rule)
        }

        repository.saveLotteryResult(result, aVoid -> {
            android.util.Log.d("App", "Lottery result saved for event: " + result.getEventId());
            onSuccess.onSuccess(aVoid);
        }, e -> {
            android.util.Log.e("App", "Failed to save lottery result", e);
            onFailure.onFailure(e);
        });
    }

    /**
     * Validates and updates an existing lottery result (merge write).
     * Requires timestamp (part of repo doc id) and non-empty eventId/entrantIds.
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
            android.util.Log.d("App", "Lottery result updated for event: " + result.getEventId());
            onSuccess.onSuccess(aVoid);
        }, e -> {
            android.util.Log.e("App", "Failed to update lottery result", e);
            onFailure.onFailure(e);
        });
    }

    public void deleteLotteryResult(Date timestamp, String eventId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
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
     * Draws {@code count} entrants from the {@code WAITING} pool for a given event,
     * promotes them to {@code SELECTED}, and persists a {@link LotteryResult} snapshot.
     * <p>
     * This method encapsulates randomness, status updates (batched), and result logging.
     * If {@code count <= 0} or the waiting pool is empty, a result with an empty list
     * is returned via {@code ok}.
     *
     * @param eventId event identifier
     * @param count   number of entrants to select (capped to waiting size)
     * @param ok      receives the persisted {@link LotteryResult} (possibly empty)
     * @param err     receives any failure while reading/updating/saving
     */
    public void drawLottery(@NonNull String eventId, int count,
                            @NonNull OnSuccessListener<LotteryResult> ok,
                            @NonNull OnFailureListener err) {
        if (count <= 0) {
            ok.onSuccess(new LotteryResult(eventId, new ArrayList<>()));
            return;
        }

        // 1) Load WAITING registrations
        registrationHistoryService.getByEventAndStatus(
                eventId, ST_WAITING,
                waitingList -> {
                    if (waitingList.isEmpty()) {
                        ok.onSuccess(new LotteryResult(eventId, new ArrayList<>()));
                        return;
                    }

                    // 2) Shuffle and pick N
                    List<String> waitingIds = new ArrayList<>();
                    for (RegistrationHistory rh : waitingList) waitingIds.add(rh.getUserId());
                    Collections.shuffle(waitingIds, new Random());
                    List<String> picked = waitingIds.subList(0, Math.min(count, waitingIds.size()));

                    // 3) Promote to SELECTED in a single batch
                    registrationHistoryService.bulkUpdateStatus(
                            eventId, picked, ST_SELECTED,
                            updated -> {
                                // 4) Save a LotteryResult snapshot (timestamped in repo)
                                LotteryResult result = new LotteryResult(eventId, new ArrayList<>(picked));
                                repository.saveLotteryResult(result,
                                        v -> ok.onSuccess(result),
                                        err);
                            },
                            err);
                },
                err);
    }

    /**
     * Computes open selection slots as {@code quota - (SELECTED + CONFIRMED)} and,
     * if positive, draws that many entrants from {@code WAITING}. If no slots are open,
     * returns an empty {@link LotteryResult}.
     *
     * @param eventId event identifier
     * @param ok      receives a {@link LotteryResult} (possibly empty)
     * @param err     receives any failure while reading or updating
     */
    public void refillCanceledSlots(@NonNull String eventId,
                                    @NonNull OnSuccessListener<LotteryResult> ok,
                                    @NonNull OnFailureListener err) {

        // Step 1: fetch quota
        eventService.getSelectionQuota(eventId, quota ->

                // Step 2: count SELECTED
                registrationHistoryService.countByStatus(eventId, ST_SELECTED, selectedCount ->

                        // Step 3: count CONFIRMED
                        registrationHistoryService.countByStatus(eventId, ST_CONFIRMED, confirmedCount -> {
                            int open = Math.max(0, quota - (selectedCount + confirmedCount));
                            if (open <= 0) {
                                ok.onSuccess(new LotteryResult(eventId, new ArrayList<>()));
                            } else {
                                // Step 4: draw up to 'open' from WAITING
                                drawLottery(eventId, open, ok, err);
                            }
                        }, err), err), err);
    }

    /**
     * Cancels {@code SELECTED} users who missed a response deadline.
     * <p>
     * Very simple Part-3 rule: if {@code registeredAt} is older than {@code deadlineEpochMs},
     * that user is considered stale and is moved to {@code CANCELED}.
     *
     * @param eventId          event identifier
     * @param deadlineEpochMs  epoch millis cutoff
     * @param okCanceledCount  receives the number of users moved to CANCELED
     * @param err              receives any failure during read or batch update
     */
    public void cancelNonResponders(@NonNull String eventId, long deadlineEpochMs,
                                    @NonNull OnSuccessListener<Integer> okCanceledCount,
                                    @NonNull OnFailureListener err) {
        // Load current SELECTED
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

                    // Bulk move to CANCELED
                    registrationHistoryService.bulkUpdateStatus(
                            eventId, stale, ST_CANCELED, okCanceledCount, err);
                },
                err);
    }

    /**
     * Compatibility wrapper for existing callers that expect a "run lottery" API.
     * <p>
     * New behavior: samples from {@code WAITING} and promotes to {@code SELECTED}.
     * Internally delegates to {@link #drawLottery(String, int, OnSuccessListener, OnFailureListener)}.
     *
     * @param eventId           event identifier
     * @param numberOfEntrants  number of entrants to draw
     * @param onSuccess         receives the {@link LotteryResult}
     * @param onFailure         receives any failure
     */
    public void runLottery(String eventId, int numberOfEntrants,
                           OnSuccessListener<LotteryResult> onSuccess,
                           OnFailureListener onFailure) {
        drawLottery(eventId, numberOfEntrants, onSuccess, onFailure);
    }

}
