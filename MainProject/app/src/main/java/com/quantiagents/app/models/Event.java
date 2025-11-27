package com.quantiagents.app.models;

import com.quantiagents.app.Constants.constant;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Representation of an event, with getters and setters for each variable.
 * <p>
 * This class serves as the primary data model for events within the system, storing
 * details such as scheduling, registration constraints, organizer information, and
 * the status of the lottery process.
 * </p>
 */
public class Event implements Serializable {

    private String eventId;
    private String title;
    private String posterImageId;
    private String description;
    private String category;
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

    /**
     * Constructor for creating a minimal event instance, primarily used for administrative lists.
     *
     * @param eventId       The unique identifier of the event.
     * @param title         The title of the event.
     * @param posterImageId The unique identifier of the event's poster image.
     */
    public Event(String eventId, String title, String posterImageId) {
        this.eventId = eventId;
        this.title = title;
        this.posterImageId = posterImageId;
    }

    /**
     * Gets the unique identifier for this event.
     * @return The event ID string.
     */
    public String getEventId() { return eventId; }

    /**
     * Sets the unique identifier for this event.
     * @param eventId The event ID string.
     */
    public void setEventId(String eventId) { this.eventId = eventId; }

    /**
     * Gets the title of the event.
     * @return The event title.
     */
    public String getTitle() { return title; }

    /**
     * Sets the title of the event.
     * @param title The event title.
     */
    public void setTitle(String title) { this.title = title; }

    /**
     * Gets the ID of the poster image associated with this event.
     * @return The image ID.
     */
    public String getPosterImageId() { return posterImageId; }

    /**
     * Sets the ID of the poster image associated with this event.
     * @param posterImageId The image ID.
     */
    public void setPosterImageId(String posterImageId) { this.posterImageId = posterImageId; }

    /**
     * Gets the description of the event.
     * @return The description string.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of the event.
     * @param description The description string.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the category of the event.
     * @return The category string.
     */
    public String getCategory() {
        return category;
    }

    /**
     * Sets the category of the event.
     * @param category The category string.
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * Gets the start date and time of the actual event.
     * @return The start date.
     */
    public Date getEventStartDate() {
        return eventStartDate;
    }

    /**
     * Sets the start date and time of the actual event.
     * @param eventStartDate The start date.
     */
    public void setEventStartDate(Date eventStartDate) {
        this.eventStartDate = eventStartDate;
    }

    /**
     * Gets the end date and time of the actual event.
     * @return The end date.
     */
    public Date getEventEndDate() {
        return eventEndDate;
    }

    /**
     * Sets the end date and time of the actual event.
     * @param eventEndDate The end date.
     */
    public void setEventEndDate(Date eventEndDate) {
        this.eventEndDate = eventEndDate;
    }

    /**
     * Gets the date when registration opens for entrants.
     * @return The registration start date.
     */
    public Date getRegistrationStartDate() {
        return registrationStartDate;
    }

    /**
     * Sets the date when registration opens for entrants.
     * @param registrationStartDate The registration start date.
     */
    public void setRegistrationStartDate(Date registrationStartDate) {
        this.registrationStartDate = registrationStartDate;
    }

    /**
     * Gets the date when registration closes.
     * @return The registration end date.
     */
    public Date getRegistrationEndDate() {
        return registrationEndDate;
    }

    /**
     * Sets the date when registration closes.
     * @param registrationEndDate The registration end date.
     */
    public void setRegistrationEndDate(Date registrationEndDate) {
        this.registrationEndDate = registrationEndDate;
    }

    /**
     * Gets the physical location string of the event.
     * @return The location.
     */
    public String getLocation() {
        return location;
    }

    /**
     * Sets the physical location string of the event.
     * @param location The location.
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Gets the cost of the event.
     * @return The cost.
     */
    public Double getCost() {
        return cost;
    }

    /**
     * Sets the cost of the event.
     * @param cost The cost.
     */
    public void setCost(Double cost) {
        this.cost = cost;
    }

