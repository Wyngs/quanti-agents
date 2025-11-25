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
 * Keeps entrant profiles tied to a device id so I can skip usernames and still edit or delete safely.
 */
public class UserService {

    private final UserRepository repository;
    private final DeviceIdManager deviceIdManager;
    private final Pattern emailPattern = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    public UserService(Context context) {
        // Instantiate repositories and dependencies internally
        FireBaseRepository fireBaseRepository = new FireBaseRepository();
        this.repository = new UserRepository(fireBaseRepository);
        this.deviceIdManager = new DeviceIdManager(context);
    }

    /**
     * Synchronous helper for quick calls on background threads; still uses local cache.
     */
    public User getCurrentUser() {
        // Efficiently query by device ID instead of fetching all users
        String deviceId = deviceIdManager.ensureDeviceId();
        return repository.getUserByDeviceId(deviceId);
    }

    /**
     * Forces a server read when I truly need the latest profile info (mainly for tests).
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
     * Blocking create that I still lean on inside background work or tests.
     *
     * @return the created user snapshot (already contains the generated id)
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
     * Async create hook I call from the UI so Firestore work stays off the main thread.
     */
    public void createUser(String name,
                           String username,
                           String email,
                           String phone,
                           String password,
                           OnSuccessListener<User> onSuccess,
                           OnFailureListener onFailure) {

        repository.usernameAndEmailExists(username, email,
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
     * Async update path used by edit profile so I can show success/failure to the user.
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
     * Returns null to onSuccess if credentials fail.
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
     */
    public boolean authenticateDevice(String deviceId) {
        User user = repository.getUserByDeviceId(deviceId);
        return user != null;
    }

    /**
     * Async device auth that SplashActivity can await before routing.
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
     */
    public void getCurrentUser(OnSuccessListener<User> onSuccess, OnFailureListener onFailure) {
        String deviceId = deviceIdManager.ensureDeviceId();
        repository.getUserByDeviceId(deviceId, onSuccess, onFailure);
    }

    /**
     * Ensures the stored profile mirrors whatever device id we have right now.
     */
    public void attachDeviceToCurrentUser() {
        attachDeviceToCurrentUser(null);
    }

    /**
     * Same as {@link #attachDeviceToCurrentUser()} but lets callers pass a cached user to avoid re-fetches.
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
     * Blocking password update used outside the UI (e.g., tests, migrations).
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
     */
    public void updateNotificationPreference(boolean enabled) {
        updateNotificationPreference(enabled, null, null);
    }

    /**
     * Full toggle variant so I can disable the UI switch until Firestore confirms.
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
     * Background cleanup used when I just need the profile gone without UI callbacks.
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
     * Delete helper that surfaces callbacks so I can route success to the UI stack.
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

    public User getUserById(String userId) {
        return repository.getUserById(userId);
    }

    private User requireUser() {
        User current = getCurrentUser();
        if (current == null) {
            throw new IllegalStateException("Profile missing");
        }
        return current;
    }

    /**
     * Verifies that the given string is valid
     * @param name
     * String name to check
     */
    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name missing");
        }
    }

    /**
     * Verifies that the given string is valid
     * @param username
     * String username to check
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
     * Verifies that the given string is valid
     * @param email
     * String email to check
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
     * Verifies that the given string is valid
     * @param password
     * String password to check
     */
    private void validatePassword(String password) {
        if (password == null || password.trim().length() < 6) {
            throw new IllegalArgumentException("Password weak");
        }
    }

    /**
     * Verifies and creates a user from inputs
     * @param name
     * String name to use
     * @param username
     * String username to use (no spaces and >3 characters)
     * @param email
     * String email to use (in form []@[].[])
     * @param phone
     * String phone to use
     * @param password
     * String password to use (will be hashed)
     * @return
     * Returns created User
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
     * Hashes password
     * @param password
     * Password to hash
     * @return
     * Returns hashed password
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