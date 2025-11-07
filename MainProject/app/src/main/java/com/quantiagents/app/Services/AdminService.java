package com.quantiagents.app.Services;

import android.content.Context;

import androidx.annotation.Nullable;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.QuerySnapshot;
import com.quantiagents.app.Repository.AdminLogRepository;
import com.quantiagents.app.models.AdminActionLog;
import com.quantiagents.app.models.DeviceIdManager;

public class AdminService {

    private final EventService eventService;
    private final ImageService imageService;
    private final AdminLogRepository logRepository;
    private final DeviceIdManager deviceIdManager;
    private final UserService userService;

    public AdminService(Context context) {
        ServiceLocator locator = new ServiceLocator(context);
        this.eventService = locator.eventService();
        this.imageService = locator.imageService();
        this.logRepository = new AdminLogRepository(context);
        this.deviceIdManager = new DeviceIdManager(context);
        this.userService = new UserService(context, null);
    }

    public Task<QuerySnapshot> listAllEvents() {
        return eventService.getAllEvents();
    }

    public Task<Void> removeEvent(String eventId, boolean confirmed, @Nullable String note) {
        if (!confirmed) {
            throw new IllegalArgumentException("confirmation required");
        }

        return imageService.deleteImagesByEventId(eventId).onSuccessTask(aVoid ->
                eventService.deleteEvent(eventId)
        ).addOnSuccessListener(aVoid -> {
            logRepository.append(new AdminActionLog(
                    AdminActionLog.KIND_EVENT,
                    eventId,
                    System.currentTimeMillis(),
                    deviceIdManager.ensureDeviceId(),
                    note
            ));
        });
    }

    public Task<QuerySnapshot> listAllProfiles() {
        return userService.getAllUsers();
    }

    public Task<Void> removeProfile(String userId, boolean confirmed, @Nullable String note) {
        if (!confirmed) {
            throw new IllegalArgumentException("confirmation required");
        }

        return userService.deleteUserProfile(userId)
                .addOnSuccessListener(aVoid -> {
                    logRepository.append(new AdminActionLog(
                            AdminActionLog.KIND_PROFILE,
                            userId,
                            System.currentTimeMillis(),
                            deviceIdManager.ensureDeviceId(),
                            note
                    ));
                });
    }

    public Task<QuerySnapshot> listAllImages() {
        return imageService.getAllImages();
    }

    public Task<Void> removeImage(String imageId, boolean confirmed, @Nullable String note) {
        if (!confirmed) {
            throw new IllegalArgumentException("confirmation required");
        }

        return imageService.deleteImageById(imageId)
                .addOnSuccessListener(aVoid -> {
                    logRepository.append(new AdminActionLog(
                            AdminActionLog.KIND_IMAGE,
                            imageId,
                            System.currentTimeMillis(),
                            deviceIdManager.ensureDeviceId(),
                            note
                    ));
                });
    }
}