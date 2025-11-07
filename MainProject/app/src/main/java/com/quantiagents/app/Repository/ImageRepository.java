package com.quantiagents.app.Repository;

import androidx.annotation.NonNull;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.quantiagents.app.models.Image;
import java.util.List;

public class ImageRepository {

    private final CollectionReference context;

    public ImageRepository(FireBaseRepository fireBaseRepository) {
        this.context = fireBaseRepository.getImageCollectionRef();
    }

    public Task<DocumentSnapshot> getImageById(String imageId) {
        return context.document(imageId).get();
    }

    public Task<QuerySnapshot> getAllImages() {
        return context.get();
    }

    public void saveImage(Image image, OnSuccessListener<String> onSuccess, OnFailureListener onFailure) {
        if (image.getImageId() == null || image.getImageId().trim().isEmpty()) {
            Task<DocumentReference> addTask = context.add(image);
            addTask.addOnSuccessListener(documentReference -> {
                String docId = documentReference.getId();
                image.setImageId(docId);
                context.document(docId).update("imageId", docId)
                        .addOnSuccessListener(aVoid -> onSuccess.onSuccess(docId))
                        .addOnFailureListener(e -> onSuccess.onSuccess(docId));
            }).addOnFailureListener(onFailure);
        } else {
            String imageId = image.getImageId();
            DocumentReference docRef = context.document(imageId);
            docRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    docRef.set(image, SetOptions.merge())
                            .addOnSuccessListener(aVoid -> onSuccess.onSuccess(imageId))
                            .addOnFailureListener(onFailure);
                } else {
                    docRef.set(image)
                            .addOnSuccessListener(aVoid -> onSuccess.onSuccess(imageId))
                            .addOnFailureListener(onFailure);
                }
            }).addOnFailureListener(onFailure);
        }
    }

    public Task<Void> updateImage(@NonNull Image image) {
        return context.document(image.getImageId()).set(image, SetOptions.merge());
    }

    public Task<Void> deleteImageById(String imageId) {
        return context.document(imageId).delete();
    }

    public Task<Void> deleteImagesByEventId(String eventId) {
        return context.whereEqualTo("eventId", eventId).get()
                .onSuccessTask(querySnapshot -> {
                    WriteBatch batch = context.getFirestore().batch();
                    List<DocumentSnapshot> documents = querySnapshot.getDocuments();
                    for (DocumentSnapshot doc : documents) {
                        batch.delete(doc.getReference());
                    }
                    return batch.commit();
                });
    }
}