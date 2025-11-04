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
import com.quantiagents.app.models.Image;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class ImageRepository {

    private final CollectionReference context;

    public ImageRepository(FireBaseRepository fireBaseRepository) {
        this.context = fireBaseRepository.getPosterCollectionRef();
    }

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

    public void saveImage(Image image, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        if (image.getImageId() == null || image.getImageId().isEmpty()) {
            onFailure.onFailure(new IllegalArgumentException("Image ID cannot be null or empty"));
            return;
        }
        context.document(image.getImageId())
                .set(image)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

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

    public void deleteImageById(String imageId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        context.document(imageId)
                .delete()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

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
