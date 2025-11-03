package com.quantiagents.app.models;

import java.time.LocalDateTime;

public class GeoLocation {
    private double latitude;
    private double longitude;
    private LocalDateTime timeStamp;
    private int userId;
    private int eventId;

    public GeoLocation(double latitude, double longitude, int userId, int eventId) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.timeStamp = LocalDateTime.now();
        this.userId = userId;
        this.eventId = eventId;
    }
    public GeoLocation(double latitude, double longitude, LocalDateTime timeStamp, int userId, int eventId) {
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

    public LocalDateTime getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(LocalDateTime timeStamp) {
        this.timeStamp = timeStamp;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getEventId() {
        return eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }
}
