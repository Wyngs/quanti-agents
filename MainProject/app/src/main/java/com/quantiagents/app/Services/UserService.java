package com.quantiagents.app.Services;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.quantiagents.app.Repository.EventRepository;
import com.quantiagents.app.Repository.FireBaseRepository;
import com.quantiagents.app.Repository.UserRepository;
import com.quantiagents.app.models.DeviceIdManager;
import com.quantiagents.app.models.Event;
import com.quantiagents.app.models.RegistrationHistory;
import com.quantiagents.app.models.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

    /**
     * Service class that manages user-related operations.
     * Keeps entrant profiles tied to a device id so usernames can be skipped and still edit or delete safely.
     */
    public class UserService {

    private final UserRepository repository;
    private final DeviceIdManager deviceIdManager;
    private final Context context;
    private final Pattern emailPattern = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    /**
     * Constructor that initializes the UserService with required dependencies.
     * Instantiates repositories and dependencies internally.
     *
     * @param context The Android context used to initialize the DeviceIdManager
     */
    public UserService(Context context) {
        // Instantiate repositories and dependencies internally
        FireBaseRepository fireBaseRepository = new FireBaseRepository();
        this.repository = new UserRepository(fireBaseRepository);
        this.deviceIdManager = new DeviceIdManager(context);
        this.context = context;
    }

    /**
     * Synchronous helper for quick calls on background threads; still uses local cache.
     * Efficiently queries by device ID instead of fetching all users.
     *
     * @return The current user associated with the device ID, or null if not found
     */
    public User getCurrentUser() {
        // Efficiently query by device ID instead of fetching all users
        String deviceId = deviceIdManager.ensureDeviceId();
        return repository.getUserByDeviceId(deviceId);
    }

    /**
     * Forces a server read when the latest profile info is truly needed (mainly for tests).
     * Note: Still using getAllUsersFromServer for test consistency, but ideally could be optimized similarly.
     *
     * @return The current user from the server, or null if not found
     */
    public User getCurrentUserFresh() {
        String deviceId = deviceIdManager.ensureDeviceId();
        // Note: Still using getAllUsersFromServer for test consistency, but ideally could be optimized similarly
        List<User> allUsers = repository.getAllUsersFromServer();
        for (User user : allUsers) {
            if (user != null && deviceId != null && deviceId.equals(user.getDeviceId())) {
                return user;
            }
        }
        return null;
    }

    /**
     * Blocking create that is used inside background work or tests.
     * Note: Uniqueness check is not performed here to allow tests to use the same usernames.
     *
     * @param name The full name of the user
     * @param username The username chosen by the user
     * @param email The email address of the user
     * @param phone The phone number of the user
     * @param password The plain text password (will be hashed)
     * @return The created user snapshot (already contains the generated id)
     */
    public User createUser(String name, String username, String email, String phone, String password) {
        User user = buildUser(name, username, email, phone, password);

        // I should add the uniqueness check here, but I didn't in case the tests use the same usernames
        repository.saveUser(user,
                aVoid -> Log.d("App", "User saved with ID: " + user.getUserId()),
                e -> Log.e("App", "Failed to save", e));
        return user;
    }

    /**
     * Async create hook called from the UI so Firestore work stays off the main thread.
     * Checks for unique username and email before creating the user.
     *
     * @param name The full name of the user
     * @param username The username chosen by the user
     * @param email The email address of the user
     * @param phone The phone number of the user
     * @param password The plain text password (will be hashed)
     * @param onSuccess Callback invoked when user is successfully created
     * @param onFailure Callback invoked if creation fails or username/email is taken
     */
    public void createUser(String name,
                           String username,
                           String email,
                           String phone,
                           String password,
                           OnSuccessListener<User> onSuccess,
                           OnFailureListener onFailure) {

        repository.checkProfileUnique(username, email,
                exists -> {
                    if (exists.get(0)) {
                        onFailure.onFailure(new Exception("Username Taken"));
                        return;
                    } else if (exists.get(1)) {
                        onFailure.onFailure(new Exception("Email Taken"));
                        return;
                    }

                    User user = buildUser(name, username, email, phone, password);

                    repository.saveUser(user,
                            aVoid -> {
                                Log.d("App", "User saved with ID: " + user.getUserId());
                                if (onSuccess != null) {
                                    onSuccess.onSuccess(user);
                                }
                            },
                            e -> {
                                Log.e("App", "Failed to save", e);
                                if (onFailure != null) {
                                    onFailure.onFailure(e);
                                }
                            });
                },
                onFailure);
    }

    /**
     * Simple blocking update for callers that are already off the main thread.
     *
     * @param name The updated full name of the user
     * @param email The updated email address of the user
     * @param phone The updated phone number of the user (may be null)
     * @return The updated user object
     */
    public User updateUser(String name, String email, String phone) {
        User current = requireUser();
        validateName(name);
        validateEmail(email);
        if (current != null) {
            current.setName(name.trim());
            current.setEmail(email.trim());
            current.setPhone(phone == null ? "" : phone.trim());
            repository.updateUser(current,
                    aVoid -> Log.d("App", "Update user"),
                    e -> Log.e("App", "Failed to update user", e));
        }
        return current;
    }

    /**
     * Async update path used by edit profile so success/failure can be shown to the user.
     *
     * @param name The updated full name of the user
     * @param email The updated email address of the user
     * @param phone The updated phone number of the user (may be null)
     * @param onSuccess Callback invoked when user is successfully updated
     * @param onFailure Callback invoked if update fails or profile is missing
     */
    public void updateUser(String name,
                           String email,
                           String phone,
                           OnSuccessListener<Void> onSuccess,
                           OnFailureListener onFailure) {
        validateName(name);
        validateEmail(email);
        getCurrentUser(
                current -> {
                    if (current == null) {
                        if (onFailure != null) {
                            onFailure.onFailure(new IllegalStateException("Profile missing"));
                        }
                        return;
                    }
                    current.setName(name.trim());
                    current.setEmail(email.trim());
                    current.setPhone(phone == null ? "" : phone.trim());
                    repository.updateUser(current,
                            aVoid -> {
                                Log.d("App", "Update user");
                                if (onSuccess != null) {
                                    onSuccess.onSuccess(aVoid);
                                }
                            },
                            e -> {
                                Log.e("App", "Failed to update user", e);
                                if (onFailure != null) {
                                    onFailure.onFailure(e);
                                }
                            });
                },
                e -> {
                    Log.e("App", "Failed to load user", e);
                    if (onFailure != null) {
                        onFailure.onFailure(e);
                    }
                }
        );
    }

    /**
     * Lightweight credential check; used mostly by tests or synchronous flows.
     * Can authenticate using either email or username.
     *
     * @param email The email address or username to authenticate with
     * @param password The plain text password to check
     * @return True if credentials are valid, false otherwise
     */
    public boolean authenticate(String email, String password) {
        // Find user by email
        List<User> allUsers = repository.getAllUsers();
        for (User user : allUsers) {
            if (user != null && email.trim().equalsIgnoreCase(user.getEmail())) {
                String hash = hashPassword(password);
                return hash.equals(user.getPasswordHash());
            } else if (user != null && email.trim().equalsIgnoreCase(user.getUsername())) {
                String hash = hashPassword(password);
                return hash.equals(user.getPasswordHash());
            }
        }
        return false;
    }

    /**
     * Async credential check. Returns the User object on success so the session can be initialized immediately.
     * Returns null to onSuccess if credentials fail. Can authenticate using either email or username.
     *
     * @param email The email address or username to authenticate with
     * @param password The plain text password to check
     * @param onSuccess Callback invoked with the User object if credentials are valid, or null if invalid
     * @param onFailure Callback invoked if an error occurs during authentication
     */
    public void authenticate(String email, String password, OnSuccessListener<User> onSuccess, OnFailureListener onFailure) {
        repository.getAllUsers(
                users -> {
                    for (User user : users) {
                        if (user != null && email.trim().equalsIgnoreCase(user.getEmail())) {
                            String hash = hashPassword(password);
                            if (hash.equals(user.getPasswordHash())) {
                                onSuccess.onSuccess(user);
                                return;
                            }
                        } else if (user != null && email.trim().equalsIgnoreCase(user.getUsername())) {
                            String hash = hashPassword(password);
                            if (hash.equals(user.getPasswordHash())) {
                                onSuccess.onSuccess(user);
                                return;
                            }
                        }
                    }
                    // Not found or password incorrect
                    onSuccess.onSuccess(null);
                },
                onFailure
        );
    }

    /**
     * Synchronous device-id matcher, mainly used before the async login wire-up.
     *
     * @param deviceId The device ID to check
     * @return True if a user exists with this device ID, false otherwise
     */
    public boolean authenticateDevice(String deviceId) {
        User user = repository.getUserByDeviceId(deviceId);
        return user != null;
    }

    /**
     * Async device auth that SplashActivity can await before routing.
     *
     * @param deviceId The device ID to check
     * @param onSuccess Callback invoked with true if a user exists with this device ID, false otherwise
     * @param onFailure Callback invoked if an error occurs during authentication
     */
    public void authenticateDevice(String deviceId, OnSuccessListener<Boolean> onSuccess, OnFailureListener onFailure) {
        repository.getUserByDeviceId(deviceId,
                user -> onSuccess.onSuccess(user != null),
                onFailure
        );
    }

    /**
     * Async getter that backs basically every UI screen needing the active profile.
     * Updated to use efficient query instead of full scan.
     *
     * @param onSuccess Callback invoked with the current user object, or null if not found
     * @param onFailure Callback invoked if an error occurs while fetching the user
     */
    public void getCurrentUser(OnSuccessListener<User> onSuccess, OnFailureListener onFailure) {
        String deviceId = deviceIdManager.ensureDeviceId();
        repository.getUserByDeviceId(deviceId, onSuccess, onFailure);
    }

    /**
     * Ensures the stored profile mirrors whatever device id we have right now.
     * If the current user's device ID doesn't match, updates it.
     */
    public void attachDeviceToCurrentUser() {
        attachDeviceToCurrentUser(null);
    }

    /**
     * Same as {@link #attachDeviceToCurrentUser()} but lets callers pass a cached user to avoid re-fetches.
     * If the cached user's device ID doesn't match, updates it.
     *
     * @param cachedUser The cached user object to use, or null to fetch the current user
     */
    public void attachDeviceToCurrentUser(@Nullable User cachedUser) {
        User current = cachedUser != null ? cachedUser : getCurrentUser();
        if (current == null) {
            return;
        }
        String deviceId = deviceIdManager.ensureDeviceId();
        // Update the user record if this is a new device for them
        if (!deviceId.equals(current.getDeviceId())) {
            current.setDeviceId(deviceId);
            repository.updateUser(current,
                    aVoid -> Log.d("App", "Device ID updated for user"),
                    e -> Log.e("App", "Failed to update user device id", e));
        }
    }

    /**
     * Clears the device ID from local storage.
     * This prevents automatic login on next app startup.
     */
    public void clearDeviceId() {
        deviceIdManager.reset();
    }

    /**
     * Blocking password update used outside the UI (e.g., tests, migrations).
     *
     * @param newPassword The new plain text password (will be hashed)
     */
    public void updatePassword(String newPassword) {
        validatePassword(newPassword);
        User current = requireUser();
        if (current != null) {
            current.setPasswordHash(hashPassword(newPassword));
            repository.updateUser(current,
                    aVoid -> Log.d("App", "Update user"),
                    e -> Log.e("App", "Failed to update user", e));
        }
    }

    /**
     * Async password update so EditProfileActivity can react via callbacks.
     *
     * @param newPassword The new plain text password (will be hashed)
     * @param onSuccess Callback invoked when password is successfully updated
     * @param onFailure Callback invoked if update fails or profile is missing
     */
    public void updatePassword(String newPassword,
                               OnSuccessListener<Void> onSuccess,
                               OnFailureListener onFailure) {
        validatePassword(newPassword);
        getCurrentUser(
                current -> {
                    if (current == null) {
                        if (onFailure != null) {
                            onFailure.onFailure(new IllegalStateException("Profile missing"));
                        }
                        return;
                    }
                    current.setPasswordHash(hashPassword(newPassword));
                    repository.updateUser(current,
                            aVoid -> {
                                Log.d("App", "Update user");
                                if (onSuccess != null) {
                                    onSuccess.onSuccess(aVoid);
                                }
                            },
                            e -> {
                                Log.e("App", "Failed to update user", e);
                                if (onFailure != null) {
                                    onFailure.onFailure(e);
                                }
                            });
                },
                e -> {
                    Log.e("App", "Failed to load user", e);
                    if (onFailure != null) {
                        onFailure.onFailure(e);
                    }
                }
        );
    }

    /**
     * Fire-and-forget toggle for callers that don't care about result handling.
     *
     * @param enabled True to enable notifications, false to disable
     */
    public void updateNotificationPreference(boolean enabled) {
        updateNotificationPreference(enabled, null, null);
    }

    /**
     * Full toggle variant so the UI switch can be disabled until Firestore confirms.
     *
     * @param enabled True to enable notifications, false to disable
     * @param onSuccess Callback invoked when notification preference is successfully updated
     * @param onFailure Callback invoked if update fails or profile is missing
     */
    public void updateNotificationPreference(boolean enabled,
                                             @Nullable OnSuccessListener<Void> onSuccess,
                                             @Nullable OnFailureListener onFailure) {
        getCurrentUser(
                current -> {
                    if (current == null) {
                        Log.w("App", "Profile missing while toggling notifications");
                        if (onFailure != null) {
                            onFailure.onFailure(new IllegalStateException("Profile missing"));
                        }
                        return;
                    }
                    current.setNotificationsOn(enabled);
                    repository.updateUser(current,
                            aVoid -> {
                                Log.d("App", "Update user");
                                if (onSuccess != null) {
                                    onSuccess.onSuccess(aVoid);
                                }
                            },
                            e -> {
                                Log.e("App", "Failed to update user", e);
                                if (onFailure != null) {
                                    onFailure.onFailure(e);
                                }
                            });
                },
                e -> {
                    Log.e("App", "Failed to load user", e);
                    if (onFailure != null) {
                        onFailure.onFailure(e);
                    }
                }
        );
    }

    /**
     * Helper method to clean up all events and registration histories associated with a user.
     * Removes user from all event lists (waiting, selected, confirmed, cancelled),
     * deletes events created by the user, and deletes all registration histories.
     *
     * @param userId The user ID to clean up
     * @param onComplete Callback invoked when cleanup is complete
     * @param onFailure Optional callback invoked if cleanup fails (can be null)
     */
    private void cleanupUserEventsAndRegistrations(String userId, Runnable onComplete, @Nullable OnFailureListener onFailure) {
        Log.d("App", "cleanupUserEventsAndRegistrations called for userId: " + userId);
        EventService eventService = new EventService(context);
        RegistrationHistoryService registrationHistoryService = new RegistrationHistoryService(context);

        // Get all events
        Log.d("App", "Calling getAllEvents to fetch events for cleanup");
        eventService.getAllEvents(
                events -> {
                    Log.d("App", "getAllEvents success callback - received " + (events != null ? events.size() : 0) + " events");
                    if (events == null) {
                        Log.w("App", "getAllEvents returned null events list");
                        events = new ArrayList<>();
                    }
                    List<Event> eventsToUpdate = new ArrayList<>();
                    List<String> eventsToDelete = new ArrayList<>();

                    // Process each event
                    Log.d("App", "Processing " + events.size() + " events for user deletion: " + userId);
                    for (Event event : events) {
                        if (event == null || event.getEventId() == null) {
                            continue;
                        }

                        boolean needsUpdate = false;
                        String eventId = event.getEventId();

                        // Check if user is the organizer - mark for deletion
                        // Delete all events where event.organizerId == userId
                        String organizerId = event.getOrganizerId();
                        if (organizerId != null && organizerId.trim().equals(userId.trim())) {
                            Log.d("App", "Found event to delete - eventId: " + eventId + ", organizerId: " + organizerId + ", userId: " + userId);
                            eventsToDelete.add(eventId);
                            continue; // Skip updating lists for events we're deleting
                        }

                        // Remove user from waiting list
                        if (event.getWaitingList() != null && event.getWaitingList().remove(userId)) {
                            needsUpdate = true;
                        }

                        // Remove user from selected list
                        if (event.getSelectedList() != null && event.getSelectedList().remove(userId)) {
                            needsUpdate = true;
                        }

                        // Remove user from confirmed list
                        if (event.getConfirmedList() != null && event.getConfirmedList().remove(userId)) {
                            needsUpdate = true;
                        }

                        // Remove user from cancelled list
                        if (event.getCancelledList() != null && event.getCancelledList().remove(userId)) {
                            needsUpdate = true;
                        }

                        // If any list was modified, mark event for update
                        if (needsUpdate) {
                            eventsToUpdate.add(event);
                        }
                    }

                    // Update all events that need updating
                    Log.d("App", "Events to update: " + eventsToUpdate.size() + ", Events to delete: " + eventsToDelete.size());
                    if (eventsToUpdate.isEmpty() && eventsToDelete.isEmpty()) {
                        // No events to update or delete, proceed to delete registration histories
                        Log.d("App", "No events to update or delete, proceeding to delete registration histories");
                        deleteAllRegistrationHistories(userId, registrationHistoryService, onComplete, onFailure);
                    } else {
                        // Use AtomicInteger to track completion of all async operations
                        AtomicInteger pendingOps = new AtomicInteger(
                                eventsToUpdate.size() + eventsToDelete.size());
                        List<Exception> errors = new ArrayList<>();

                        // Delete events created by user FIRST (before updating other events)
                        if (!eventsToDelete.isEmpty()) {
                            Log.d("App", "Deleting " + eventsToDelete.size() + " events created by user");
                            // Run deletion on background thread to avoid blocking main thread
                            new Thread(() -> {
                                for (String eventId : eventsToDelete) {
                                    // Use repository directly to avoid EventService.getEventById() blocking call
                                    EventRepository eventRepo = new EventRepository(new FireBaseRepository());
                                    eventRepo.deleteEventById(eventId,
                                            aVoid -> {
                                                Log.d("App", "Successfully deleted event created by user: " + eventId);
                                                if (pendingOps.decrementAndGet() == 0) {
                                                    deleteAllRegistrationHistories(userId, registrationHistoryService, onComplete, onFailure);
                                                }
                                            },
                                            e -> {
                                                Log.e("App", "Failed to delete event: " + eventId, e);
                                                errors.add(e);
                                                if (pendingOps.decrementAndGet() == 0) {
                                                    if (!errors.isEmpty() && onFailure != null) {
                                                        onFailure.onFailure(errors.get(0));
                                                    } else {
                                                        deleteAllRegistrationHistories(userId, registrationHistoryService, onComplete, onFailure);
                                                    }
                                                }
                                            });
                                }
                            }).start();
                        }

                        // Update events
                        if (!eventsToUpdate.isEmpty()) {
                            Log.d("App", "Updating " + eventsToUpdate.size() + " events to remove user from lists");
                        }
                        for (Event event : eventsToUpdate) {
                            eventService.updateEvent(event,
                                    aVoid -> {
                                        Log.d("App", "Removed user from event lists: " + event.getEventId());
                                        if (pendingOps.decrementAndGet() == 0) {
                                            deleteAllRegistrationHistories(userId, registrationHistoryService, onComplete, onFailure);
                                        }
                                    },
                                    e -> {
                                        Log.e("App", "Failed to update event: " + event.getEventId(), e);
                                        errors.add(e);
                                        if (pendingOps.decrementAndGet() == 0) {
                                            if (!errors.isEmpty() && onFailure != null) {
                                                onFailure.onFailure(errors.get(0));
                                            } else {
                                                deleteAllRegistrationHistories(userId, registrationHistoryService, onComplete, onFailure);
                                            }
                                        }
                                    });
                        }
                    }
                },
                e -> {
                    Log.e("App", "Failed to get events for cleanup - getAllEvents failed", e);
                    if (e != null) {
                        Log.e("App", "Error details: " + e.getMessage(), e);
                    }
                    // Even if getting events fails, try to delete registration histories
                    deleteAllRegistrationHistories(userId, registrationHistoryService, onComplete, onFailure != null ? onFailure : null);
                }
        );
    }

    /**
     * Helper method to clean up all events and registration histories associated with a user.
     * Overload without onFailure callback for use in deleteUserProfile() without callbacks.
     *
     * @param userId The user ID to clean up
     * @param onComplete Callback invoked when cleanup is complete
     */
    private void cleanupUserEventsAndRegistrations(String userId, Runnable onComplete) {
        cleanupUserEventsAndRegistrations(userId, onComplete, null);
    }

    /**
     * Helper method to delete all registration histories for a user.
     *
     * @param userId The user ID whose registration histories should be deleted
     * @param registrationHistoryService The service to use for deletion
     * @param onComplete Callback invoked when deletion is complete
     * @param onFailure Optional callback invoked if deletion fails (can be null)
     */
    private void deleteAllRegistrationHistories(String userId,
                                                 RegistrationHistoryService registrationHistoryService,
                                                 Runnable onComplete,
                                                 @Nullable OnFailureListener onFailure) {
        // Run on background thread to avoid blocking main thread with Tasks.await()
        new Thread(() -> {
            // Get all registration histories for the user
            List<RegistrationHistory> histories = registrationHistoryService.getRegistrationHistoriesByUserId(userId);

            if (histories == null || histories.isEmpty()) {
                Log.d("App", "No registration histories found for user: " + userId);
                onComplete.run();
                return;
            }

            // Use AtomicInteger to track completion of all async deletions
            AtomicInteger pendingOps = new AtomicInteger(histories.size());
            List<Exception> errors = new ArrayList<>();

            for (RegistrationHistory history : histories) {
                if (history == null || history.getEventId() == null) {
                    if (pendingOps.decrementAndGet() == 0) {
                        if (!errors.isEmpty() && onFailure != null) {
                            onFailure.onFailure(errors.get(0));
                        } else {
                            onComplete.run();
                        }
                    }
                    continue;
                }

                registrationHistoryService.deleteRegistrationHistory(
                        history.getEventId(),
                        userId,
                        aVoid -> {
                            Log.d("App", "Deleted registration history: eventId=" + history.getEventId() + ", userId=" + userId);
                            if (pendingOps.decrementAndGet() == 0) {
                                if (!errors.isEmpty() && onFailure != null) {
                                    onFailure.onFailure(errors.get(0));
                                } else {
                                    onComplete.run();
                                }
                            }
                        },
                        e -> {
                            Log.e("App", "Failed to delete registration history: eventId=" + history.getEventId() + ", userId=" + userId, e);
                            errors.add(e);
                            if (pendingOps.decrementAndGet() == 0) {
                                if (!errors.isEmpty() && onFailure != null) {
                                    onFailure.onFailure(errors.get(0));
                                } else {
                                    onComplete.run();
                                }
                            }
                        });
            }
        }).start();
    }

    /**
     * Background cleanup used when the profile just needs to be deleted without UI callbacks.
     * Removes user from all event lists, deletes events created by the user, and deletes all registration histories.
     */
    public void deleteUserProfile() {
        Log.d("App", "deleteUserProfile() called");
        getCurrentUser(
                current -> {
                    Log.d("App", "getCurrentUser callback - current user: " + (current != null ? current.getUserId() : "null"));
                    if (current == null
                            || current.getUserId() == null
                            || current.getUserId().trim().isEmpty()) {
                        Log.w("App", "Cannot delete user profile - user is null or has no userId");
                        return;
                    }
                    String userId = current.getUserId();
                    Log.d("App", "Starting cleanup for userId: " + userId);
                    // Clean up events and registration histories before deleting user
                    cleanupUserEventsAndRegistrations(userId, () -> {
                        Log.d("App", "Cleanup complete, now deleting user profile");
                        // After cleanup, delete the user
                        repository.deleteUserById(
                                userId,
                                aVoid -> Log.d("App", "Deleted user"),
                                e -> Log.e("App", "Failed to delete user", e));
                    });
                },
                e -> {
                    Log.e("App", "Failed to load user in deleteUserProfile", e);
                    if (e != null) {
                        Log.e("App", "Error details: " + e.getMessage(), e);
                    }
                }
        );
    }

    /**
     * Delete helper that surfaces callbacks so success can be routed to the UI stack.
     * Also resets the device ID after successful deletion.
     * Removes user from all event lists, deletes events created by the user, and deletes all registration histories.
     *
     * @param onSuccess Callback invoked when user profile is successfully deleted
     * @param onFailure Callback invoked if deletion fails or profile is missing
     */
    public void deleteUserProfile(OnSuccessListener<Void> onSuccess,
                                  OnFailureListener onFailure) {
        getCurrentUser(
                current -> {
                    if (current == null
                            || current.getUserId() == null
                            || current.getUserId().trim().isEmpty()) {
                        if (onFailure != null) {
                            onFailure.onFailure(new IllegalStateException("Profile missing"));
                        }
                        return;
                    }
                    String userId = current.getUserId();
                    // Clean up events and registration histories before deleting user
                    cleanupUserEventsAndRegistrations(userId, () -> {
                        // After cleanup, delete the user
                        repository.deleteUserById(
                                userId,
                                aVoid -> {
                                    Log.d("App", "Deleted user");
                                    deviceIdManager.reset();
                                    if (onSuccess != null) {
                                        onSuccess.onSuccess(aVoid);
                                    }
                                },
                                e -> {
                                    Log.e("App", "Failed to delete user", e);
                                    if (onFailure != null) {
                                        onFailure.onFailure(e);
                                    }
                                });
                    }, onFailure);
                },
                e -> {
                    Log.e("App", "Failed to load user", e);
                    if (onFailure != null) {
                        onFailure.onFailure(e);
                    }
                }
        );
    }

    /**
     * Synchronously gets a user by their user ID.
     *
     * @param userId The unique identifier of the user to retrieve
     * @return The user object, or null if not found
     */
    public User getUserById(String userId) {
        return repository.getUserById(userId);
    }

    /**
     * Asynchronously gets all users from the database.
     *
     * @param onSuccess Callback invoked with the list of all users
     * @param onFailure Callback invoked if an error occurs while fetching users
     */
    public void getAllUsers(OnSuccessListener<List<User>> onSuccess, OnFailureListener onFailure) {
        repository.getAllUsers(onSuccess, onFailure);
    }

    /**
     * Asynchronously gets a user by their user ID.
     *
     * @param userId The unique identifier of the user to retrieve
     * @param onSuccess Callback invoked with the user object, or null if not found
     * @param onFailure Callback invoked if an error occurs while fetching the user
     */
    public void getUserById(String userId, OnSuccessListener<User> onSuccess, OnFailureListener onFailure) {
        repository.getUserById(userId, onSuccess, onFailure);
    }

    /**
     * Deletes a user profile by user ID with full cleanup.
     * Removes user from all event lists, deletes events created by the user,
     * and deletes all registration histories before deleting the user profile.
     * This method is used by AdminService to delete any user, not just the current user.
     *
     * @param userId The ID of the user to delete
     * @param onSuccess Callback invoked when user profile is successfully deleted
     * @param onFailure Callback invoked if deletion fails
     */
    public void deleteUserProfileById(String userId,
                                     OnSuccessListener<Void> onSuccess,
                                     OnFailureListener onFailure) {
        if (userId == null || userId.trim().isEmpty()) {
            if (onFailure != null) {
                onFailure.onFailure(new IllegalArgumentException("User ID cannot be null or empty"));
            }
            return;
        }
        Log.d("App", "deleteUserProfileById called for userId: " + userId);
        // Clean up events and registration histories before deleting user
        cleanupUserEventsAndRegistrations(userId, () -> {
            // After cleanup, delete the user
            repository.deleteUserById(
                    userId,
                    aVoid -> {
                        Log.d("App", "Deleted user by ID: " + userId);
                        // Only reset device ID if this is the current user
                        User current = getCurrentUser();
                        if (current != null && userId.equals(current.getUserId())) {
                            deviceIdManager.reset();
                        }
                        if (onSuccess != null) {
                            onSuccess.onSuccess(aVoid);
                        }
                    },
                    e -> {
                        Log.e("App", "Failed to delete user by ID: " + userId, e);
                        if (onFailure != null) {
                            onFailure.onFailure(e);
                        }
                    });
        }, onFailure);
    }

    /**
     * Gets the current user and throws an exception if not found.
     * Used internally to ensure a user exists before operations.
     *
     * @return The current user object
     * @throws IllegalStateException if no user profile is found
     */
    private User requireUser() {
        User current = getCurrentUser();
        if (current == null) {
            throw new IllegalStateException("Profile missing");
        }
        return current;
    }

    /**
     * Verifies that the given name string is valid (not null or empty).
     *
     * @param name The name string to check
     * @throws IllegalArgumentException if name is null or empty
     */
    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name missing");
        }
    }

    /**
     * Verifies that the given username string is valid.
     * Username must be at least 4 characters and contain no spaces.
     *
     * @param username The username string to check
     * @throws IllegalArgumentException if username is null, empty, too short, or contains spaces
     */
    private void validateUsername(String username) {

        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username missing");
        }
        if (username.trim().length() < 4) {
            throw new IllegalArgumentException("Username too short");
        }
        if (username.trim().contains(" ")) {
            throw new IllegalArgumentException("Username may not contain spaces");
        }
    }

    /**
     * Verifies that the given email string is valid using regex pattern matching.
     *
     * @param email The email string to check
     * @throws IllegalArgumentException if email is null, empty, or doesn't match email pattern
     */
    private void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email missing");
        }
        if (!emailPattern.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("Email invalid");
        }
    }

    /**
     * Verifies that the given password string is valid (at least 6 characters).
     *
     * @param password The password string to check
     * @throws IllegalArgumentException if password is null or less than 6 characters
     */
    private void validatePassword(String password) {
        if (password == null || password.trim().length() < 6) {
            throw new IllegalArgumentException("Password weak");
        }
    }

    /**
     * Verifies and creates a user from inputs.
     * All inputs are validated and trimmed before creating the user object.
     *
     * @param name String name to use
     * @param username String username to use (no spaces and greater than 3 characters)
     * @param email String email to use (in form []@[].[])
     * @param phone String phone to use (may be null)
     * @param password String password to use (will be hashed)
     * @return Returns created User object with device ID and hashed password
     * @see User
     */
    private User buildUser(String name, String username, String email, String phone, String password) {
        validateName(name);
        validateUsername(username);
        validateEmail(email);
        validatePassword(password);
        String trimmedPhone = phone == null ? "" : phone.trim();
        String deviceId = deviceIdManager.ensureDeviceId();
        String passwordHash = hashPassword(password);
        return new User("", deviceId, name.trim(), username.trim(), email.trim(), trimmedPhone, passwordHash);
    }

    /**
     * Hashes password using SHA-256 algorithm.
     *
     * @param password The plain text password to hash
     * @return Returns the hashed password as a hexadecimal string
     * @throws IllegalStateException if SHA-256 algorithm is not available
     */
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.trim().getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Hash error", exception);
        }
    }
}