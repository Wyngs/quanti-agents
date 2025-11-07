package com.quantiagents.app.Services;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.quantiagents.app.Repository.FireBaseRepository;
import com.quantiagents.app.Repository.UserRepository;
import com.quantiagents.app.models.DeviceIdManager;
import com.quantiagents.app.models.User;
import com.quantiagents.app.Constants.constant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

public class UserService {

    private final UserRepository repository;
    private final DeviceIdManager deviceIdManager;
    private final Pattern emailPattern = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);
    private User currentUserCache;

    public UserService(Context context, Object o) {
        // Instantiate repositories and dependencies internally
        FireBaseRepository fireBaseRepository = new FireBaseRepository();
        this.repository = new UserRepository(fireBaseRepository);
        this.deviceIdManager = new DeviceIdManager(context);
    }

    public Task<QuerySnapshot> getAllUsers() {
        return repository.getAllUsers();
    }

    public Task<User> getCurrentUser() {
        if (currentUserCache != null) {
            return com.google.android.gms.tasks.Tasks.forResult(currentUserCache);
        }
        String deviceId = deviceIdManager.ensureDeviceId();
        return repository.findUserByDeviceId(deviceId).onSuccessTask(querySnapshot -> {
            if (!querySnapshot.isEmpty()) {
                currentUserCache = querySnapshot.getDocuments().get(0).toObject(User.class);
                return com.google.android.gms.tasks.Tasks.forResult(currentUserCache);
            }
            return com.google.android.gms.tasks.Tasks.forResult(null);
        });
    }

    public void saveUser(User user, com.google.android.gms.tasks.OnSuccessListener<String> onSuccess, com.google.android.gms.tasks.OnFailureListener onFailure) {
        validateName(user.getName());
        validateEmail(user.getEmail());
        if (user.getPasswordHash() == null || user.getPasswordHash().isEmpty()) {
            onFailure.onFailure(new IllegalArgumentException("Password hash missing"));
            return;
        }
        repository.saveUser(user, onSuccess, onFailure);
    }

    public Task<Void> updateUser(User user) {
        validateName(user.getName());
        validateEmail(user.getEmail());
        if (currentUserCache != null && currentUserCache.getUserId().equals(user.getUserId())) {
            currentUserCache = user;
        }
        return repository.updateUser(user);
    }

    public Task<Boolean> authenticate(String email, String password) {
        return repository.findUserByEmail(email).onSuccessTask(querySnapshot -> {
            if (querySnapshot.isEmpty()) {
                return com.google.android.gms.tasks.Tasks.forResult(false);
            }
            User user = querySnapshot.getDocuments().get(0).toObject(User.class);
            if (user == null) {
                return com.google.android.gms.tasks.Tasks.forResult(false);
            }
            String hash = hashPassword(password);
            boolean success = hash.equals(user.getPasswordHash());
            if (success) {
                currentUserCache = user;
                attachDeviceToCurrentUser(user);
            }
            return com.google.android.gms.tasks.Tasks.forResult(success);
        });
    }

    public Task<Boolean> authenticateDevice(String deviceId) {
        return repository.findUserByDeviceId(deviceId).onSuccessTask(querySnapshot -> {
            if (!querySnapshot.isEmpty()) {
                currentUserCache = querySnapshot.getDocuments().get(0).toObject(User.class);
                return com.google.android.gms.tasks.Tasks.forResult(true);
            }
            return com.google.android.gms.tasks.Tasks.forResult(false);
        });
    }

    private void attachDeviceToCurrentUser(User user) {
        String deviceId = deviceIdManager.ensureDeviceId();
        if (!deviceId.equals(user.getDeviceId())) {
            user.setDeviceId(deviceId);
            updateUser(user);
        }
    }

    public void logout() {
        currentUserCache = null;
    }

    public Task<Void> deleteUserProfile(String userId) {
        if (currentUserCache != null && currentUserCache.getUserId().equals(userId)) {
            currentUserCache = null;
        }
        return repository.deleteUserById(userId);
    }

    public void createNewUser(String name, String email, String phone, String password, com.google.android.gms.tasks.OnSuccessListener<String> onSuccess, com.google.android.gms.tasks.OnFailureListener onFailure) {
        try {
            validateName(name);
            validateEmail(email);
            validatePassword(password);
        } catch (IllegalArgumentException e) {
            onFailure.onFailure(e);
            return;
        }

        String trimmedPhone = phone == null ? "" : phone.trim();
        String deviceId = deviceIdManager.ensureDeviceId();
        String passwordHash = hashPassword(password);

        User user = new User("", deviceId, name.trim(), email.trim(), trimmedPhone, passwordHash);
        user.setRole(constant.UserRole.ENTRANT);

        repository.saveUser(user, onSuccess, onFailure);
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

    public String hashPassword(String password) {
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