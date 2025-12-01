package com.quantiagents.app.Constants;

/**
 * This class is a collection of constants used across the app.
 * Contains enums for user roles, event statuses, registration statuses, notification types,
 * and collection names for Firestore.
 */
public class constant {

    /**
     * Enumeration of user roles in the system.
     */
    public enum UserRole{
        /** Regular user who can register for events */
        ENTRANT,
        /** Administrator with elevated privileges */
        ADMIN
    }
    
    /**
     * Enumeration of event registration statuses.
     */
    public enum EventRegistrationStatus{
        /** User is on the waiting list */
        WAITLIST,
        /** User has cancelled their registration */
        CANCELLED,
        /** User has confirmed their attendance */
        CONFIRMED,
        /** User has been selected via lottery */
        SELECTED
    }
    
    /**
     * Enumeration of event statuses.
     */
    public enum EventStatus {
        /** Event is open for registration */
        OPEN,
        /** Event registration is closed */
        CLOSED,
        /** Event has reached capacity */
        FULL,
        /** Event has been cancelled */
        CANCELLED
    }
    
    /**
     * Enumeration of notification types.
     */
    public enum NotificationType {
        /** Positive/good news notification */
        GOOD,
        /** Warning notification */
        WARNING,
        /** Reminder notification */
        REMINDER,
        /** Bad/negative news notification */
        BAD
    }

    /** Firestore collection name for users */
    public static final String UserCollectionName = "USER";
    /** Firestore collection name for events */
    public static final String EventCollectionName = "EVENT";
    /** Firestore collection name for poster images */
    public static final String PosterCollectionName = "POSTER";
    /** Firestore collection name for lottery results */
    public static final String LotteryCollectionName = "LOTTERY";
    /** Firestore collection name for QR codes */
    public static final String QrCodeCollectionName = "QRCODE";
    /** Firestore collection name for notifications */
    public static final String NotificationCollectionName = "NOTIFICATION";
    /** Firestore collection name for geolocation data */
    public static final String GeoLocationCollectionName = "GEO_LOCATION";
    /** Firestore collection name for registration history */
    public static final String RegistrationHistoryCollectionName = "REGISTRATION_HISTORY";
    /** Firestore collection name for device IDs */
    public static final String DeviceIdCollectionName = "DEVICE_ID";
    /** Firestore collection name for chats */
    public static final String ChatCollectionName = "CHAT";
    /** Firestore collection name for messages */
    public static final String MessageCollectionName = "MESSAGE";
}

