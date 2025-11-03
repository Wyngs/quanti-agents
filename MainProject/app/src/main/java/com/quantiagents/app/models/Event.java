package com.quantiagents.app.models;

import com.quantiagents.app.Constants.constant;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class Event implements Serializable {

    private final String eventId;
    private String title;
    private String posterImageId;
    private String description;
    private Date eventStartDate;
    private Date eventEndDate;
    private Date registrationStartDate;
    private Date registrationEndDate;
    private String location;
    private Double cost;
    private constant.EventStatus status;
    private int organizerId;
    private double waitingListLimit;
    private double eventCapacity;
    private boolean isGeoLocationOn;
    private List<Integer> waitingList;
    private List<Integer> selectedList;
    private List<Integer> confirmedList;
    private List<Integer> cancelledList;
    private boolean isFirstLotteryDone;


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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getEventStartDate() {
        return eventStartDate;
    }

    public void setEventStartDate(Date eventStartDate) {
        this.eventStartDate = eventStartDate;
    }

    public Date getEventEndDate() {
        return eventEndDate;
    }

    public void setEventEndDate(Date eventEndDate) {
        this.eventEndDate = eventEndDate;
    }

    public Date getRegistrationStartDate() {
        return registrationStartDate;
    }

    public void setRegistrationStartDate(Date registrationStartDate) {
        this.registrationStartDate = registrationStartDate;
    }

    public Date getRegistrationEndDate() {
        return registrationEndDate;
    }

    public void setRegistrationEndDate(Date registrationEndDate) {
        this.registrationEndDate = registrationEndDate;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Double getCost() {
        return cost;
    }

    public void setCost(Double cost) {
        this.cost = cost;
    }

    public constant.EventStatus getStatus() {
        return status;
    }

    public void setStatus(constant.EventStatus status) {
        this.status = status;
    }

    public int getOrganizerId() {
        return organizerId;
    }

    public void setOrganizerId(int organizerId) {
        this.organizerId = organizerId;
    }

    public double getWaitingListLimit() {
        return waitingListLimit;
    }

    public void setWaitingListLimit(double waitingListLimit) {
        this.waitingListLimit = waitingListLimit;
    }

    public double getEventCapacity() {
        return eventCapacity;
    }

    public void setEventCapacity(double eventCapacity) {
        this.eventCapacity = eventCapacity;
    }

    public boolean isGeoLocationOn() {
        return isGeoLocationOn;
    }

    public void setGeoLocationOn(boolean geoLocationOn) {
        isGeoLocationOn = geoLocationOn;
    }

    public List<Integer> getWaitingList() {
        return waitingList;
    }

    public void setWaitingList(List<Integer> waitingList) {
        this.waitingList = waitingList;
    }

    public List<Integer> getSelectedList() {
        return selectedList;
    }

    public void setSelectedList(List<Integer> selectedList) {
        this.selectedList = selectedList;
    }

    public List<Integer> getConfirmedList() {
        return confirmedList;
    }

    public void setConfirmedList(List<Integer> confirmedList) {
        this.confirmedList = confirmedList;
    }

    public List<Integer> getCancelledList() {
        return cancelledList;
    }

    public void setCancelledList(List<Integer> cancelledList) {
        this.cancelledList = cancelledList;
    }

    public boolean isFirstLotteryDone() {
        return isFirstLotteryDone;
    }

    public void setFirstLotteryDone(boolean firstLotteryDone) {
        isFirstLotteryDone = firstLotteryDone;
    }
}
