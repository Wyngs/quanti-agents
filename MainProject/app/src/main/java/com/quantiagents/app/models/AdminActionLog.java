package com.quantiagents.app.models;

import java.io.Serializable;

/**
 * Creates a admin action log, which contains kind of deletion, id of deletion, timestamp, id of deletion item's user, and an optional message
 */
public class AdminActionLog implements Serializable {

    public static final String KIND_EVENT = "EVENT";
    public static final String KIND_PROFILE = "PROFILE";
    public static final String KIND_IMAGE = "IMAGE";

    private final String kind;     //EVENT/PROFILE/IMAGE
    private final String targetId; //id of the deleted thing
    private final long timestamp;  //epoch millis
    private final String actorDeviceId;
    private final String note;     //optional message

    /**
     * Constructor that creates an immutable admin audit entry.
     *
     * @param kind The kind of action (EVENT, PROFILE, or IMAGE)
     * @param targetId The unique identifier of the item that was acted upon
     * @param timestamp The timestamp when the action occurred (epoch milliseconds)
     * @param actorDeviceId The device ID of the admin who performed the action
     * @param note Optional message or note about the action
     */
    public AdminActionLog(String kind, String targetId, long timestamp, String actorDeviceId, String note) {
        //immutable admin audit entry
        this.kind = kind;
        this.targetId = targetId;
        this.timestamp = timestamp;
        this.actorDeviceId = actorDeviceId;
        this.note = note == null ? "" : note;
    }

    /**
     * Gets the kind of action performed (EVENT, PROFILE, or IMAGE).
     *
     * @return The action kind
     */
    public String getKind() { return kind; }
    
    /**
     * Gets the unique identifier of the item that was acted upon.
     *
     * @return The target ID
     */
    public String getTargetId() { return targetId; }
    
    /**
     * Gets the timestamp when the action occurred.
     *
     * @return The timestamp in epoch milliseconds
     */
    public long getTimestamp() { return timestamp; }
    
    /**
     * Gets the device ID of the admin who performed the action.
     *
     * @return The actor's device ID
     */
    public String getActorDeviceId() { return actorDeviceId; }
    
    /**
     * Gets the optional message or note about the action.
     *
     * @return The note string, or empty string if no note was provided
     */
    public String getNote() { return note; }
}
