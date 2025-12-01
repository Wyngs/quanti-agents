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

    /**
     * Default constructor that initializes a message with current timestamp.
     */
    public Message() {
        this.timestamp = new Date();
    }

    /**
     * Constructor that creates a message with all required fields.
     * Timestamp is automatically set to the current time.
     *
     * @param messageId The unique identifier for the message
     * @param chatId The unique identifier of the chat this message belongs to
     * @param senderId The unique identifier of the user who sent the message
     * @param senderName The display name of the user who sent the message
     * @param text The text content of the message
     */
    public Message(String messageId, String chatId, String senderId, String senderName, String text) {
        this.messageId = messageId;
        this.chatId = chatId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.text = text;
        this.timestamp = new Date();
    }

    /**
     * Constructor that creates a message with all fields including a custom timestamp.
     *
     * @param messageId The unique identifier for the message
     * @param chatId The unique identifier of the chat this message belongs to
     * @param senderId The unique identifier of the user who sent the message
     * @param senderName The display name of the user who sent the message
     * @param text The text content of the message
     * @param timestamp The timestamp when the message was sent
     */
    public Message(String messageId, String chatId, String senderId, String senderName, String text, Date timestamp) {
        this.messageId = messageId;
        this.chatId = chatId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.text = text;
        this.timestamp = timestamp;
    }

    /**
     * Gets the unique identifier for this message.
     *
     * @return The message ID
     */
    public String getMessageId() {
        return messageId;
    }

    /**
     * Sets the unique identifier for this message.
     *
     * @param messageId The message ID to set
     */
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    /**
     * Gets the unique identifier of the chat this message belongs to.
     *
     * @return The chat ID
     */
    public String getChatId() {
        return chatId;
    }

    /**
     * Sets the unique identifier of the chat this message belongs to.
     *
     * @param chatId The chat ID to set
     */
    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    /**
     * Gets the unique identifier of the user who sent this message.
     *
     * @return The sender's user ID
     */
    public String getSenderId() {
        return senderId;
    }

    /**
     * Sets the unique identifier of the user who sent this message.
     *
     * @param senderId The sender's user ID to set
     */
    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    /**
     * Gets the display name of the user who sent this message.
     *
     * @return The sender's name
     */
    public String getSenderName() {
        return senderName;
    }

    /**
     * Sets the display name of the user who sent this message.
     *
     * @param senderName The sender's name to set
     */
    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    /**
     * Gets the text content of this message.
     *
     * @return The message text
     */
    public String getText() {
        return text;
    }

    /**
     * Sets the text content of this message.
     *
     * @param text The message text to set
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Gets the timestamp when this message was sent.
     *
     * @return The message timestamp
     */
    public Date getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the timestamp when this message was sent.
     *
     * @param timestamp The message timestamp to set
     */
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}