    /**
     * Gets the current status of the event (e.g., OPEN, CLOSED, CANCELLED).
     * @return The event status.
     */
    public constant.EventStatus getStatus() {
        return status;
    }

    /**
     * Sets the current status of the event.
     * @param status The event status.
     */
    public void setStatus(constant.EventStatus status) {
        this.status = status;
    }

    /**
     * Gets the User ID of the organizer who created this event.
     * @return The organizer's User ID.
     */
    public String getOrganizerId() {
        return organizerId;
    }

    /**
     * Sets the User ID of the organizer who created this event.
     * @param organizerId The organizer's User ID.
     */
    public void setOrganizerId(String organizerId) {
        this.organizerId = organizerId;
    }

    /**
     * Gets the maximum number of entrants allowed on the waiting list.
     * A value of 0 indicates no limit.
     * @return The waiting list limit.
     */
    public double getWaitingListLimit() {
        return waitingListLimit;
    }

    /**
     * Sets the maximum number of entrants allowed on the waiting list.
     * @param waitingListLimit The waiting list limit.
     */
    public void setWaitingListLimit(double waitingListLimit) {
        this.waitingListLimit = waitingListLimit;
    }

    /**
     * Gets the capacity of participants for the event.
     * @return The capacity.
     */
    public double getEventCapacity() {
        return eventCapacity;
    }

    /**
     * Sets the capacity of participants for the event.
     * @param eventCapacity The capacity.
     */
    public void setEventCapacity(double eventCapacity) {
        this.eventCapacity = eventCapacity;
    }

    /**
     * Checks if geolocation is required for this event.
     * @return True if geolocation is required, false otherwise.
     */
    public boolean isGeoLocationOn() {
        return isGeoLocationOn;
    }

    /**
     * Sets whether geolocation is required for this event.
     * @param geoLocationOn True to require geolocation.
     */
    public void setGeoLocationOn(boolean geoLocationOn) {
        isGeoLocationOn = geoLocationOn;
    }

    /**
     * Gets the list of User IDs currently in the waiting list.
     * @return List of User IDs.
     */
    public List<String> getWaitingList() {
        return waitingList;
    }

    /**
     * Sets the list of User IDs currently in the waiting list.
     * @param waitingList List of User IDs.
     */
    public void setWaitingList(List<String> waitingList) {
        this.waitingList = waitingList;
    }

    /**
     * Gets the list of User IDs selected via lottery.
     * @return List of User IDs.
     */
    public List<String> getSelectedList() {
        return selectedList;
    }

    /**
     * Sets the list of User IDs selected via lottery.
     * @param selectedList List of User IDs.
     */
    public void setSelectedList(List<String> selectedList) {
        this.selectedList = selectedList;
    }

    /**
     * Gets the list of User IDs who have confirmed their attendance.
     * @return List of User IDs.
     */
    public List<String> getConfirmedList() {
        return confirmedList;
    }

    /**
     * Sets the list of User IDs who have confirmed their attendance.
     * @param confirmedList List of User IDs.
     */
    public void setConfirmedList(List<String> confirmedList) {
        this.confirmedList = confirmedList;
    }

    /**
     * Gets the list of User IDs who have cancelled or declined.
     * @return List of User IDs.
     */
    public List<String> getCancelledList() {
        return cancelledList;
    }

    /**
     * Sets the list of User IDs who have cancelled or declined.
     * @param cancelledList List of User IDs.
     */
    public void setCancelledList(List<String> cancelledList) {
        this.cancelledList = cancelledList;
    }

    /**
     * Checks if the initial lottery draw has been performed.
     * @return True if done, false otherwise.
     */
    public boolean isFirstLotteryDone() {
        return isFirstLotteryDone;
    }

    /**
     * Sets the flag indicating if the initial lottery draw has been performed.
     * @param firstLotteryDone True if done.
     */
    public void setFirstLotteryDone(boolean firstLotteryDone) {
        isFirstLotteryDone = firstLotteryDone;
    }
}