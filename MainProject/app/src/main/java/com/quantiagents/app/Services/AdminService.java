package com.quantiagents.app.Services;

import android.content.Context;

import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.quantiagents.app.Repository.AdminLogRepository;
import com.quantiagents.app.Repository.FireBaseRepository;
import com.quantiagents.app.Repository.UserRepository;
import com.quantiagents.app.Constants.constant;
import com.quantiagents.app.models.AdminActionLog;
import com.quantiagents.app.models.DeviceIdManager;
import com.quantiagents.app.models.Event;
import com.quantiagents.app.models.Image;
import com.quantiagents.app.models.Notification;
import com.quantiagents.app.models.User;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AdminService {

    private final EventService eventService;
    private final ImageService imageService;
    private final UserRepository userRepository; // Direct repo access for admin deletes
    private final UserService userService;       // Kept for local profile cleanup
    private final AdminLogRepository logRepository;
    private final DeviceIdManager deviceIdManager;
    private final NotificationService notificationService;
    private final RegistrationHistoryService registrationHistoryService;

    public AdminService(Context context) {
        ServiceLocator locator = new ServiceLocator(context);
        FireBaseRepository fbRepo = new FireBaseRepository();

        this.eventService = locator.eventService();
        this.imageService = locator.imageService();
        this.userService = locator.userService();
        this.userRepository = new UserRepository(fbRepo); // Needed for deleteUserById(String)
        this.logRepository = new AdminLogRepository(context);
        this.deviceIdManager = new DeviceIdManager(context);
        this.notificationService = new NotificationService(context);
        this.registrationHistoryService = new RegistrationHistoryService(context);
    }

    // --- Events ---

    public void getAllEvents(OnSuccessListener<List<Event>> onSuccess, OnFailureListener onFailure) {
        eventService.getAllEvents(onSuccess, onFailure);
    }

    private void logAction(String kind, String targetId, String note) {
        logRepository.append(new AdminActionLog(
                kind,
                targetId,
                System.currentTimeMillis(),
                deviceIdManager.ensureDeviceId(),
                note
        ));
    }

    public void removeEvent(String eventId, boolean confirmed, @Nullable String note,
                            OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        if (!confirmed) {
            onFailure.onFailure(new IllegalArgumentException("Confirmation required"));
            return;
        }

        // Move synchronous operations to background thread
        new Thread(() -> {
            try {
                // Get event before deleting to send notifications
                Event event = eventService.getEventById(eventId);
                if (event != null) {
                    // Send notifications before deleting
                    sendEventDeletedByAdminNotifications(event);
                }

                // 1. Attempt to delete images (best effort)
                imageService.deleteImagesByEventId(eventId);

                // 2. Delete event (async call, but we're already on background thread)
                eventService.deleteEvent(eventId,
                        aVoid -> {
                            logAction(AdminActionLog.KIND_EVENT, eventId, note);
                            onSuccess.onSuccess(aVoid);
                        },
                        onFailure);
            } catch (Exception e) {
                onFailure.onFailure(e);
            }
        }).start();
    }

    // --- Profiles ---



    public void removeProfile(String userId, boolean confirmed, @Nullable String note,
                              OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        if (!confirmed) {
            onFailure.onFailure(new IllegalArgumentException("Confirmation required"));
            return;
        }

        // 1. Delete from Firestore
        userRepository.deleteUserById(userId,
                aVoid -> {
                    // 2. If it matches local user, wipe local session
                    userService.getCurrentUser(
                            currentUser -> {
                                if (currentUser != null && userId.equals(currentUser.getUserId())) {
                                    userService.deleteUserProfile(
                                            unused -> {}, // Already deleted from remote, just clearing local
                                            e -> {}
                                    );
                                }
                            },
                            e -> {} // Ignore if we can't fetch current user
                    );

                    logAction(AdminActionLog.KIND_PROFILE, userId, note);
                    onSuccess.onSuccess(aVoid);
                },
                onFailure
        );
    }

    public void listAllProfiles(OnSuccessListener<List<User>> onSuccess, OnFailureListener onFailure) {
        userRepository.getAllUsers(onSuccess, onFailure);
    }

    // --- Images ---
    //us 03.03.01a: list all uploaded images
    /**
     * Synchronous listAllImages (Legacy support or if ImageService is sync).
     * If ImageService.getAllImages() is blocking, wrap in thread in ViewModel.
     */
    public List<Image> listAllImages() {
        return imageService.getAllImages();
    }

    /**
     * Asynchronous overload for listAllImages.
     * Useful if we want to move the thread logic here.
     */
    public void listAllImages(OnSuccessListener<List<Image>> onSuccess, OnFailureListener onFailure) {
        new Thread(() -> {
            try {
                List<Image> images = imageService.getAllImages();
                onSuccess.onSuccess(images);
            } catch (Exception e) {
                onFailure.onFailure(e);
            }
        }).start();
    }


    public void removeImage(String imageId, boolean confirmed, @Nullable String note,
                            OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        if (!confirmed) {
            onFailure.onFailure(new IllegalArgumentException("Confirmation required"));
            return;
        }

        // Move synchronous operations to background thread
        new Thread(() -> {
            try {
                // Get image to check if it's an event poster
                Image image = imageService.getImageById(imageId);
                if (image != null && image.getEventId() != null && !image.getEventId().trim().isEmpty()) {
                    // This is an event poster, send notification to organizer
                    Event event = eventService.getEventById(image.getEventId());
                    if (event != null) {
                        sendImageRemovedNotification(event);
                    }
                }

                imageService.deleteImage(imageId,
                        aVoid -> {
                            logAction(AdminActionLog.KIND_IMAGE, imageId, note);
                            onSuccess.onSuccess(aVoid);
                        },
                        onFailure);
            } catch (Exception e) {
                onFailure.onFailure(e);
            }
        }).start();
    }



    //us 03.03.01b+c: select an image and confirm deletion
    public boolean removeImage(String imageId, boolean confirmed, @Nullable String note) {
        if (!confirmed) {
            throw new IllegalArgumentException("confirmation required");
        }
        boolean removed = imageService.deleteImage(imageId);
        if (removed) {
            logRepository.append(new AdminActionLog(
                    AdminActionLog.KIND_IMAGE,
                    imageId,
                    System.currentTimeMillis(),
                    deviceIdManager.ensureDeviceId(),
                    note
            ));
        }
        return removed;
    }

    /**
     * Sends notifications when an event is deleted by admin.
     * Notifies all users in waiting list, selected list, and confirmed list, plus the organizer.
     */
    private void sendEventDeletedByAdminNotifications(Event event) {
        if (event == null) return;

        String eventId = event.getEventId();
        String organizerId = event.getOrganizerId();
        String eventName = event.getTitle() != null ? event.getTitle() : "Event";

        if (eventId == null) return;

        int eventIdInt = Math.abs(eventId.hashCode());
        
        // Get admin ID (current user)
        User adminUser = userService.getCurrentUser();
        int adminIdInt = -1; // Default to -1 if admin not found
        if (adminUser != null && adminUser.getUserId() != null) {
            adminIdInt = Math.abs(adminUser.getUserId().hashCode());
        }

        // Collect all affected user IDs
        Set<String> affectedUserIds = new HashSet<>();
        if (event.getWaitingList() != null) {
            affectedUserIds.addAll(event.getWaitingList());
        }
        if (event.getSelectedList() != null) {
            affectedUserIds.addAll(event.getSelectedList());
        }
        if (event.getConfirmedList() != null) {
            affectedUserIds.addAll(event.getConfirmedList());
        }

        // Send notification to all affected users
        for (String userId : affectedUserIds) {
            if (userId == null || userId.trim().isEmpty()) continue;
            int userIdInt = Math.abs(userId.hashCode());

            Notification notification = new Notification(
                    0, // Auto-generate ID
                    constant.NotificationType.BAD,
                    userIdInt,
                    adminIdInt, // senderId = AdminId
                    eventIdInt,
                    "Event Canceled",
                    "Due to unforeseen reasons, Event: " + eventName + " has been canceled by the Administrator. Please find another one."
            );

            notificationService.saveNotification(notification,
                    aVoid -> {},
                    e -> {}
            );
        }

        // Send notification to organizer
        if (organizerId != null && !organizerId.trim().isEmpty()) {
            int organizerIdInt = Math.abs(organizerId.hashCode());
            Notification organizerNotification = new Notification(
                    0, // Auto-generate ID
                    constant.NotificationType.BAD,
                    organizerIdInt,
                    adminIdInt, // senderId = AdminId
                    eventIdInt,
                    "Event Canceled",
                    "Due to community guideline violations, Event: " + eventName + " has been canceled by the Administrator."
            );

            notificationService.saveNotification(organizerNotification,
                    aVoid -> {},
                    e -> {}
            );
        }
    }

    /**
     * Sends notification to organizer when admin removes poster/image from event.
     */
    private void sendImageRemovedNotification(Event event) {
        if (event == null) return;

        String eventId = event.getEventId();
        String organizerId = event.getOrganizerId();
        String eventName = event.getTitle() != null ? event.getTitle() : "Event";

        if (eventId == null || organizerId == null) return;

        int eventIdInt = Math.abs(eventId.hashCode());
        int organizerIdInt = Math.abs(organizerId.hashCode());

        // Get admin ID (current user)
        User adminUser = userService.getCurrentUser();
        int adminIdInt = -1; // Default to -1 if admin not found
        if (adminUser != null && adminUser.getUserId() != null) {
            adminIdInt = Math.abs(adminUser.getUserId().hashCode());
        }

        Notification notification = new Notification(
                0, // Auto-generate ID
                constant.NotificationType.BAD,
                organizerIdInt,
                adminIdInt, // senderId = AdminId
                eventIdInt,
                "Image removed",
                "Due to community guideline violations, The poster for Event: " + eventName + " has been removed by the Administrator."
        );

        notificationService.saveNotification(notification,
                aVoid -> {},
                e -> {}
        );
    }
}