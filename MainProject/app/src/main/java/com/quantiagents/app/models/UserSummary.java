package com.quantiagents.app.models;

import java.io.Serializable;

/**
 * Representation of a user summary, with getters and setters for each variable
 * <p>
 * Contains:
 * </p>
 * String: user id, String: name, String: email
 */
public class UserSummary implements Serializable {

    private String userId;
    private String name;
    private String username;
    private String email;
    public UserSummary(){}
    public UserSummary(String userId, String name, String username, String email) {
        //admin-facing minimal profile
        this.userId = userId;
        this.name = name;
        this.username = username;
        this.email = email;
    }

    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getUsername() {return username; }
    public String getEmail() { return email; }


    public void setUserId(String userId) { this.userId = userId; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
}
