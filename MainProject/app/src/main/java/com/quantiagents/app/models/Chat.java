package com.quantiagents.app.models;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Representation of a group chat for an event, with getters and setters for each variable.
 * <p>
 * A chat is automatically created when a lottery is drawn for an event.
 * Users are added to the chat when they accept the lottery invitation (CONFIRMED status).
 * </p>
 */
public class Chat implements Serializable {
    private String chatId;
    private String eventId;
    private String eventName;
    private List<String> memberIds; // User IDs of chat members
    private Date createdAt;
    private Date lastMessageTime;
    private Map<String, Date> lastReadTimestamps; // Map of userId -> last read message timestamp

    /**
     * Default constructor that initializes a chat with current creation date
     * and an empty map for last read timestamps.
     */
    public Chat() {
        this.createdAt = new Date();
        this.lastReadTimestamps = new HashMap<>();
    }

    /**
     * Constructor that creates a chat for an event with initial members.
     *
     * @param chatId The unique identifier for the chat
     * @param eventId The unique identifier of the event this chat is associated with
     * @param eventName The name of the event this chat is associated with
     * @param memberIds List of user IDs who are members of this chat
     */
    public Chat(String chatId, String eventId, String eventName, List<String> memberIds) {
        this.chatId = chatId;
        this.eventId = eventId;
        this.eventName = eventName;
        this.memberIds = memberIds;
        this.createdAt = new Date();
    }

    /**
     * Gets the unique identifier for this chat.
     *
     * @return The chat ID
     */
    public String getChatId() {
        return chatId;
    }

    /**
     * Sets the unique identifier for this chat.
     *
     * @param chatId The chat ID to set
     */
    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    /**
     * Gets the unique identifier of the event this chat is associated with.
     *
     * @return The event ID
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * Sets the unique identifier of the event this chat is associated with.
     *
     * @param eventId The event ID to set
     */
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    /**
     * Gets the name of the event this chat is associated with.
     *
     * @return The event name
     */
    public String getEventName() {
        return eventName;
    }

    /**
     * Sets the name of the event this chat is associated with.
     *
     * @param eventName The event name to set
     */
    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    /**
     * Gets the list of user IDs who are members of this chat.
     *
     * @return List of member user IDs
     */
    public List<String> getMemberIds() {
        return memberIds;
    }

    /**
     * Sets the list of user IDs who are members of this chat.
     *
     * @param memberIds List of member user IDs to set
     */
    public void setMemberIds(List<String> memberIds) {
        this.memberIds = memberIds;
    }

    /**
     * Gets the date when this chat was created.
     *
     * @return The creation date
     */
    public Date getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the date when this chat was created.
     *
     * @param createdAt The creation date to set
     */
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Gets the timestamp of the last message sent in this chat.
     *
     * @return The last message timestamp, or null if no messages exist
     */
    public Date getLastMessageTime() {
        return lastMessageTime;
    }

    /**
     * Sets the timestamp of the last message sent in this chat.
     *
     * @param lastMessageTime The last message timestamp to set
     */
    public void setLastMessageTime(Date lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    /**
     * Gets the map of user IDs to their last read message timestamps.
     * If the map is null, initializes and returns a new empty map.
     *
     * @return Map of user ID to last read timestamp
     */
    public Map<String, Date> getLastReadTimestamps() {
        if (lastReadTimestamps == null) {
            lastReadTimestamps = new HashMap<>();
        }
        return lastReadTimestamps;
    }

    /**
     * Sets the map of user IDs to their last read message timestamps.
     *
     * @param lastReadTimestamps Map of user ID to last read timestamp to set
     */
    public void setLastReadTimestamps(Map<String, Date> lastReadTimestamps) {
        this.lastReadTimestamps = lastReadTimestamps;
    }

    /**
     * Gets the last read timestamp for a specific user.
     * @param userId The user ID
     * @return The last read timestamp, or null if never read
     */
    public Date getLastReadTimestamp(String userId) {
        if (lastReadTimestamps == null || userId == null) {
            return null;
        }
        return lastReadTimestamps.get(userId);
    }

    /**
     * Sets the last read timestamp for a specific user.
     * @param userId The user ID
     * @param timestamp The timestamp of the last read message
     */
    public void setLastReadTimestamp(String userId, Date timestamp) {
        if (lastReadTimestamps == null) {
            lastReadTimestamps = new HashMap<>();
        }
        if (userId != null && timestamp != null) {
            lastReadTimestamps.put(userId, timestamp);
        }
    }
}

