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

/**
 * Manages locating and saving of QR codes
 * @see QRCode
 */
public class QRCodeRepository {

    private final CollectionReference context;

    public QRCodeRepository(FireBaseRepository fireBaseRepository) {
        this.context = fireBaseRepository.getQrCodeCollectionRef();
    }

    /**
     * Locates a qr code by it's id
     * @param qrCodeId
     * Qr code to locate
     * @return
     * Returns qr code
     * @see QRCode
     */
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

    /**
     * Returns a list of all qr codes
     * @return
     * Returns list of qr codes
     * @see QRCode
     */
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

    /**
     * Returns a list of all qr codes with an event id
     * @param eventId
     * Event id to search for
     * @return
     * Returns list of qr code
     * @see QRCode
     */
    public List<QRCode> getQRCodesByEventId(String eventId) {
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

    /**
     * Saves a qr code to the firebase
     * @param qrCode
     * Qr code to be saved
     * @param onSuccess
     * Calls a function on success
     * @param onFailure
     * Calls a function on failure
     * @see QRCode
     */
    public void saveQRCode(QRCode qrCode, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        // If id is 0 or negative, generate one and use it as the Document ID
        if (qrCode.getId() <= 0) {
            // Generate a random ID locally from a new document reference
            String autoDocId = context.document().getId();
            // Hash it to an int to match the model's ID type
            int generatedId = autoDocId.hashCode() & Integer.MAX_VALUE;
            if (generatedId == 0) {
                generatedId = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
                if (generatedId == 0) generatedId = 1;
            }

            qrCode.setId(generatedId);

            // Save with the generated INT as the document key so getById(int) works
            int finalGeneratedId = generatedId;
            context.document(String.valueOf(generatedId))
                    .set(qrCode)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("Firestore", "QR code saved with ID: " + finalGeneratedId);
                        onSuccess.onSuccess(aVoid);
                    })
                    .addOnFailureListener(onFailure);
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

    /**
     * Updates a qr code in the firebase
     * @param qrCode
     * Qr code to be updated
     * @param onSuccess
     * Calls a function on success
     * @param onFailure
     * Calls a function on failure
     * @see QRCode
     */
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

    /**
     * Deletes a qr code via it's id from the firebase
     * @param qrCodeId
     * Qr code id to delete
     * @param onSuccess
     * Calls a function on success
     * @param onFailure
     * Calls a function on failure
     * @see QRCode
     */
    public void deleteQRCodeById(int qrCodeId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        context.document(String.valueOf(qrCodeId))
                .delete()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    /**
     * Deletes a qr code via it's id from the firebase
     * @param qrCodeId
     * Qr code id to delete
     * @see QRCode
     */
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