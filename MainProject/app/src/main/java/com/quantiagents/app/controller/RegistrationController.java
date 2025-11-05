package com.quantiagents.app.controller;

import com.quantiagents.app.domain.gateway.PersistenceGateway;
import com.quantiagents.app.domain.Registration;
import com.quantiagents.app.domain.services.GeolocationService;
import com.quantiagents.app.domain.services.NotificationService;

import java.time.Instant;
import java.util.Objects;

/**
 * Handles user registration actions from event details.
 */
public final class RegistrationController {

    private final PersistenceGateway gateway;
    private final GeolocationService geo;
    private final NotificationService notifier;

    /**
     * Create a RegistrationController.
     *
     * @param gateway abstraction over persistence (repositories / Firebase); required
     * @param geo geolocation domain service; required
     * @param notifier notification domain service; required
     * @throws NullPointerException if any argument is null
     */
    public RegistrationController(final PersistenceGateway gateway,
                                  final GeolocationService geo,
                                  final NotificationService notifier) {
        this.gateway = Objects.requireNonNull(gateway, "gateway");
        this.geo = Objects.requireNonNull(geo, "geo");
        this.notifier = Objects.requireNonNull(notifier, "notifier");
    }

    /**
     * Creates a registration entry for a user from the event details screen.
     * <p>
     * Rules:
     * <ul>
     *   <li>Registration window must be open.</li>
     *   <li>Geolocation validation passes if required.</li>
     * </ul>
     *
     * @param eventId event identifier
     * @param userId user identifier
     * @param nowUtc current instant in UTC used for time gating
     * @return true if registration created successfully, false otherwise
     */
    public boolean registerFromEventDetails(final String eventId, final String userId, final Instant nowUtc) {
        if (eventId == null || eventId.isBlank() || userId == null || userId.isBlank() || nowUtc == null) {
            return false;
        }

        // Time window is owned by the gateway since Event has no scheduling fields.
        if (!gateway.isRegistrationOpen(eventId, nowUtc)) {
            return false;
        }

        // Geo requirement is owned by the gateway since Event has no geo fields.
        if (gateway.isGeolocationRequired(eventId)) {
            // TODO capture device location and validate radius using geo service
            // Example:
            // final GeolocationService.Fix fix = geo.tryCaptureFix();
            // if (!geo.isWithinAllowedRadius(/* event or policy */, fix)) return false;
        }

        final boolean success =
                gateway.createOrUpsertRegistration(eventId, userId, Registration.Status.PENDING);

        if (success) {
            // TODO send a notification acknowledging registration receipt
            // notifier.notifyRegistrationReceived(eventId, userId);
        }

        return success;
    }

    /**
     * Accept a selected invitation and confirm the registration.
     */
    public boolean acceptInvitation(final String eventId, final String userId) {
        if (eventId == null || eventId.isBlank() || userId == null || userId.isBlank()) return false;
        final Registration existing = gateway.getRegistration(eventId, userId);
        if (existing == null) return false;                         // nothing to accept
        if (existing.getStatus() != Registration.Status.SELECTED) { // only selected can be accepted
            return false;
        }
        return gateway.updateRegistrationStatus(eventId, userId, Registration.Status.CONFIRMED);
    }

    /**
     * Decline a selected invitation.
     */
    public boolean declineInvitation(final String eventId, final String userId) {
        if (eventId == null || eventId.isBlank() || userId == null || userId.isBlank()) return false;
        final Registration existing = gateway.getRegistration(eventId, userId);
        if (existing == null) return false;
        if (existing.getStatus() != Registration.Status.SELECTED) {
            return false;
        }
        return gateway.updateRegistrationStatus(eventId, userId, Registration.Status.DECLINED);
    }

}
