package com.quantiagents.app.models;

import java.util.Date;

/**
 * Representation of a geolocation, with getters and setters for each variable
 * <p>
 * Contains:
 * </p>
 * double: latitude, double: longitude, Date: timestamp, String: user id, String: event id
 */
public class GeoLocation {
    private double latitude;
    private double longitude;
    private Date timeStamp;
    private String userId;
    private String eventId;

    /**
     * Default constructor that creates an empty geolocation.
     */
    public GeoLocation(){}
    
    /**
     * Constructor that creates a geolocation with coordinates, user, and event.
     * Timestamp is automatically set to the current time.
     *
     * @param latitude The latitude coordinate
     * @param longitude The longitude coordinate
     * @param userId The unique identifier of the user associated with this location
     * @param eventId The unique identifier of the event associated with this location
     */
    public GeoLocation(double latitude, double longitude, String userId, String eventId) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.timeStamp = new Date();
        this.userId = userId;
        this.eventId = eventId;
    }
    
    /**
     * Constructor that creates a geolocation with all fields including custom timestamp.
     *
     * @param latitude The latitude coordinate
     * @param longitude The longitude coordinate
     * @param timeStamp The timestamp when this location was recorded
     * @param userId The unique identifier of the user associated with this location
     * @param eventId The unique identifier of the event associated with this location
     */
    public GeoLocation(double latitude, double longitude, Date timeStamp, String userId, String eventId) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.timeStamp = timeStamp;
        this.userId = userId;
        this.eventId = eventId;
    }

    /**
     * Gets the latitude coordinate of this location.
     *
     * @return The latitude value
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * Sets the latitude coordinate of this location.
     *
     * @param latitude The latitude value to set
     */
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    /**
     * Gets the longitude coordinate of this location.
     *
     * @return The longitude value
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * Sets the longitude coordinate of this location.
     *
     * @param longitude The longitude value to set
     */
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    /**
     * Gets the timestamp when this location was recorded.
     *
     * @return The location timestamp
     */
    public Date getTimeStamp() {
        return timeStamp;
    }

    /**
     * Sets the timestamp when this location was recorded.
     *
     * @param timeStamp The location timestamp to set
     */
    public void setTimeStamp(Date timeStamp) {
        this.timeStamp = timeStamp;
    }

    /**
     * Gets the unique identifier of the user associated with this location.
     *
     * @return The user ID
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Sets the unique identifier of the user associated with this location.
     *
     * @param userId The user ID to set
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * Gets the unique identifier of the event associated with this location.
     *
     * @return The event ID
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * Sets the unique identifier of the event associated with this location.
     *
     * @param eventId The event ID to set
     */
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
}
