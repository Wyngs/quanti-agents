package com.quantiagents.app.models;

import com.quantiagents.app.Constants.constant;

import java.util.Date;

/**
 * Representation of an event's registration history, with getters and setters for each variable
 * <p>
 * Contains:
 * </p>
 * String: event id, String: user id, (EventRegistrationStatus): status, Date: registered at
 */
public class RegistrationHistory {

    private String eventId;
    private String userId;
    private constant.EventRegistrationStatus eventRegStatus;
    private Date registeredAt;

    /**
     * Default constructor that creates an empty registration history entry.
     */
    public RegistrationHistory(){}

    /**
     * Constructor that creates a registration history entry with all required fields.
     *
     * @param eventId The unique identifier of the event
     * @param userId The unique identifier of the user
     * @param eventRegStatus The registration status for this event
     * @param registeredAt The date when the user registered for the event
     */
    public RegistrationHistory(String eventId, String userId, constant.EventRegistrationStatus eventRegStatus, Date registeredAt ){
        this.eventId = eventId;
        this.userId = userId;
        this.eventRegStatus = eventRegStatus;
        this.registeredAt = registeredAt;
    }

    /**
     * Gets the unique identifier of the event in this registration history.
     *
     * @return The event ID
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * Sets the unique identifier of the event in this registration history.
     *
     * @param eventId The event ID to set
     */
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    /**
     * Gets the unique identifier of the user in this registration history.
     *
     * @return The user ID
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Sets the unique identifier of the user in this registration history.
     *
     * @param userId The user ID to set
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * Gets the registration status for this event registration.
     *
     * @return The event registration status
     */
    public constant.EventRegistrationStatus getEventRegistrationStatus() {
        return eventRegStatus;
    }

    /**
     * Sets the registration status for this event registration.
     *
     * @param eventRegStatus The event registration status to set
     */
    public void setEventRegistrationStatus(constant.EventRegistrationStatus eventRegStatus) {
        this.eventRegStatus = eventRegStatus;
    }

    /**
     * Gets the date when the user registered for the event.
     *
     * @return The registration date
     */
    public Date getRegisteredAt() {
        return registeredAt;
    }

    /**
     * Sets the date when the user registered for the event.
     *
     * @param registeredAt The registration date to set
     */
    public void setRegisteredAt(Date registeredAt) {
        this.registeredAt = registeredAt;
    }
}
