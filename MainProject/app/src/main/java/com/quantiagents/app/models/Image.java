package com.quantiagents.app.models;

import java.io.Serializable;

/**
 * Representation of an image, with getters and setters for each variable
 */
public class Image implements Serializable {

    private String imageId;
    private String eventId; //may be null for non-event images
    private String uri; //could be file path or remote url
    private String uploadedBy;

    public Image(){}

    public Image(String imageId, String eventId, String uri) {
        //lightweight reference for admin gallery
        this.imageId = imageId;
        this.eventId = eventId;
        this.uri = uri;
    }

    public String getImageId() { return imageId; }
    public String getEventId() { return eventId; }
    public String getUri() { return uri; }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
}
