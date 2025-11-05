package com.quantiagents.app.models;

import com.quantiagents.app.Constants.constant;

import java.util.Date;

public class Notification {
    private int notificationId;
    private constant.NotificationType type;
    private int recipientId;
    private int senderId;
    private int affiliatedEventId;
    private Date timestamp;
    private boolean hasRead;

    public Notification(){}
    public Notification(int notificationId, constant.NotificationType type, int recipientId, int senderId, int affiliatedEventId  ) {
        this.notificationId = notificationId;
        this.type = type;
        this.recipientId = recipientId;
        this.senderId = senderId;
        this.affiliatedEventId = affiliatedEventId;
        this.timestamp = new Date();
        this.hasRead = false;
    }
    public Notification(int notificationId, constant.NotificationType type, int recipientId, int senderId, int affiliatedEventId, Date timestamp, boolean hasRead) {
        this.notificationId = notificationId;
        this.type = type;
        this.recipientId = recipientId;
        this.senderId = senderId;
        this.affiliatedEventId = affiliatedEventId;
        this.timestamp = timestamp;
        this.hasRead = hasRead;
    }

    public int getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(int notificationId) {
        this.notificationId = notificationId;
    }

    public constant.NotificationType getType() {
        return type;
    }

    public void setType(constant.NotificationType type) {
        this.type = type;
    }

    public int getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(int recipientId) {
        this.recipientId = recipientId;
    }

    public int getSenderId() {
        return senderId;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public int getAffiliatedEventId() {
        return affiliatedEventId;
    }

    public void setAffiliatedEventId(int affiliatedEventId) {
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
