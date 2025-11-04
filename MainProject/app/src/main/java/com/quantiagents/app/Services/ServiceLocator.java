package com.quantiagents.app.Services;

import android.content.Context;

public class ServiceLocator {

    private final Context appContext;
    private UserService userService;
    private LoginService loginService;
    private AdminService adminService;
    private EventService eventService;

    public ServiceLocator(Context context) {
        // Stick with the app context to avoid leaks.
        this.appContext = context.getApplicationContext();
    }

    public synchronized UserService userService() {
        if (userService == null) {
            // UserService instantiates its own repositories internally
            userService = new UserService(appContext);
        }
        return userService;
    }

    public synchronized LoginService loginService() {
        if (loginService == null) {
            // Light wrapper to handle auth checks.
            loginService = new LoginService(userService());
        }
        return loginService;
    }

    public synchronized AdminService adminService() {
        if (adminService == null) {
            // AdminService instantiates its own repositories internally
            adminService = new AdminService(appContext);
        }
        return adminService;
    }

    public synchronized EventService eventService() {
        if (eventService == null) {
            // EventService instantiates its own repositories internally
            eventService = new EventService(appContext);
        }
        return eventService;
    }
}
