package com.quantiagents.app.controllertests;

import com.quantiagents.app.domain.Event;
import com.quantiagents.app.domain.Registration;
import com.quantiagents.app.domain.gateway.PersistenceGateway;

import java.time.Instant;
import java.util.*;

/** Concrete test double for PersistenceGateway. */
public final class FakeGateway implements PersistenceGateway {

    public boolean open = true;
    public boolean geoRequired = false;

    private List<Event> openEvents = new ArrayList<>();
    public final Map<String, Registration> registrations = new HashMap<>();
    public final Map<String, Set<String>> waitlists = new HashMap<>();

    // helper to seed open events
    public void setOpenEvents(List<Event> events) { this.openEvents = new ArrayList<>(events); }

    @Override public List<Event> loadOpenEvents(Instant nowUtc) { return new ArrayList<>(openEvents); }
    @Override public Event loadEvent(String eventId) { return null; }

    @Override public boolean isUserOnWaitlist(String eventId, String userId) {
        return waitlists.getOrDefault(eventId, Collections.emptySet()).contains(userId);
    }

    @Override public boolean addToWaitlist(String eventId, String userId) {
        waitlists.computeIfAbsent(eventId, k -> new HashSet<>()).add(userId);
        return true;
    }

    @Override public int getWaitlistCount(String eventId) {
        return waitlists.getOrDefault(eventId, Collections.emptySet()).size();
    }

    @Override public boolean hasRegistration(String eventId, String userId) {
        return registrations.containsKey(eventId + ":" + userId);
    }

    @Override public boolean createOrUpsertRegistration(String eventId, String userId, Registration.Status status) {
        registrations.put(eventId + ":" + userId, new Registration(eventId, userId, status));
        return true;
    }

    @Override public Registration getRegistration(String eventId, String userId) {
        return registrations.get(eventId + ":" + userId);
    }

    @Override public boolean updateRegistrationStatus(String eventId, String userId, Registration.Status newStatus) {
        Registration reg = registrations.get(eventId + ":" + userId);
        if (reg == null) return false;
        reg.setStatus(newStatus);
        return true;
    }

    @Override public boolean removeFromWaitlist(String eventId, String userId) {
        return waitlists.getOrDefault(eventId, Collections.emptySet()).remove(userId);
    }

    @Override public boolean isRegistrationOpen(String eventId, Instant nowUtc) { return open; }

    @Override public boolean isGeolocationRequired(String eventId) { return geoRequired; }
}
