package com.quantiagents.app.Services;

import android.content.Context;
import androidx.annotation.NonNull;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.quantiagents.app.Repository.EventRepository;
import com.quantiagents.app.Repository.FireBaseRepository;
import com.quantiagents.app.models.Event;

import java.util.Collections;
import java.util.List;

public class EventService {

    private final EventRepository repository;

    public EventService(Context context) {
        FireBaseRepository fireBaseRepository = new FireBaseRepository();
        this.repository = new EventRepository(fireBaseRepository);
    }

    public Task<DocumentSnapshot> getEventById(String eventId) {
        return repository.getEventById(eventId);
    }

    public Task<QuerySnapshot> getAllEvents() {
        return repository.getAllEvents();
    }

    public void saveEvent(Event event, OnSuccessListener<String> onSuccess, OnFailureListener onFailure) {
        if (event == null) {
            onFailure.onFailure(new IllegalArgumentException("Event cannot be null"));
            return;
        }
        if (event.getTitle() == null || event.getTitle().trim().isEmpty()) {
            onFailure.onFailure(new IllegalArgumentException("Event title is required"));
            return;
        }
        repository.saveEvent(event, onSuccess, onFailure);
    }

    public Task<Void> updateEvent(@NonNull Event event) {
        if (event.getTitle() == null || event.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Event title is required");
        }
        return repository.updateEvent(event);
    }

    public Task<Void> deleteEvent(String eventId) {
        return repository.deleteEventById(eventId);
    }
}
