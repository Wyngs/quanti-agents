package com.quantiagents.app.Services;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.QuerySnapshot;
import com.quantiagents.app.Constants.constant;
import com.quantiagents.app.Repository.FireBaseRepository;
import com.quantiagents.app.Repository.UserRepository;
import com.quantiagents.app.models.DeviceIdManager;
import com.quantiagents.app.models.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

/**
 * User-facing domain service that:
 * - Validates inputs
 * - Handles deviceId binding
 * - Provides both Task-based and callback-based APIs (for legacy call sites)
 * - Caches the active User in memory for quick access
 */
public class UserService {

    private static final String TAG = "UserService";

    private final UserRepository repository;
    private final DeviceIdManager deviceIdManager;
    private final Pattern emailPattern =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    private volatile @Nullable User currentUserCache;

    /**
     * Keep the existing constructor signature so other code compiles.
     * The second parameter is unused but retained for compatibility.
     */
    public UserService(@NonNull Context context, @SuppressWarnings("unused") Object ignored) {
        FireBaseRepository fireBaseRepository = new FireBaseRepository();
        this.repository = new UserRepository(fireBaseRepository);
        this.deviceIdManager = new DeviceIdManager(context);
    }

    // ---------------------------------------------------------------------
    // GETTERS
    // ---------------------------------------------------------------------

    /** Task-based current user fetch keyed by deviceId. Caches on success. */
    public Task<User> getCurrentUser() {
        if (currentUserCache != null) return Tasks.forResult(currentUserCache);
        String deviceId = deviceIdManager.ensureDeviceId();
        return repository.findUserByDeviceId(deviceId)
                .onSuccessTask(q -> {
                    if (!q.isEmpty()) {
                        currentUserCache = q.getDocuments().get(0).toObject(User.class);
                        return Tasks.forResult(currentUserCache);
                    }
                    return Tasks.forResult(null);
                });
    }

    /** Callback wrapper for {@link #getCurrentUser()}. */
    public void getCurrentUser(@NonNull OnSuccessListener<User> onSuccess,
                               @NonNull OnFailureListener onFailure) {
        getCurrentUser().addOnSuccessListener(onSuccess).addOnFailureListener(onFailure);
    }

    /** Expose raw Task in case callers want to list all users. */
    public Task<QuerySnapshot> getAllUsers() {
        return repository.getAllUsers();
    }

    // ---------------------------------------------------------------------
    // CREATE
    // ---------------------------------------------------------------------

    /** Task-based create (returns effective userId). Also sets role to ENTRANT and binds deviceId. */
    public Task<String> createNewUser(@NonNull String name,
                                      @NonNull String email,
                                      @Nullable String phone,
                                      @NonNull String password) {
        validateName(name);
        validateEmail(email);
        validatePassword(password);

        String trimmedPhone = phone == null ? "" : phone.trim();
        String deviceId = deviceIdManager.ensureDeviceId();
        String passwordHash = hashPassword(password);

        User user = new User(
                "",                       // userId (let Firestore assign if empty)
                deviceId,                 // deviceId
                name.trim(),
                email.trim(),
                trimmedPhone,
                passwordHash
        );
        user.setRole(constant.UserRole.ENTRANT);

        // Prefer the Task-based repo API; saves id back into the document.
        return repository.saveUserTask(user)
                .onSuccessTask(userId -> {
                    // Cache active user after creation
                    currentUserCache = user;
                    currentUserCache.setUserId(userId);
                    return Tasks.forResult(userId);
                });
    }

    /** Callback wrapper for create. */
    public void createUser(@NonNull String name,
                           @NonNull String email,
                           @Nullable String phone,
                           @NonNull String password,
                           @NonNull OnSuccessListener<User> onSuccess,
                           @NonNull OnFailureListener onFailure) {
        createNewUser(name, email, phone, password)
                .addOnSuccessListener(userId -> {
                    // currentUserCache already set in createNewUser; ensure non-null
                    if (currentUserCache != null) {
                        onSuccess.onSuccess(currentUserCache);
                    } else {
                        // Fallback: construct a minimal user object
                        String deviceId = deviceIdManager.ensureDeviceId();
                        User u = new User(userId, deviceId, name.trim(), email.trim(),
                                phone == null ? "" : phone.trim(), hashPassword(password));
                        u.setRole(constant.UserRole.ENTRANT);
                        currentUserCache = u;
                        onSuccess.onSuccess(u);
                    }
                })
                .addOnFailureListener(onFailure);
    }

    // ---------------------------------------------------------------------
    // UPDATE / DELETE
    // ---------------------------------------------------------------------

    /** Task-based update (merge) of a full user object. Keeps cache in sync if ids match. */
    public Task<Void> updateUser(@NonNull User user) {
        validateName(user.getName());
        validateEmail(user.getEmail());
        if (currentUserCache != null && user.getUserId() != null &&
                user.getUserId().equals(currentUserCache.getUserId())) {
            currentUserCache = user;
        }
        return repository.updateUser(user);
    }

    /** Callback helper for the common “edit profile” flow (name/email/phone only). */
    public void updateUser(@NonNull String name,
                           @NonNull String email,
                           @Nullable String phone,
                           @NonNull OnSuccessListener<Void> onSuccess,
                           @NonNull OnFailureListener onFailure) {
        validateName(name);
        validateEmail(email);
        getCurrentUser().addOnSuccessListener(current -> {
            if (current == null) {
                onFailure.onFailure(new IllegalStateException("Profile missing"));
                return;
            }
            current.setName(name.trim());
            current.setEmail(email.trim());
            current.setPhone(phone == null ? "" : phone.trim());
            updateUser(current).addOnSuccessListener(onSuccess).addOnFailureListener(onFailure);
        }).addOnFailureListener(onFailure);
    }

