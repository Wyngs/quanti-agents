package com.quantiagents.app.domain;

import java.io.Serializable;

public class Event implements Serializable {

    private final String eventId;
    private String title;
    private String posterImageId;

    public Event(String eventId, String title, String posterImageId) {
        //hold minimal admin-facing fields only
        this.eventId = eventId;
        this.title = title;
        this.posterImageId = posterImageId;
    }

    public String getEventId() { return eventId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getPosterImageId() { return posterImageId; }
    public void setPosterImageId(String posterImageId) { this.posterImageId = posterImageId; }
}
