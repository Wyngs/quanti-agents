package com.quantiagents.app.Services;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.quantiagents.app.models.User;

/**
 * Thin auth wrapper that unifies Task-based and callback-based flows.
 * - Task APIs for chaining
 * - Callback helpers for legacy call sites
 * - In-memory cache of the active User
 *
 * NOTE: Any *Blocking() helper is for tests/background threads only. Do NOT use on the UI thread.
 */
public class LoginService {

    private final UserService userService;
    // simple in-memory cache of the last-resolved user
    private volatile @Nullable User current;

    public LoginService(@NonNull UserService userService) {
        this.userService = userService;
    }

    // ---------------------------------------------------------------------
    // Email/password login
    // ---------------------------------------------------------------------

    /**
     * Task-based login. Resolves to true on success, false if credentials are invalid.
     * Also updates the in-memory cached user when successful.
     */
    public Task<Boolean> login(@NonNull String email, @NonNull String password) {
        // userService.authenticate(...) should return Task<Boolean>
        return userService.authenticate(email, password)
                .onSuccessTask(success -> {
                    if (Boolean.TRUE.equals(success)) {
                        // fetch and cache current user after successful auth
                        return userService.getCurrentUser()
                                .onSuccessTask(user -> {
                                    current = user;
                                    return Tasks.forResult(true);
                                });
                    } else {
                        return Tasks.forResult(false);
                    }
                });
    }

    /** Callback convenience wrapper for {@link #login(String, String)}. */
    public void login(@NonNull String email,
                      @NonNull String password,
                      @NonNull OnSuccessListener<Boolean> onSuccess,
                      @NonNull OnFailureListener onFailure) {
        login(email, password).addOnSuccessListener(onSuccess).addOnFailureListener(onFailure);
    }

    // ---------------------------------------------------------------------
    // Device-based login
    // ---------------------------------------------------------------------

    /**
     * Task-based device login. Resolves true if a profile matched the device id.
     * Also updates the in-memory cached user when successful.
     */
    public Task<Boolean> loginWithDevice(@NonNull String deviceId) {
        // userService.authenticateDevice(...) should return Task<Boolean>
        return userService.authenticateDevice(deviceId)
                .onSuccessTask(success -> {
                    if (Boolean.TRUE.equals(success)) {
                        return userService.getCurrentUser()
                                .onSuccessTask(user -> {
                                    current = user;
                                    return Tasks.forResult(true);
                                });
                    } else {
                        return Tasks.forResult(false);
                    }
                });
    }

    /** Callback convenience wrapper for {@link #loginWithDevice(String)}. */
    public void loginWithDevice(@NonNull String deviceId,
                                @NonNull OnSuccessListener<Boolean> onSuccess,
                                @NonNull OnFailureListener onFailure) {
        loginWithDevice(deviceId).addOnSuccessListener(onSuccess).addOnFailureListener(onFailure);
    }

    // ---------------------------------------------------------------------
    // Session helpers
    // ---------------------------------------------------------------------

    /** Clears auth state and in-memory cache. */
    public void logout() {
        userService.logout();
        current = null;
    }

    /**
     * Returns a Task for the active user. If we already have one cached, resolves immediately.
     * Also refreshes the cache if we fetch from the service.
     */
    public Task<User> getActiveUser() {
        User cached = current;
        if (cached != null) return Tasks.forResult(cached);
        return userService.getCurrentUser().onSuccessTask(user -> {
            current = user;
            return Tasks.forResult(user);
        });
    }

    /** Callback convenience wrapper for {@link #getActiveUser()}. */
    public void getActiveUser(@NonNull OnSuccessListener<User> onSuccess,
                              @NonNull OnFailureListener onFailure) {
        getActiveUser().addOnSuccessListener(onSuccess).addOnFailureListener(onFailure);
    }

    /** Quick check for UI logic; does NOT force a fetch. */
    public boolean hasActiveSessionCached() {
        return current != null;
    }

    // ---------------------------------------------------------------------
    // Blocking helpers for tests/background ONLY (never on UI)
    // ---------------------------------------------------------------------

    /** Blocking helper to get the active user. */
    public @Nullable User getActiveUserBlocking() {
        try {
            return Tasks.await(getActiveUser());
        } catch (Exception e) {
            return null;
        }
    }

    /** Blocking helper that returns true iff an active user exists. */
    public boolean hasActiveSessionBlocking() {
        return getActiveUserBlocking() != null;
    }
}
