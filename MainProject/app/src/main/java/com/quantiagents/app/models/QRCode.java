package com.quantiagents.app.models;

public class QRCode {
    private int id;
    private String qrCodeValue;
    private int eventId;

    public QRCode(int id, String qrCodeValue, int eventId) {
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

    public int getEventId() {
        return eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }
}
