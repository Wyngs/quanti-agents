package com.quantiagents.app.Services;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.quantiagents.app.Repository.EventRepository;
import com.quantiagents.app.Repository.FireBaseRepository;
import com.quantiagents.app.models.Event;

import java.util.Collections;
import java.util.List;

public class EventService {

    private final EventRepository repository;

    public EventService(Context context) {
        // EventService instantiates its own repositories internally
        FireBaseRepository fireBaseRepository = new FireBaseRepository();
        this.repository = new EventRepository(fireBaseRepository);
    }

    public Event getEventById(String eventId) {
        return repository.getEventById(eventId);
    }

    public List<Event> getAllEvents() {
        return repository.getAllEvents();
    }

    public void saveEvent(Event event, OnSuccessListener<String> onSuccess, OnFailureListener onFailure) {
        // Validate event before saving
        if (event == null) {
            onFailure.onFailure(new IllegalArgumentException("Event cannot be null"));
            return;
        }
        if (event.getTitle() == null || event.getTitle().trim().isEmpty()) {
            onFailure.onFailure(new IllegalArgumentException("Event title is required"));
            return;
        }
        
        repository.saveEvent(event, 
                eventId -> {
                    Log.d("App", "Event saved: " + event.getEventId());
                    onSuccess.onSuccess(eventId);
                },
                e -> {
                    Log.e("App", "Failed to save event", e);
                    onFailure.onFailure(e);
                });
    }

    public void updateEvent(@NonNull Event event, 
                           @NonNull OnSuccessListener<Void> onSuccess, 
                           @NonNull OnFailureListener onFailure) {
        // Validate event before updating
        if (event.getTitle() == null || event.getTitle().trim().isEmpty()) {
            onFailure.onFailure(new IllegalArgumentException("Event title is required"));
            return;
        }
        
        repository.updateEvent(event,
                aVoid -> {
                    Log.d("App", "Event updated: " + event.getEventId());
                    onSuccess.onSuccess(aVoid);
                },
                e -> {
                    Log.e("App", "Failed to update event", e);
                    onFailure.onFailure(e);
                });
    }

    public void deleteEvent(String eventId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        repository.deleteEventById(eventId,
                aVoid -> {
                    Log.d("App", "Event deleted: " + eventId);
                    onSuccess.onSuccess(aVoid);
                },
                e -> {
                    Log.e("App", "Failed to delete event", e);
                    onFailure.onFailure(e);
                });
    }

    public boolean deleteEvent(String eventId) {
        return repository.deleteEventById(eventId);
    }


    /**
     * Asynchronously reads the organizer's selection quota for an {@code Event}.
     * <p>
     * The quota is stored as a Firestore field named {@code "selectionQuota"} on the
     * event document. If the document does not exist or the field is missing, this
     * method resolves with {@code 0}. Firestore returns integer fields as {@link Long},
     * so the value is converted to {@code int} before invoking the callback.
     *
     * @param eventId the Firestore ID of the event document
     * @param ok      invoked with the quota value (defaults to 0 if missing)
     * @param err     invoked if the read fails
     */
    public void getSelectionQuota(@NonNull String eventId,
                                  @NonNull OnSuccessListener<Integer> ok,
                                  @NonNull OnFailureListener err) {
        // Read the event doc and convert the Long to int (or 0 if absent).
        context.document(eventId)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.exists()) { ok.onSuccess(0); return; }
                    Long v = snap.getLong("selectionQuota");
                    ok.onSuccess(v == null ? 0 : v.intValue());
                })
                .addOnFailureListener(err);
    }

    /**
     * Asynchronously upserts the {@code selectionQuota} field on the event document.
     * <p>
     * Uses {@link SetOptions#merge()} so only this field is written, avoiding
     * accidental overwrites to other event fields.
     *
     * @param eventId the Firestore ID of the event document
     * @param quota   the number of seats intended to be filled through selection
     * @param ok      invoked when the write succeeds
     * @param err     invoked if the write fails
     */
    public void setSelectionQuota(@NonNull String eventId, int quota,
                                  @NonNull OnSuccessListener<Void> ok,
                                  @NonNull OnFailureListener err) {
        context.document(eventId)
                .set(Collections.singletonMap("selectionQuota", quota), SetOptions.merge())
                .addOnSuccessListener(ok)
                .addOnFailureListener(err);
    }

}
