package com.quantiagents.app.domain.gateway;

import com.quantiagents.app.domain.Event;
import com.quantiagents.app.domain.Registration;

import java.time.Instant;
import java.util.List;

/**
 * Abstraction layer between controllers and data storage.
 */
public interface PersistenceGateway {

    List<Event> loadOpenEvents(Instant nowUtc);
    Event loadEvent(String eventId);

    boolean isUserOnWaitlist(String eventId, String userId);
    boolean addToWaitlist(String eventId, String userId);
    int getWaitlistCount(String eventId);

    boolean hasRegistration(String eventId, String userId);
    boolean createOrUpsertRegistration(String eventId, String userId, Registration.Status status);
    Registration getRegistration(String eventId, String userId);
    boolean updateRegistrationStatus(String eventId, String userId, Registration.Status newStatus);
    boolean removeFromWaitlist(String eventId, String userId);

    /**
     * Returns true if registration is open for the event at the given time.
     */
    boolean isRegistrationOpen(String eventId, Instant nowUtc);

    /**
     * Returns true if the event requires geolocation to register.
     */
    boolean isGeolocationRequired(String eventId);
}
