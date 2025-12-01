package com.quantiagents.app.models;

import java.io.Serializable;

/**
 * Representation of an image, with getters and setters for each variable
 * <p>
 * Contains:
 * </p>
 * String: image id, String: event id, String: url, String: uploaded by
 */
public class Image implements Serializable {

    private String imageId;
    private String eventId; //may be null for non-event images
    private String uri; //could be file path or remote url
    private String uploadedBy;

    /**
     * Default constructor that creates an empty image.
     */
    public Image(){}

    /**
     * Constructor that creates a lightweight image reference for admin gallery.
     *
     * @param imageId The unique identifier for the image
     * @param eventId The unique identifier of the event this image is associated with (may be null)
     * @param uri The URI of the image (could be file path or remote URL)
     */
    public Image(String imageId, String eventId, String uri) {
        //lightweight reference for admin gallery
        this.imageId = imageId;
        this.eventId = eventId;
        this.uri = uri;
    }

    /**
     * Gets the unique identifier for this image.
     *
     * @return The image ID
     */
    public String getImageId() { return imageId; }
    
    /**
     * Gets the unique identifier of the event this image is associated with.
     * May be null for non-event images.
     *
     * @return The event ID, or null if not associated with an event
     */
    public String getEventId() { return eventId; }
    
    /**
     * Gets the URI of this image (could be file path or remote URL).
     *
     * @return The image URI
     */
    public String getUri() { return uri; }

    /**
     * Gets the user ID of the person who uploaded this image.
     *
     * @return The uploader's user ID
     */
    public String getUploadedBy() {
        return uploadedBy;
    }

    /**
     * Sets the user ID of the person who uploaded this image.
     *
     * @param uploadedBy The uploader's user ID to set
     */
    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    /**
     * Sets the unique identifier for this image.
     *
     * @param imageId The image ID to set
     */
    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    /**
     * Sets the unique identifier of the event this image is associated with.
     * May be null for non-event images.
     *
     * @param eventId The event ID to set, or null if not associated with an event
     */
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    /**
     * Sets the URI of this image (could be file path or remote URL).
     *
     * @param uri The image URI to set
     */
    public void setUri(String uri) {
        this.uri = uri;
    }
}
