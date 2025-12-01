package com.quantiagents.app.ui.myevents;

import android.app.Application;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.quantiagents.app.App;
import com.quantiagents.app.Constants.constant;
import com.quantiagents.app.Services.ChatService;
import com.quantiagents.app.Services.EventService;
import com.quantiagents.app.Services.NotificationService;
import com.quantiagents.app.Services.RegistrationHistoryService;
import com.quantiagents.app.Services.UserService;
import com.quantiagents.app.models.Event;
import com.quantiagents.app.models.Notification;
import com.quantiagents.app.models.RegistrationHistory;
import com.quantiagents.app.models.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyEventsViewModel extends AndroidViewModel {

    private static final String TAG = "MyEventsViewModel";

    private final UserService userService;
    private final RegistrationHistoryService regService;
    private final EventService eventService;
    private final NotificationService notificationService;
    private final ChatService chatService;
    private final ExecutorService executor;

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private final MutableLiveData<List<MyEventsAdapter.MyEventItem>> waitingList = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<MyEventsAdapter.MyEventItem>> selectedList = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<MyEventsAdapter.MyEventItem>> confirmedList = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<MyEventsAdapter.MyEventItem>> pastList = new MutableLiveData<>(new ArrayList<>());

    public MyEventsViewModel(@NonNull Application application) {
        super(application);
        App app = (App) application;
        this.userService = app.locator().userService();
        this.regService = app.locator().registrationHistoryService();
        this.eventService = app.locator().eventService();
        this.notificationService = new NotificationService(application);
        this.chatService = app.locator().chatService();
        this.executor = Executors.newSingleThreadExecutor();
    }

    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public LiveData<List<MyEventsAdapter.MyEventItem>> getWaitingList() { return waitingList; }
    public LiveData<List<MyEventsAdapter.MyEventItem>> getSelectedList() { return selectedList; }
    public LiveData<List<MyEventsAdapter.MyEventItem>> getConfirmedList() { return confirmedList; }
    public LiveData<List<MyEventsAdapter.MyEventItem>> getPastList() { return pastList; }

    public void loadData() {
        isLoading.setValue(true);
        userService.getCurrentUser(
                user -> {
                    if (user == null) {
                        isLoading.postValue(false);
                        errorMessage.postValue("User not signed in.");
                        return;
                    }
                    fetchRegistrations(user.getUserId());
                },
                e -> {
                    isLoading.postValue(false);
                    errorMessage.postValue("Failed to load user profile.");
                }
        );
    }

    private void fetchRegistrations(String userId) {
        executor.execute(() -> {
            try {
                List<RegistrationHistory> allRegs = regService.getRegistrationHistoriesByUserId(userId);

                List<MyEventsAdapter.MyEventItem> tempWaiting = new ArrayList<>();
                List<MyEventsAdapter.MyEventItem> tempSelected = new ArrayList<>();
                List<MyEventsAdapter.MyEventItem> tempConfirmed = new ArrayList<>();
                List<MyEventsAdapter.MyEventItem> tempPast = new ArrayList<>();

                Map<String, Event> eventCache = new HashMap<>();
                Map<String, String> organizerCache = new HashMap<>();

                if (allRegs != null) {
                    for (RegistrationHistory rh : allRegs) {
                        if (rh == null || TextUtils.isEmpty(rh.getEventId())) continue;

                        String eventId = rh.getEventId();
                        Event event = eventCache.get(eventId);

                        if (event == null) {
                            try {
                                event = eventService.getEventById(eventId);
                                if (event != null) eventCache.put(eventId, event);
                            } catch (Exception e) {
                                Log.e(TAG, "Error loading event: " + eventId, e);
                            }
                        }

                        if (event == null) continue;

                        String orgId = event.getOrganizerId();
                        String orgName = "Unknown";
                        if (orgId != null) {
                            if (organizerCache.containsKey(orgId)) {
                                orgName = organizerCache.get(orgId);
                            } else {
                                try {
                                    User org = userService.getUserById(orgId);
                                    if (org != null && org.getName() != null) {
                                        orgName = org.getName();
                                        organizerCache.put(orgId, orgName);
                                    }
                                } catch (Exception e) {
                                    Log.w(TAG, "Error loading organizer: " + orgId);
                                }
                            }
                        }

                        MyEventsAdapter.MyEventItem item = new MyEventsAdapter.MyEventItem(rh, event, orgName);
                        constant.EventRegistrationStatus status = rh.getEventRegistrationStatus();

                        if (status == null) continue;

                        switch (status) {
                            case WAITLIST:
                                tempWaiting.add(item);
                                break;
                            case SELECTED:
                                tempSelected.add(item);
                                break;
                            case CONFIRMED:
                                tempConfirmed.add(item);
                                break;
                            case CANCELLED:
                                tempPast.add(item);
                                break;
                        }
                    }
                }

                waitingList.postValue(tempWaiting);
                selectedList.postValue(tempSelected);
                confirmedList.postValue(tempConfirmed);
                pastList.postValue(tempPast);

                isLoading.postValue(false);

            } catch (Exception e) {
                Log.e(TAG, "Fatal error in fetchRegistrations", e);
                errorMessage.postValue("Error loading events.");
                isLoading.postValue(false);
            }
        });
    }

    public void leaveWaitlist(String eventId, Runnable onSuccess) {
        isLoading.setValue(true);
        userService.getCurrentUser(
                user -> {
                    if (user == null) return;
                    regService.deleteRegistrationHistory(eventId, user.getUserId(),
                            aVoid -> {
                                // BUG FIX: Sync Event waiting list remove if possible
                                executor.execute(() -> {
                                    try {
                                        Event evt = eventService.getEventById(eventId);
                                        if (evt != null && evt.getWaitingList() != null) {
                                            if (evt.getWaitingList().remove(user.getUserId())) {
                                                eventService.updateEvent(evt, v -> {}, e -> {});
                                            }
                                        }
                                    } catch (Exception ignored) {}
                                });

                                loadData();
                                if (onSuccess != null) onSuccess.run();
                            },
                            e -> {
                                isLoading.postValue(false);
                                errorMessage.postValue("Failed to leave waitlist");
                            }
                    );
                },
                e -> isLoading.postValue(false)
        );
    }

    public void updateStatus(RegistrationHistory history, constant.EventRegistrationStatus newStatus, Runnable onSuccess) {
        isLoading.setValue(true);
        history.setEventRegistrationStatus(newStatus);
        
        // Get event and user info before updating to send notifications
        final String eventId = history.getEventId();
        final String userId = history.getUserId();
        final constant.EventRegistrationStatus finalStatus = newStatus;
        
        // Fetch event and user on background thread
        executor.execute(() -> {
            Event event = (eventId != null) ? eventService.getEventById(eventId) : null;
            User user = (userId != null) ? userService.getUserById(userId) : null;
            
            // Update registration history on main thread
        regService.updateRegistrationHistory(history,
                aVoid -> {
                    // Send notifications based on status change
                    if (event != null && user != null) {
                        if (finalStatus == constant.EventRegistrationStatus.CONFIRMED) {
                            sendAcceptNotification(event, user);
                                // Add user to chat when they accept
                                addUserToChat(event.getEventId(), user.getUserId());
                        } else if (finalStatus == constant.EventRegistrationStatus.CANCELLED) {
                            // Only send decline notification if it was previously SELECTED
                            // (not if it was already CANCELLED)
                            sendDeclineNotification(event, user);
                        }
                    }
                    loadData();
                    if (onSuccess != null) onSuccess.run();
                },
                e -> {
                    isLoading.postValue(false);
                    errorMessage.postValue("Failed to update status");
                }
        );
        });
    }

    /**
     * Sends notification to organizer when user accepts lottery win.
     */
    private void sendAcceptNotification(Event event, User user) {
        if (event == null || user == null) return;

        String eventId = event.getEventId();
        String organizerId = event.getOrganizerId();
        String eventName = event.getTitle() != null ? event.getTitle() : "Event";
        String userName = user.getName() != null && !user.getName().trim().isEmpty() 
                ? user.getName().trim() 
                : (user.getUsername() != null && !user.getUsername().trim().isEmpty() 
                    ? user.getUsername().trim() 
                    : "User");

        if (eventId == null || organizerId == null) return;

        int eventIdInt = Math.abs(eventId.hashCode());
        int organizerIdInt = Math.abs(organizerId.hashCode());

        Notification notification = new Notification(
                0, // Auto-generate ID
                constant.NotificationType.GOOD,
                organizerIdInt,
                -1, // senderId = -1 means system Generated
                eventIdInt,
                "User Accepted",
                "User " + userName + " has accepted lottery invite for your Event " + eventName
        );

        notificationService.saveNotification(notification,
                aVoid -> Log.d("MyEventsVM", "Accept notification sent to organizer"),
                e -> Log.e("MyEventsVM", "Failed to send accept notification", e)
        );
    }

    /**
     * Sends notification to organizer when user declines lottery win.
     */
    private void sendDeclineNotification(Event event, User user) {
        if (event == null || user == null) return;

        String eventId = event.getEventId();
        String organizerId = event.getOrganizerId();
        String eventName = event.getTitle() != null ? event.getTitle() : "Event";
        String userName = user.getName() != null && !user.getName().trim().isEmpty() 
                ? user.getName().trim() 
                : (user.getUsername() != null && !user.getUsername().trim().isEmpty() 
                    ? user.getUsername().trim() 
                    : "User");

        if (eventId == null || organizerId == null) return;

        int eventIdInt = Math.abs(eventId.hashCode());
        int organizerIdInt = Math.abs(organizerId.hashCode());

        Notification notification = new Notification(
                0, // Auto-generate ID
                constant.NotificationType.BAD,
                organizerIdInt,
                -1, // senderId = -1 means system Generated
                eventIdInt,
                "User Declined",
                "User " + userName + " has declined lottery invite for your Event " + eventName
        );

        notificationService.saveNotification(notification,
                aVoid -> Log.d("MyEventsVM", "Decline notification sent to organizer"),
                e -> Log.e("MyEventsVM", "Failed to send decline notification", e)
        );
    }

    /**
     * Adds user to the event's group chat when they accept the invitation.
     * If the chat doesn't exist, it will be created.
     */
    private void addUserToChat(String eventId, String userId) {
        if (eventId == null || userId == null) return;

        executor.execute(() -> {
            try {
                Event event = eventService.getEventById(eventId);
                if (event == null) {
                    Log.w("MyEventsVM", "Event not found: " + eventId);
                    return;
                }

                String eventName = event.getTitle() != null ? event.getTitle() : "Event";
                String organizerId = event.getOrganizerId();

                // Ensure chat exists and add user (creates chat if it doesn't exist)
                chatService.ensureChatExistsAndAddUser(eventId, eventName, organizerId, userId,
                        aVoid -> Log.d("MyEventsVM", "User added to chat for event: " + eventId),
                        e -> Log.e("MyEventsVM", "Failed to add user to chat", e)
                );
            } catch (Exception e) {
                Log.e("MyEventsVM", "Error adding user to chat", e);
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}