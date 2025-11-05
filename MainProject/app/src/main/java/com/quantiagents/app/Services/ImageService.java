package com.quantiagents.app.Services;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.quantiagents.app.Repository.FireBaseRepository;
import com.quantiagents.app.Repository.ImageRepository;
import com.quantiagents.app.models.Image;

import java.util.List;

public class ImageService {

    private final ImageRepository repository;

    public ImageService(Context context) {
        // ImageService instantiates its own repositories internally
        FireBaseRepository fireBaseRepository = new FireBaseRepository();
        this.repository = new ImageRepository(fireBaseRepository);
    }

    public Image getImageById(String imageId) {
        return repository.getImageById(imageId);
    }

    public List<Image> getAllImages() {
        return repository.getAllImages();
    }

    public void saveImage(Image image, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        // Validate image before saving
        if (image == null) {
            onFailure.onFailure(new IllegalArgumentException("Image cannot be null"));
            return;
        }
        if (image.getImageId() == null || image.getImageId().trim().isEmpty()) {
            onFailure.onFailure(new IllegalArgumentException("Image ID is required"));
            return;
        }
        if (image.getUri() == null || image.getUri().trim().isEmpty()) {
            onFailure.onFailure(new IllegalArgumentException("Image URI is required"));
            return;
        }
        
        repository.saveImage(image,
                aVoid -> {
                    Log.d("App", "Image saved: " + image.getImageId());
                    onSuccess.onSuccess(aVoid);
                },
                e -> {
                    Log.e("App", "Failed to save image", e);
                    onFailure.onFailure(e);
                });
    }

    public void updateImage(@NonNull Image image,
                           @NonNull OnSuccessListener<Void> onSuccess,
                           @NonNull OnFailureListener onFailure) {
        // Validate image before updating
        if (image.getImageId() == null || image.getImageId().trim().isEmpty()) {
            onFailure.onFailure(new IllegalArgumentException("Image ID is required"));
            return;
        }
        
        repository.updateImage(image,
                aVoid -> {
                    Log.d("App", "Image updated: " + image.getImageId());
                    onSuccess.onSuccess(aVoid);
                },
                e -> {
                    Log.e("App", "Failed to update image", e);
                    onFailure.onFailure(e);
                });
    }

    public void deleteImage(String imageId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        if (imageId == null || imageId.trim().isEmpty()) {
            onFailure.onFailure(new IllegalArgumentException("Image ID is required"));
            return;
        }
        
        repository.deleteImageById(imageId,
                aVoid -> {
                    Log.d("App", "Image deleted: " + imageId);
                    onSuccess.onSuccess(aVoid);
                },
                e -> {
                    Log.e("App", "Failed to delete image", e);
                    onFailure.onFailure(e);
                });
    }

    public boolean deleteImage(String imageId) {
        if (imageId == null || imageId.trim().isEmpty()) {
            return false;
        }
        return repository.deleteImageById(imageId);
    }

    public int deleteImagesByEventId(String eventId) {
        if (eventId == null || eventId.trim().isEmpty()) {
            return 0;
        }
        return repository.deleteImagesByEventId(eventId);
    }
}

