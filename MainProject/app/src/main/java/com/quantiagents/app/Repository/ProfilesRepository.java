package com.quantiagents.app.Repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.quantiagents.app.models.UserSummary;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * This class manages the listing and saving of user profiles
 * @see UserSummary
 */
public class ProfilesRepository {

    private static final String PREF_NAME = "admin_profiles_store";
    private static final String KEY_PROFILES = "profiles_json";
    private final SharedPreferences preferences;

    public ProfilesRepository(Context context) {
        //admin-side index of profiles for browse/delete
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Creates and returns a list of all user profiles
     * @return
     * Returns list of user summaries
     * @see UserSummary
     */
    public synchronized List<UserSummary> listProfiles() {
        String raw = preferences.getString(KEY_PROFILES, "[]");
        List<UserSummary> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                out.add(new UserSummary(
                        o.optString("userId", ""),
                        o.optString("name", ""),
                        o.optString("username", ""),
                        o.optString("email", "")
                ));
            }
        } catch (JSONException ignore) {
            //swallow and return empty
        }
        return out;
    }

    /**
     * Save or replaces a given user summary
     * @param summary
     * User summary to save or replace
     * @return
     * Returns a boolean for success
     * @see UserSummary
     */
    public synchronized boolean saveOrReplace(UserSummary summary) {
        try {
            JSONArray arr = new JSONArray(preferences.getString(KEY_PROFILES, "[]"));
            JSONArray next = new JSONArray();
            boolean replaced = false;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                if (summary.getUserId().equals(o.optString("userId"))) {
                    next.put(toJson(summary));
                    replaced = true;
                } else {
                    next.put(o);
                }
            }
            if (!replaced) {
                next.put(toJson(summary));
            }
            preferences.edit().putString(KEY_PROFILES, next.toString()).apply();
            return true;
        } catch (JSONException e) {
            return false;
        }
    }

    /**
     * Deletes a profile via their user id
     * @param userId
     * User id to delete
     * @return
     * Returns a boolean for success
     */
    public synchronized boolean deleteProfile(String userId) {
        try {
            JSONArray arr = new JSONArray(preferences.getString(KEY_PROFILES, "[]"));
            JSONArray next = new JSONArray();
            boolean removed = false;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                if (userId.equals(o.optString("userId"))) {
                    removed = true;
                } else {
                    next.put(o);
                }
            }
            if (removed) {
                preferences.edit().putString(KEY_PROFILES, next.toString()).apply();
            }
            return removed;
        } catch (JSONException e) {
            return false;
        }
    }

    /**
     * Converts a user summary to a json format
     * @param s
     * User summary to convert
     * @return
     * Returns JSON of user summary
     * @throws JSONException
     * Error to be throw if failure in conversion
     * @see UserSummary
     */
    private JSONObject toJson(UserSummary s) throws JSONException {
        JSONObject o = new JSONObject();
        o.put("userId", s.getUserId());
        o.put("name", s.getName());
        o.put("email", s.getEmail());
        return o;
    }
}
