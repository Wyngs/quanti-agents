package com.quantiagents.app.models;

import java.io.Serializable;
import java.util.Date;

/**
 * Representation of a message in a chat, with getters and setters for each variable.
 * <p>
 * Contains:
 * </p>
 * String: message id, String: chat id, String: sender id, String: sender name, String: text, Date: timestamp
 */
public class Message implements Serializable {
    private String messageId;
    private String chatId;
    private String senderId;
    private String senderName;
    private String text;
    private Date timestamp;

    public Message() {
        this.timestamp = new Date();
    }

    public Message(String messageId, String chatId, String senderId, String senderName, String text) {
        this.messageId = messageId;
        this.chatId = chatId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.text = text;
        this.timestamp = new Date();
    }

    public Message(String messageId, String chatId, String senderId, String senderName, String text, Date timestamp) {
        this.messageId = messageId;
        this.chatId = chatId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}

