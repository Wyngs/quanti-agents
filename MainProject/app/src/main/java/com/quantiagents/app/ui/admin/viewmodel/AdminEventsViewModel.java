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
import com.quantiagents.app.models.UserSummary;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AdminEventsViewModel extends AndroidViewModel {

    private final AdminService adminService;

    //master lists (to keep all original data while searching)
    private List<Event> masterEventList = new ArrayList<>();
    private List<UserSummary> masterProfileList = new ArrayList<>();
    private List<Image> masterImageList = new ArrayList<>();

    private final MutableLiveData<List<Event>> events = new MutableLiveData<>();
    public LiveData<List<Event>> getEvents() {
        return events;
    }

    private final MutableLiveData<List<UserSummary>> profiles = new MutableLiveData<>();
    public LiveData<List<UserSummary>> getProfiles() {
        return profiles;
    }

    private final MutableLiveData<List<Image>> images = new MutableLiveData<>();
    public LiveData<List<Image>> getImages() {
        return images;
    }

    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();
    public LiveData<String> getToastMessage() {
        return toastMessage;
    }


    public AdminEventsViewModel(@NonNull Application application) {
        super(application);
        App app = (App) application;
        this.adminService = app.locator().adminService();
    }

    //EVENTS
    public void loadEvents() {
        new Thread(() -> {
            try {
                List<Event> eventList = adminService.listAllEvents();
                masterEventList = new ArrayList<>(eventList);
                events.postValue(eventList);
            } catch (Exception e) {
                Log.e("AdminVM", "Error loading events", e);
                toastMessage.postValue("Error loading events: " + e.getMessage());
            }
        }).start();
    }

    public void deleteEvent(Event event) {
        new Thread(() -> {
            try {
                boolean success = adminService.removeEvent(event.getEventId(), true, "Admin deletion");

                if (success) {
                    toastMessage.postValue("Event deleted");
                    loadEvents();
                } else {
                    toastMessage.postValue("Error: Failed to delete event. Check logs.");
                }
            } catch (Exception e) {
                Log.e("AdminVM", "Error deleting event", e);
                toastMessage.postValue("Error: " + e.getMessage());
            }
        }).start();
    }

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

    //PROFILES
    public void loadProfiles() {
        new Thread(() -> {
            try {
                List<UserSummary> profileList = adminService.listAllProfiles();
                masterProfileList = new ArrayList<>(profileList);
                profiles.postValue(profileList);
            } catch (Exception e) {
                Log.e("AdminVM", "Error loading profiles", e);
                toastMessage.postValue("Error loading profiles: " + e.getMessage());
            }
        }).start();
    }

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

    //IMAGES
    public void loadImages() {
        new Thread(() -> {
            try {
                List<Image> imageList = adminService.listAllImages();
                masterImageList = new ArrayList<>(imageList);
                images.postValue(imageList);
            } catch (Exception e) {
                Log.e("AdminVM", "Error loading images", e);
                toastMessage.postValue("Error loading images: " + e.getMessage());
            }
        }).start();
    }

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
}