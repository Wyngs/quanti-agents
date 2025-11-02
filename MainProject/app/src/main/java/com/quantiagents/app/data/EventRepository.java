package com.quantiagents.app.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.quantiagents.app.domain.Event;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class EventRepository {

    private static final String PREF_NAME = "admin_event_store";
    private static final String KEY_EVENTS = "events_json";
    private final SharedPreferences preferences;

    public EventRepository(Context context) {
        //persist a lightweight event catalog for admin screens
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public synchronized List<Event> listEvents() {
        //return all events; tolerant of malformed json
        String raw = preferences.getString(KEY_EVENTS, "[]");
        List<Event> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                String eventId = o.optString("eventId", "");
                if (eventId.isEmpty()) continue;
                String title = o.optString("title", "");
                String poster = o.optString("posterImageId", "");
                poster = poster.isEmpty() ? null : poster;
                out.add(new Event(eventId, title, poster));
            }
        } catch (JSONException ignore) {
            //swallow and return what we have
        }
        return out;
    }

    public synchronized boolean saveOrReplace(Event event) {
        //upsert an event by id
        try {
            JSONArray arr = new JSONArray(preferences.getString(KEY_EVENTS, "[]"));
            JSONArray next = new JSONArray();
            boolean replaced = false;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                if (event.getEventId().equals(o.optString("eventId"))) {
                    next.put(toJson(event));
                    replaced = true;
                } else {
                    next.put(o);
                }
            }
            if (!replaced) next.put(toJson(event));
            preferences.edit().putString(KEY_EVENTS, next.toString()).apply();
            return true;
        } catch (JSONException e) {
            return false;
        }
    }

    public synchronized boolean deleteEvent(String eventId) {
        //remove an event by id
        try {
            JSONArray arr = new JSONArray(preferences.getString(KEY_EVENTS, "[]"));
            JSONArray next = new JSONArray();
            boolean removed = false;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                if (eventId.equals(o.optString("eventId"))) {
                    removed = true;
                } else {
                    next.put(o);
                }
            }
            if (removed) preferences.edit().putString(KEY_EVENTS, next.toString()).apply();
            return removed;
        } catch (JSONException e) {
            return false;
        }
    }

    private JSONObject toJson(Event e) throws JSONException {
        //serialize with null-safe fields
        JSONObject o = new JSONObject();
        o.put("eventId", e.getEventId() == null ? "" : e.getEventId());
        o.put("title", e.getTitle() == null ? "" : e.getTitle());
        o.putOpt("posterImageId", e.getPosterImageId()); //safe when null
        return o;
    }
}
