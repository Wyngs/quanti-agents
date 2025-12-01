package com.quantiagents.app.Services;

import android.content.Context;

import com.quantiagents.app.models.DeviceIdManager;

/**
 * Service locator pattern implementation for managing service instances.
 * Provides lazy initialization and singleton-like access to all services.
 * Uses application context to avoid memory leaks.
 */
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
    private ChatService chatService;
    private DeviceIdManager deviceIdManager;

    /**
     * Constructor that initializes the service locator with application context.
     * Stick with the app context to avoid leaks.
     *
     * @param context The Android context (will be converted to application context)
     */
    public ServiceLocator(Context context) {
        // Stick with the app context to avoid leaks.
        this.appContext = context.getApplicationContext();
    }

    /**
     * Gets or creates the UserService instance (lazy initialization).
     * UserService instantiates its own repositories internally.
     *
     * @return The UserService instance
     */
    public synchronized UserService userService() {
        if (userService == null) {
            // UserService instantiates its own repositories internally
            userService = new UserService(appContext);
        }
        return userService;
    }

    /**
     * Gets or creates the LoginService instance (lazy initialization).
     * Light wrapper to handle auth checks.
     *
     * @return The LoginService instance
     */
    public synchronized LoginService loginService() {
        if (loginService == null) {
            // Light wrapper to handle auth checks.
            loginService = new LoginService(userService());
        }
        return loginService;
    }

    /**
     * Gets or creates the AdminService instance (lazy initialization).
     * AdminService instantiates its own repositories internally.
     *
     * @return The AdminService instance
     */
    public synchronized AdminService adminService() {
        if (adminService == null) {
            // AdminService instantiates its own repositories internally
            adminService = new AdminService(appContext);
        }
        return adminService;
    }

    /**
     * Gets or creates the EventService instance (lazy initialization).
     * EventService instantiates its own repositories internally.
     *
     * @return The EventService instance
     */
    public synchronized EventService eventService() {
        if (eventService == null) {
            // EventService instantiates its own repositories internally
            eventService = new EventService(appContext);
        }
        return eventService;
    }

    /**
     * Gets or creates the ImageService instance (lazy initialization).
     * ImageService instantiates its own repositories internally.
     *
     * @return The ImageService instance
     */
    public synchronized ImageService imageService() {
        if (imageService == null) {
            // ImageService instantiates its own repositories internally
            imageService = new ImageService(appContext);
        }
        return imageService;
    }

    /**
     * Gets or creates the NotificationService instance (lazy initialization).
     * NotificationService instantiates its own repositories internally.
     *
     * @return The NotificationService instance
     */
    public synchronized NotificationService notificationService() {
        if (notificationService == null) {
            // NotificationService instantiates its own repositories internally
            notificationService = new NotificationService(appContext);
        }
        return notificationService;
    }

    /**
     * Gets or creates the RegistrationHistoryService instance (lazy initialization).
     * RegistrationHistoryService instantiates its own repositories internally.
     *
     * @return The RegistrationHistoryService instance
     */
    public synchronized RegistrationHistoryService registrationHistoryService() {
        if (registrationHistoryService == null) {
            // RegistrationHistoryService instantiates its own repositories internally
            registrationHistoryService = new RegistrationHistoryService(appContext);
        }
        return registrationHistoryService;
    }

    /**
     * Gets or creates the GeoLocationService instance (lazy initialization).
     * GeoLocationService instantiates its own repositories internally.
     *
     * @return The GeoLocationService instance
     */
    public synchronized GeoLocationService geoLocationService() {
        if (geoLocationService == null) {
            // GeoLocationService instantiates its own repositories internally
            geoLocationService = new GeoLocationService(appContext);
        }
        return geoLocationService;
    }

    /**
     * Gets or creates the LotteryResultService instance (lazy initialization).
     * LotteryResultService instantiates its own repositories internally.
     *
     * @return The LotteryResultService instance
     */
    public synchronized LotteryResultService lotteryResultService() {
        if (lotteryResultService == null) {
            // LotteryResultService instantiates its own repositories internally
            lotteryResultService = new LotteryResultService(appContext);
        }
        return lotteryResultService;
    }

    /**
     * Gets or creates the QRCodeService instance (lazy initialization).
     * QRCodeService instantiates its own repositories internally.
     *
     * @return The QRCodeService instance
     */
    public synchronized QRCodeService qrCodeService() {
        if (qrCodeService == null) {
            // QRCodeService instantiates its own repositories internally
            qrCodeService = new QRCodeService(appContext);
        }
        return qrCodeService;
    }

    /**
     * Gets or creates the ChatService instance (lazy initialization).
     * ChatService instantiates its own repositories internally.
     *
     * @return The ChatService instance
     */
    public synchronized ChatService chatService() {
        if (chatService == null) {
            // ChatService instantiates its own repositories internally
            chatService = new ChatService(appContext);
        }
        return chatService;
    }

    /**
     * Gets or creates the DeviceIdManager instance (lazy initialization).
     * DeviceIdManager manages device identity.
     *
     * @return The DeviceIdManager instance
     */
    public synchronized DeviceIdManager deviceIdManager() {
        if (deviceIdManager == null) {
            // DeviceIdManager manages device identity
            deviceIdManager = new DeviceIdManager(appContext);
        }
        return deviceIdManager;
    }

    /**
     * Replaces the UserService instance (useful for tests/mocks).
     *
     * @param svc The UserService instance to use
     */
    public void replaceUserService(UserService svc) { this.userService = svc; }
    
    /**
     * Replaces the EventService instance (useful for tests/mocks).
     *
     * @param svc The EventService instance to use
     */
    public void replaceEventService(EventService svc) { this.eventService = svc; }
    
    /**
     * Replaces the RegistrationHistoryService instance (useful for tests/mocks).
     *
     * @param svc The RegistrationHistoryService instance to use
     */
    public void replaceRegistrationHistoryService(RegistrationHistoryService svc) { this.registrationHistoryService = svc; }
    
    /**
     * Override the default EventService instance (useful for tests/mocks).
     *
     * @param svc The EventService instance to use
     */
    public synchronized void setEventService(EventService svc) {
        this.eventService = svc;
    }

    /**
     * Override the default RegistrationHistoryService instance (useful for tests/mocks).
     *
     * @param svc The RegistrationHistoryService instance to use
     */
    public synchronized void setRegistrationHistoryService(RegistrationHistoryService svc) {
        this.registrationHistoryService = svc;
    }
}
