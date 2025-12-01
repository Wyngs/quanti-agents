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
    /**
     * Default constructor that creates an empty user summary.
     */
    public UserSummary(){}
    
    /**
     * Constructor that creates an admin-facing minimal profile summary.
     *
     * @param userId The unique identifier for the user
     * @param name The full name of the user
     * @param username The username of the user
     * @param email The email address of the user
     */
    public UserSummary(String userId, String name, String username, String email) {
        //admin-facing minimal profile
        this.userId = userId;
        this.name = name;
        this.username = username;
        this.email = email;
    }

    /**
     * Gets the unique identifier for this user.
     *
     * @return The user ID
     */
    public String getUserId() { return userId; }
    
    /**
     * Gets the full name of the user.
     *
     * @return The user's name
     */
    public String getName() { return name; }
    
    /**
     * Gets the username of the user.
     *
     * @return The username
     */
    public String getUsername() {return username; }
    
    /**
     * Gets the email address of the user.
     *
     * @return The email address
     */
    public String getEmail() { return email; }


    /**
     * Sets the unique identifier for this user.
     *
     * @param userId The user ID to set
     */
    public void setUserId(String userId) { this.userId = userId; }
    
    /**
     * Sets the full name of the user.
     *
     * @param name The name to set
     */
    public void setName(String name) { this.name = name; }
    
    /**
     * Sets the email address of the user.
     *
     * @param email The email address to set
     */
    public void setEmail(String email) { this.email = email; }
}
