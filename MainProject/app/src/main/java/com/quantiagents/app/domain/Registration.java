package com.quantiagents.app.domain;

/**
 * Represents a user's registration status for a specific event.
 */
public final class Registration {

    /**
     * Possible registration states.
     */
    public enum Status {
        PENDING,
        SELECTED,
        CONFIRMED,
        DECLINED,
        CANCELLED
    }

    private String eventId;
    private String userId;
    private Status status;

    /**
     * Empty constructor required for serialization or frameworks.
     */
    public Registration() { }

    /**
     * Create a Registration.
     *
     * @param eventId event identifier
     * @param userId user identifier
     * @param status registration status
     */
    public Registration(final String eventId, final String userId, final Status status) {
        this.eventId = eventId;
        this.userId = userId;
        this.status = status;
    }

    /**
     * @return event identifier
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * @return user identifier
     */
    public String getUserId() {
        return userId;
    }

    /**
     * @return current registration status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * @param eventId new event identifier
     */
    public void setEventId(final String eventId) {
        this.eventId = eventId;
    }

    /**
     * @param userId new user identifier
     */
    public void setUserId(final String userId) {
        this.userId = userId;
    }

    /**
     * @param status new registration status
     */
    public void setStatus(final Status status) {
        this.status = status;
    }
}
