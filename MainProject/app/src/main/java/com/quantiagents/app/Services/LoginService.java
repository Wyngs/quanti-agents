package com.quantiagents.app.Services;

import android.content.Context;

import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.quantiagents.app.models.User;

/**
 * Thin wrapper so the same auth flow can be reused everywhere without duplicating logic.
 * Manages login state and provides both synchronous and asynchronous authentication methods.
 */
public class LoginService {

    private final UserService userService;
    @Nullable
    private User current;

    /**
     * Constructor that initializes the LoginService with a UserService dependency.
     *
     * @param userService The UserService instance to use for authentication
     */
    public LoginService(UserService userService) {
        this.userService = userService;
    }

    /**
     * Quick synchronous login used in tests and blocking flows.
     *
     * @param email The email address or username to authenticate with
     * @param password The plain text password
     * @return true if the credentials matched an existing profile
     */
    public boolean login(String email, String password) {
        return login(email, password, true);
    }

    /**
     * Quick synchronous login used in tests and blocking flows with remember me option.
     *
     * @param email The email address or username to authenticate with
     * @param password The plain text password
     * @param rememberMe Whether to save the device ID for automatic login
     * @return true if the credentials matched an existing profile
     */
    public boolean login(String email, String password, boolean rememberMe) {
        boolean success = userService.authenticate(email, password);
        if (success) {
            current = userService.getCurrentUser();
            // Always attach device ID to user for the current session to work properly.
            // This ensures fragments can retrieve the user by device ID.
            // The "Remember Me" preference is tracked separately to control auto-login.
            userService.attachDeviceToCurrentUser(current);
        }
        return success;
    }

    /**
     * Async login tied to the UI; allows showing loading states without blocking.
     * <p>
     * Fixed to capture the User object from authentication directly, avoiding
     * issues where 'getCurrentUser' (by device ID) would return null if logging
     * in on a new device.
     *
     * @param email The email address or username to authenticate with
     * @param password The plain text password
     * @param onSuccess Callback that emits true when logged in, false when credentials were invalid
     * @param onFailure Callback invoked if an error occurs during authentication
     */
    public void login(String email,
                      String password,
                      OnSuccessListener<Boolean> onSuccess,
                      OnFailureListener onFailure) {
        login(email, password, true, onSuccess, onFailure);
    }

    /**
     * Async login tied to the UI; allows showing loading states without blocking.
     * <p>
     * Fixed to capture the User object from authentication directly, avoiding
     * issues where 'getCurrentUser' (by device ID) would return null if logging
     * in on a new device.
     *
     * @param email The email address or username to authenticate with
     * @param password The plain text password
     * @param rememberMe Whether to save the device ID for automatic login
     * @param onSuccess Callback that emits true when logged in, false when credentials were invalid
     * @param onFailure Callback invoked if an error occurs during authentication
     */
    public void login(String email,
                      String password,
                      boolean rememberMe,
                      OnSuccessListener<Boolean> onSuccess,
                      OnFailureListener onFailure) {
        userService.authenticate(email, password,
                user -> {
                    if (user != null) {
                        // Credentials match. Set the current session.
                        current = user;
                        // Always attach device ID to user for the current session to work properly.
                        // This ensures fragments can retrieve the user by device ID.
                        // The "Remember Me" preference is tracked separately to control auto-login.
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
     * @param deviceId The device ID to authenticate with
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
     * @param deviceId The device ID to authenticate with
     * @param onSuccess Callback that emits true when the device id matched a user
     * @param onFailure Callback invoked if an error occurs during authentication
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
     * Does not delete the user profile or device ID.
     */
    public void logout() {
        current = null;
    }

    /**
     * Returns the cached user if already logged in.
     *
     * @return The currently logged in user, or null if not logged in
     */
    @Nullable
    public User getActiveUser() {
        return current;
    }

    /**
     * Helper for fragments/activities that just need to know if someone is logged in.
     *
     * @return True if there is an active session, false otherwise
     */
    public boolean hasActiveSession() {
        return getActiveUser() != null;
    }

}