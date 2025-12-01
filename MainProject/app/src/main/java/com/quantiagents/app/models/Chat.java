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

    public Chat() {
        this.createdAt = new Date();
        this.lastReadTimestamps = new HashMap<>();
    }

    public Chat(String chatId, String eventId, String eventName, List<String> memberIds) {
        this.chatId = chatId;
        this.eventId = eventId;
        this.eventName = eventName;
        this.memberIds = memberIds;
        this.createdAt = new Date();
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public List<String> getMemberIds() {
        return memberIds;
    }

    public void setMemberIds(List<String> memberIds) {
        this.memberIds = memberIds;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(Date lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public Map<String, Date> getLastReadTimestamps() {
        if (lastReadTimestamps == null) {
            lastReadTimestamps = new HashMap<>();
        }
        return lastReadTimestamps;
    }

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

