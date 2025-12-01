package com.quantiagents.app.ui.admin.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.quantiagents.app.App;
import com.quantiagents.app.Services.AdminService;
import com.quantiagents.app.models.Event;
import com.quantiagents.app.models.Image;
import com.quantiagents.app.models.User;
import com.quantiagents.app.models.UserSummary;
import com.quantiagents.app.Services.NotificationService;
import com.quantiagents.app.models.Notification;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ViewModel for admin management screens that handles events, profiles, images, and notifications.
 * Supports search functionality and CRUD operations for admin entities.
 */
public class AdminEventsViewModel extends AndroidViewModel {

    private final AdminService adminService;
    private final NotificationService notificationService;

    // Master lists to support Search functionality
    private List<Event> masterEventList = new ArrayList<>();
    private List<UserSummary> masterOrganizerList = new ArrayList<>();
    private List<UserSummary> masterProfileList = new ArrayList<>();
    private List<Image> masterImageList = new ArrayList<>();

    private final MutableLiveData<List<Event>> events = new MutableLiveData<>();
    
    /**
     * Gets the events LiveData.
     *
     * @return LiveData containing the list of events
     */
    public LiveData<List<Event>> getEvents() { return events; }

    private final MutableLiveData<List<UserSummary>> profiles = new MutableLiveData<>();
    
    /**
     * Gets the profiles LiveData.
     *
     * @return LiveData containing the list of user profiles
     */
    public LiveData<List<UserSummary>> getProfiles() { return profiles; }

    // ADDED: LiveData for Organizers
    private final MutableLiveData<List<UserSummary>> organizers = new MutableLiveData<>();
    public LiveData<List<UserSummary>> getOrganizers() { return organizers; }

    private final MutableLiveData<List<Image>> images = new MutableLiveData<>();
    
    /**
     * Gets the images LiveData.
     *
     * @return LiveData containing the list of images
     */
    public LiveData<List<Image>> getImages() { return images; }

    // --- NOTIFICATIONS ---
    private final MutableLiveData<List<Notification>> notifications = new MutableLiveData<>();
    
    /**
     * Gets the notifications LiveData.
     *
     * @return LiveData containing the list of notifications
     */
    public LiveData<List<Notification>> getNotifications() { return notifications; }

    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();
    
    /**
     * Gets the toast message LiveData.
     *
     * @return LiveData containing toast messages to display
     */
    public LiveData<String> getToastMessage() { return toastMessage; }

    /**
     * Constructor that initializes the ViewModel with required services.
     *
     * @param application The application instance
     */
    public AdminEventsViewModel(@NonNull Application application) {
        super(application);
        App app = (App) application;
        this.adminService = app.locator().adminService();
        this.notificationService = app.locator().notificationService();
    }

    // --- EVENTS ---
    /**
     * Loads all events from the database.
     */
    public void loadEvents() {
        adminService.getAllEvents(
                eventList -> {
                    masterEventList = new ArrayList<>(eventList);
                    events.postValue(eventList);
                },
                e -> {
                    Log.e("AdminVM", "Error loading events", e);
                    toastMessage.postValue("Error loading events: " + e.getMessage());
                }
        );
    }

    /**
     * Deletes an event from the database.
     *
     * @param event The event to delete
     */
    public void deleteEvent(Event event) {
        adminService.removeEvent(event.getEventId(), true, "Admin deletion",
                aVoid -> {
                    toastMessage.postValue("Event deleted");
                    loadEvents();
                },
                e -> {
                    Log.e("AdminVM", "Error deleting event", e);
                    toastMessage.postValue("Error: " + e.getMessage());
                }
        );
    }

    /**
     * Searches events by title or ID.
     *
     * @param query The search query string
     */
    public void searchEvents(String query) {
        if (query == null || query.trim().isEmpty()) {
            events.setValue(new ArrayList<>(masterEventList));
            return;
        }
        String q = query.toLowerCase();
        List<Event> filtered = masterEventList.stream()
                .filter(e -> (e.getTitle() != null && e.getTitle().toLowerCase().contains(q)) ||
                        (e.getEventId() != null && e.getEventId().toLowerCase().contains(q)))
                .collect(Collectors.toList());
        events.setValue(filtered);
    }

    // --- PROFILES ---
    /**
     * Loads all user profiles from the database.
     */
    public void loadProfiles() {
        adminService.listAllProfiles(
                userList -> {
                    List<UserSummary> summaryList = new ArrayList<>();
                    if (userList != null) {
                        for (User user : userList) {

                            summaryList.add(new UserSummary(user.getUserId(),
                                    user.getName(),
                                    user.getUsername(),
                                    user.getEmail()));
                        }
                    }
                    masterProfileList = new ArrayList<>(summaryList);
                    profiles.postValue(summaryList);
                },
                e -> {
                    Log.e("AdminVM", "Error loading profiles", e);
                    toastMessage.postValue("Error loading profiles: " + e.getMessage());
                }
        );
    }

