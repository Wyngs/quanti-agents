package com.quantiagents.app.domain;

import java.io.Serializable;

public class UserSummary implements Serializable {

    private final String userId;
    private final String name;
    private final String email;

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
