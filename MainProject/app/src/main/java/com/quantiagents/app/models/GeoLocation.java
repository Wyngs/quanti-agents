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

    public GeoLocation(){}
    public GeoLocation(double latitude, double longitude, String userId, String eventId) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.timeStamp = new Date();
        this.userId = userId;
        this.eventId = eventId;
    }
    public GeoLocation(double latitude, double longitude, Date timeStamp, String userId, String eventId) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.timeStamp = timeStamp;
        this.userId = userId;
        this.eventId = eventId;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public Date getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Date timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
}
