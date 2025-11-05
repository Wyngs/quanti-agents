package com.quantiagents.app.Services;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.quantiagents.app.Repository.FireBaseRepository;
import com.quantiagents.app.Repository.GeoLocationRepository;
import com.quantiagents.app.models.GeoLocation;

import java.util.List;

public class GeoLocationService {

    private final GeoLocationRepository repository;

    public GeoLocationService(Context context) {
        // GeoLocationService instantiates its own repositories internally
        FireBaseRepository fireBaseRepository = new FireBaseRepository();
        this.repository = new GeoLocationRepository(fireBaseRepository);
    }

    public GeoLocation getGeoLocationByUserIdAndEventId(String userId, String eventId) {
        return repository.getGeoLocationByUserIdAndEventId(userId, eventId);
    }

    public List<GeoLocation> getAllGeoLocations() {
        return repository.getAllGeoLocations();
    }

    public List<GeoLocation> getGeoLocationsByEventId(String eventId) {
        return repository.getGeoLocationsByEventId(eventId);
    }

    public List<GeoLocation> getGeoLocationsByUserId(String userId) {
        return repository.getGeoLocationsByUserId(userId);
    }

    public List<GeoLocation> getGeoLocationsByEventIdAndUserId(String eventId, String userId) {
        return repository.getGeoLocationsByEventIdAndUserId(eventId, userId);
    }

    public void saveGeoLocation(GeoLocation geoLocation, OnSuccessListener<String> onSuccess, OnFailureListener onFailure) {
        // Validate geo location before saving
        if (geoLocation == null) {
            onFailure.onFailure(new IllegalArgumentException("Geo location cannot be null"));
            return;
        }
        // Validate coordinates are within valid range
        if (geoLocation.getLatitude() < -90 || geoLocation.getLatitude() > 90) {
            onFailure.onFailure(new IllegalArgumentException("Latitude must be between -90 and 90"));
            return;
        }
        if (geoLocation.getLongitude() < -180 || geoLocation.getLongitude() > 180) {
            onFailure.onFailure(new IllegalArgumentException("Longitude must be between -180 and 180"));
            return;
        }
        
        repository.saveGeoLocation(geoLocation,
                docId -> {
                    Log.d("App", "Geo location saved with ID: " + docId);
                    onSuccess.onSuccess(docId);
                },
                e -> {
                    Log.e("App", "Failed to save geo location", e);
                    onFailure.onFailure(e);
                });
    }

    public void updateGeoLocation(@NonNull GeoLocation geoLocation,
                                 @NonNull OnSuccessListener<Void> onSuccess,
                                 @NonNull OnFailureListener onFailure) {
        // Validate geo location before updating
        if (geoLocation == null) {
            onFailure.onFailure(new IllegalArgumentException("Geo location cannot be null"));
            return;
        }
        if (geoLocation.getLatitude() < -90 || geoLocation.getLatitude() > 90) {
            onFailure.onFailure(new IllegalArgumentException("Latitude must be between -90 and 90"));
            return;
        }
        if (geoLocation.getLongitude() < -180 || geoLocation.getLongitude() > 180) {
            onFailure.onFailure(new IllegalArgumentException("Longitude must be between -180 and 180"));
            return;
        }
        
        repository.updateGeoLocation(geoLocation,
                aVoid -> {
                    Log.d("App", "Geo location updated: userId=" + geoLocation.getUserId() + ", eventId=" + geoLocation.getEventId());
                    onSuccess.onSuccess(aVoid);
                },
                e -> {
                    Log.e("App", "Failed to update geo location", e);
                    onFailure.onFailure(e);
                });
    }

    public void deleteGeoLocation(String userId, String eventId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        repository.deleteGeoLocationByUserIdAndEventId(userId, eventId,
                aVoid -> {
                    Log.d("App", "Geo location deleted: userId=" + userId + ", eventId=" + eventId);
                    onSuccess.onSuccess(aVoid);
                },
                e -> {
                    Log.e("App", "Failed to delete geo location", e);
                    onFailure.onFailure(e);
                });
    }

    public boolean deleteGeoLocation(String userId, String eventId) {
        return repository.deleteGeoLocationByUserIdAndEventId(userId, eventId);
    }
}
