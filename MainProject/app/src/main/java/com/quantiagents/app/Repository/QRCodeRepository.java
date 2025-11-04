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
import com.quantiagents.app.models.QRCode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class QRCodeRepository {

    private final CollectionReference context;

    public QRCodeRepository(FireBaseRepository fireBaseRepository) {
        this.context = fireBaseRepository.getQrCodeCollectionRef();
    }

    public QRCode getQRCodeById(int qrCodeId) {
        try {
            DocumentSnapshot snapshot = Tasks.await(context.document(String.valueOf(qrCodeId)).get());
            if (snapshot.exists()) {
                return snapshot.toObject(QRCode.class);
            } else {
                Log.d("Firestore", "No QR code found for ID: " + qrCodeId);
                return null;
            }
        } catch (ExecutionException | InterruptedException e) {
            Log.e("Firestore", "Error getting QR code", e);
            return null;
        }
    }

    public List<QRCode> getAllQRCodes() {
        try {
            QuerySnapshot snapshot = Tasks.await(context.get());
            List<QRCode> qrCodes = new ArrayList<>();
            for (QueryDocumentSnapshot document : snapshot) {
                QRCode qrCode = document.toObject(QRCode.class);
                if (qrCode != null) {
                    qrCodes.add(qrCode);
                }
            }
            return qrCodes;
        } catch (ExecutionException | InterruptedException e) {
            Log.e("Firestore", "Error getting all QR codes", e);
            return new ArrayList<>();
        }
    }

    public List<QRCode> getQRCodesByEventId(int eventId) {
        try {
            QuerySnapshot snapshot = Tasks.await(context.whereEqualTo("eventId", eventId).get());
            List<QRCode> qrCodes = new ArrayList<>();
            for (QueryDocumentSnapshot document : snapshot) {
                QRCode qrCode = document.toObject(QRCode.class);
                if (qrCode != null) {
                    qrCodes.add(qrCode);
                }
            }
            return qrCodes;
        } catch (ExecutionException | InterruptedException e) {
            Log.e("Firestore", "Error getting QR codes by event ID", e);
            return new ArrayList<>();
        }
    }

    public void saveQRCode(QRCode qrCode, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        // If id is 0 or negative, let Firebase auto-generate an ID
        if (qrCode.getId() <= 0) {
            // Use .add() to create a new document with auto-generated ID
            Task<DocumentReference> addTask = context.add(qrCode);
            addTask.addOnSuccessListener(documentReference -> {
                // Extract the auto-generated document ID
                String docId = documentReference.getId();

                // Generate a unique int ID from the Firestore document ID
                // Use hash code and ensure it's positive
                int generatedId = docId.hashCode();
                if (generatedId < 0) {
                    generatedId = Math.abs(generatedId);
                }
                // If somehow still 0, use a timestamp-based fallback
                if (generatedId == 0) {
                    generatedId = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
                }

                // Update the QRCode object with the generated ID
                qrCode.setId(generatedId);

                // Update the document with the generated id field
                context.document(docId).update("id", generatedId)
                        .addOnSuccessListener(aVoid -> {
                            Log.d("Firestore", "QR code created with auto-generated ID: " + generatedId + " (docId: " + docId + ")");
                            onSuccess.onSuccess(aVoid);
                        })
                        .addOnFailureListener(e -> {
                            Log.e("Firestore", "Error updating QR code with generated ID", e);
                            // Still call success since document was created, just the ID update failed
                            onSuccess.onSuccess(null);
                        });
            }).addOnFailureListener(onFailure);
        } else {
            // Check if QR code with this ID already exists
            DocumentReference docRef = context.document(String.valueOf(qrCode.getId()));
            docRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    // Document exists, update it
                    docRef.set(qrCode, SetOptions.merge())
                            .addOnSuccessListener(onSuccess)
                            .addOnFailureListener(onFailure);
                } else {
                    // Document doesn't exist, create it
                    docRef.set(qrCode)
                            .addOnSuccessListener(onSuccess)
                            .addOnFailureListener(onFailure);
                }
            }).addOnFailureListener(onFailure);
        }
    }

    public void updateQRCode(@NonNull QRCode qrCode,
                             @NonNull OnSuccessListener<Void> onSuccess,
                             @NonNull OnFailureListener onFailure) {
        context.document(String.valueOf(qrCode.getId()))
                .set(qrCode, SetOptions.merge()) // merge only changed fields
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "QR code updated: " + qrCode.getId());
                    onSuccess.onSuccess(aVoid);
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error updating QR code", e);
                    onFailure.onFailure(e);
                });
    }

    public void deleteQRCodeById(int qrCodeId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        context.document(String.valueOf(qrCodeId))
                .delete()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public boolean deleteQRCodeById(int qrCodeId) {
        try {
            Tasks.await(context.document(String.valueOf(qrCodeId)).delete());
            Log.d("Firestore", "QR code deleted: " + qrCodeId);
            return true;
        } catch (ExecutionException | InterruptedException e) {
            Log.e("Firestore", "Error deleting QR code", e);
            return false;
        }
    }
}
