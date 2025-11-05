package com.quantiagents.app.controller;

import com.quantiagents.app.domain.Event;
import com.quantiagents.app.domain.gateway.PersistenceGateway;
import com.quantiagents.app.domain.services.LotteryService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Coordinates read-only event flows for the Event Catalogue / Event Details screens.
 */
public final class EventController {

    private final PersistenceGateway gateway;
    private final LotteryService lottery;

    /**
     * Create an EventController.
     *
     * @param gateway abstraction over persistence (repositories / Firebase); required
     * @param lottery lottery domain service used for criteria text; required
     * @throws NullPointerException if any argument is null
     */
    public EventController(final PersistenceGateway gateway, final LotteryService lottery) {
        this.gateway = Objects.requireNonNull(gateway, "gateway");
        this.lottery = Objects.requireNonNull(lottery, "lottery");
    }

    /**
     * Returns the events the user can join the waiting list for.
     *
     * @param userId the current user's identifier
     * @param nowUtc current instant in UTC used for time gating
     * @return immutable list of joinable events; empty if none
     */
    public List<Event> getJoinableEvents(final String userId, final Instant nowUtc) {
        if (userId == null || userId.isBlank() || nowUtc == null) {
            return Collections.emptyList();
        }

        final List<Event> open = gateway.loadOpenEvents(nowUtc);
        if (open == null || open.isEmpty()) {
            return Collections.emptyList();
        }

        // Filter without streams for broader Android compatibility
        final ArrayList<Event> out = new ArrayList<>();
        for (int i = 0; i < open.size(); i++) {
            final Event e = open.get(i);
            if (e == null) continue;
            final String eid = e.getEventId();
            if (eid == null || eid.isBlank()) continue;
            if (gateway.isUserOnWaitlist(eid, userId)) continue;
            if (gateway.hasRegistration(eid, userId)) continue;
            out.add(e);
        }

        return Collections.unmodifiableList(new ArrayList<>(out));
    }

    /**
     * @param eventId target event id
     * @return non-negative count; 0 if unknown or none
     */
    public int getWaitlistCount(final String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return 0;
        }
        return Math.max(0, gateway.getWaitlistCount(eventId));
    }

    /**
     * Retrieves the lottery selection criteria or guidelines text.
     *
     * @return markdown or plaintext describing lottery criteria; never null
     */
    public String getLotteryCriteriaMarkdown() {
        final String custom = lottery.getSelectionCriteria();
        if (custom != null && !custom.isBlank()) {
            return custom;
        }
        return defaultOrEmpty(lottery.getDefaultCriteria());
    }

    // helpers

    private static String defaultOrEmpty(final String s) {
        return (s == null) ? "" : s;
    }
}
