package com.quantiagents.app.models;

import java.io.Serializable;

/**
 * Representation of a user summary, with getters and setters for each variable
 */
public class UserSummary implements Serializable {

    private String userId;
    private String name;
    private String email;
    public UserSummary(){}
    public UserSummary(String userId, String name, String email) {
        //admin-facing minimal profile
        this.userId = userId;
        this.name = name;
        this.email = email;
    }

    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
}
