package com.quantiagents.app.models;

import com.quantiagents.app.Constants.constant;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Representation of an event, with getters and setters for each variable
 * <p>
 * Contains:
 * </p>
 * String: event id, String: title, String: poster image id, String: description, Date: event start date, Date: event end date, Date: registration start date, Date: registration end date, String location, Double: cost, (EventStatus): status, String: organizer id, double: waiting list limit, double: event capacity, boolean: is geolocation on, List(String): waiting list, List(String): selected list, List(String): confirmed list, List(String): cancelled list, boolean: first lottery done
 */
public class Event implements Serializable {

    private String eventId;
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
    private String organizerId;
    private double waitingListLimit;
    private double eventCapacity;
    private boolean isGeoLocationOn;
    private List<String> waitingList;
    private List<String> selectedList;
    private List<String> confirmedList;
    private List<String> cancelledList;
    private boolean isFirstLotteryDone;

    public Event (){ }
    public Event(String eventId, String title, String posterImageId) {
        //hold minimal admin-facing fields only
        this.eventId = eventId;
        this.title = title;
        this.posterImageId = posterImageId;
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
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

    public String getOrganizerId() {
        return organizerId;
    }

    public void setOrganizerId(String organizerId) {
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

    public List<String> getWaitingList() {
        return waitingList;
    }

    public void setWaitingList(List<String> waitingList) {
        this.waitingList = waitingList;
    }

    public List<String> getSelectedList() {
        return selectedList;
    }

    public void setSelectedList(List<String> selectedList) {
        this.selectedList = selectedList;
    }

    public List<String> getConfirmedList() {
        return confirmedList;
    }

    public void setConfirmedList(List<String> confirmedList) {
        this.confirmedList = confirmedList;
    }

    public List<String> getCancelledList() {
        return cancelledList;
    }

    public void setCancelledList(List<String> cancelledList) {
        this.cancelledList = cancelledList;
    }

    public boolean isFirstLotteryDone() {
        return isFirstLotteryDone;
    }

    public void setFirstLotteryDone(boolean firstLotteryDone) {
        isFirstLotteryDone = firstLotteryDone;
    }
}
