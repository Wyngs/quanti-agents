package com.quantiagents.app.models;

import com.quantiagents.app.Constants.constant;

import java.util.Date;

/**
 * Representation of an event's registration history, with getters and setters for each variable
 */
public class RegistrationHistory {

    private String eventId;
    private String userId;
    private constant.EventRegistrationStatus eventRegStatus;
    private Date registeredAt;

    public RegistrationHistory(){}

    public RegistrationHistory(String eventId, String userId, constant.EventRegistrationStatus eventRegStatus, Date registeredAt ){
        this.eventId = eventId;
        this.userId = userId;
        this.eventRegStatus = eventRegStatus;
        this.registeredAt = registeredAt;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public constant.EventRegistrationStatus getEventRegistrationStatus() {
        return eventRegStatus;
    }

    public void setEventRegistrationStatus(constant.EventRegistrationStatus eventRegStatus) {
        this.eventRegStatus = eventRegStatus;
    }

    public Date getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(Date registeredAt) {
        this.registeredAt = registeredAt;
    }
}
