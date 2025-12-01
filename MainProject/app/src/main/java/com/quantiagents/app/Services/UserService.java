package com.quantiagents.app.Services;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.quantiagents.app.Repository.FireBaseRepository;
import com.quantiagents.app.Repository.UserRepository;
import com.quantiagents.app.models.DeviceIdManager;
import com.quantiagents.app.models.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.regex.Pattern;

    /**
     * Service class that manages user-related operations.
     * Keeps entrant profiles tied to a device id so usernames can be skipped and still edit or delete safely.
     */
    public class UserService {

    private final UserRepository repository;
    private final DeviceIdManager deviceIdManager;
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
     * Background cleanup used when the profile just needs to be deleted without UI callbacks.
     */
    public void deleteUserProfile() {
        getCurrentUser(
                current -> {
                    if (current == null
                            || current.getUserId() == null
                            || current.getUserId().trim().isEmpty()) {
                        return;
                    }
                    repository.deleteUserById(
                            current.getUserId(),
                            aVoid -> Log.d("App", "Deleted user"),
                            e -> Log.e("App", "Failed to delete user", e));
                },
                e -> Log.e("App", "Failed to load user", e)
        );
    }

    /**
     * Delete helper that surfaces callbacks so success can be routed to the UI stack.
     * Also resets the device ID after successful deletion.
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
                    repository.deleteUserById(
                            current.getUserId(),
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