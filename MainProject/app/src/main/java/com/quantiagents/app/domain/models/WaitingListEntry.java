package com.quantiagents.app.domain.models;

import java.time.Instant;

/**
 * Represents a user's entry in an event waiting list.
 */
public final class WaitingListEntry {

    private String eventId;
    private String userId;
    private Instant joinedAt;

    /**
     * Empty constructor required for serialization or frameworks.
     */
    public WaitingListEntry() { }

    /**
     * Create a WaitingListEntry.
     *
     * @param eventId event identifier
     * @param userId user identifier
     * @param joinedAt timestamp of when the user joined
     */
    public WaitingListEntry(final String eventId, final String userId, final Instant joinedAt) {
        this.eventId = eventId;
        this.userId = userId;
        this.joinedAt = joinedAt;
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
     * @return timestamp when the user joined the waiting list
     */
    public Instant getJoinedAt() {
        return joinedAt;
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
     * @param joinedAt new timestamp for when the user joined
     */
    public void setJoinedAt(final Instant joinedAt) {
        this.joinedAt = joinedAt;
    }
}
