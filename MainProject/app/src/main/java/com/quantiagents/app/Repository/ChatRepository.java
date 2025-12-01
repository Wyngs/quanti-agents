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
import com.quantiagents.app.models.Chat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Manages direct data access for Chats in Firestore.
 * Handles CRUD operations (Create, Read, Update, Delete).
 */
public class ChatRepository {

    private final CollectionReference context;

    public ChatRepository(FireBaseRepository fireBaseRepository) {
        this.context = fireBaseRepository.getChatCollectionRef();
    }

    /**
     * Synchronously retrieves a Chat by its unique ID.
     */
    public Chat getChatById(String chatId) {
        if (chatId == null || chatId.trim().isEmpty()) {
            Log.w("Firestore", "getChatById called with null or empty ID");
            return null;
        }

        try {
            DocumentSnapshot snapshot = Tasks.await(context.document(chatId).get());
            if (snapshot.exists()) {
                Chat chat = snapshot.toObject(Chat.class);
                if (chat != null && (chat.getChatId() == null || chat.getChatId().trim().isEmpty())) {
                    chat.setChatId(snapshot.getId());
                }
                return chat;
            } else {
                Log.d("Firestore", "No chat found for ID: " + chatId);
                return null;
            }
        } catch (Exception e) {
            Log.e("Firestore", "Error getting chat", e);
            return null;
        }
    }

    /**
     * Asynchronously retrieves a Chat by its unique ID.
     */
    public void getChatById(String chatId, OnSuccessListener<Chat> onSuccess, OnFailureListener onFailure) {
        if (chatId == null || chatId.trim().isEmpty()) {
            onFailure.onFailure(new IllegalArgumentException("Chat ID cannot be null or empty"));
            return;
        }

        context.document(chatId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Chat chat = documentSnapshot.toObject(Chat.class);
                        if (chat != null && (chat.getChatId() == null || chat.getChatId().trim().isEmpty())) {
                            chat.setChatId(documentSnapshot.getId());
                        }
                        onSuccess.onSuccess(chat);
                    } else {
                        onSuccess.onSuccess(null);
                    }
                })
                .addOnFailureListener(onFailure);
    }

    /**
     * Synchronously retrieves a Chat by event ID.
     */
    public Chat getChatByEventId(String eventId) {
        if (eventId == null || eventId.trim().isEmpty()) {
            return null;
        }

        try {
            QuerySnapshot snapshot = Tasks.await(context.whereEqualTo("eventId", eventId).limit(1).get());
            if (!snapshot.isEmpty()) {
                DocumentSnapshot document = snapshot.getDocuments().get(0);
                Chat chat = document.toObject(Chat.class);
                if (chat != null && (chat.getChatId() == null || chat.getChatId().trim().isEmpty())) {
                    chat.setChatId(document.getId());
                }
                return chat;
            }
            return null;
        } catch (Exception e) {
            Log.e("Firestore", "Error getting chat by event ID", e);
            return null;
        }
    }

    /**
     * Asynchronously retrieves a Chat by event ID.
     */
    public void getChatByEventId(String eventId, OnSuccessListener<Chat> onSuccess, OnFailureListener onFailure) {
        if (eventId == null || eventId.trim().isEmpty()) {
            onFailure.onFailure(new IllegalArgumentException("Event ID cannot be null or empty"));
            return;
        }

        context.whereEqualTo("eventId", eventId)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                        Chat chat = document.toObject(Chat.class);
                        if (chat != null && (chat.getChatId() == null || chat.getChatId().trim().isEmpty())) {
                            chat.setChatId(document.getId());
                        }
                        onSuccess.onSuccess(chat);
                    } else {
                        onSuccess.onSuccess(null);
                    }
                })
                .addOnFailureListener(onFailure);
    }

    /**
     * Synchronously retrieves all chats for a user (where user is a member).
     */
    public List<Chat> getChatsByUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            QuerySnapshot snapshot = Tasks.await(context.get());
            List<Chat> chats = new ArrayList<>();
            for (QueryDocumentSnapshot document : snapshot) {
                Chat chat = document.toObject(Chat.class);
                if (chat != null && chat.getMemberIds() != null && chat.getMemberIds().contains(userId)) {
                    if (chat.getChatId() == null || chat.getChatId().trim().isEmpty()) {
                        chat.setChatId(document.getId());
                    }
                    chats.add(chat);
                }
            }
            return chats;
        } catch (Exception e) {
            Log.e("Firestore", "Error getting chats by user ID", e);
            return new ArrayList<>();
        }
    }

    /**
     * Saves a new Chat or updates an existing one.
     */
    public void saveChat(Chat chat, OnSuccessListener<String> onSuccess, OnFailureListener onFailure) {
        if (chat.getChatId() == null || chat.getChatId().trim().isEmpty()) {
            Task<DocumentReference> addTask = context.add(chat);
            addTask.addOnSuccessListener(documentReference -> {
                String docId = documentReference.getId();
                chat.setChatId(docId);
                context.document(docId).update("chatId", docId)
                        .addOnSuccessListener(aVoid -> onSuccess.onSuccess(docId))
                        .addOnFailureListener(e -> onSuccess.onSuccess(docId));
            }).addOnFailureListener(onFailure);
        } else {
            String chatId = chat.getChatId();
            context.document(chatId).set(chat, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> onSuccess.onSuccess(chatId))
                    .addOnFailureListener(onFailure);
        }
    }

    /**
     * Updates an existing Chat in Firestore.
     */
    public void updateChat(@NonNull Chat chat, @NonNull OnSuccessListener<Void> onSuccess, @NonNull OnFailureListener onFailure) {
        if (chat.getChatId() == null || chat.getChatId().trim().isEmpty()) {
            onFailure.onFailure(new IllegalArgumentException("Chat ID is required for update"));
            return;
        }

        context.document(chat.getChatId())
                .set(chat, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Chat updated: " + chat.getChatId());
                    onSuccess.onSuccess(aVoid);
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error updating chat", e);
                    onFailure.onFailure(e);
                });
    }

    /**
     * Synchronously updates a Chat.
     */
    public boolean updateChat(Chat chat) {
        if (chat.getChatId() == null || chat.getChatId().trim().isEmpty()) {
            return false;
        }

        try {
            Tasks.await(context.document(chat.getChatId()).set(chat, SetOptions.merge()));
            return true;
        } catch (Exception e) {
            Log.e("Firestore", "Error updating chat", e);
            return false;
        }
    }

    /**
     * Deletes a chat by its ID.
     */
    public void deleteChatById(String chatId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        context.document(chatId)
                .delete()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }
}

