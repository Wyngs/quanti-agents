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

public class QRCodeService {

    private final QRCodeRepository repository;

    public QRCodeService(Context context) {
        // QRCodeService instantiates its own repositories internally
        FireBaseRepository fireBaseRepository = new FireBaseRepository();
        this.repository = new QRCodeRepository(fireBaseRepository);
    }

    public QRCode getQRCodeById(int qrCodeId) {
        return repository.getQRCodeById(qrCodeId);
    }

    public List<QRCode> getAllQRCodes() {
        return repository.getAllQRCodes();
    }

    public List<QRCode> getQRCodesByEventId(int eventId) {
        return repository.getQRCodesByEventId(eventId);
    }

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
        if (qrCode.getEventId() <= 0) {
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
        if (qrCode.getEventId() <= 0) {
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

    public boolean deleteQRCode(int qrCodeId) {
        if (qrCodeId <= 0) {
            return false;
        }
        return repository.deleteQRCodeById(qrCodeId);
    }
}
