package com.quantiagents.app.models;

import java.time.LocalDateTime;
import java.util.List;

public class LotteryResult {
    private int eventId;
    private List<Integer> entrantIds;
    private LocalDateTime timeStamp;

    public LotteryResult(int eventId, List<Integer> entrantIds) {
        this.eventId = eventId;
        this.entrantIds = entrantIds;
        this.timeStamp = LocalDateTime.now();
    }

    public LotteryResult(int eventId, List<Integer> entrantIds, LocalDateTime timeStamp) {
        this.eventId = eventId;
        this.entrantIds = entrantIds;
        this.timeStamp = timeStamp;
    }

    public int getEventId() {
        return eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public List<Integer> getEntrantIds() {
        return entrantIds;
    }

    public void setEntrantIds(List<Integer> entrantIds) {
        this.entrantIds = entrantIds;
    }

    public LocalDateTime getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(LocalDateTime timeStamp) {
        this.timeStamp = timeStamp;
    }
}
