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
import com.quantiagents.app.models.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Manages direct data access for Messages in Firestore.
 * Handles CRUD operations (Create, Read, Update, Delete).
 */
public class MessageRepository {

    private final CollectionReference context;

    public MessageRepository(FireBaseRepository fireBaseRepository) {
        this.context = fireBaseRepository.getMessageCollectionRef();
    }

    /**
     * Synchronously retrieves a Message by its unique ID.
     */
    public Message getMessageById(String messageId) {
        if (messageId == null || messageId.trim().isEmpty()) {
            return null;
        }

        try {
            DocumentSnapshot snapshot = Tasks.await(context.document(messageId).get());
            if (snapshot.exists()) {
                Message message = snapshot.toObject(Message.class);
                if (message != null && (message.getMessageId() == null || message.getMessageId().trim().isEmpty())) {
                    message.setMessageId(snapshot.getId());
                }
                return message;
            }
            return null;
        } catch (Exception e) {
            Log.e("Firestore", "Error getting message", e);
            return null;
        }
    }

    /**
     * Synchronously retrieves all messages for a chat, ordered by timestamp.
     */
    public List<Message> getMessagesByChatId(String chatId) {
        if (chatId == null || chatId.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            QuerySnapshot snapshot = Tasks.await(
                    context.whereEqualTo("chatId", chatId)
                            .orderBy("timestamp")
                            .get()
            );
            List<Message> messages = new ArrayList<>();
            for (QueryDocumentSnapshot document : snapshot) {
                Message message = document.toObject(Message.class);
                if (message != null) {
                    if (message.getMessageId() == null || message.getMessageId().trim().isEmpty()) {
                        message.setMessageId(document.getId());
                    }
                    messages.add(message);
                }
            }
            return messages;
        } catch (Exception e) {
            Log.e("Firestore", "Error getting messages by chat ID", e);
            return new ArrayList<>();
        }
    }

    /**
     * Asynchronously retrieves all messages for a chat, ordered by timestamp.
     */
    public void getMessagesByChatId(String chatId, OnSuccessListener<List<Message>> onSuccess, OnFailureListener onFailure) {
        if (chatId == null || chatId.trim().isEmpty()) {
            onFailure.onFailure(new IllegalArgumentException("Chat ID cannot be null or empty"));
            return;
        }

        context.whereEqualTo("chatId", chatId)
                .orderBy("timestamp")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Message> messages = new ArrayList<>();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        Message message = document.toObject(Message.class);
                        if (message != null) {
                            if (message.getMessageId() == null || message.getMessageId().trim().isEmpty()) {
                                message.setMessageId(document.getId());
                            }
                            messages.add(message);
                        }
                    }
                    onSuccess.onSuccess(messages);
                })
                .addOnFailureListener(onFailure);
    }

    /**
     * Saves a new Message.
     */
    public void saveMessage(Message message, OnSuccessListener<String> onSuccess, OnFailureListener onFailure) {
        if (message.getMessageId() == null || message.getMessageId().trim().isEmpty()) {
            Task<DocumentReference> addTask = context.add(message);
            addTask.addOnSuccessListener(documentReference -> {
                String docId = documentReference.getId();
                message.setMessageId(docId);
                context.document(docId).update("messageId", docId)
                        .addOnSuccessListener(aVoid -> {
                            Log.d("Firestore", "Message saved: " + docId);
                            onSuccess.onSuccess(docId);
                        })
                        .addOnFailureListener(e -> onSuccess.onSuccess(docId));
            }).addOnFailureListener(onFailure);
        } else {
            String messageId = message.getMessageId();
            context.document(messageId).set(message, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Log.d("Firestore", "Message saved: " + messageId);
                        onSuccess.onSuccess(messageId);
                    })
                    .addOnFailureListener(onFailure);
        }
    }

    /**
     * Updates an existing Message.
     */
    public void updateMessage(@NonNull Message message, @NonNull OnSuccessListener<Void> onSuccess, @NonNull OnFailureListener onFailure) {
        if (message.getMessageId() == null || message.getMessageId().trim().isEmpty()) {
            onFailure.onFailure(new IllegalArgumentException("Message ID is required for update"));
            return;
        }

        context.document(message.getMessageId())
                .set(message, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Message updated: " + message.getMessageId());
                    onSuccess.onSuccess(aVoid);
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error updating message", e);
                    onFailure.onFailure(e);
                });
    }

    /**
     * Deletes a message by its ID.
     */
    public void deleteMessageById(String messageId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        context.document(messageId)
                .delete()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }
}

