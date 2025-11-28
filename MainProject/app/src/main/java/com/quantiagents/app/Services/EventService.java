package com.quantiagents.app.Services;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.quantiagents.app.Repository.EventRepository;
import com.quantiagents.app.Repository.FireBaseRepository;
import com.quantiagents.app.models.Event;

import java.util.List;

/**
 * Service layer for Event operations.
 * <p>
 * Acts as an intermediary between the UI controllers and the Data Repository.
 * Enforces business logic and validation rules (e.g., requiring a title for events).
 * </p>
 */
public class EventService {

    private final EventRepository repository;

    /**
     * Constructs an EventService.
     * @param context The application context.
     */
    public EventService(Context context) {
        // EventService instantiates its own repositories internally
        FireBaseRepository fireBaseRepository = new FireBaseRepository();
        this.repository = new EventRepository(fireBaseRepository);
    }

    /**
     * Retrieves an Event by ID synchronously.
     * @param eventId Unique identifier of the event.
     * @return The Event object or null if not found.
     */
    public Event getEventById(String eventId) {
        return repository.getEventById(eventId);
    }

    /**
     * Retrieves all events synchronously.
     * @return List of all events.
     */
    public List<Event> getAllEvents() {
        return repository.getAllEvents();
    }

    /**
     * Retrieves all events asynchronously.
     * @param onSuccess Callback receiving the list of events.
     * @param onFailure Callback receiving any error exception.
     */
    public void getAllEvents(OnSuccessListener<List<Event>> onSuccess,
                             OnFailureListener onFailure) {
        repository.getAllEvents(onSuccess, onFailure);
    }

    /**
     * Validates and saves a new Event.
     * <p>
     * Validation:
     * </p>
     * <ul>
     *     <li>Event cannot be null.</li>
     *     <li>Event title is required.</li>
     * </ul>
     *
     * @param event     The event to save.
     * @param onSuccess Callback receiving the saved Event ID.
     * @param onFailure Callback receiving validation or database errors.
     */
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

    /**
     * Validates and updates an existing Event.
     * <p>
     * Validation:
     * </p>
     * <ul>
     *     <li>Event title is required.</li>
     * </ul>
     *
     * @param event     The event to update.
     * @param onSuccess Callback invoked on success.
     * @param onFailure Callback invoked on failure.
     */
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

    /**
     * Asynchronously deletes an event.
     * @param eventId   The ID of the event to delete.
     * @param onSuccess Callback invoked on success.
     * @param onFailure Callback invoked on failure.
     */
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

    /**
     * Synchronously deletes an event.
     * @param eventId The ID of the event to delete.
     * @return True if successful.
     */
    public boolean deleteEvent(String eventId) {
        return repository.deleteEventById(eventId);
    }
}