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

    public User(String userId){
        super();
        this.userId = userId;
    }

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

    public String getUserId() {
        return userId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() { return username; }

    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean hasNotificationsOn() {
        return notificationsOn;
    }

    public void setNotificationsOn(boolean notificationsOn) {
        this.notificationsOn = notificationsOn;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
    }

    public RegistrationHistory getRegistrationHistory() {
        return registrationHistory;
    }

    public void setRegistrationHistory(RegistrationHistory registrationHistory) {
        this.registrationHistory = registrationHistory;
    }

    public constant.UserRole getRole() {
        return role;
    }

    public void setRole(constant.UserRole role) {
        this.role = role;
    }

    public Date getLastViewedBrowse() { return this.lastViewedBrowse; }

    public void setLastViewedBrowse(Date viewedBrowse) { this.lastViewedBrowse = viewedBrowse; }
}
