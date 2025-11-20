package com.quantiagents.app.Repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.quantiagents.app.models.AdminActionLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages functions related to admin logs
 * @see AdminActionLog
 */
public class AdminLogRepository {

    private static final String PREF_NAME = "admin_log_store";
    private static final String KEY_LOGS = "logs_json";
    private final SharedPreferences preferences;

    public AdminLogRepository(Context context) {
        //append-only audit log for admin deletions
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Adds log to admin log
     * @param log
     * AdminActionLog to add
     * @see AdminActionLog
     */
    public synchronized void append(AdminActionLog log) {
        try {
            JSONArray arr = new JSONArray(preferences.getString(KEY_LOGS, "[]"));
            arr.put(toJson(log));
            preferences.edit().putString(KEY_LOGS, arr.toString()).apply();
        } catch (JSONException ignore) {
            //best effort
        }
    }

    /**
     * Gets list of all logs in admin log
     * @return
     * Returns list of AdminActionLogs
     * @see AdminActionLog
     */
    public synchronized List<AdminActionLog> listAll() {
        String raw = preferences.getString(KEY_LOGS, "[]");
        List<AdminActionLog> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                out.add(new AdminActionLog(
                        o.optString("kind", ""),
                        o.optString("targetId", ""),
                        o.optLong("timestamp", System.currentTimeMillis()),
                        o.optString("actorDeviceId", ""),
                        o.optString("note", "")
                ));
            }
        } catch (JSONException ignore) {
            //swallow
        }
        return out;
    }

    /**
     * Converts a log into a JSON format
     * @param log
     * Log to be converted
     * @return
     * Returns a JSONObject
     * @throws JSONException
     * If JSON conversion fails
     * @see AdminActionLog
     */
    private JSONObject toJson(AdminActionLog log) throws JSONException {
        JSONObject o = new JSONObject();
        o.put("kind", log.getKind());
        o.put("targetId", log.getTargetId());
        o.put("timestamp", log.getTimestamp());
        o.put("actorDeviceId", log.getActorDeviceId());
        o.put("note", log.getNote());
        return o;
    }
}
