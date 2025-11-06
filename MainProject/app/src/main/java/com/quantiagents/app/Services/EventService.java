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
     * Returns the event's selection quota as an int (defaults to 0 if missing).
     * Delegates storage to the repository and converts Firestore's Long→int.
     */
    public void getSelectionQuota(@NonNull String eventId,
                                  @NonNull OnSuccessListener<Integer> ok,
                                  @NonNull OnFailureListener err) {
        repository.getSelectionQuota(eventId,
                v -> ok.onSuccess(v == null ? 0 : v.intValue()),
                err);
    }

    /**
     * Validates and writes the selection quota.
     * Rejects negatives; actual write is delegated to the repository.
     */
    public void setSelectionQuota(@NonNull String eventId, int quota,
                                  @NonNull OnSuccessListener<Void> ok,
                                  @NonNull OnFailureListener err) {
        if (quota < 0) {
            err.onFailure(new IllegalArgumentException("Selection quota must be >= 0"));
            return;
        }
        repository.setSelectionQuota(eventId, quota, ok, err);
    }

}
