package com.quantiagents.app.Services;

import android.content.Context;
import android.util.Log;

import com.quantiagents.app.Repository.FireBaseRepository;
import com.quantiagents.app.Repository.UserRepository;
import com.quantiagents.app.models.DeviceIdManager;
import com.quantiagents.app.models.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.regex.Pattern;

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

    public User getCurrentUser() {
        // Find current user by device ID
        String deviceId = deviceIdManager.ensureDeviceId();
        List<User> allUsers = repository.getAllUsers();
        for (User user : allUsers) {
            if (user != null && deviceId != null && deviceId.equals(user.getDeviceId())) {
                return user;
            }
        }
        return null;
    }

    public User createUser(String name, String email, String phone, String password) {
        // Validate everything once before I write to prefs.
        validateName(name);
        validateEmail(email);
        validatePassword(password);
        String trimmedPhone = phone == null ? "" : phone.trim();
        String deviceId = deviceIdManager.ensureDeviceId();
        String passwordHash = hashPassword(password);
        // Create user with userId -1, Firebase will auto-generate an ID when saving
        User user = new User(-1, deviceId, name.trim(), email.trim(), trimmedPhone, passwordHash);

        // Save user - if userId is -1 or <= 0, Firebase will auto-generate an ID
        // The user object will be updated with the generated ID after saving
        repository.saveUser(user, aVoid -> Log.d("App", "User saved with ID: " + user.getUserId()),
                e -> Log.e("App", "Failed to save", e));

        return user;
    }

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

    public boolean authenticate(String email, String password) {
        // Quick email/password check for manual login.
        // Find user by email
        List<User> allUsers = repository.getAllUsers();
        for (User user : allUsers) {
            if (user != null && email.trim().equalsIgnoreCase(user.getEmail())) {
                String hash = hashPassword(password);
                return hash.equals(user.getPasswordHash());
            }
        }
        return false;
    }

    public boolean authenticateDevice(String deviceId) {
        // Device-only gate for auto login.
        // Find user by device ID by checking all users
        List<User> allUsers = repository.getAllUsers();
        for (User user : allUsers) {
            if (user != null && deviceId != null && deviceId.equals(user.getDeviceId())) {
                return true;
            }
        }
        return false;
    }

    public void attachDeviceToCurrentUser() {
        // Make sure the stored profile follows the latest device id.
        User current = getCurrentUser();
        if (current == null) {
            return;
        }
        String deviceId = deviceIdManager.ensureDeviceId();
        if (!deviceId.equals(current.getDeviceId())) {
            User updated = new User(
                    current.getUserId(),
                    deviceId,
                    current.getName(),
                    current.getEmail(),
                    current.getPhone(),
                    current.getPasswordHash()
            );
            updated.setNotificationsOn(current.hasNotificationsOn());
            updated.setCreatedOn(current.getCreatedOn());
            repository.updateUser(updated,
                    aVoid -> Log.d("App", "Update user"),
                    e -> Log.e("App", "Failed to update user", e));
        }
    }

    public void updatePassword(String newPassword) {
        // Allow password refresh on demand.
        validatePassword(newPassword);
        User current = requireUser();
        if (current != null) {
            current.setPasswordHash(hashPassword(newPassword));
            repository.updateUser(current,
                    aVoid -> Log.d("App", "Update user"),
                    e -> Log.e("App", "Failed to update user", e));
        }
    }

    public void updateNotificationPreference(boolean enabled) {
        // Keep notification toggle in sync.
        User current = requireUser();
        if (current != null) {
            current.setNotificationsOn(enabled);
            repository.updateUser(current,
                    aVoid -> Log.d("App", "Update user"),
                    e -> Log.e("App", "Failed to update user", e));
        }
    }

    public void deleteUserProfile() {
        // Hard delete wipes local profile entirely.
        User current = getCurrentUser();
        if (current != null) {
            repository.deleteUserById(
                    current.getUserId(),
                    aVoid -> Log.d("App", "Deleted user"),
                    e -> Log.e("App", "Failed to delete user", e));
        }
    }

    public User getUserById(int userId) {
        return repository.getUserById(userId);
    }

    private User requireUser() {
        User current = getCurrentUser();
        if (current == null) {
            throw new IllegalStateException("Profile missing");
        }
        return current;
    }

    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name missing");
        }
    }

    private void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email missing");
        }
        if (!emailPattern.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("Email invalid");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.trim().length() < 6) {
            throw new IllegalArgumentException("Password weak");
        }
    }

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