    /** Task-based delete by userId. Clears cache if you delete the active user. */
    public Task<Void> deleteUserProfile(@NonNull String userId) {
        if (currentUserCache != null && userId.equals(currentUserCache.getUserId())) {
            currentUserCache = null;
        }
        return repository.deleteUserById(userId);
    }

    // ---------------------------------------------------------------------
    // AUTH
    // ---------------------------------------------------------------------

    /** Task-based email/password auth. Caches user and binds deviceId on success. */
    public Task<Boolean> authenticate(@NonNull String email, @NonNull String password) {
        return repository.findUserByEmail(email)
                .onSuccessTask(q -> {
                    if (q.isEmpty()) return Tasks.forResult(false);
                    User u = q.getDocuments().get(0).toObject(User.class);
                    if (u == null) return Tasks.forResult(false);
                    boolean ok = hashPassword(password).equals(u.getPasswordHash());
                    if (!ok) return Tasks.forResult(false);
                    currentUserCache = u;
                    attachDeviceToCurrentUser(u);
                    return Tasks.forResult(true);
                });
    }

    /** Callback wrapper for {@link #authenticate(String, String)}. */
    public void authenticate(@NonNull String email,
                             @NonNull String password,
                             @NonNull OnSuccessListener<Boolean> onSuccess,
                             @NonNull OnFailureListener onFailure) {
        authenticate(email, password).addOnSuccessListener(onSuccess).addOnFailureListener(onFailure);
    }

    /** Task-based device auth. Caches user on success. */
    public Task<Boolean> authenticateDevice(@NonNull String deviceId) {
        return repository.findUserByDeviceId(deviceId)
                .onSuccessTask(q -> {
                    if (q.isEmpty()) return Tasks.forResult(false);
                    currentUserCache = q.getDocuments().get(0).toObject(User.class);
                    return Tasks.forResult(true);
                });
    }

    /** Callback wrapper for {@link #authenticateDevice(String)}. */
    public void authenticateDevice(@NonNull String deviceId,
                                   @NonNull OnSuccessListener<Boolean> onSuccess,
                                   @NonNull OnFailureListener onFailure) {
        authenticateDevice(deviceId).addOnSuccessListener(onSuccess).addOnFailureListener(onFailure);
    }

    /** Attach current deviceId to the user document (no-op if already matches). */
    private void attachDeviceToCurrentUser(@NonNull User user) {
        String deviceId = deviceIdManager.ensureDeviceId();
        if (deviceId.equals(user.getDeviceId())) return;
        user.setDeviceId(deviceId);
        updateUser(user).addOnFailureListener(e ->
                Log.w(TAG, "attachDeviceToCurrentUser: failed to persist deviceId", e));
    }

    /** Clear in-memory session only (no remote call). */
    public void logout() {
        currentUserCache = null;
    }

    // ---------------------------------------------------------------------
    // PASSWORD / PREFS (common helpers)
    // ---------------------------------------------------------------------

    public void updatePassword(@NonNull String newPassword,
                               @NonNull OnSuccessListener<Void> onSuccess,
                               @NonNull OnFailureListener onFailure) {
        validatePassword(newPassword);
        getCurrentUser().addOnSuccessListener(current -> {
            if (current == null) {
                onFailure.onFailure(new IllegalStateException("Profile missing"));
                return;
            }
            current.setPasswordHash(hashPassword(newPassword));
            updateUser(current).addOnSuccessListener(onSuccess).addOnFailureListener(onFailure);
        }).addOnFailureListener(onFailure);
    }

    public void updateNotificationPreference(boolean enabled,
                                             @Nullable OnSuccessListener<Void> onSuccess,
                                             @Nullable OnFailureListener onFailure) {
        getCurrentUser().addOnSuccessListener(current -> {
            if (current == null) {
                if (onFailure != null) onFailure.onFailure(new IllegalStateException("Profile missing"));
                return;
            }
            current.setNotificationsOn(enabled);
            updateUser(current)
                    .addOnSuccessListener(v -> { if (onSuccess != null) onSuccess.onSuccess(v); })
                    .addOnFailureListener(e -> { if (onFailure != null) onFailure.onFailure(e); });
        }).addOnFailureListener(e -> { if (onFailure != null) onFailure.onFailure(e); });
    }

    // ---------------------------------------------------------------------
    // VALIDATION / UTILS
    // ---------------------------------------------------------------------

    private void validateName(@NonNull String name) {
        if (name.trim().isEmpty()) throw new IllegalArgumentException("Name missing");
    }

    private void validateEmail(@NonNull String email) {
        String e = email.trim();
        if (e.isEmpty()) throw new IllegalArgumentException("Email missing");
        if (!emailPattern.matcher(e).matches()) throw new IllegalArgumentException("Email invalid");
    }

    private void validatePassword(@NonNull String password) {
        if (password.trim().length() < 6) throw new IllegalArgumentException("Password weak");
    }

    /** SHA-256 hashing used for demo-grade password storage. */
    public String hashPassword(@NonNull String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.trim().getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Hash error", e);
        }
    }
}
