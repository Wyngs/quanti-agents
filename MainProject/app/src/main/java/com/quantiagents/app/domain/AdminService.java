package com.quantiagents.app.domain;

import androidx.annotation.Nullable;

import com.quantiagents.app.data.AdminLogRepository;
import com.quantiagents.app.data.EventRepository;
import com.quantiagents.app.data.ImageRepository;
import com.quantiagents.app.data.ProfilesRepository;
import com.quantiagents.app.data.UserRepository;

import java.util.List;

public class AdminService {

    private final EventRepository eventRepository;
    private final ImageRepository imageRepository;
    private final ProfilesRepository profilesRepository;
    private final AdminLogRepository logRepository;
    private final DeviceIdManager deviceIdManager;
    private final UserRepository userRepository;

    public AdminService(EventRepository eventRepository,
                        ImageRepository imageRepository,
                        ProfilesRepository profilesRepository,
                        AdminLogRepository logRepository,
                        DeviceIdManager deviceIdManager,
                        UserRepository userRepository) {
        //central admin orchestration layer
        this.eventRepository = eventRepository;
        this.imageRepository = imageRepository;
        this.profilesRepository = profilesRepository;
        this.logRepository = logRepository;
        this.deviceIdManager = deviceIdManager;
        this.userRepository = userRepository;
    }

    //us 03.01.01a: browse all events
    public List<Event> listAllEvents() {
        return eventRepository.listEvents();
    }

    //us 03.01.01b+c: select and confirm deletion of an event (cascades images)
    public boolean removeEvent(String eventId, boolean confirmed, @Nullable String note) {
        if (!confirmed) {
            throw new IllegalArgumentException("confirmation required");
        }
        //cascade: delete event poster images by event id
        imageRepository.deleteImagesByEventId(eventId);
        boolean removed = eventRepository.deleteEvent(eventId);
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
        User local = userRepository.getUser();
        if (local != null && userId.equals(local.getUserId())) {
            userRepository.clearUser();
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
    public List<StoredImage> listAllImages() {
        return imageRepository.listImages();
    }

    //us 03.03.01b+c: select an image and confirm deletion
    public boolean removeImage(String imageId, boolean confirmed, @Nullable String note) {
        if (!confirmed) {
            throw new IllegalArgumentException("confirmation required");
        }
        boolean removed = imageRepository.deleteImage(imageId);
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
