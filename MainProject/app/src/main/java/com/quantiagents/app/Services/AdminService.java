package com.quantiagents.app.Services;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.quantiagents.app.Repository.AdminLogRepository;
import com.quantiagents.app.Repository.FireBaseRepository;
import com.quantiagents.app.Repository.UserRepository;
import com.quantiagents.app.models.AdminActionLog;
import com.quantiagents.app.models.DeviceIdManager;
import com.quantiagents.app.models.Event;
import com.quantiagents.app.models.Image;
import com.quantiagents.app.models.User;
import com.quantiagents.app.models.UserSummary;

import java.util.ArrayList;
import java.util.List;

public class AdminService {

    private final EventService eventService;
    private final ImageService imageService;
    private final UserRepository userRepository;
    private final AdminLogRepository logRepository;
    private final DeviceIdManager deviceIdManager;
    private final UserService userService;

    public AdminService(Context context) {
        // Instantiate services and repositories internally
        this.eventService = new EventService(context);
        this.imageService = new ImageService(context);
        this.logRepository = new AdminLogRepository(context);
        this.deviceIdManager = new DeviceIdManager(context);
        this.userService = new UserService(context);

        // Use the main FireBaseRepository to get the real user list
        FireBaseRepository fireBaseRepository = new FireBaseRepository();
        this.userRepository = new UserRepository(fireBaseRepository);
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

        //cascade: delete event poster images by event id
        imageService.deleteImagesByEventId(eventId);

        // Delete event using EventService
        boolean removed = eventService.deleteEvent(eventId);

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
        // Get all users from the real repository
        List<User> allUsers = userRepository.getAllUsers();
        // Convert them to UserSummary objects for the admin view
        List<UserSummary> summaries = new ArrayList<>();
        for (User user : allUsers) {
            summaries.add(new UserSummary(user.getUserId(), user.getName(), user.getEmail()));
        }
        return summaries;
    }

    //us 03.02.01b+c: select a profile and confirm deletion; also clears local profile if it matches
    public boolean removeProfile(String userId, boolean confirmed, @Nullable String note) {
        if (!confirmed) {
            throw new IllegalArgumentException("confirmation required");
        }

        // Delete from the real repository
        boolean removed = userRepository.deleteUserById(userId);

        //if the locally stored profile matches, clear it too
        User local = userService.getCurrentUser();
        if (local != null && userId != null && userId.equals(local.getUserId())) {
            userService.deleteUserProfile();
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