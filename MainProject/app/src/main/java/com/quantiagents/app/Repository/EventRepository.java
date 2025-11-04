package com.quantiagents.app.Repository;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.quantiagents.app.models.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class EventRepository {

    private final CollectionReference context;

    public EventRepository(FireBaseRepository fireBaseRepository) {
        this.context = fireBaseRepository.getEventCollectionRef();
    }

    public Event getEventById(String eventId) {
        try {
            DocumentSnapshot snapshot = Tasks.await(context.document(eventId).get());
            if (snapshot.exists()) {
                return snapshot.toObject(Event.class);
            } else {
                Log.d("Firestore", "No event found for ID: " + eventId);
                return null;
            }
        } catch (ExecutionException | InterruptedException e) {
            Log.e("Firestore", "Error getting event", e);
            return null;
        }
    }

    public List<Event> getAllEvents() {
        try {
            QuerySnapshot snapshot = Tasks.await(context.get());
            List<Event> events = new ArrayList<>();
            for (QueryDocumentSnapshot document : snapshot) {
                Event event = document.toObject(Event.class);
                if (event != null) {
                    events.add(event);
                }
            }
            return events;
        } catch (ExecutionException | InterruptedException e) {
            Log.e("Firestore", "Error getting all events", e);
            return new ArrayList<>();
        }
    }

    public void saveEvent(Event event, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        context.document(event.getEventId())
                .set(event)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

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

    public void deleteEventById(String eventId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        context.document(eventId)
                .delete()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

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
