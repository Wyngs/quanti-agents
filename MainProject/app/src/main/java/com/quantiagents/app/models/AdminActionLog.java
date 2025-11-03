package com.quantiagents.app.models;

import java.io.Serializable;

public class AdminActionLog implements Serializable {

    public static final String KIND_EVENT = "EVENT";
    public static final String KIND_PROFILE = "PROFILE";
    public static final String KIND_IMAGE = "IMAGE";

    private final String kind;     //EVENT/PROFILE/IMAGE
    private final String targetId; //id of the deleted thing
    private final long timestamp;  //epoch millis
    private final String actorDeviceId;
    private final String note;     //optional message

    public AdminActionLog(String kind, String targetId, long timestamp, String actorDeviceId, String note) {
        //immutable admin audit entry
        this.kind = kind;
        this.targetId = targetId;
        this.timestamp = timestamp;
        this.actorDeviceId = actorDeviceId;
        this.note = note == null ? "" : note;
    }

    public String getKind() { return kind; }
    public String getTargetId() { return targetId; }
    public long getTimestamp() { return timestamp; }
    public String getActorDeviceId() { return actorDeviceId; }
    public String getNote() { return note; }
}
