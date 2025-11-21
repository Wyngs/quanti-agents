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
import com.google.firebase.firestore.Source;
import com.quantiagents.app.models.User;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Direct Firestore access layer so I keep the SDK details out of the services.
 */
public class UserRepository {

    private final CollectionReference context;

    /**
     * @param fireBaseRepository wrapper I built around the Firebase entry points.
     */
    public UserRepository(FireBaseRepository fireBaseRepository) {
        this.context = fireBaseRepository.getUserCollectionRef();
    }

    /**
     * Fetches a user document by id. Used sparingly since most flows key off device id.
     */
    public User getUserById(String userId) {
        // CRITICAL FIX: Check for invalid ID before calling Firestore to prevent IllegalArgumentException crash
        if (userId == null || userId.trim().isEmpty()) {
            Log.w("Firestore", "getUserById called with null or empty ID");
            return null;
        }

        try {
            DocumentSnapshot snapshot = Tasks.await(context.document(userId).get());
            if (snapshot.exists()) {
                return snapshot.toObject(User.class);
            } else {
                Log.d("Firestore", "No user found for ID: " + userId);
                return null;
            }
        } catch (IllegalArgumentException e) {
            Log.e("Firestore", "Invalid argument for user ID: " + userId, e);
            return null;
        } catch (ExecutionException | InterruptedException e) {
            Log.e("Firestore", "Error getting user", e);
            return null;
        } catch (Exception e) {
            Log.e("Firestore", "Unexpected error getting user", e);
            return null;
        }
    }

    /**
     * Upserts the user document, auto-generating an id if none was provided.
     */
    public void saveUser(User user, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        if (user.getUserId() == null || user.getUserId().trim().isEmpty()) {
            Task<DocumentReference> addTask = context.add(user);
            addTask.addOnSuccessListener(documentReference -> {
                String docId = documentReference.getId();
                user.setUserId(docId);
                context.document(docId).update("userId", docId)
                        .addOnSuccessListener(aVoid -> {
                            Log.d("Firestore", "User created with auto-generated ID: " + docId);
                            onSuccess.onSuccess(aVoid);
                        })
                        .addOnFailureListener(e -> {
                            Log.e("Firestore", "Error updating user with generated ID", e);
                            onSuccess.onSuccess(null);
                        });
            }).addOnFailureListener(onFailure);
        } else {
            DocumentReference docRef = context.document(user.getUserId());
            docRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    docRef.set(user, SetOptions.merge())
                            .addOnSuccessListener(onSuccess)
                            .addOnFailureListener(onFailure);
                } else {
                    docRef.set(user)
                            .addOnSuccessListener(onSuccess)
                            .addOnFailureListener(onFailure);
                }
            }).addOnFailureListener(onFailure);
        }
    }

    /**
     * Writes a merge update for the supplied user id.
     */
    public void updateUser(@NonNull User user,
                           @NonNull OnSuccessListener<Void> onSuccess,
                           @NonNull OnFailureListener onFailure) {
        context.document(user.getUserId())
                .set(user, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "User updated: " + user.getUserId());
                    onSuccess.onSuccess(aVoid);
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error updating user", e);
                    onFailure.onFailure(e);
                });
    }

    /**
     * Deletes the given user document outright.
     */
    public void deleteUserById(String userId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        if (userId == null || userId.trim().isEmpty()) {
            Log.e("Firestore", "Cannot delete user: userId is null or empty");
            onFailure.onFailure(new IllegalArgumentException("userId cannot be null or empty"));
            return;
        }
        context.document(userId)
                .delete()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    /**
     * Reads using the default cache policy; fine for most UI flows.
     */
    public List<User> getAllUsers() {
        try {
            QuerySnapshot snapshot = Tasks.await(context.get());
            List<User> users = new ArrayList<>();
            for (QueryDocumentSnapshot document : snapshot) {
                User user = document.toObject(User.class);
                if (user != null) {
                    users.add(user);
                }
            }
            return users;
        } catch (ExecutionException | InterruptedException e) {
            Log.e("Firestore", "Error getting all users", e);
            return new ArrayList<>();
        }
    }

    /**
     * Forces a server fetch; I only call this when tests need the absolute latest state.
     */
    public List<User> getAllUsersFromServer() {
        try {
            QuerySnapshot snapshot = Tasks.await(context.get(Source.SERVER));
            List<User> users = new ArrayList<>();
            for (QueryDocumentSnapshot document : snapshot) {
                User user = document.toObject(User.class);
                if (user != null) {
                    users.add(user);
                }
            }
            return users;
        } catch (ExecutionException | InterruptedException e) {
            Log.e("Firestore", "Error getting all users from server", e);
            return new ArrayList<>();
        }
    }

    public void getAllUsers(OnSuccessListener<List<User>> onSuccess, OnFailureListener onFailure) {
        context.get()
                .addOnSuccessListener(querySnapshot -> {
                    List<User> users = new ArrayList<>();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        User user = document.toObject(User.class);
                        if (user != null) {
                            users.add(user);
                        }
                    }
                    onSuccess.onSuccess(users);
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error getting all users", e);
                    onFailure.onFailure(e);
                });
    }
}