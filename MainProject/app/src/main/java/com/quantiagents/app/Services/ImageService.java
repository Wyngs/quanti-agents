package com.quantiagents.app.Services;

import android.content.Context;
import androidx.annotation.NonNull;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.quantiagents.app.Repository.FireBaseRepository;
import com.quantiagents.app.Repository.ImageRepository;
import com.quantiagents.app.models.Image;

public class ImageService {

    private final ImageRepository repository;

    public ImageService(Context context) {
        FireBaseRepository fireBaseRepository = new FireBaseRepository();
        this.repository = new ImageRepository(fireBaseRepository);
    }

    public Task<DocumentSnapshot> getImageById(String imageId) {
        return repository.getImageById(imageId);
    }

    public Task<QuerySnapshot> getAllImages() {
        return repository.getAllImages();
    }

    public void saveImage(Image image, OnSuccessListener<String> onSuccess, OnFailureListener onFailure) {
        if (image == null) {
            onFailure.onFailure(new IllegalArgumentException("Image cannot be null"));
            return;
        }
        if (image.getUri() == null || image.getUri().trim().isEmpty()) {
            onFailure.onFailure(new IllegalArgumentException("Image URI is required"));
            return;
        }
        repository.saveImage(image, onSuccess, onFailure);
    }

    public Task<Void> updateImage(@NonNull Image image) {
        if (image.getImageId() == null || image.getImageId().trim().isEmpty()) {
            throw new IllegalArgumentException("Image ID is required");
        }
        return repository.updateImage(image);
    }

    public Task<Void> deleteImageById(String imageId) {
        if (imageId == null || imageId.trim().isEmpty()) {
            throw new IllegalArgumentException("Image ID is required");
        }
        return repository.deleteImageById(imageId);
    }

    public Task<Void> deleteImagesByEventId(String eventId) {
        if (eventId == null || eventId.trim().isEmpty()) {
            throw new IllegalArgumentException("Event ID is required");
        }
        return repository.deleteImagesByEventId(eventId);
    }
}