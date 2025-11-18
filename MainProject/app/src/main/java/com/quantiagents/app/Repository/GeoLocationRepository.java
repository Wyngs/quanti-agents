package com.quantiagents.app.Repository;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.quantiagents.app.models.GeoLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Manages functions for locating and saving geolocations
 * @see GeoLocation
 */
public class GeoLocationRepository {

    private final CollectionReference context;

    public GeoLocationRepository(FireBaseRepository fireBaseRepository) {
        this.context = fireBaseRepository.getGeoLocationCollectionRef();
    }

    /**
     * Finds a geolocation by user id and event id
     * @param userId
     * User id to locate
     * @param eventId
     * Event id to locate
     * @return
     * Returns geolocation if exists, null otherwise
     * @see GeoLocation
     */
    public GeoLocation getGeoLocationByUserIdAndEventId(String userId, String eventId) {
        try {
            // Use composite key: userId_eventId
            String docId = userId + "_" + eventId;
            DocumentSnapshot snapshot = Tasks.await(context.document(docId).get());
            if (snapshot.exists()) {
                return snapshot.toObject(GeoLocation.class);
            } else {
                Log.d("Firestore", "No geo location found for userId: " + userId + ", eventId: " + eventId);
                return null;
            }
        } catch (ExecutionException | InterruptedException e) {
            Log.e("Firestore", "Error getting geo location", e);
            return null;
        }
    }

    /**
     * Returns all geolocations
     * @return
     * Returns a list of geolocations
     * @see GeoLocation
     */
    public List<GeoLocation> getAllGeoLocations() {
        try {
            QuerySnapshot snapshot = Tasks.await(context.get());
            List<GeoLocation> geoLocations = new ArrayList<>();
            for (QueryDocumentSnapshot document : snapshot) {
                GeoLocation geoLocation = document.toObject(GeoLocation.class);
                if (geoLocation != null) {
                    geoLocations.add(geoLocation);
                }
            }
            return geoLocations;
        } catch (ExecutionException | InterruptedException e) {
            Log.e("Firestore", "Error getting all geo locations", e);
            return new ArrayList<>();
        }
    }

    /**
     * Gets a list of geolocations by event id
     * @param eventId
     * Event id to locate
     * @return
     * Returns a list of geolocations
     * @see GeoLocation
     */
    public List<GeoLocation> getGeoLocationsByEventId(String eventId) {
        try {
            QuerySnapshot snapshot = Tasks.await(context.whereEqualTo("eventId", eventId).get());
            List<GeoLocation> geoLocations = new ArrayList<>();
            for (QueryDocumentSnapshot document : snapshot) {
                GeoLocation geoLocation = document.toObject(GeoLocation.class);
                if (geoLocation != null) {
                    geoLocations.add(geoLocation);
                }
            }
            return geoLocations;
        } catch (ExecutionException | InterruptedException e) {
            Log.e("Firestore", "Error getting geo locations by event ID", e);
            return new ArrayList<>();
        }
    }

    /**
     * Gets a list of geolocations by user id
     * @param userId
     * User id to locate
     * @return
     * Returns a list of geolocations
     * @see GeoLocation
     */
    public List<GeoLocation> getGeoLocationsByUserId(String userId) {
        try {
            QuerySnapshot snapshot = Tasks.await(context.whereEqualTo("userId", userId).get());
            List<GeoLocation> geoLocations = new ArrayList<>();
            for (QueryDocumentSnapshot document : snapshot) {
                GeoLocation geoLocation = document.toObject(GeoLocation.class);
                if (geoLocation != null) {
                    geoLocations.add(geoLocation);
                }
            }
            return geoLocations;
        } catch (ExecutionException | InterruptedException e) {
            Log.e("Firestore", "Error getting geo locations by user ID", e);
            return new ArrayList<>();
        }
    }

    /**
     * Gets a list of geolocations by event id and user id
     * @param eventId
     * Event id to locate
     * @param userId
     * User id to locate
     * @return
     * Returns a list of geolocations
     * @see GeoLocation
     */
    public List<GeoLocation> getGeoLocationsByEventIdAndUserId(String eventId, String userId) {
        try {
            QuerySnapshot snapshot = Tasks.await(context.whereEqualTo("eventId", eventId)
                    .whereEqualTo("userId", userId).get());
            List<GeoLocation> geoLocations = new ArrayList<>();
            for (QueryDocumentSnapshot document : snapshot) {
                GeoLocation geoLocation = document.toObject(GeoLocation.class);
                if (geoLocation != null) {
                    geoLocations.add(geoLocation);
                }
            }
            return geoLocations;
        } catch (ExecutionException | InterruptedException e) {
            Log.e("Firestore", "Error getting geo locations by event ID and user ID", e);
            return new ArrayList<>();
        }
    }

    /**
     * Saves a geolocation to the firebase
     * @param geoLocation
     * Geolocation to save
     * @param onSuccess
     * Calls a function on success
     * @param onFailure
     * Calls a function on failure
     * @see GeoLocation
     */
    public void saveGeoLocation(GeoLocation geoLocation, OnSuccessListener<String> onSuccess, OnFailureListener onFailure) {
        if (geoLocation == null) {
            onFailure.onFailure(new IllegalArgumentException("Geo location cannot be null"));
            return;
        }
        // Use composite key: userId_eventId
        String docId = geoLocation.getUserId() + "_" + geoLocation.getEventId();
        context.document(docId)
                .set(geoLocation)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Geo location created with ID: " + docId);
                    onSuccess.onSuccess(docId);
                })
                .addOnFailureListener(onFailure);
    }

    /**
     * Updates a geolocation in the firebase
     * @param geoLocation
     * Geolocation to update
     * @param onSuccess
     * Calls a function on success
     * @param onFailure
     * Calls a function on failure
     * @see GeoLocation
     */
    public void updateGeoLocation(@NonNull GeoLocation geoLocation,
                                 @NonNull OnSuccessListener<Void> onSuccess,
                                 @NonNull OnFailureListener onFailure) {
        // Use composite key: userId_eventId
        String docId = geoLocation.getUserId() + "_" + geoLocation.getEventId();
        context.document(docId)
                .set(geoLocation, SetOptions.merge()) // merge only changed fields
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Geo location updated: userId=" + geoLocation.getUserId() + ", eventId=" + geoLocation.getEventId());
                    onSuccess.onSuccess(aVoid);
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error updating geo location", e);
                    onFailure.onFailure(e);
                });
    }

    /**
     * Deletes a geolocation via user id and event id from the firebase
     * @param userId
     * User id to locate
     * @param eventId
     * Event id to locate
     * @param onSuccess
     * Calls a function on success
     * @param onFailure
     * Calls a function on failure
     */
    public void deleteGeoLocationByUserIdAndEventId(String userId, String eventId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        // Use composite key: userId_eventId
        String docId = userId + "_" + eventId;
        context.document(docId)
                .delete()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    /**
     * Deletes a geolocation via user id and event id from the firebase
     * @param userId
     * User id to locate
     * @param eventId
     * Event id to locate
     * @return
     * Returns boolean if success
     */
    public boolean deleteGeoLocationByUserIdAndEventId(String userId, String eventId) {
        try {
            // Use composite key: userId_eventId
            String docId = userId + "_" + eventId;
            Tasks.await(context.document(docId).delete());
            Log.d("Firestore", "Geo location deleted: userId=" + userId + ", eventId=" + eventId);
            return true;
        } catch (ExecutionException | InterruptedException e) {
            Log.e("Firestore", "Error deleting geo location", e);
            return false;
        }
    }
}
