package com.quantiagents.app.Repository;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.quantiagents.app.Constants.constant;

public class FireBaseRepository {
    private final FirebaseFirestore db;
    private final CollectionReference UserCollectionRef;
    private final CollectionReference EventCollectionRef;
    private final CollectionReference ImageCollectionRef;
    private final CollectionReference LotteryCollectionRef;
    private final CollectionReference QrCodeCollectionRef;
    private final CollectionReference NotificationCollectionRef;
    private final CollectionReference GeoLocationCollectionRef;
    private final CollectionReference RegistrationHistoryCollectionRef;
    private final CollectionReference DeviceIdCollectionRef;

    public FireBaseRepository() {
        this.db = FirebaseFirestore.getInstance();

        UserCollectionRef = db.collection(constant.UserCollectionName);
        EventCollectionRef = db.collection(constant.EventCollectionName);
        ImageCollectionRef = db.collection(constant.ImageCollectionName);
        LotteryCollectionRef = db.collection(constant.LotteryCollectionName);
        QrCodeCollectionRef = db.collection(constant.QrCodeCollectionName);
        NotificationCollectionRef = db.collection(constant.NotificationCollectionName);
        GeoLocationCollectionRef = db.collection(constant.GeoLocationCollectionName);
        RegistrationHistoryCollectionRef = db.collection(constant.RegistrationHistoryCollectionName);
        DeviceIdCollectionRef = db.collection(constant.DeviceIdCollectionName);
    }


    public CollectionReference getUserCollectionRef() {
        return UserCollectionRef;
    }

    public CollectionReference getEventCollectionRef() {
        return EventCollectionRef;
    }

    public CollectionReference getImageCollectionRef() {
        return ImageCollectionRef;
    }

    public CollectionReference getLotteryCollectionRef() {
        return LotteryCollectionRef;
    }

    public CollectionReference getQrCodeCollectionRef() {
        return QrCodeCollectionRef;
    }

    public CollectionReference getNotificationCollectionRef() {
        return NotificationCollectionRef;
    }

    public CollectionReference getGeoLocationCollectionRef() {
        return GeoLocationCollectionRef;
    }

    public CollectionReference getRegistrationHistoryCollectionRef() {
        return RegistrationHistoryCollectionRef;
    }

    public CollectionReference getDeviceIdCollectionRef() {
        return DeviceIdCollectionRef;
    }
}