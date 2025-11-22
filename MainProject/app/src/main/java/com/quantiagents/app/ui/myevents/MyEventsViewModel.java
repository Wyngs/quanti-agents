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
import com.quantiagents.app.Services.EventService;
import com.quantiagents.app.Services.RegistrationHistoryService;
import com.quantiagents.app.Services.UserService;
import com.quantiagents.app.models.Event;
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
        regService.updateRegistrationHistory(history,
                aVoid -> {
                    loadData();
                    if (onSuccess != null) onSuccess.run();
                },
                e -> {
                    isLoading.postValue(false);
                    errorMessage.postValue("Failed to update status");
                }
        );
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}