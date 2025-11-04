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
import com.quantiagents.app.models.User;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class UserRepository {

    private final CollectionReference context;

    public UserRepository(FireBaseRepository fireBaseRepository) {
        this.context = fireBaseRepository.getUserCollectionRef();
    }
    public User getUserById(int userId) {
        try {
            DocumentSnapshot snapshot = Tasks.await(context.document(Integer.toString(userId)).get());
            if (snapshot.exists()) {
                return snapshot.toObject(User.class);
            } else {
                Log.d("Firestore", "No user found for ID: " + userId);
                return null;
            }
        } catch (ExecutionException | InterruptedException e) {
            Log.e("Firestore", "Error getting user", e);
            return null;
        }
    }
    public void saveUser(User user, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        // If userId is 0 or negative, let Firebase auto-generate an ID
        if (user.getUserId() <= 0) {
            // Use .add() to create a new document with auto-generated ID
            Task<DocumentReference> addTask = context.add(user);
            addTask.addOnSuccessListener(documentReference -> {
                // Extract the auto-generated document ID
                String docId = documentReference.getId();
                
                // Generate a unique int ID from the Firestore document ID
                // Use hash code and ensure it's positive
                int generatedId = docId.hashCode();
                if (generatedId < 0) {
                    generatedId = Math.abs(generatedId);
                }
                // If somehow still 0, use a timestamp-based fallback
                if (generatedId == 0) {
                    generatedId = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
                }
                
                // Update the user object with the generated ID
                user.setUserId(generatedId);
                
                // Update the document with the generated userId field
                context.document(docId).update("userId", generatedId)
                        .addOnSuccessListener(aVoid -> {
                            Log.d("Firestore", "User created with auto-generated " + " (docId: " + docId + ")");
                            onSuccess.onSuccess(aVoid);
                        })
                        .addOnFailureListener(e -> {
                            Log.e("Firestore", "Error updating user with generated ID", e);
                            // Still call success since document was created, just the ID update failed
                            onSuccess.onSuccess(null);
                        });
            }).addOnFailureListener(onFailure);
        } else {
            // Check if user with this ID already exists
            DocumentReference docRef = context.document(String.valueOf(user.getUserId()));
            docRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    // Document exists, update it
                    docRef.set(user, SetOptions.merge())
                            .addOnSuccessListener(onSuccess)
                            .addOnFailureListener(onFailure);
                } else {
                    // Document doesn't exist, create it
                    docRef.set(user)
                            .addOnSuccessListener(onSuccess)
                            .addOnFailureListener(onFailure);
                }
            }).addOnFailureListener(onFailure);
        }
    }
    public void updateUser(@NonNull User user,
                           @NonNull OnSuccessListener<Void> onSuccess,
                           @NonNull OnFailureListener onFailure) {
        context.document(String.valueOf(user.getUserId()))
                .set(user, SetOptions.merge()) // merge only changed fields
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "User updated: " + user.getUserId());
                    onSuccess.onSuccess(aVoid);
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error updating user", e);
                    onFailure.onFailure(e);
                });
    }
    public void deleteUserById(int userId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        context.document(String.valueOf(userId))
                .delete()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

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
}
