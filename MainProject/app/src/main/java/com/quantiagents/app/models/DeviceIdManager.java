package com.quantiagents.app.models;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

/**
 * This class manages all functions related to device id
 */
public class DeviceIdManager {

    private static final String PREF_NAME = "device_identity_store";
    private static final String KEY_DEVICE_ID = "device_id";
    private final SharedPreferences preferences;

    /**
     * Constructor that initializes the device ID manager with shared preferences.
     * Store device id locally so it can be re-used.
     *
     * @param context The Android context used to access shared preferences
     */
    public DeviceIdManager(Context context) {
        // Store device id locally so I can re-use it.
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Gets or creates a device ID for the current user.
     * If a device ID already exists, returns it. Otherwise, generates a new UUID
     * and stores it in shared preferences.
     *
     * @return The device ID string
     */
    public String ensureDeviceId() {
        String existing = preferences.getString(KEY_DEVICE_ID, null);
        if (existing != null && !existing.isEmpty()) {
            return existing;
        }
        String generated = UUID.randomUUID().toString();
        preferences.edit().putString(KEY_DEVICE_ID, generated).apply();
        return generated;
    }

    /**
     * Gets the device ID for the current user.
     * If no device ID exists, creates one using ensureDeviceId().
     *
     * @return The device ID string
     */
    public String getDeviceId() {
        String current = preferences.getString(KEY_DEVICE_ID, null);
        if (current == null || current.isEmpty()) {
            return ensureDeviceId();
        }
        return current;
    }

    /**
     * Fully resets the device ID by removing it from shared preferences.
     * Warning: This should rarely be used, only for full reset scenarios.
     */
    public void reset() {
        // Rarely touch this, only for full reset.
        preferences.edit().remove(KEY_DEVICE_ID).apply();
    }
}
