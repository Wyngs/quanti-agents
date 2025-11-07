package com.quantiagents.app.models;
import com.quantiagents.app.Constants.constant;
import java.io.Serializable;
import java.util.Date;

public class User implements Serializable {

    private String userId;
    private String deviceId;
    private String name;
    private String email;
    private String phone;
    private String passwordHash;
    private boolean notificationsOn;
    private Date createdOn;
    private constant.UserRole role;

    public User(){
        this.userId = "";
        this.deviceId = "";
        this.name = "";
        this.email = "";
        this.phone = "";
        this.passwordHash = "";
        this.notificationsOn = true;
        this.createdOn = new Date();
    }

    public User(String userId){
        super();
        this.userId = userId;
    }

    public User(String userId, String deviceId, String name, String email, String phone, String passwordHash) {
        this.userId = userId;
        this.deviceId = deviceId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.notificationsOn = true;
        this.createdOn = new Date();
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

    public String getEmail() {
        return email;
    }

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

    public constant.UserRole getRole() {
        return role;
    }

    public void setRole(constant.UserRole role) {
        this.role = role;
    }
}