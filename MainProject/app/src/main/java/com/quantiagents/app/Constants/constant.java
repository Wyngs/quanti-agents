package com.quantiagents.app.Constants;

public class constant {

    public enum UserRole{
        ENTRANT,
        ORGANIZER,
        ADMIN
    }
    public enum EventRegistrationStatus{
        WAITLIST,
        CANCELLED,
        CONFIRMED,
        SELECTED
    }
    public enum EventStatus {
        OPEN,
        CLOSED,
        FULL,
        CANCELLED
    }
    public enum NotificationType {
        GOOD,
        WARNING,
        REMINDER,
        BAD
    }

    public static final String UserCollectionName = "USER";
    public static final String EventCollectionName = "EVENT";
    public static final String ImageCollectionName = "IMAGE";
    public static final String LotteryCollectionName = "LOTTERY";
    public static final String QrCodeCollectionName = "QRCODE";
    public static final String NotificationCollectionName = "NOTIFICATION";
    public static final String GeoLocationCollectionName = "GEO_LOCATION";
    public static final String RegistrationHistoryCollectionName = "REGISTRATION_HISTORY";
    public static final String DeviceIdCollectionName = "DEVICE_ID";
}