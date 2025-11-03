package com.quantiagents.app.models;

import java.io.Serializable;

public class StoredImage implements Serializable {

    private final String imageId;
    private final String eventId; //may be null for non-event images
    private final String uri; //could be file path or remote url

    public StoredImage(String imageId, String eventId, String uri) {
        //lightweight reference for admin gallery
        this.imageId = imageId;
        this.eventId = eventId;
        this.uri = uri;
    }

    public String getImageId() { return imageId; }
    public String getEventId() { return eventId; }
    public String getUri() { return uri; }
}
