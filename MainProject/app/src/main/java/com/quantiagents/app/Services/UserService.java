package com.quantiagents.app.Services;

import com.quantiagents.app.Repository.UserRepository;
import com.quantiagents.app.models.DeviceIdManager;
import com.quantiagents.app.models.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.regex.Pattern;

public class UserService {

    private final UserRepository repository;
    private final DeviceIdManager deviceIdManager;
    private final Pattern emailPattern = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    public UserService(UserRepository repository, DeviceIdManager deviceIdManager) {
        this.repository = repository;
        this.deviceIdManager = deviceIdManager;
    }

    public User getCurrentUser() {
        return repository.getUser();
    }

    public User createUser(String name, String email, String phone, String password) {
        // Validate everything once before I write to prefs.
        validateName(name);
        validateEmail(email);
        validatePassword(password);
        String trimmedPhone = phone == null ? "" : phone.trim();
        String deviceId = deviceIdManager.ensureDeviceId();
        String passwordHash = hashPassword(password);
        User user = new User(UUID.randomUUID().toString(), deviceId, name.trim(), email.trim(), trimmedPhone, passwordHash);
        repository.saveUser(user);
        return user;
    }

    public User updateUser(String name, String email, String phone) {
        User current = requireUser();
        validateName(name);
        validateEmail(email);
        current.setName(name.trim());
        current.setEmail(email.trim());
        current.setPhone(phone == null ? "" : phone.trim());
        repository.saveUser(current);
        return current;
    }

    public boolean authenticate(String email, String password) {
        // Quick email/password check for manual login.
        User user = repository.getUser();
        if (user == null) {
            return false;
        }
        if (!user.getEmail().equalsIgnoreCase(email.trim())) {
            return false;
        }
        String hash = hashPassword(password);
        return hash.equals(user.getPasswordHash());
    }

    public boolean authenticateDevice(String deviceId) {
        // Device-only gate for auto login.
        User user = repository.getUser();
        return user != null && deviceId != null && deviceId.equals(user.getDeviceId());
    }

    public void attachDeviceToCurrentUser() {
        // Make sure the stored profile follows the latest device id.
        User current = repository.getUser();
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
            repository.saveUser(updated);
        }
    }

    public boolean updatePassword(String newPassword) {
        // Allow password refresh on demand.
        validatePassword(newPassword);
        User current = requireUser();
        current.setPasswordHash(hashPassword(newPassword));
        return repository.saveUser(current);
    }

    public void updateNotificationPreference(boolean enabled) {
        // Keep notification toggle in sync.
        User current = requireUser();
        current.setNotificationsOn(enabled);
        repository.saveUser(current);
    }

    public void deleteUserProfile() {
        // Hard delete wipes local profile entirely.
        repository.clearUser();
    }

    private User requireUser() {
        User current = repository.getUser();
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
