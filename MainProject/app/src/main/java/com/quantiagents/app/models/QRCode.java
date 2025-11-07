package com.quantiagents.app.models;

/**
 * Representation of an qr code, with getters and setters for each variable
 */
public class QRCode {
    private int id;
    private String qrCodeValue;
    private String eventId;

    public QRCode(){}

    public QRCode(int id, String qrCodeValue, String eventId) {
        this.id = id;
        this.qrCodeValue = qrCodeValue;
        this.eventId = eventId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getQrCodeValue() {
        return qrCodeValue;
    }

    public void setQrCodeValue(String qrCodeValue) {
        this.qrCodeValue = qrCodeValue;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
}