    // ADDED: Logic to load ONLY Organizers
    public void loadOrganizers() {
        // 1. Get all Events to find organizer IDs
        adminService.getAllEvents(
                allEvents -> {
                    Set<String> organizerIds = new HashSet<>();
                    if (allEvents != null) {
                        for (Event e : allEvents) {
                            if (e.getOrganizerId() != null) organizerIds.add(e.getOrganizerId());
                        }
                    }

                    // 2. Get all Users and filter
                    adminService.listAllProfiles(
                            allUsers -> {
                                List<UserSummary> orgSummaries = new ArrayList<>();
                                if (allUsers != null) {
                                    for (User user : allUsers) {
                                        if (organizerIds.contains(user.getUserId())) {
                                            orgSummaries.add(new UserSummary(user.getUserId(), user.getName(), user.getUsername(), user.getEmail()));
                                        }
                                    }
                                }
                                masterOrganizerList = new ArrayList<>(orgSummaries);
                                organizers.postValue(orgSummaries);
                            },
                            e -> toastMessage.postValue("Error loading organizers: " + e.getMessage())
                    );
                },
                e -> toastMessage.postValue("Error checking events for organizers")
        );
    }

    /**
     * Deletes a user profile from the database.
     *
     * @param profile The profile to delete
     */
    public void deleteProfile(UserSummary profile) {
        adminService.removeProfile(profile.getUserId(), true, "Admin deletion",
                success -> {
                    toastMessage.postValue("Profile deleted");
                    loadProfiles();
                },
                failure -> {
                    Log.e("AdminVM", "Error deleting profile", failure);
                    toastMessage.postValue("Error: " + failure.getMessage());
                }
        );
    }
    /**
     * Searches profiles by name or email.
     *
     * @param query The search query string
     */
    public void searchProfiles(String query) {
        if (query == null || query.trim().isEmpty()) {
            profiles.setValue(new ArrayList<>(masterProfileList));
            return;
        }
        String q = query.toLowerCase();
        List<UserSummary> filtered = masterProfileList.stream()
                .filter(u -> (u.getName() != null && u.getName().toLowerCase().contains(q)) ||
                        (u.getEmail() != null && u.getEmail().toLowerCase().contains(q)))
                .collect(Collectors.toList());
        profiles.setValue(filtered);
    }

    /**
     * Searches organizers by name or email.
     *
     * @param query The search query string
     */
    public void searchOrganizers(String query) {
        if (query == null || query.trim().isEmpty()) {
            organizers.setValue(new ArrayList<>(masterOrganizerList));
            return;
        }
        String q = query.toLowerCase();
        List<UserSummary> filtered = masterOrganizerList.stream()
                .filter(u -> (u.getName() != null && u.getName().toLowerCase().contains(q)) ||
                        (u.getEmail() != null && u.getEmail().toLowerCase().contains(q)))
                .collect(Collectors.toList());
        organizers.setValue(filtered);
    }

    // --- IMAGES ---

    /**
     * Loads all images from the database.
     */
    public void loadImages() {
        adminService.listAllImages(
                imageList -> {
                    masterImageList = new ArrayList<>(imageList);
                    images.postValue(imageList);
                },
                e -> {
                    Log.e("AdminVM", "Error loading images", e);
                    toastMessage.postValue("Error loading images: " + e.getMessage());
                }
        );
    }

    /**
     * Deletes an image from the database.
     *
     * @param image The image to delete
     */
    public void deleteImage(Image image) {
        adminService.removeImage(image.getImageId(), true, "Admin deletion",
                success -> {
                    toastMessage.postValue("Image deleted");
                    loadImages();
                },
                failure -> {
                    Log.e("AdminVM", "Error deleting image", failure);
                    toastMessage.postValue("Error: " + failure.getMessage());
                }
        );
    }

    /**
     * Searches images by image ID.
     *
     * @param query The search query string
     */
    public void searchImages(String query) {
        if (query == null || query.trim().isEmpty()) {
            images.setValue(new ArrayList<>(masterImageList));
            return;
        }
        String q = query.toLowerCase();
        List<Image> filtered = masterImageList.stream()
                .filter(i -> (i.getImageId() != null && i.getImageId().toLowerCase().contains(q)))
                .collect(Collectors.toList());
        images.setValue(filtered);
    }

    // --- NOTIFICATIONS ---

    /**
     * Loads all notifications from the database.
     */
    public void loadNotifications() {
        new Thread(() -> {
            try {
                List<Notification> list = notificationService.getAllNotifications();
                notifications.postValue(list != null ? list : new ArrayList<>());
            } catch (Exception e) {
                Log.e("AdminVM", "Error loading notifications", e);
                toastMessage.postValue("Error loading logs");
            }
        }).start();
    }

}