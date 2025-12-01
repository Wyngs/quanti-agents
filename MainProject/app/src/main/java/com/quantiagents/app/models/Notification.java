package com.quantiagents.app.models;

import com.quantiagents.app.Constants.constant;

import java.util.Date;

/**
 * Representation of a notification, with getters and setters for each variable
 * <p>
 * Contains:
 * </p>
 * int: notification id, (NotificationType): type, int: recipient id, int: sender id, int: affiliated event id, Date: timestamp, boolean: has read
 */
public class Notification {
    private int notificationId;
    private constant.NotificationType type;
    private int recipientId;
    private int senderId;
    private int affiliatedEventId;
    private String status;
    private String details;
    private Date timestamp;
    private boolean hasRead;

    /**
     * Default constructor that initializes a notification with empty status and details.
     */
    public Notification(){
        this.status = "";
        this.details = "";
    }
    
    /**
     * Constructor that creates a notification with all required fields.
     * Timestamp is automatically set to current time and hasRead is set to false.
     *
     * @param notificationId The unique identifier for the notification
     * @param type The type of notification
     * @param recipientId The user ID of the notification recipient
     * @param senderId The user ID of the notification sender
     * @param affiliatedEventId The event ID this notification is related to
     * @param status The status string associated with the notification
     * @param details Additional details about the notification
     */
    public Notification(int notificationId, constant.NotificationType type, int recipientId, int senderId, int affiliatedEventId, String status , String details) {
        this.notificationId = notificationId;
        this.type = type;
        this.recipientId = recipientId;
        this.senderId = senderId;
        this.affiliatedEventId = affiliatedEventId;
        this.timestamp = new Date();
        this.hasRead = false;
        this.status = status != null ? status : "";
        this.details = details != null ? details : "";
    }
    
    /**
     * Constructor that creates a notification with all fields including custom timestamp and read status.
     *
     * @param notificationId The unique identifier for the notification
     * @param type The type of notification
     * @param recipientId The user ID of the notification recipient
     * @param senderId The user ID of the notification sender
     * @param affiliatedEventId The event ID this notification is related to
     * @param status The status string associated with the notification
     * @param details Additional details about the notification
     * @param timestamp The timestamp when the notification was created
     * @param hasRead Whether the notification has been read by the recipient
     */
    public Notification(int notificationId, constant.NotificationType type, int recipientId, int senderId, int affiliatedEventId, String status , String details, Date timestamp, boolean hasRead) {
        this.notificationId = notificationId;
        this.type = type;
        this.recipientId = recipientId;
        this.senderId = senderId;
        this.affiliatedEventId = affiliatedEventId;
        this.timestamp = timestamp;
        this.hasRead = hasRead;
        this.status = status != null ? status : "";
        this.details = details != null ? details : "";
    }

    /**
     * Gets the unique identifier for this notification.
     *
     * @return The notification ID
     */
    public int getNotificationId() {
        return notificationId;
    }

    /**
     * Sets the unique identifier for this notification.
     *
     * @param notificationId The notification ID to set
     */
    public void setNotificationId(int notificationId) {
        this.notificationId = notificationId;
    }

    /**
     * Gets the type of this notification.
     *
     * @return The notification type
     */
    public constant.NotificationType getType() {
        return type;
    }

    /**
     * Sets the type of this notification.
     *
     * @param type The notification type to set
     */
    public void setType(constant.NotificationType type) {
        this.type = type;
    }

    /**
     * Gets the user ID of the notification recipient.
     *
     * @return The recipient's user ID
     */
    public int getRecipientId() {
        return recipientId;
    }

    /**
     * Sets the user ID of the notification recipient.
     *
     * @param recipientId The recipient's user ID to set
     */
    public void setRecipientId(int recipientId) {
        this.recipientId = recipientId;
    }

    /**
     * Gets the user ID of the notification sender.
     *
     * @return The sender's user ID
     */
    public int getSenderId() {
        return senderId;
    }

    /**
     * Sets the user ID of the notification sender.
     *
     * @param senderId The sender's user ID to set
     */
    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    /**
     * Gets the event ID this notification is related to.
     *
     * @return The affiliated event ID
     */
    public int getAffiliatedEventId() {
        return affiliatedEventId;
    }

    /**
     * Sets the event ID this notification is related to.
     *
     * @param affiliatedEventId The affiliated event ID to set
     */
    public void setAffiliatedEventId(int affiliatedEventId) {
        this.affiliatedEventId = affiliatedEventId;
    }

    /**
     * Gets the timestamp when this notification was created.
     *
     * @return The notification timestamp
     */
    public Date getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the timestamp when this notification was created.
     *
     * @param timestamp The notification timestamp to set
     */
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Checks if this notification has been read by the recipient.
     *
     * @return True if the notification has been read, false otherwise
     */
    public boolean isHasRead() {
        return hasRead;
    }

    /**
     * Sets whether this notification has been read by the recipient.
     *
     * @param hasRead True if the notification has been read, false otherwise
     */
    public void setHasRead(boolean hasRead) {
        this.hasRead = hasRead;
    }

    /**
     * Gets the status string associated with this notification.
     *
     * @return The status string
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the status string associated with this notification.
     * If null is provided, sets to empty string.
     *
     * @param status The status string to set
     */
    public void setStatus(String status) {
        this.status = status != null ? status : "";
    }

    /**
     * Gets the additional details about this notification.
     *
     * @return The details string
     */
    public String getDetails() {
        return details;
    }

    /**
     * Sets the additional details about this notification.
     * If null is provided, sets to empty string.
     *
     * @param details The details string to set
     */
    public void setDetails(String details) {
        this.details = details != null ? details : "";
    }
}
