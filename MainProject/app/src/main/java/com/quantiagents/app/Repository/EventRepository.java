package com.quantiagents.app.Repository;

import androidx.annotation.NonNull;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.quantiagents.app.models.Event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class EventRepository {

    private final CollectionReference context;

    public EventRepository(FireBaseRepository fireBaseRepository) {
        this.context = fireBaseRepository.getEventCollectionRef();
    }

    public Task<DocumentSnapshot> getEventById(String eventId) {
        return context.document(eventId).get();
    }

    public Task<QuerySnapshot> getAllEvents() {
        return context.get();
    }

    public void saveEvent(Event event, OnSuccessListener<String> onSuccess, OnFailureListener onFailure) {
        if (event.getEventId() == null || event.getEventId().trim().isEmpty()) {
            Task<DocumentReference> addTask = context.add(event);
            addTask.addOnSuccessListener(documentReference -> {
                String docId = documentReference.getId();
                event.setEventId(docId);
                context.document(docId).update("eventId", docId)
                        .addOnSuccessListener(aVoid -> onSuccess.onSuccess(docId))
                        .addOnFailureListener(e -> onSuccess.onSuccess(docId));
            }).addOnFailureListener(onFailure);
        } else {
            String eventId = event.getEventId();
            DocumentReference docRef = context.document(event.getEventId());
            docRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    docRef.set(event, SetOptions.merge())
                            .addOnSuccessListener(aVoid -> onSuccess.onSuccess(eventId))
                            .addOnFailureListener(onFailure);
                } else {
                    docRef.set(event)
                            .addOnSuccessListener(aVoid -> onSuccess.onSuccess(eventId))
                            .addOnFailureListener(onFailure);
                }
            }).addOnFailureListener(onFailure);
        }
    }

    public Task<Void> updateEvent(@NonNull Event event) {
        return context.document(event.getEventId()).set(event, SetOptions.merge());
    }

    public Task<Void> deleteEventById(String eventId) {
        return context.document(eventId).delete();
    }
}
