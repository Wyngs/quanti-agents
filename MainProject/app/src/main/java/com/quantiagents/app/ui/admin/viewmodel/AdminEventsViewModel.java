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

import java.util.List;

public class AdminEventsViewModel extends AndroidViewModel {

    private final AdminService adminService;

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

    public void loadEvents() {
        new Thread(() -> {
            try {
                List<Event> eventList = adminService.listAllEvents();
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

    public void loadProfiles() {
        new Thread(() -> {
            try {
                List<UserSummary> profileList = adminService.listAllProfiles();
                profiles.postValue(profileList);
            } catch (Exception e) {
                Log.e("AdminVM", "Error loading profiles", e);
                toastMessage.postValue("Error loading profiles: " + e.getMessage());
            }
        }).start();
    }

    public void deleteProfile(UserSummary profile) {
        new Thread(() -> {
            try {
                boolean success = adminService.removeProfile(profile.getUserId(), true, "Admin deletion");

                if (success) {
                    toastMessage.postValue("Profile deleted");
                    loadProfiles();
                } else {
                    toastMessage.postValue("Error: Failed to delete profile. Check logs.");
                }
            } catch (Exception e) {
                Log.e("AdminVM", "Error deleting profile", e);
                toastMessage.postValue("Error: " + e.getMessage());
            }
        }).start();
    }
}