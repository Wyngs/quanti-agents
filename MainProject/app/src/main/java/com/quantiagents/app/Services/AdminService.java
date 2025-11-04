package com.quantiagents.app.Services;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.quantiagents.app.Repository.AdminLogRepository;
import com.quantiagents.app.Repository.ProfilesRepository;
import com.quantiagents.app.models.AdminActionLog;
import com.quantiagents.app.models.DeviceIdManager;
import com.quantiagents.app.models.Event;
import com.quantiagents.app.models.Image;
import com.quantiagents.app.models.User;
import com.quantiagents.app.models.UserSummary;

import java.util.List;

public class AdminService {

    private final EventService eventService;
    private final ImageService imageService;
    private final ProfilesRepository profilesRepository;
    private final AdminLogRepository logRepository;
    private final DeviceIdManager deviceIdManager;
    private final UserService userService;

    public AdminService(Context context) {
        // Instantiate services and repositories internally
        this.eventService = new EventService(context);
        this.imageService = new ImageService(context);
        this.profilesRepository = new ProfilesRepository(context);
        this.logRepository = new AdminLogRepository(context);
        this.deviceIdManager = new DeviceIdManager(context);
        this.userService = new UserService(context);
    }

    //us 03.01.01a: browse all events
    public List<Event> listAllEvents() {
        return eventService.getAllEvents();
    }

    //us 03.01.01b+c: select and confirm deletion of an event (cascades images)
    public boolean removeEvent(String eventId, boolean confirmed, @Nullable String note) {
        if (!confirmed) {
            throw new IllegalArgumentException("confirmation required");
        }
        // Convert String eventId to int
        int eventIdInt;
        try {
            eventIdInt = Integer.parseInt(eventId);
        } catch (NumberFormatException e) {
            Log.e("App", "Invalid event ID: " + eventId, e);
            return false;
        }
        
        //cascade: delete event poster images by event id
        imageService.deleteImagesByEventId(eventId);
        
        // Delete event using EventService
        boolean removed = eventService.deleteEvent(eventIdInt);
        
        // Log the deletion
        if (removed) {
            logRepository.append(new AdminActionLog(
                    AdminActionLog.KIND_EVENT,
                    eventId,
                    System.currentTimeMillis(),
                    deviceIdManager.ensureDeviceId(),
                    note
            ));
        }
        
        return removed;
    }

    //us 03.02.01a: browse all profiles (search handled in ui)
    public List<UserSummary> listAllProfiles() {
        return profilesRepository.listProfiles();
    }

    //us 03.02.01b+c: select a profile and confirm deletion; also clears local profile if it matches
    public boolean removeProfile(String userId, boolean confirmed, @Nullable String note) {
        if (!confirmed) {
            throw new IllegalArgumentException("confirmation required");
        }
        boolean removed = profilesRepository.deleteProfile(userId);
        //if the locally stored profile matches, clear it too
        User local = userService.getCurrentUser();
        if (local != null) {
            // Convert String userId to int for comparison
            try {
                int userIdInt = Integer.parseInt(userId);
                if (userIdInt == local.getUserId()) {
                    userService.deleteUserProfile();
                }
            } catch (NumberFormatException e) {
                // If userId is not a valid integer, skip comparison
            }
        }
        if (removed) {
            logRepository.append(new AdminActionLog(
                    AdminActionLog.KIND_PROFILE,
                    userId,
                    System.currentTimeMillis(),
                    deviceIdManager.ensureDeviceId(),
                    note
            ));
        }
        return removed;
    }

    //us 03.03.01a: list all uploaded images
    public List<Image> listAllImages() {
        return imageService.getAllImages();
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
}
