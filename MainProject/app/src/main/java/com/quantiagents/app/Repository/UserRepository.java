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
import com.quantiagents.app.models.User;

public class UserRepository {

    private final CollectionReference context;

    public UserRepository(FireBaseRepository fireBaseRepository) {
        this.context = fireBaseRepository.getUserCollectionRef();
    }

    public Task<DocumentSnapshot> getUserById(String userId) {
        return context.document(userId).get();
    }

    public Task<QuerySnapshot> findUserByDeviceId(String deviceId) {
        return context.whereEqualTo("deviceId", deviceId).limit(1).get();
    }

    public Task<QuerySnapshot> findUserByEmail(String email) {
        return context.whereEqualTo("email", email.trim().toLowerCase()).limit(1).get();
    }

    public void saveUser(User user, OnSuccessListener<String> onSuccess, OnFailureListener onFailure) {
        if (user.getUserId() == null || user.getUserId().trim().isEmpty()) {
            Task<DocumentReference> addTask = context.add(user);
            addTask.addOnSuccessListener(documentReference -> {
                String docId = documentReference.getId();
                user.setUserId(docId);
                context.document(docId).update("userId", docId)
                        .addOnSuccessListener(aVoid -> onSuccess.onSuccess(docId))
                        .addOnFailureListener(e -> onSuccess.onSuccess(docId));
            }).addOnFailureListener(onFailure);
        } else {
            String userId = user.getUserId();
            DocumentReference docRef = context.document(userId);
            docRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    docRef.set(user, SetOptions.merge())
                            .addOnSuccessListener(aVoid -> onSuccess.onSuccess(userId))
                            .addOnFailureListener(onFailure);
                } else {
                    docRef.set(user)
                            .addOnSuccessListener(aVoid -> onSuccess.onSuccess(userId))
                            .addOnFailureListener(onFailure);
                }
            }).addOnFailureListener(onFailure);
        }
    }

    public Task<Void> updateUser(@NonNull User user) {
        return context.document(user.getUserId()).set(user, SetOptions.merge());
    }

    public Task<Void> deleteUserById(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return com.google.android.gms.tasks.Tasks.forException(new IllegalArgumentException("userId cannot be null or empty"));
        }
        return context.document(userId).delete();
    }

    public Task<QuerySnapshot> getAllUsers() {
        return context.get();
    }
}