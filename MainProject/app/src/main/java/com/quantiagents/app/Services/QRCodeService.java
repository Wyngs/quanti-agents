package com.quantiagents.app.Services;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.quantiagents.app.Repository.FireBaseRepository;
import com.quantiagents.app.Repository.QRCodeRepository;
import com.quantiagents.app.models.QRCode;

import java.util.List;

/**
 * Service layer for QRCode operations.
 * Handles business logic for saving, updating, and deleting QR codes.
 */
public class QRCodeService {

    private final QRCodeRepository repository;

    /**
     * Constructor that initializes the QRCodeService with required dependencies.
     * QRCodeService instantiates its own repositories internally.
     *
     * @param context The Android context (currently unused but kept for consistency)
     */
    public QRCodeService(Context context) {
        // QRCodeService instantiates its own repositories internally
        FireBaseRepository fireBaseRepository = new FireBaseRepository();
        this.repository = new QRCodeRepository(fireBaseRepository);
    }

    /**
     * Retrieves a QR code by its ID synchronously.
     *
     * @param qrCodeId The unique identifier of the QR code
     * @return The QRCode object, or null if not found
     */
    public QRCode getQRCodeById(int qrCodeId) {
        return repository.getQRCodeById(qrCodeId);
    }

    /**
     * Retrieves all QR codes synchronously.
     *
     * @return List of all QR codes
     */
    public List<QRCode> getAllQRCodes() {
        return repository.getAllQRCodes();
    }

    /**
     * Retrieves all QR codes associated with a specific event synchronously.
     *
     * @param eventId The unique identifier of the event
     * @return List of QR codes for the event
     */
    public List<QRCode> getQRCodesByEventId(String eventId) {
        return repository.getQRCodesByEventId(eventId);
    }

    /**
     * Validates and saves a new QR code.
     * If id is 0 or negative, Firebase will auto-generate an ID.
     *
     * @param qrCode The QR code to save
     * @param onSuccess Callback invoked on successful save
     * @param onFailure Callback receiving validation or database errors
     */
    public void saveQRCode(QRCode qrCode, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        // Validate QR code before saving
        if (qrCode == null) {
            onFailure.onFailure(new IllegalArgumentException("QR code cannot be null"));
            return;
        }
        if (qrCode.getQrCodeValue() == null || qrCode.getQrCodeValue().trim().isEmpty()) {
            onFailure.onFailure(new IllegalArgumentException("QR code value is required"));
            return;
        }
        if (qrCode.getEventId() == null || qrCode.getEventId().trim().isEmpty()) {
            onFailure.onFailure(new IllegalArgumentException("Event ID is required"));
            return;
        }

        // If id is 0 or negative, Firebase will auto-generate an ID
        // The QRCode object will be updated with the generated ID after saving
        repository.saveQRCode(qrCode,
                aVoid -> {
                    Log.d("App", "QR code saved with ID: " + qrCode.getId());
                    onSuccess.onSuccess(aVoid);
                },
                e -> {
                    Log.e("App", "Failed to save QR code", e);
                    onFailure.onFailure(e);
                });
    }

    /**
     * Validates and updates an existing QR code.
     *
     * @param qrCode The QR code to update (must have valid id, value, and eventId)
     * @param onSuccess Callback invoked on successful update
     * @param onFailure Callback invoked if update fails or validation fails
     */
    public void updateQRCode(@NonNull QRCode qrCode,
                             @NonNull OnSuccessListener<Void> onSuccess,
                             @NonNull OnFailureListener onFailure) {
        // Validate QR code before updating
        if (qrCode.getId() <= 0) {
            onFailure.onFailure(new IllegalArgumentException("QR code ID is required"));
            return;
        }
        if (qrCode.getQrCodeValue() == null || qrCode.getQrCodeValue().trim().isEmpty()) {
            onFailure.onFailure(new IllegalArgumentException("QR code value is required"));
            return;
        }
        if (qrCode.getEventId() == null || qrCode.getEventId().trim().isEmpty()) {
            onFailure.onFailure(new IllegalArgumentException("Event ID is required"));
            return;
        }

        repository.updateQRCode(qrCode,
                aVoid -> {
                    Log.d("App", "QR code updated: " + qrCode.getId());
                    onSuccess.onSuccess(aVoid);
                },
                e -> {
                    Log.e("App", "Failed to update QR code", e);
                    onFailure.onFailure(e);
                });
    }

    /**
     * Deletes a QR code asynchronously.
     *
     * @param qrCodeId The ID of the QR code to delete (must be positive)
     * @param onSuccess Callback invoked on successful deletion
     * @param onFailure Callback invoked if deletion fails or qrCodeId is invalid
     */
    public void deleteQRCode(int qrCodeId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        if (qrCodeId <= 0) {
            onFailure.onFailure(new IllegalArgumentException("QR code ID must be positive"));
            return;
        }

        repository.deleteQRCodeById(qrCodeId,
                aVoid -> {
                    Log.d("App", "QR code deleted: " + qrCodeId);
                    onSuccess.onSuccess(aVoid);
                },
                e -> {
                    Log.e("App", "Failed to delete QR code", e);
                    onFailure.onFailure(e);
                });
    }

    /**
     * Deletes a QR code synchronously.
     *
     * @param qrCodeId The ID of the QR code to delete (must be positive)
     * @return True if QR code was successfully deleted, false otherwise
     */
    public boolean deleteQRCode(int qrCodeId) {
        if (qrCodeId <= 0) {
            return false;
        }
        return repository.deleteQRCodeById(qrCodeId);
    }
}
