package com.quantiagents.app.Services;

import com.google.android.gms.tasks.Task;
import com.quantiagents.app.models.User;

public class LoginService {

    private final UserService userService;

    public LoginService(UserService userService) {
        this.userService = userService;
    }

    public Task<Boolean> login(String email, String password) {
        return userService.authenticate(email, password);
    }

    public Task<Boolean> loginWithDevice(String deviceId) {
        return userService.authenticateDevice(deviceId);
    }

    public void logout() {
        userService.logout();
    }

    public Task<User> getActiveUser() {
        return userService.getCurrentUser();
    }
}