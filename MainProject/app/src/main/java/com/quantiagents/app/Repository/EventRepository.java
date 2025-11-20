package com.quantiagents.app.Repository;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.quantiagents.app.models.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Manages saving and locating events
 * @see Event
 */
public class EventRepository {

    private final CollectionReference context;

    public EventRepository(FireBaseRepository fireBaseRepository) {
        this.context = fireBaseRepository.getEventCollectionRef();
    }

    /**
     * Finds an event from it's id
     * @param eventId
     * Id to locate
     * @return
     * Returns located event if it exists, otherwise null
     * @see Event
     */
    public Event getEventById(String eventId) {
        try {
            DocumentSnapshot snapshot = Tasks.await(context.document(eventId).get());
            if (snapshot.exists()) {
                Event event = snapshot.toObject(Event.class);
                if (event != null && (event.getEventId() == null || event.getEventId().trim().isEmpty())) {
                    event.setEventId(snapshot.getId());
                }
                return event;
            } else {
                Log.d("Firestore", "No event found for ID: " + eventId);
                return null;
            }
        } catch (ExecutionException | InterruptedException e) {
            Log.e("Firestore", "Error getting event", e);
            return null;
        }
    }

    /**
     * Gets all events
     * @return
     * Returns a list of events
     * @see Event
     */
    public List<Event> getAllEvents() {
        try {
            QuerySnapshot snapshot = Tasks.await(context.get());
            List<Event> events = new ArrayList<>();
            for (QueryDocumentSnapshot document : snapshot) {
                Event event = document.toObject(Event.class);
                if (event != null) {
                    if (event.getEventId() == null || event.getEventId().trim().isEmpty()) {
                        event.setEventId(document.getId());
                    }
                    events.add(event);
                }
            }
            return events;
        } catch (ExecutionException | InterruptedException e) {
            Log.e("Firestore", "Error getting all events", e);
            return new ArrayList<>();
        }
    }

    /**
     * Gets a list of all events
     * @param onSuccess
     * Calls a function on success
     * @param onFailure
     * Calls a function on failure
     * @see Event
     */
    public void getAllEvents(OnSuccessListener<List<Event>> onSuccess,
                             OnFailureListener onFailure) {
        context.get()
                .addOnSuccessListener(qs -> {
                    List<Event> out = new ArrayList<>();
                    for (QueryDocumentSnapshot d : qs) {
                        Event e = d.toObject(Event.class);
                        if (e != null) {
                            if (e.getEventId() == null || e.getEventId().trim().isEmpty()) {
                                e.setEventId(d.getId());
                            }
                            out.add(e);
                        }
                    }
                    onSuccess.onSuccess(out);
                })
                .addOnFailureListener(onFailure);
    }

    /**
     * Saves event to the firebase
     * @param event
     * Event to save
     * @param onSuccess
     * Calls a function on success
     * @param onFailure
     * Calls a function on failure
     * @see Event
     */
    public void saveEvent(Event event, OnSuccessListener<String> onSuccess, OnFailureListener onFailure) {

        //if eventId is null or empty, let Firebase auto-generate an ID
        if (event.getEventId() == null || event.getEventId().trim().isEmpty()) {

            // Create a new document reference *first*
            DocumentReference newEventRef = context.document();


            String generatedId = newEventRef.getId();


            event.setEventId(generatedId);


            newEventRef.set(event)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("Firestore", "Event created with auto-generated ID: " + generatedId);
                        onSuccess.onSuccess(generatedId);
                    })
                    .addOnFailureListener(onFailure);
        } else {

            String eventId = event.getEventId();
            DocumentReference docRef = context.document(event.getEventId());
            docRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    //document exists, update it
                    docRef.set(event, SetOptions.merge())
                            .addOnSuccessListener(aVoid -> onSuccess.onSuccess(eventId))
                            .addOnFailureListener(onFailure);
                } else {
                    //document doesn't exist, create it
                    docRef.set(event)
                            .addOnSuccessListener(aVoid -> onSuccess.onSuccess(eventId))
                            .addOnFailureListener(onFailure);
                }
            }).addOnFailureListener(onFailure);
        }
    }
<<<<<<< HEAD
=======

    /**
     * Updates event in the firebase
     * @param event
     * Event to update
     * @param onSuccess
     * Calls a function on success
     * @param onFailure
     * Calls a function on failure
     * @see Event
     */
>>>>>>> 64eb91ae6be76ab0be0cf20563395b74bc6542d5
    public void updateEvent(@NonNull Event event,
                           @NonNull OnSuccessListener<Void> onSuccess,
                           @NonNull OnFailureListener onFailure) {
        context.document(event.getEventId())
                .set(event, SetOptions.merge()) // merge only changed fields
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Event updated: " + event.getEventId());
                    onSuccess.onSuccess(aVoid);
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error updating event", e);
                    onFailure.onFailure(e);
                });
    }

    /**
     * Deletes event by id from firebase
     * @param eventId
     * Event id to delete
     * @param onSuccess
     * Calls a function on success
     * @param onFailure
     * Calls a function on failure
     */
    public void deleteEventById(String eventId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        context.document(eventId)
                .delete()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    /**
     * Deletes event by id from firebase
     * @param eventId
     * Event id to delete
     * @return
     * Returns boolean for success
     */
    public boolean deleteEventById(String eventId) {
        try {
            Tasks.await(context.document(eventId).delete());
            Log.d("Firestore", "Event deleted: " + eventId);
            return true;
        } catch (ExecutionException | InterruptedException e) {
            Log.e("Firestore", "Error deleting event", e);
            return false;
        }
    }
}
