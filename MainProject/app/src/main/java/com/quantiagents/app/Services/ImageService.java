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

/**
 * Service layer for Image operations.
 * Handles business logic for saving, updating, and deleting images.
 */
public class ImageService {

    private final ImageRepository repository;

    /**
     * Constructor that initializes the ImageService with required dependencies.
     * ImageService instantiates its own repositories internally.
     *
     * @param context The Android context (currently unused but kept for consistency)
     */
    public ImageService(Context context) {
        // ImageService instantiates its own repositories internally
        FireBaseRepository fireBaseRepository = new FireBaseRepository();
        this.repository = new ImageRepository(fireBaseRepository);
    }

    /**
     * Retrieves an image by its ID synchronously.
     *
     * @param imageId The unique identifier of the image
     * @return The Image object, or null if not found
     */
    public Image getImageById(String imageId) {
        return repository.getImageById(imageId);
    }

    /**
     * Retrieves all images synchronously.
     *
     * @return List of all images
     */
    public List<Image> getAllImages() {
        return repository.getAllImages();
    }

    /**
     * Validates and saves a new image.
     * Note: imageId is optional - repository will auto-generate if null/empty.
     *
     * @param image The image to save
     * @param onSuccess Callback receiving the saved image ID
     * @param onFailure Callback receiving validation or database errors
     */
    public void saveImage(Image image, OnSuccessListener<String> onSuccess, OnFailureListener onFailure) {
        // Validate image before saving
        if (image == null) {
            onFailure.onFailure(new IllegalArgumentException("Image cannot be null"));
            return;
        }
        // Note: imageId is optional - repository will auto-generate if null/empty
        if (image.getUri() == null || image.getUri().trim().isEmpty()) {
            onFailure.onFailure(new IllegalArgumentException("Image URI is required"));
            return;
        }
        
        repository.saveImage(image,
                imageId -> {
                    Log.d("App", "Image saved with ID: " + imageId);
                    onSuccess.onSuccess(imageId);
                },
                e -> {
                    Log.e("App", "Failed to save image", e);
                    onFailure.onFailure(e);
                });
    }

    /**
     * Validates and updates an existing image.
     *
     * @param image The image to update (must have a valid imageId)
     * @param onSuccess Callback invoked on successful update
     * @param onFailure Callback invoked if update fails or imageId is missing
     */
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

    /**
     * Deletes an image asynchronously.
     *
     * @param imageId The ID of the image to delete
     * @param onSuccess Callback invoked on successful deletion
     * @param onFailure Callback invoked if deletion fails or imageId is invalid
     */
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

    /**
     * Deletes an image synchronously.
     *
     * @param imageId The ID of the image to delete
     * @return True if image was successfully deleted, false otherwise
     */
    public boolean deleteImage(String imageId) {
        if (imageId == null || imageId.trim().isEmpty()) {
            return false;
        }
        return repository.deleteImageById(imageId);
    }

    /**
     * Deletes all images associated with a specific event.
     *
     * @param eventId The ID of the event whose images should be deleted
     * @return The number of images deleted
     */
    public int deleteImagesByEventId(String eventId) {
        if (eventId == null || eventId.trim().isEmpty()) {
            return 0;
        }
        return repository.deleteImagesByEventId(eventId);
    }
}

