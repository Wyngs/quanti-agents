package com.quantiagents.app.Services;

import androidx.annotation.Nullable;

import com.quantiagents.app.models.User;

public class LoginService {

    private final UserService userService;
    @Nullable
    private User current;

    public LoginService(UserService userService) {
        this.userService = userService;
    }

    public boolean login(String email, String password) {
        boolean success = userService.authenticate(email, password);
        if (success) {
            current = userService.getCurrentUser();
            userService.attachDeviceToCurrentUser();
        }
        return success;
    }

    public boolean loginWithDevice(String deviceId) {
        boolean success = userService.authenticateDevice(deviceId);
        if (success) {
            current = userService.getCurrentUser();
        }
        return success;
    }

    public void logout() {
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
