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

import java.util.ArrayList;
import java.util.List;

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

    public void searchEvents(String query) {
        if (query == null || query.trim().isEmpty()) {
            events.setValue(new ArrayList<>(masterEventList));
            return;
        }
        String q = query.toLowerCase();
        List<Event> filtered = new ArrayList<>();
        for (Event e : masterEventList) {
            if ((e.getTitle() != null && e.getTitle().toLowerCase().contains(q)) ||
                    (e.getEventId() != null && e.getEventId().toLowerCase().contains(q))) {
                filtered.add(e);
            }
        }
        events.setValue(filtered);
    }

    //PROFILES
    public void loadProfiles() {
        adminService.listAllProfiles(
                users -> {
                    List<UserSummary> summaries = new ArrayList<>();
                    if (users != null) {
                        for (User u : users) {
                            summaries.add(new UserSummary(u.getUserId(), u.getName(), u.getEmail()));
                        }
                    }
                    masterProfileList = new ArrayList<>(summaries);
                    profiles.postValue(summaries);
                },
                e -> {
                    Log.e("AdminVM", "Error loading profiles", e);
                    toastMessage.postValue("Error loading profiles: " + e.getMessage());
                }
        );
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
        List<UserSummary> filtered = new ArrayList<>();
        for (UserSummary u : masterProfileList) {
            if ((u.getName() != null && u.getName().toLowerCase().contains(q)) ||
                    (u.getEmail() != null && u.getEmail().toLowerCase().contains(q))) {
                filtered.add(u);
            }
        }
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
        List<Image> filtered = new ArrayList<>();
        for (Image i : masterImageList) {
            if (i.getImageId() != null && i.getImageId().toLowerCase().contains(q)) {
                filtered.add(i);
            }
        }
        images.setValue(filtered);
    }
}