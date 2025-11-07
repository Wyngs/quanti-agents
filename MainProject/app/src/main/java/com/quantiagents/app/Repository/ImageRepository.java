package com.quantiagents.app.Repository;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.quantiagents.app.models.Image;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Manages functions for locating and saving images
 */
public class ImageRepository {

    private final CollectionReference context;

    public ImageRepository(FireBaseRepository fireBaseRepository) {
        this.context = fireBaseRepository.getPosterCollectionRef();
    }

    /**
     * Finds image by it's id
     * @param imageId
     * Image id to locate
     * @return
     * Returns Image if exists, null otherwise
     */
    public Image getImageById(String imageId) {
        try {
            DocumentSnapshot snapshot = Tasks.await(context.document(imageId).get());
            if (snapshot.exists()) {
                return snapshot.toObject(Image.class);
            } else {
                Log.d("Firestore", "No image found for ID: " + imageId);
                return null;
            }
        } catch (ExecutionException | InterruptedException e) {
            Log.e("Firestore", "Error getting image", e);
            return null;
        }
    }

    /**
     * Gets a list of all images
     * @return
     * Returns list of images
     */
    public List<Image> getAllImages() {
        try {
            QuerySnapshot snapshot = Tasks.await(context.get());
            List<Image> images = new ArrayList<>();
            for (QueryDocumentSnapshot document : snapshot) {
                Image image = document.toObject(Image.class);
                if (image != null) {
                    images.add(image);
                }
            }
            return images;
        } catch (ExecutionException | InterruptedException e) {
            Log.e("Firestore", "Error getting all images", e);
            return new ArrayList<>();
        }
    }

    /**
     * Saves image to the firebase
     * @param image
     * Image to save
     * @param onSuccess
     * Calls a function on success
     * @param onFailure
     * Calls a function on failure
     */
    public void saveImage(Image image, OnSuccessListener<String> onSuccess, OnFailureListener onFailure) {
        // If imageId is null or empty, let Firebase auto-generate an ID
        if (image.getImageId() == null || image.getImageId().trim().isEmpty()) {
            // Use .add() to create a new document with auto-generated ID
            Task<DocumentReference> addTask = context.add(image);
            addTask.addOnSuccessListener(documentReference -> {
                // Extract the auto-generated document ID
                String docId = documentReference.getId();
                
                // Use the Firestore document ID as the imageId
                String generatedId = docId;
                
                // Update the image object with the generated ID
                image.setImageId(generatedId);
                
                // Update the document with the generated imageId field
                context.document(docId).update("imageId", generatedId)
                        .addOnSuccessListener(aVoid -> {
                            Log.d("Firestore", "Image created with auto-generated ID: " + generatedId + " (docId: " + docId + ")");
                            onSuccess.onSuccess(generatedId);
                        })
                        .addOnFailureListener(e -> {
                            Log.e("Firestore", "Error updating image with generated ID", e);
                            // Still call success since document was created, just the ID update failed
                            onSuccess.onSuccess(generatedId);
                        });
            }).addOnFailureListener(onFailure);
        } else {
            // Check if image with this ID already exists
            String imageId = image.getImageId();
            DocumentReference docRef = context.document(imageId);
            docRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    // Document exists, update it
                    docRef.set(image, SetOptions.merge())
                            .addOnSuccessListener(aVoid -> onSuccess.onSuccess(imageId))
                            .addOnFailureListener(onFailure);
                } else {
                    // Document doesn't exist, create it
                    docRef.set(image)
                            .addOnSuccessListener(aVoid -> onSuccess.onSuccess(imageId))
                            .addOnFailureListener(onFailure);
                }
            }).addOnFailureListener(onFailure);
        }
    }

    /**
     * Updates image in the firebase
     * @param image
     * Image to update
     * @param onSuccess
     * Calls a function on success
     * @param onFailure
     * Calls a function on failure
     */
    public void updateImage(@NonNull Image image,
                           @NonNull OnSuccessListener<Void> onSuccess,
                           @NonNull OnFailureListener onFailure) {
        if (image.getImageId() == null || image.getImageId().isEmpty()) {
            onFailure.onFailure(new IllegalArgumentException("Image ID cannot be null or empty"));
            return;
        }
        context.document(image.getImageId())
                .set(image, SetOptions.merge()) // merge only changed fields
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Image updated: " + image.getImageId());
                    onSuccess.onSuccess(aVoid);
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error updating image", e);
                    onFailure.onFailure(e);
                });
    }

    /**
     * Deletes image via image id from the firebase
     * @param imageId
     * Image id to delete
     * @param onSuccess
     * Calls a function on success
     * @param onFailure
     * Calls a function on failure
     */
    public void deleteImageById(String imageId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        context.document(imageId)
                .delete()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    /**
     * Deletes image via image id from the firebase
     * @param imageId
     * Image id to delete
     * @return
     * Returns boolean if success
     */
    public boolean deleteImageById(String imageId) {
        try {
            Tasks.await(context.document(imageId).delete());
            Log.d("Firestore", "Image deleted: " + imageId);
            return true;
        } catch (ExecutionException | InterruptedException e) {
            Log.e("Firestore", "Error deleting image", e);
            return false;
        }
    }

    /**
     * Deletes multiple images via the event's id from the firebase
     * @param eventId
     * Event id to delete images from
     * @return
     * Returns amount of images deleted
     */
    public int deleteImagesByEventId(String eventId) {
        if (eventId == null || eventId.isEmpty()) {
            return 0;
        }
        try {
            QuerySnapshot snapshot = Tasks.await(context.whereEqualTo("eventId", eventId).get());
            int removed = 0;
            for (QueryDocumentSnapshot document : snapshot) {
                try {
                    Tasks.await(document.getReference().delete());
                    removed++;
                } catch (ExecutionException | InterruptedException e) {
                    Log.e("Firestore", "Error deleting image during cascade", e);
                }
            }
            Log.d("Firestore", "Deleted " + removed + " images for event: " + eventId);
            return removed;
        } catch (ExecutionException | InterruptedException e) {
            Log.e("Firestore", "Error getting images by event ID", e);
            return 0;
        }
    }
}
