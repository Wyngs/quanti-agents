package com.quantiagents.app.Repository;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
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
        context.document(String.valueOf(user.getUserId()))
                .set(user)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
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
}
