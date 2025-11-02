package com.quantiagents.app.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.quantiagents.app.domain.UserSummary;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ProfilesRepository {

    private static final String PREF_NAME = "admin_profiles_store";
    private static final String KEY_PROFILES = "profiles_json";
    private final SharedPreferences preferences;

    public ProfilesRepository(Context context) {
        //admin-side index of profiles for browse/delete
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

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
                        o.optString("email", "")
                ));
            }
        } catch (JSONException ignore) {
            //swallow and return empty
        }
        return out;
    }

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

    private JSONObject toJson(UserSummary s) throws JSONException {
        JSONObject o = new JSONObject();
        o.put("userId", s.getUserId());
        o.put("name", s.getName());
        o.put("email", s.getEmail());
        return o;
    }
}
