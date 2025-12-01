package com.quantiagents.app.Repository;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.quantiagents.app.Constants.constant;
import com.quantiagents.app.models.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages all functions that use info from the Firebase database.
 * Provides access to Firestore collection references for all entity types.
 */
public class FireBaseRepository {
    private final FirebaseFirestore db;
    private final CollectionReference UserCollectionRef;
    private final CollectionReference EventCollectionRef;
    private final CollectionReference PosterCollectionRef;
    private final CollectionReference LotteryCollectionRef;
    private final CollectionReference QrCodeCollectionRef;
    private final CollectionReference NotificationCollectionRef;
    private final CollectionReference GeoLocationCollectionRef;
    private final CollectionReference RegistrationHistoryCollectionRef;
    private final CollectionReference DeviceIdCollectionRef;
    private final CollectionReference ChatCollectionRef;
    private final CollectionReference MessageCollectionRef;
    
    /**
     * Constructor that initializes the FireBaseRepository with Firebase Firestore instance
     * and sets up all collection references.
     */
    public FireBaseRepository() {
        this.db = FirebaseFirestore.getInstance();

        UserCollectionRef = db.collection(constant.UserCollectionName);
        EventCollectionRef = db.collection(constant.EventCollectionName);
        PosterCollectionRef = db.collection(constant.PosterCollectionName);
        LotteryCollectionRef = db.collection(constant.LotteryCollectionName);
        QrCodeCollectionRef = db.collection(constant.QrCodeCollectionName);
        NotificationCollectionRef = db.collection(constant.NotificationCollectionName);
        GeoLocationCollectionRef = db.collection(constant.GeoLocationCollectionName);
        RegistrationHistoryCollectionRef = db.collection(constant.RegistrationHistoryCollectionName);
        DeviceIdCollectionRef = db.collection(constant.DeviceIdCollectionName);
        ChatCollectionRef = db.collection(constant.ChatCollectionName);
        MessageCollectionRef = db.collection(constant.MessageCollectionName);
    }


    /**
     * Gets the Firestore collection reference for users.
     *
     * @return The User collection reference
     */
    public CollectionReference getUserCollectionRef() {
        return UserCollectionRef;
    }

    /**
     * Gets the Firestore collection reference for events.
     *
     * @return The Event collection reference
     */
    public CollectionReference getEventCollectionRef() {
        return EventCollectionRef;
    }

    /**
     * Gets the Firestore collection reference for poster images.
     *
     * @return The Poster collection reference
     */
    public CollectionReference getPosterCollectionRef() {
        return PosterCollectionRef;
    }

    /**
     * Gets the Firestore collection reference for lottery results.
     *
     * @return The Lottery collection reference
     */
    public CollectionReference getLotteryCollectionRef() {
        return LotteryCollectionRef;
    }

    /**
     * Gets the Firestore collection reference for QR codes.
     *
     * @return The QRCode collection reference
     */
    public CollectionReference getQrCodeCollectionRef() {
        return QrCodeCollectionRef;
    }

    /**
     * Gets the Firestore collection reference for notifications.
     *
     * @return The Notification collection reference
     */
    public CollectionReference getNotificationCollectionRef() {
        return NotificationCollectionRef;
    }

    /**
     * Gets the Firestore collection reference for geolocations.
     *
     * @return The GeoLocation collection reference
     */
    public CollectionReference getGeoLocationCollectionRef() {
        return GeoLocationCollectionRef;
    }

    /**
     * Gets the Firestore collection reference for registration histories.
     *
     * @return The RegistrationHistory collection reference
     */
    public CollectionReference getRegistrationHistoryCollectionRef() {
        return RegistrationHistoryCollectionRef;
    }

    /**
     * Gets the Firestore collection reference for device IDs.
     *
     * @return The DeviceId collection reference
     */
    public CollectionReference getDeviceIdCollectionRef() {
        return DeviceIdCollectionRef;
    }

    /**
     * Gets the Firestore collection reference for chats.
     *
     * @return The Chat collection reference
     */
    public CollectionReference getChatCollectionRef() {
        return ChatCollectionRef;
    }

    /**
     * Gets the Firestore collection reference for messages.
     *
     * @return The Message collection reference
     */
    public CollectionReference getMessageCollectionRef() {
        return MessageCollectionRef;
    }
}
