package com.quantiagents.app.Services;

import androidx.annotation.Nullable;

import com.quantiagents.app.models.User;

public class LoginService {

    private final UserService userService;
    @Nullable
    private User current;

    public LoginService(UserService userService) {
        // Hold onto user service so I can delegate checks.
        this.userService = userService;
    }

    public boolean login(String email, String password) {
        // Authenticate against the cached profile.
        boolean success = userService.authenticate(email, password);
        if (success) {
            current = userService.getCurrentUser();
            userService.attachDeviceToCurrentUser();
        }
        return success;
    }

    public boolean loginWithDevice(String deviceId) {
        // Device-only login path from splash.
        boolean success = userService.authenticateDevice(deviceId);
        if (success) {
            current = userService.getCurrentUser();
        }
        return success;
    }

    public void logout() {
        // Drop my in-memory pointer.
        current = null;
    }

    @Nullable
    public User getActiveUser() {
        if (current != null) {
            return current;
        }
        return userService.getCurrentUser();
    }

    public boolean hasActiveSession() {
        return getActiveUser() != null;
    }
}
