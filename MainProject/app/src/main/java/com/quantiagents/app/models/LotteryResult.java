package com.quantiagents.app.models;

import java.util.Date;
import java.util.List;

/**
 * Representation of a lottery result, with getters and setters for each variable
 * <p>
 * Contains:
 * </p>
 * String: event id, List(String): entrant ids, Date: timestamp
 */
public class LotteryResult {
    private String eventId;
    private List<String> entrantIds;
    private Date timeStamp;
    /**
     * Default constructor that creates an empty lottery result.
     */
    public LotteryResult(){}
    
    /**
     * Constructor that creates a lottery result with event ID and entrant list.
     * Timestamp is automatically set to the current time.
     *
     * @param eventId The unique identifier of the event
     * @param entrantIds List of user IDs who were selected in the lottery
     */
    public LotteryResult(String eventId, List<String> entrantIds) {
        this.eventId = eventId;
        this.entrantIds = entrantIds;
        this.timeStamp = new Date();
    }

    /**
     * Constructor that creates a lottery result with all fields including custom timestamp.
     *
     * @param eventId The unique identifier of the event
     * @param entrantIds List of user IDs who were selected in the lottery
     * @param timeStamp The timestamp when the lottery was drawn
     */
    public LotteryResult(String eventId, List<String> entrantIds, Date timeStamp) {
        this.eventId = eventId;
        this.entrantIds = entrantIds;
        this.timeStamp = timeStamp;
    }

    /**
     * Gets the unique identifier of the event for this lottery result.
     *
     * @return The event ID
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * Sets the unique identifier of the event for this lottery result.
     *
     * @param eventId The event ID to set
     */
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    /**
     * Gets the list of user IDs who were selected in the lottery.
     *
     * @return List of entrant user IDs
     */
    public List<String> getEntrantIds() {
        return entrantIds;
    }

    /**
     * Sets the list of user IDs who were selected in the lottery.
     *
     * @param entrantIds List of entrant user IDs to set
     */
    public void setEntrantIds(List<String> entrantIds) {
        this.entrantIds = entrantIds;
    }

    /**
     * Gets the timestamp when the lottery was drawn.
     *
     * @return The lottery timestamp
     */
    public Date getTimeStamp() {
        return timeStamp;
    }

    /**
     * Sets the timestamp when the lottery was drawn.
     *
     * @param timeStamp The lottery timestamp to set
     */
    public void setTimeStamp(Date timeStamp) {
        this.timeStamp = timeStamp;
    }
}
