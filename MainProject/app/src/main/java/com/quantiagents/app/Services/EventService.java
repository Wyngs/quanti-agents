package com.quantiagents.app.Services;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.quantiagents.app.Constants.constant;
import com.quantiagents.app.Repository.EventRepository;
import com.quantiagents.app.Repository.FireBaseRepository;
import com.quantiagents.app.models.Event;
import com.quantiagents.app.models.Notification;
import com.quantiagents.app.models.User;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service layer for Event operations.
 * <p>
 * Acts as an intermediary between the UI controllers and the Data Repository.
 * Enforces business logic and validation rules (e.g., requiring a title for events).
 * </p>
 */
public class EventService {

    private final EventRepository repository;
    private final NotificationService notificationService;
    private final UserService userService;
    private final Context context;

    /**
     * Constructs an EventService with required dependencies.
     * EventService instantiates its own repositories internally.
     *
     * @param context The application context
     */
    public EventService(Context context) {
        // EventService instantiates its own repositories internally
        FireBaseRepository fireBaseRepository = new FireBaseRepository();
        this.repository = new EventRepository(fireBaseRepository);
        this.context = context;
        this.notificationService = new NotificationService(context);
        this.userService = new UserService(context);
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

        // Get old event to check if there are affected users
        Event oldEvent = getEventById(event.getEventId());
        
        repository.updateEvent(event,
                aVoid -> {
                    Log.d("App", "Event updated: " + event.getEventId());
                    // Send notifications if there are affected users
                    if (oldEvent != null) {
                        sendEventUpdatedNotifications(event);
                    }
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
        // Get event before deleting to send notifications
        Event event = getEventById(eventId);
        if (event != null) {
            // Check if current user is the organizer (not admin)
            User currentUser = userService.getCurrentUser();
            if (currentUser != null && event.getOrganizerId() != null 
                    && event.getOrganizerId().equals(currentUser.getUserId())) {
                // This is organizer deletion, send notifications
                sendEventDeletedByOrganizerNotifications(event);
            }
            // Note: Admin deletion notifications are handled in AdminService
        }

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

    /**
     * Sends notifications when an event is updated by organizer.
     * Notifies all users in waiting list, selected list, and confirmed list.
     *
     * @param event The event that was updated
     */
    private void sendEventUpdatedNotifications(Event event) {
        if (event == null) return;

        String eventId = event.getEventId();
        String organizerId = event.getOrganizerId();
        String eventName = event.getTitle() != null ? event.getTitle() : "Event";

        if (eventId == null || organizerId == null) return;

        int eventIdInt = Math.abs(eventId.hashCode());
        int organizerIdInt = Math.abs(organizerId.hashCode());

        // Collect all affected user IDs
        Set<String> affectedUserIds = new HashSet<>();
        if (event.getWaitingList() != null) {
            affectedUserIds.addAll(event.getWaitingList());
        }
        if (event.getSelectedList() != null) {
            affectedUserIds.addAll(event.getSelectedList());
        }
        if (event.getConfirmedList() != null) {
            affectedUserIds.addAll(event.getConfirmedList());
        }

        // Only send notifications if there are affected users
        if (affectedUserIds.isEmpty()) return;

        // Send notification to all affected users
        for (String userId : affectedUserIds) {
            if (userId == null || userId.trim().isEmpty()) continue;
            int userIdInt = Math.abs(userId.hashCode());

            Notification notification = new Notification(
                    0, // Auto-generate ID
                    constant.NotificationType.WARNING,
                    userIdInt,
                    organizerIdInt, // senderId = EventOrganizerId
                    eventIdInt,
                    "Event Updated",
                    "Event: " + eventName + " has been updated by the Oraganizer. Please review the details"
            );

            notificationService.saveNotification(notification,
                    aVoid -> {},
                    e -> {}
            );
        }
    }

    /**
     * Sends notifications when an event is deleted by organizer.
     * Notifies all users in waiting list, selected list, and confirmed list.
     *
     * @param event The event that was deleted
     */
    private void sendEventDeletedByOrganizerNotifications(Event event) {
        if (event == null) return;

        String eventId = event.getEventId();
        String organizerId = event.getOrganizerId();
        String eventName = event.getTitle() != null ? event.getTitle() : "Event";

        if (eventId == null || organizerId == null) return;

        int eventIdInt = Math.abs(eventId.hashCode());
        int organizerIdInt = Math.abs(organizerId.hashCode());

        // Get organizer name
        User organizer = userService.getUserById(organizerId);
        String organizerName = "Event Organizer";
        if (organizer != null && organizer.getName() != null && !organizer.getName().trim().isEmpty()) {
            organizerName = organizer.getName().trim();
        }

        // Collect all affected user IDs
        Set<String> affectedUserIds = new HashSet<>();
        if (event.getWaitingList() != null) {
            affectedUserIds.addAll(event.getWaitingList());
        }
        if (event.getSelectedList() != null) {
            affectedUserIds.addAll(event.getSelectedList());
        }
        if (event.getConfirmedList() != null) {
            affectedUserIds.addAll(event.getConfirmedList());
        }

        // Send notification to all affected users
        for (String userId : affectedUserIds) {
            if (userId == null || userId.trim().isEmpty()) continue;
            int userIdInt = Math.abs(userId.hashCode());

            Notification notification = new Notification(
                    0, // Auto-generate ID
                    constant.NotificationType.BAD,
                    userIdInt,
                    organizerIdInt, // senderId = EventOrganizerId
                    eventIdInt,
                    "Event Canceled",
                    "Due to unforeseen reasons, Event Organizer " + organizerName + " has decided to cancel Event: " + eventName
            );

            notificationService.saveNotification(notification,
                    aVoid -> {},
                    e -> {}
            );
        }
    }
}