package com.quantiagents.app.Repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.quantiagents.app.models.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

public class UserRepository {

    private static final String PREF_NAME = "user_profile_store";
    private static final String KEY_USER = "user_json";
    private final SharedPreferences preferences;

    public UserRepository(Context context) {
        // Profile data stays in private prefs.
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public User getUser() {
        // Pull and decode the cached user blob.
        String stored = preferences.getString(KEY_USER, null);
        if (stored == null || stored.isEmpty()) {
            return null;
        }
        try {
            JSONObject node = new JSONObject(stored);
            User user = new User(
                    node.optString("userId", UUID.randomUUID().toString()),
                    node.optString("deviceId", ""),
                    node.optString("name", ""),
                    node.optString("email", ""),
                    node.optString("phone", ""),
                    node.optString("passwordHash", "")
            );
            user.setNotificationsOn(node.optBoolean("notificationsOn", true));
            user.setCreatedOn(node.optLong("createdOn", System.currentTimeMillis()));
            return user;
        } catch (JSONException exception) {
            preferences.edit().remove(KEY_USER).apply();
            return null;
        }
    }

    public boolean saveUser(User user) {
        try {
            // Persist user details as a single JSON string.
            JSONObject node = new JSONObject();
            node.put("userId", user.getUserId());
            node.put("deviceId", user.getDeviceId());
            node.put("name", user.getName());
            node.put("email", user.getEmail());
            node.put("phone", user.getPhone());
            node.put("passwordHash", user.getPasswordHash());
            node.put("notificationsOn", user.hasNotificationsOn());
            node.put("createdOn", user.getCreatedOn());
            preferences.edit().putString(KEY_USER, node.toString()).apply();
            return true;
        } catch (JSONException exception) {
            return false;
        }
    }

    public void clearUser() {
        // Drop the cached profile.
        preferences.edit().remove(KEY_USER).apply();
    }
}
