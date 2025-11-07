package com.quantiagents.app.models;

import com.quantiagents.app.Constants.constant;

import java.util.Date;

public class Notification {
    private String notificationId;
    private constant.NotificationType type;
    private String recipientId;
    private String senderId;
    private String affiliatedEventId;
    private Date timestamp;
    private boolean hasRead;

    public Notification(){}
    public Notification(String notificationId, constant.NotificationType type, String recipientId, String senderId, String affiliatedEventId) {
        this.notificationId = notificationId;
        this.type = type;
        this.recipientId = recipientId;
        this.senderId = senderId;
        this.affiliatedEventId = affiliatedEventId;
        this.timestamp = new Date();
        this.hasRead = false;
    }
    public Notification(String notificationId, constant.NotificationType type, String recipientId, String senderId, String affiliatedEventId, Date timestamp, boolean hasRead) {
        this.notificationId = notificationId;
        this.type = type;
        this.recipientId = recipientId;
        this.senderId = senderId;
        this.affiliatedEventId = affiliatedEventId;
        this.timestamp = timestamp;
        this.hasRead = hasRead;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public constant.NotificationType getType() {
        return type;
    }

    public void setType(constant.NotificationType type) {
        this.type = type;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getAffiliatedEventId() {
        return affiliatedEventId;
    }

    public void setAffiliatedEventId(String affiliatedEventId) {
        this.affiliatedEventId = affiliatedEventId;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isHasRead() {
        return hasRead;
    }

    public void setHasRead(boolean hasRead) {
        this.hasRead = hasRead;
    }
}