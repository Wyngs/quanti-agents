package com.quantiagents.app.Repository;

import android.content.Context;

import com.quantiagents.app.Services.AdminService;
import com.quantiagents.app.models.DeviceIdManager;
import com.quantiagents.app.Services.LoginService;
import com.quantiagents.app.Services.UserService;

public class ServiceLocator {

    private final Context appContext;
    private FireBaseRepository firebaseContext;
    private DeviceIdManager deviceIdManager;
    private UserRepository userRepository;
    private UserService userService;
    private LoginService loginService;


    //admin-related dependencies
    private EventRepository eventRepository;
    private ImageRepository imageRepository;
    private ProfilesRepository profilesRepository;
    private AdminLogRepository adminLogRepository;
    private AdminService adminService;

    public ServiceLocator(Context context) {
        // Stick with the app context to avoid leaks.
        this.appContext = context.getApplicationContext();
    }

    public synchronized DeviceIdManager deviceIdManager() {
        if (deviceIdManager == null) {
            // Keep device id generation in one place.
            deviceIdManager = new DeviceIdManager(appContext);
        }
        return deviceIdManager;
    }

    public synchronized UserRepository userRepository() {
        if (userRepository == null) {
            // Shared prefs store my entrant snapshot.
            userRepository = new UserRepository(firebaseContext);
        }
        return userRepository;
    }

    public synchronized UserService userService() {
        if (userService == null) {
            // Build user logic around the repo and device id.
            userService = new UserService(userRepository(), deviceIdManager());
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
//=== admin graph ===

    public synchronized EventRepository eventRepository() {
        if (eventRepository == null) {
            //stores a lightweight event catalog for admin
            eventRepository = new EventRepository(appContext);
        }
        return eventRepository;
    }

    public synchronized ImageRepository imageRepository() {
        if (imageRepository == null) {
            //stores image metadata for admin
            imageRepository = new ImageRepository(appContext);
        }
        return imageRepository;
    }

    public synchronized ProfilesRepository profilesRepository() {
        if (profilesRepository == null) {
            //stores minimal profile index for admin
            profilesRepository = new ProfilesRepository(appContext);
        }
        return profilesRepository;
    }

    public synchronized AdminLogRepository adminLogRepository() {
        if (adminLogRepository == null) {
            //append-only audit log of admin deletions
            adminLogRepository = new AdminLogRepository(appContext);
        }
        return adminLogRepository;
    }

    public synchronized AdminService adminService() {
        if (adminService == null) {
            //compose admin service from repos and identity
            adminService = new AdminService(
                    eventRepository(),
                    imageRepository(),
                    profilesRepository(),
                    adminLogRepository(),
                    deviceIdManager(),
                    userRepository()
            );
        }
        return adminService;
    }
}
