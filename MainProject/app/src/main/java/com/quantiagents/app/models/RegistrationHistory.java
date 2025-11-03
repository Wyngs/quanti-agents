package com.quantiagents.app.models;

import com.quantiagents.app.Constants.constant;

import java.time.LocalDateTime;
import java.util.Date;

public class RegistrationHistory {

    private int eventId;
    private int userId;
    private constant.EventRegistrationStatus eventRegStatus;
    private Date registeredAt;

    public RegistrationHistory(int eventId, int userId, constant.EventRegistrationStatus eventRegStatus, Date registeredAt ){
        this.eventId = eventId;
        this.userId = userId;
        this.eventRegStatus = eventRegStatus;
        this.registeredAt = registeredAt;
    }

    public int getEventId() {
        return eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
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
