package com.quantiagents.app.models;

import java.util.Date;
import java.util.List;

public class LotteryResult {
    private String eventId;
    private List<String> entrantIds;
    private Date timeStamp;
    public LotteryResult(){}
    public LotteryResult(String eventId, List<String> entrantIds) {
        this.eventId = eventId;
        this.entrantIds = entrantIds;
        this.timeStamp = new Date();
    }

    public LotteryResult(String eventId, List<String> entrantIds, Date timeStamp) {
        this.eventId = eventId;
        this.entrantIds = entrantIds;
        this.timeStamp = timeStamp;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public List<String> getEntrantIds() {
        return entrantIds;
    }

    public void setEntrantIds(List<String> entrantIds) {
        this.entrantIds = entrantIds;
    }

    public Date getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Date timeStamp) {
        this.timeStamp = timeStamp;
    }
}
