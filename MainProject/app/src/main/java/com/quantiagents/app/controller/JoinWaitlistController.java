package com.quantiagents.app.controller;

import com.quantiagents.app.domain.gateway.PersistenceGateway;

import java.time.Instant;
import java.util.Objects;

/**
 * Handles joining an event waiting list.
 */
public final class JoinWaitlistController {

    private final PersistenceGateway gateway;

    /**
     * Create a JoinWaitlistController.
     *
     * @param gateway abstraction over persistence (repositories / Firebase); required
     * @throws NullPointerException if gateway is null
     */
    public JoinWaitlistController(final PersistenceGateway gateway) {
        this.gateway = Objects.requireNonNull(gateway, "gateway");
    }

    /**
     * Adds the user to the waiting list for an event.
     * <p>
     * Rules:
     * <ul>
     *   <li>Registration window must be open.</li>
     *   <li>User cannot already be on the waiting list.</li>
     * </ul>
     *
     * @param eventId event identifier
     * @param userId user identifier
     * @param nowUtc current instant in UTC used for time gating
     * @return true if join succeeds, false otherwise
     */
    public boolean joinWaitlist(final String eventId, final String userId, final Instant nowUtc) {
        if (eventId == null || eventId.isBlank()) {
            return false;
        }
        if (userId == null || userId.isBlank()) {
            return false;
        }
        if (nowUtc == null) {
            return false;
        }

        // Check registration window via gateway since Event does not hold scheduling fields
        if (!gateway.isRegistrationOpen(eventId, nowUtc)) {
            return false;
        }

        // Idempotent behavior if already on waiting list
        if (gateway.isUserOnWaitlist(eventId, userId)) {
            return true;
        }

        // Add user to waiting list
        return gateway.addToWaitlist(eventId, userId);
    }
    /**
     * Removes the user from the waiting list for an event.
     */
    public boolean leaveWaitlist(final String eventId, final String userId) {
        if (eventId == null || eventId.isBlank() || userId == null || userId.isBlank()) return false;
        return gateway.removeFromWaitlist(eventId, userId);
    }


}
