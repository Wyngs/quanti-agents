package com.quantiagents.app.models;
import com.quantiagents.app.Constants.constant;
import java.io.Serializable;
import java.util.Date;

/**
 * Representation of a user, with getters and setters for each variable
 * <p>
 * Contains:
 * </p>
 * String: user id, String: device id, String: name, String: username, String: email, String: phone, String: password hash, boolean: notifications on, Date: creation, (UserRole): role, RegistrationHistory: history, Date: lastViewedBrowse
 * @see RegistrationHistory
 */
public class User implements Serializable {

    private String userId;
    private String deviceId;
    private String name;
    private String username;
    private String email;
    private String phone;
    private String passwordHash;
    private boolean notificationsOn;
    private Date createdOn;
    private constant.UserRole role;
    private RegistrationHistory registrationHistory;
    private Date lastViewedBrowse;


    /**
     * Default constructor that initializes all fields to default values.
     * Notifications are enabled by default, and creation date is set to current time.
     */
    public User(){
        this.userId = "";
        this.deviceId = "";
        this.name = "";
        this.username = "";
        this.email = "";
        this.phone = "";
        this.passwordHash = "";
        this.notificationsOn = true;
        this.createdOn = new Date();
        this.lastViewedBrowse = null;
    }

    /**
     * Constructor that initializes a User with only a user ID.
     * All other fields are set to default values.
     *
     * @param userId The unique identifier for the user
     */
    public User(String userId){
        super();
        this.userId = userId;
    }

    /**
     * Constructor that creates a User with all basic profile information.
     * Capture the snapshot of the profile at creation.
     *
     * @param userId The unique identifier for the user
     * @param deviceId The unique identifier for the user's device
     * @param name The full name of the user
     * @param username The username chosen by the user
     * @param email The email address of the user
     * @param phone The phone number of the user
     * @param passwordHash The hashed password for the user account
     */
    public User(String userId, String deviceId, String name, String username, String email, String phone, String passwordHash) {
        // Capture the snapshot of my profile at creation.
        this.userId = userId;
        this.deviceId = deviceId;
        this.name = name;
        this.username = username;
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.notificationsOn = true;
        this.createdOn = new Date();
        this.lastViewedBrowse = null;
    }

    /**
     * Gets the unique identifier for this user.
     *
     * @return The user ID string
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Gets the device identifier associated with this user.
     *
     * @return The device ID string
     */
    public String getDeviceId() {
        return deviceId;
    }

    /**
     * Sets the unique identifier for this user.
     *
     * @param userId The user ID string to set
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * Sets the device identifier for this user.
     *
     * @param deviceId The device ID string to set
     */
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    /**
     * Gets the full name of the user.
     *
     * @return The user's name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the full name of the user.
     *
     * @param name The name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the username of the user.
     *
     * @return The username
     */
    public String getUsername() { return username; }

    /**
     * Sets the username of the user.
     *
     * @param username The username to set
     */
    public void setUsername(String username) { this.username = username; }

    /**
     * Gets the email address of the user.
     *
     * @return The email address
     */
    public String getEmail() { return email; }

    /**
     * Sets the email address of the user.
     *
     * @param email The email address to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Gets the phone number of the user.
     *
     * @return The phone number
     */
    public String getPhone() {
        return phone;
    }

    /**
     * Sets the phone number of the user.
     *
     * @param phone The phone number to set
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * Gets the hashed password for the user account.
     *
     * @return The password hash
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * Sets the hashed password for the user account.
     *
     * @param passwordHash The password hash to set
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * Checks if notifications are enabled for this user.
     *
     * @return True if notifications are enabled, false otherwise
     */
    public boolean hasNotificationsOn() {
        return notificationsOn;
    }

    /**
     * Gets whether notifications are enabled for this user.
     * This method is required for Firestore serialization.
     *
     * @return True if notifications are enabled, false otherwise
     */
    public boolean getNotificationsOn() {
        return notificationsOn;
    }

    /**
     * Sets whether notifications are enabled for this user.
     *
     * @param notificationsOn True to enable notifications, false to disable
     */
    public void setNotificationsOn(boolean notificationsOn) {
        this.notificationsOn = notificationsOn;
    }

    /**
     * Gets the date when this user account was created.
     *
     * @return The creation date
     */
    public Date getCreatedOn() {
        return createdOn;
    }

    /**
     * Sets the date when this user account was created.
     *
     * @param createdOn The creation date to set
     */
    public void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
    }

    /**
     * Gets the registration history for this user.
     *
     * @return The registration history object
     */
    public RegistrationHistory getRegistrationHistory() {
        return registrationHistory;
    }

    /**
     * Sets the registration history for this user.
     *
     * @param registrationHistory The registration history object to set
     */
    public void setRegistrationHistory(RegistrationHistory registrationHistory) {
        this.registrationHistory = registrationHistory;
    }

    /**
     * Gets the role of this user (e.g., USER, ADMIN, ORGANIZER).
     *
     * @return The user role
     */
    public constant.UserRole getRole() {
        return role;
    }

    /**
     * Sets the role of this user.
     *
     * @param role The user role to set
     */
    public void setRole(constant.UserRole role) {
        this.role = role;
    }

    /**
     * Gets the date when the user last viewed the browse events page.
     *
     * @return The last viewed browse date, or null if never viewed
     */
    public Date getLastViewedBrowse() { return this.lastViewedBrowse; }

    /**
     * Sets the date when the user last viewed the browse events page.
     *
     * @param viewedBrowse The date when browse was last viewed
     */
    public void setLastViewedBrowse(Date viewedBrowse) { this.lastViewedBrowse = viewedBrowse; }
}
