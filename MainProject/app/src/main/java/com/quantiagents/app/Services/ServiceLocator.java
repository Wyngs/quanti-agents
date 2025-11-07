package com.quantiagents.app.Services;

import android.content.Context;

import com.quantiagents.app.models.DeviceIdManager;

public class ServiceLocator {

    private final Context appContext;
    private UserService userService;
    private LoginService loginService;
    private AdminService adminService;
    private EventService eventService;
    private ImageService imageService;
    private NotificationService notificationService;
    private RegistrationHistoryService registrationHistoryService;
    private GeoLocationService geoLocationService;
    private LotteryResultService lotteryResultService;
    private QRCodeService qrCodeService;
    private DeviceIdManager deviceIdManager;

    public ServiceLocator(Context context) {
        // Stick with the app context to avoid leaks.
        this.appContext = context.getApplicationContext();
    }

    public synchronized UserService userService() {
        if (userService == null) {
            // UserService instantiates its own repositories internally
            userService = new UserService(appContext, null);
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

    public synchronized ImageService imageService() {
        if (imageService == null) {
            // ImageService instantiates its own repositories internally
            imageService = new ImageService(appContext);
        }
        return imageService;
    }

    public synchronized NotificationService notificationService() {
        if (notificationService == null) {
            // NotificationService instantiates its own repositories internally
            notificationService = new NotificationService(appContext);
        }
        return notificationService;
    }

    public synchronized RegistrationHistoryService registrationHistoryService() {
        if (registrationHistoryService == null) {
            // RegistrationHistoryService instantiates its own repositories internally
            registrationHistoryService = new RegistrationHistoryService(appContext);
        }
        return registrationHistoryService;
    }

    public synchronized GeoLocationService geoLocationService() {
        if (geoLocationService == null) {
            // GeoLocationService instantiates its own repositories internally
            geoLocationService = new GeoLocationService(appContext);
        }
        return geoLocationService;
    }

    public synchronized LotteryResultService lotteryResultService() {
        if (lotteryResultService == null) {
            // LotteryResultService instantiates its own repositories internally
            lotteryResultService = new LotteryResultService(appContext);
        }
        return lotteryResultService;
    }

    public synchronized QRCodeService qrCodeService() {
        if (qrCodeService == null) {
            // QRCodeService instantiates its own repositories internally
            qrCodeService = new QRCodeService(appContext);
        }
        return qrCodeService;
    }

    public synchronized DeviceIdManager deviceIdManager() {
        if (deviceIdManager == null) {
            // DeviceIdManager manages device identity
            deviceIdManager = new DeviceIdManager(appContext);
        }
        return deviceIdManager;
    }
    // In com.quantiagents.app.Services.ServiceLocator
    public void replaceUserService(UserService svc) { this.userService = svc; }
    public void replaceEventService(EventService svc) { this.eventService = svc; }
    public void replaceRegistrationHistoryService(RegistrationHistoryService svc) { this.registrationHistoryService = svc; }

}
