package com.quantiagents.app.models;

/**
 * Representation of an qr code, with getters and setters for each variable
 * <p>
 * Contains:
 * </p>
 * int: id, String: qr code value, String: event id
 */
public class QRCode {
    private int id;
    private String qrCodeValue;
    private String eventId;

    /**
     * Default constructor that creates an empty QR code.
     */
    public QRCode(){}

    /**
     * Constructor that creates a QR code with all required fields.
     *
     * @param id The unique identifier for the QR code
     * @param qrCodeValue The encoded value of the QR code
     * @param eventId The unique identifier of the event this QR code is associated with
     */
    public QRCode(int id, String qrCodeValue, String eventId) {
        this.id = id;
        this.qrCodeValue = qrCodeValue;
        this.eventId = eventId;
    }

    /**
     * Gets the unique identifier for this QR code.
     *
     * @return The QR code ID
     */
    public int getId() {
        return id;
    }

    /**
     * Sets the unique identifier for this QR code.
     *
     * @param id The QR code ID to set
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Gets the encoded value of this QR code.
     *
     * @return The QR code value string
     */
    public String getQrCodeValue() {
        return qrCodeValue;
    }

    /**
     * Sets the encoded value of this QR code.
     *
     * @param qrCodeValue The QR code value string to set
     */
    public void setQrCodeValue(String qrCodeValue) {
        this.qrCodeValue = qrCodeValue;
    }

    /**
     * Gets the unique identifier of the event this QR code is associated with.
     *
     * @return The event ID
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * Sets the unique identifier of the event this QR code is associated with.
     *
     * @param eventId The event ID to set
     */
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
}
