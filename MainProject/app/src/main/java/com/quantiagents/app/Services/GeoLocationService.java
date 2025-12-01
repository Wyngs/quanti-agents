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

/**
 * Service layer for GeoLocation operations.
 * Handles business logic for saving, updating, and deleting geolocation data.
 * Validates coordinates are within valid ranges (-90 to 90 for latitude, -180 to 180 for longitude).
 */
public class GeoLocationService {

    private final GeoLocationRepository repository;

    /**
     * Constructor that initializes the GeoLocationService with required dependencies.
     * GeoLocationService instantiates its own repositories internally.
     *
     * @param context The Android context (currently unused but kept for consistency)
     */
    public GeoLocationService(Context context) {
        // GeoLocationService instantiates its own repositories internally
        FireBaseRepository fireBaseRepository = new FireBaseRepository();
        this.repository = new GeoLocationRepository(fireBaseRepository);
    }

    /**
     * Retrieves a geolocation by user ID and event ID synchronously.
     *
     * @param userId The unique identifier of the user
     * @param eventId The unique identifier of the event
     * @return The GeoLocation object, or null if not found
     */
    public GeoLocation getGeoLocationByUserIdAndEventId(String userId, String eventId) {
        return repository.getGeoLocationByUserIdAndEventId(userId, eventId);
    }

    /**
     * Retrieves all geolocations synchronously.
     *
     * @return List of all geolocations
     */
    public List<GeoLocation> getAllGeoLocations() {
        return repository.getAllGeoLocations();
    }

    /**
     * Retrieves all geolocations for a specific event synchronously.
     *
     * @param eventId The unique identifier of the event
     * @return List of geolocations for the event
     */
    public List<GeoLocation> getGeoLocationsByEventId(String eventId) {
        return repository.getGeoLocationsByEventId(eventId);
    }

    /**
     * Retrieves all geolocations for a specific user synchronously.
     *
     * @param userId The unique identifier of the user
     * @return List of geolocations for the user
     */
    public List<GeoLocation> getGeoLocationsByUserId(String userId) {
        return repository.getGeoLocationsByUserId(userId);
    }

    /**
     * Retrieves all geolocations for a specific event and user synchronously.
     *
     * @param eventId The unique identifier of the event
     * @param userId The unique identifier of the user
     * @return List of geolocations matching the criteria
     */
    public List<GeoLocation> getGeoLocationsByEventIdAndUserId(String eventId, String userId) {
        return repository.getGeoLocationsByEventIdAndUserId(eventId, userId);
    }

    /**
     * Validates and saves a new geolocation.
     * Validates coordinates are within valid ranges.
     *
     * @param geoLocation The geolocation to save
     * @param onSuccess Callback receiving the document ID of the saved geolocation
     * @param onFailure Callback receiving validation or database errors
     */
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

    /**
     * Validates and updates an existing geolocation.
     * Validates coordinates are within valid ranges.
     *
     * @param geoLocation The geolocation to update
     * @param onSuccess Callback invoked on successful update
     * @param onFailure Callback invoked if update fails or validation fails
     */
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

    /**
     * Deletes a geolocation by user ID and event ID asynchronously.
     *
     * @param userId The unique identifier of the user
     * @param eventId The unique identifier of the event
     * @param onSuccess Callback invoked on successful deletion
     * @param onFailure Callback invoked if deletion fails
     */
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

    /**
     * Deletes a geolocation by user ID and event ID synchronously.
     *
     * @param userId The unique identifier of the user
     * @param eventId The unique identifier of the event
     * @return True if geolocation was successfully deleted, false otherwise
     */
    public boolean deleteGeoLocation(String userId, String eventId) {
        return repository.deleteGeoLocationByUserIdAndEventId(userId, eventId);
    }
}
