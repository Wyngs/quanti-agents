package com.quantiagents.app.Services;

import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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

    public void login(String email, String password, OnSuccessListener<Boolean> onSuccess, OnFailureListener onFailure) {
        // Async version for UI thread
        userService.authenticate(email, password,
                success -> {
                    if (success) {
                        userService.getCurrentUser(
                                user -> {
                                    current = user;
                                    userService.attachDeviceToCurrentUser();
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

    public boolean loginWithDevice(String deviceId) {
        boolean success = userService.authenticateDevice(deviceId);
        if (success) {
            current = userService.getCurrentUser();
        }
        return success;
    }

    public void loginWithDevice(String deviceId, OnSuccessListener<Boolean> onSuccess, OnFailureListener onFailure) {
        // Async version for UI thread
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
