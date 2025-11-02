package com.quantiagents.app.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.quantiagents.app.domain.StoredImage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ImageRepository {

    private static final String PREF_NAME = "admin_image_store";
    private static final String KEY_IMAGES = "images_json";
    private final SharedPreferences preferences;

    public ImageRepository(Context context) {
        //tracks image metadata for admin deletion
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public synchronized List<StoredImage> listImages() {
        //return all images; tolerant of malformed json
        String raw = preferences.getString(KEY_IMAGES, "[]");
        List<StoredImage> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                String imageId = o.optString("imageId", "");
                if (imageId.isEmpty()) continue;
                String eid = o.optString("eventId", "");
                String eventId = eid.isEmpty() ? null : eid;
                String uri = o.optString("uri", "");
                out.add(new StoredImage(imageId, eventId, uri));
            }
        } catch (JSONException ignore) {
            //swallow and return what we have
        }
        return out;
    }

    public synchronized boolean saveOrReplace(StoredImage img) {
        //upsert an image by id
        try {
            JSONArray arr = new JSONArray(preferences.getString(KEY_IMAGES, "[]"));
            JSONArray next = new JSONArray();
            boolean replaced = false;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                if (img.getImageId().equals(o.optString("imageId"))) {
                    next.put(toJson(img));
                    replaced = true;
                } else {
                    next.put(o);
                }
            }
            if (!replaced) next.put(toJson(img));
            preferences.edit().putString(KEY_IMAGES, next.toString()).apply();
            return true;
        } catch (JSONException e) {
            return false;
        }
    }

    public synchronized boolean deleteImage(String imageId) {
        //remove a single image by id
        try {
            JSONArray arr = new JSONArray(preferences.getString(KEY_IMAGES, "[]"));
            JSONArray next = new JSONArray();
            boolean removed = false;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                if (imageId.equals(o.optString("imageId"))) {
                    removed = true;
                } else {
                    next.put(o);
                }
            }
            if (removed) preferences.edit().putString(KEY_IMAGES, next.toString()).apply();
            return removed;
        } catch (JSONException e) {
            return false;
        }
    }

    public synchronized int deleteImagesByEventId(String eventId) {
        //remove all images linked to a given event id
        if (eventId == null || eventId.isEmpty()) return 0;
        try {
            JSONArray arr = new JSONArray(preferences.getString(KEY_IMAGES, "[]"));
            JSONArray next = new JSONArray();
            int removed = 0;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                String eid = o.optString("eventId", "");
                if (eventId.equals(eid)) {
                    removed++;
                } else {
                    next.put(o);
                }
            }
            if (removed > 0) preferences.edit().putString(KEY_IMAGES, next.toString()).apply();
            return removed;
        } catch (JSONException e) {
            return 0;
        }
    }

    private JSONObject toJson(StoredImage img) throws JSONException {
        //serialize with null-safe fields
        JSONObject o = new JSONObject();
        o.put("imageId", img.getImageId() == null ? "" : img.getImageId());
        o.putOpt("eventId", img.getEventId()); //safe when null
        o.put("uri", img.getUri() == null ? "" : img.getUri());
        return o;
    }
}
