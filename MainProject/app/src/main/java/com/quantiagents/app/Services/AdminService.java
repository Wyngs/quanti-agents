package com.quantiagents.app.Services;

import android.content.Context;

import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.quantiagents.app.Repository.AdminLogRepository;
import com.quantiagents.app.Repository.FireBaseRepository;
import com.quantiagents.app.Repository.UserRepository;
import com.quantiagents.app.models.AdminActionLog;
import com.quantiagents.app.models.DeviceIdManager;
import com.quantiagents.app.models.Event;
import com.quantiagents.app.models.Image;
import com.quantiagents.app.models.User;
import android.widget.EditText;

import java.util.List;

public class AdminService {

    private final EventService eventService;
    private final ImageService imageService;
    private final UserRepository userRepository; // Direct repo access for admin deletes
    private final UserService userService;       // Kept for local profile cleanup
    private final AdminLogRepository logRepository;
    private final DeviceIdManager deviceIdManager;

    public AdminService(Context context) {
        ServiceLocator locator = new ServiceLocator(context);
        FireBaseRepository fbRepo = new FireBaseRepository();

        this.eventService = locator.eventService();
        this.imageService = locator.imageService();
        this.userService = locator.userService();
        this.userRepository = new UserRepository(fbRepo); // Needed for deleteUserById(String)
        this.logRepository = new AdminLogRepository(context);
        this.deviceIdManager = new DeviceIdManager(context);
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

        // 1. Attempt to delete images (best effort)
        imageService.deleteImagesByEventId(eventId);

        // 2. Delete event
        eventService.deleteEvent(eventId,
                aVoid -> {
                    logAction(AdminActionLog.KIND_EVENT, eventId, note);
                    onSuccess.onSuccess(aVoid);
                },
                onFailure);
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

        imageService.deleteImage(imageId,
                aVoid -> {
                    logAction(AdminActionLog.KIND_IMAGE, imageId, note);
                    onSuccess.onSuccess(aVoid);
                },
                onFailure);
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