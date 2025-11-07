package com.quantiagents.app.Repository;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
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
 * Repository for Firestore {@code USER} documents.
 * Exposes both Task-based and listener-based helpers so all call sites stay compatible.
 *
 * NOTE: Any {@code Tasks.await(...)} method here is for tests/background threads only.
 * Do NOT call them on the main thread.
 */
public class UserRepository {

    private static final String TAG = "UserRepository";
    private final CollectionReference context;

    /**
     * @param fireBaseRepository App wrapper around Firebase entry points.
     */
    public UserRepository(@NonNull FireBaseRepository fireBaseRepository) {
        this.context = fireBaseRepository.getUserCollectionRef();
    }

    // ---------------------------------------------------------------------
    // Query APIs (Task-returning)
    // ---------------------------------------------------------------------

    /** Get a user doc by its document id. */
    public Task<DocumentSnapshot> getUserById(@NonNull String userId) {
        return context.document(userId).get();
    }

    /** Find the first user with a matching deviceId. */
    public Task<QuerySnapshot> findUserByDeviceId(@NonNull String deviceId) {
        return context.whereEqualTo("deviceId", deviceId).limit(1).get();
    }

    /** Find the first user with a matching (lowercased, trimmed) email. */
    public Task<QuerySnapshot> findUserByEmail(@NonNull String email) {
        return context.whereEqualTo("email", email.trim().toLowerCase()).limit(1).get();
    }

    /** Get all users (default cache policy). */
    public Task<QuerySnapshot> getAllUsers() {
        return context.get();
    }

    /** Get all users, forcing server fetch (useful for tests). */
    public Task<QuerySnapshot> getAllUsersFromServerTask() {
        return context.get(Source.SERVER);
    }

    // ---------------------------------------------------------------------
    // Mutations (Task-returning)
    // ---------------------------------------------------------------------

    /**
     * Upsert a user. If {@code userId} is empty, creates a new document and writes the generated id back
     * to the document field {@code userId}. Returns a Task that resolves with the effective userId.
     */
    public Task<String> saveUserTask(@NonNull User user) {
        TaskCompletionSource<String> tcs = new TaskCompletionSource<>();

        String userId = user.getUserId();
        if (userId == null || userId.trim().isEmpty()) {
            // Auto-create
            context.add(user)
                    .addOnSuccessListener(ref -> {
                        String docId = ref.getId();
                        user.setUserId(docId);
                        // Persist userId field for consistency
                        context.document(docId).update("userId", docId)
                                .addOnSuccessListener(v -> tcs.setResult(docId))
                                // Even if this minor update fails, the doc exists; return id
                                .addOnFailureListener(e -> {
                                    Log.w(TAG, "Failed to write generated userId to doc", e);
                                    tcs.setResult(docId);
                                });
                    })
                    .addOnFailureListener(tcs::setException);
        } else {
            // Upsert specific id
            DocumentReference doc = context.document(userId);
            doc.get().addOnSuccessListener(snap -> {
                Task<Void> write = snap.exists()
                        ? doc.set(user, SetOptions.merge())
                        : doc.set(user);
                write.addOnSuccessListener(v -> tcs.setResult(userId))
                     .addOnFailureListener(tcs::setException);
            }).addOnFailureListener(tcs::setException);
        }

        return tcs.getTask();
    }

    /** Merge-update the given user (must have a non-empty userId). */
    public Task<Void> updateUser(@NonNull User user) {
        return context.document(user.getUserId()).set(user, SetOptions.merge());
    }

    /** Delete by id. Returns a failed Task if id is empty. */
    public Task<Void> deleteUserById(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("userId cannot be null or empty"));
        }
        return context.document(userId).delete();
    }

    // ---------------------------------------------------------------------
    // Listener-based convenience wrappers (compatibility with existing code)
    // ---------------------------------------------------------------------

    /** Callback wrapper for {@link #saveUserTask(User)} that returns the effective userId. */
    public void saveUser(@NonNull User user,
                         @NonNull OnSuccessListener<String> onSuccess,
                         @NonNull OnFailureListener onFailure) {
        saveUserTask(user).addOnSuccessListener(onSuccess).addOnFailureListener(onFailure);
    }

    /** Callback wrapper for merge update. */
    public void updateUser(@NonNull User user,
                           @NonNull OnSuccessListener<Void> onSuccess,
                           @NonNull OnFailureListener onFailure) {
        updateUser(user).addOnSuccessListener(onSuccess).addOnFailureListener(onFailure);
    }

    /** Callback wrapper for delete. */
    public void deleteUserById(@NonNull String userId,
                               @NonNull OnSuccessListener<Void> onSuccess,
                               @NonNull OnFailureListener onFailure) {
        deleteUserById(userId).addOnSuccessListener(onSuccess).addOnFailureListener(onFailure);
    }

    /**
     * Callback wrapper to fetch all users and materialize as {@code List<User>}.
     * (Default cache policy.)
     */
    public void getAllUsers(@NonNull OnSuccessListener<List<User>> onSuccess,
                            @NonNull OnFailureListener onFailure) {
        context.get()
                .addOnSuccessListener(snap -> onSuccess.onSuccess(toUsers(snap)))
                .addOnFailureListener(onFailure);
    }

    // ---------------------------------------------------------------------
    // Blocking helpers for tests/background (NEVER on main thread)
    // ---------------------------------------------------------------------

    /** Blocking helper used in tests/background to fetch by id. */
    public User getUserByIdBlocking(@NonNull String userId) {
        try {
            DocumentSnapshot snapshot = Tasks.await(context.document(userId).get());
            return snapshot.exists() ? snapshot.toObject(User.class) : null;
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "getUserByIdBlocking failed", e);
            return null;
        }
    }

    /** Blocking helper: all users (default cache policy). */
    public List<User> getAllUsersBlocking() {
        try {
            QuerySnapshot snap = Tasks.await(context.get());
            return toUsers(snap);
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "getAllUsersBlocking failed", e);
            return new ArrayList<>();
        }
    }

    /** Blocking helper: all users (force server). */
    public List<User> getAllUsersFromServerBlocking() {
        try {
            QuerySnapshot snap = Tasks.await(context.get(Source.SERVER));
            return toUsers(snap);
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "getAllUsersFromServerBlocking failed", e);
            return new ArrayList<>();
        }
    }

    // ---------------------------------------------------------------------
    // Utilities
    // ---------------------------------------------------------------------

    private static List<User> toUsers(@NonNull QuerySnapshot snapshot) {
        List<User> out = new ArrayList<>();
        for (QueryDocumentSnapshot doc : snapshot) {
            User u = doc.toObject(User.class);
            if (u != null) out.add(u);
        }
        return out;
    }
}
