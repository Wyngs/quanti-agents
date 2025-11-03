package com.quantiagents.app.Constants;

public class constant {

    public enum UserRole{
        ENTRANT,
        ADMIN
    }

    public enum EventRegistrationStatus{
        WAITLIST,
        CANCELLED,
        CONFIRMED,
        SELECTED
    }

    public enum EventStatus {
        OPEN,
        CLOSED,
        FULL,
        CANCELLED
    }

    public enum NotificationType {
        GOOD,
        WARNING,
        REMINDER,
        BAD
    }
}

