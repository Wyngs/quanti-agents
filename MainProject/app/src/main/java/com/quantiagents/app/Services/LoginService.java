package com.quantiagents.app.Services;

import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.quantiagents.app.models.User;

/**
 * Thin wrapper so I can reuse the same auth flow everywhere without duplicating logic.
 */
public class LoginService {

    private final UserService userService;
    @Nullable
    private User current;

    public LoginService(UserService userService) {
        this.userService = userService;
    }

    /**
     * Quick synchronous login I still use in tests and blocking flows.
     *
     * @return true if the credentials matched an existing profile
     */
    public boolean login(String email, String password) {
        boolean success = userService.authenticate(email, password);
        if (success) {
            current = userService.getCurrentUser();
            userService.attachDeviceToCurrentUser(current);
        }
        return success;
    }

    /**
     * Async login tied to the UI; lets me show loading states without blocking.
     * <p>
     * Fixed to capture the User object from authentication directly, avoiding
     * issues where 'getCurrentUser' (by device ID) would return null if logging
     * in on a new device.
     *
     * @param onSuccess emits true when we logged in, false when credentials were off
     */
    public void login(String email,
                      String password,
                      OnSuccessListener<Boolean> onSuccess,
                      OnFailureListener onFailure) {
        userService.authenticate(email, password,
                user -> {
                    if (user != null) {
                        // Credentials match. Set the current session.
                        current = user;
                        // IMPORTANT: Link this device ID to the user profile immediately.
                        // This ensures subsequent startup/splash checks pass.
                        userService.attachDeviceToCurrentUser(user);
                        onSuccess.onSuccess(true);
                    } else {
                        // Invalid credentials
                        onSuccess.onSuccess(false);
                    }
                },
                onFailure
        );
    }

    /**
     * Legacy device-only login for cold starts; still used before the async path comes up.
     *
     * @return true if a profile exists for the supplied device id
     */
    public boolean loginWithDevice(String deviceId) {
        boolean success = userService.authenticateDevice(deviceId);
        if (success) {
            current = userService.getCurrentUser();
        }
        return success;
    }

    /**
     * Async variant so SplashActivity can gate on callbacks instead of blocking.
     *
     * @param onSuccess emits true when the device id matched a user
     */
    public void loginWithDevice(String deviceId,
                                OnSuccessListener<Boolean> onSuccess,
                                OnFailureListener onFailure) {
        userService.authenticateDevice(deviceId,
                success -> {
                    if (success) {
                        userService.getCurrentUser(
                                user -> {
                                    current = user;
                                    onSuccess.onSuccess(true);
                                },
                                onFailure
                        );
                    } else {
                        onSuccess.onSuccess(false);
                    }
                },
                onFailure
        );
    }

    /**
     * Clears the cached reference; caller is responsible for clearing UI state.
     */
    public void logout() {
        current = null;
    }

    /**
     * Returns the cached user if we already logged in.
     */
    @Nullable
    public User getActiveUser() {
        return current;
    }

    /**
     * Helper for fragments/activities that just need to know if someone is logged in.
     */
    public boolean hasActiveSession() {
        return getActiveUser() != null;
    }
}