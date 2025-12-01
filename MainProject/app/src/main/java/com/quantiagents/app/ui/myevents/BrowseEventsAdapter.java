package com.quantiagents.app.ui.myevents;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.quantiagents.app.R;
import com.quantiagents.app.models.Event;
import com.quantiagents.app.Services.UserService;

import java.text.BreakIterator;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying events in a RecyclerView within BrowseEventsFragment.
 * Handles event item rendering and user interaction callbacks.
 */
public class BrowseEventsAdapter extends RecyclerView.Adapter<BrowseEventsAdapter.EventVH> {

    /**
     * Interface for handling event click events.
     */
    public interface OnEventClick {
        /**
         * Called when user wants to join the waitlist for an event.
         *
         * @param event The event to join
         */
        void onJoinWaitlist(@NonNull Event event);
        
        /**
         * Called when user wants to view event details.
         *
         * @param event The event to view
         */
        void onViewEvent(@NonNull Event event);
    }

    private final List<Event> data;
    private final OnEventClick cb;
    private final UserService userService;
    private final DateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    /**
     * Constructor that initializes the adapter with event data and callbacks.
     * Updated to accept UserService for user-related operations.
     *
     * @param initial The initial list of events to display
     * @param cb The callback interface for handling event clicks
     * @param userService The UserService instance for user operations
     */
    public BrowseEventsAdapter(List<Event> initial, OnEventClick cb, UserService userService) {
        this.data = initial == null ? new ArrayList<>() : new ArrayList<>(initial);
        this.cb = cb;
        this.userService = userService;
    }

    /**
     * Sets new event data, replacing the existing list.
     *
     * @param newEvents The new list of events to display
     */
    public void setData(List<Event> newEvents) {
        data.clear();
        data.addAll(newEvents);
    }

    /**
     * Replaces the current event list with a new one and notifies the adapter.
     *
     * @param next The new list of events to display
     */
    public void replace(List<Event> next) {
        data.clear();
        if (next != null) data.addAll(next);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EventVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_single_event, parent, false);
        // Ensure correct layout params for RecyclerView
        if (v.getLayoutParams() == null) {
            v.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
        } else {
            v.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
            v.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
        }
        return new EventVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull EventVH h, int pos) {
        // Passing userService to bind method
        h.bind(data.get(pos), cb, dateFmt, userService);
    }

    @Override
    public int getItemCount() { return data.size(); }

    static final class EventVH extends RecyclerView.ViewHolder {
        private final TextView title;
        private final TextView description;
        private final TextView statusChip;
        private final TextView textDate;
        private final TextView textRegStatus;
        private final TextView textPrice;
        private final TextView textCapacity;
        private final TextView textOrganizer;
        private final MaterialButton primaryAction;
        private final MaterialButton secondaryAction;
        private final MaterialButton viewEventAction;
        private final ProgressBar progress;

        EventVH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.single_title);
            description = itemView.findViewById(R.id.single_description);
            statusChip = itemView.findViewById(R.id.single_status_chip);
            textDate = itemView.findViewById(R.id.single_text_date);
            textRegStatus = itemView.findViewById(R.id.single_text_reg_status);
            textPrice = itemView.findViewById(R.id.single_text_price);
            textCapacity = itemView.findViewById(R.id.single_text_capacity);
            textOrganizer = itemView.findViewById(R.id.single_text_organizer);
            primaryAction = itemView.findViewById(R.id.single_action_primary);
            secondaryAction = itemView.findViewById(R.id.single_action_secondary);
            viewEventAction = itemView.findViewById(R.id.single_action_view);
            progress = itemView.findViewById(R.id.single_progress);
        }

        /**
         * Binds an event to the view holder, displaying event details and action buttons.
         * Fetches organizer name asynchronously to fix "Organizer unknown" error.
         *
         * @param e The event to display
         * @param cb The callback interface for handling event clicks
         * @param dateFmt The date formatter for displaying event dates
         * @param userService The UserService instance for fetching organizer information
         */
        void bind(final Event e, final OnEventClick cb, DateFormat dateFmt, UserService userService) {
            if (e == null) {
                return;
            }

            if (progress != null) {
                progress.setVisibility(View.GONE);
            }

            // Basic info
            title.setText(nullSafe(e.getTitle(), "Untitled event"));

            String desc = "";
            try {
                Object d = Event.class.getMethod("getDescription").invoke(e);
                if (d instanceof String) desc = (String) d;
            } catch (Exception ignore) {}
            description.setText(nullSafe(desc, "No description provided."));

            // Details
            String dateStr = e.getEventStartDate() != null ? dateFmt.format(e.getEventStartDate()) : "--";
            textDate.setText(dateStr);

            boolean open = BrowseEventsFragment.isOpen(e);
            textRegStatus.setText(open ? "Registration Open" : "Registration Closed");

            Double cost = e.getCost();
            String priceLabel = (cost == null || cost == 0) ? "Free"
                    : String.format(Locale.US, "$%.2f", cost);
            textPrice.setText(priceLabel);

            double capacity = e.getEventCapacity();
            if (capacity > 0) {
                textCapacity.setText(String.format(Locale.US, "%.0f capacity", capacity));
            } else {
                textCapacity.setText("Capacity not set");
            }

            String organizerId = e.getOrganizerId();
            if (organizerId != null && !organizerId.isEmpty()) {
                textOrganizer.setText("Organizer: Loading...");
                new Thread(() -> {
                    try {
                        com.quantiagents.app.models.User orgUser = userService.getUserById(organizerId);
                        String name = (orgUser != null && orgUser.getName() != null) ? orgUser.getName() : "Unknown";
                        textOrganizer.post(() -> textOrganizer.setText("Organizer: " + name));
                    } catch (Exception ex) {
                        textOrganizer.post(() -> textOrganizer.setText("Organizer: Unknown"));
                    }
                }).start();
            } else {
                textOrganizer.setText("Organizer: Unknown");
            }

            // Status chip (simple open/closed/new)
            if (BrowseEventsFragment.isNew(e)) {
                statusChip.setText("New");
            } else {
                statusChip.setText(open ? "Open" : "Closed");
            }

            // Actions
            secondaryAction.setVisibility(View.GONE);

            if (open) {
                primaryAction.setVisibility(View.VISIBLE);
                primaryAction.setText("Join waiting list");
                primaryAction.setOnClickListener(v -> cb.onJoinWaitlist(e));
            } else {
                primaryAction.setVisibility(View.GONE);
            }

            viewEventAction.setVisibility(View.VISIBLE);
            viewEventAction.setText(R.string.view_event);
            viewEventAction.setOnClickListener(v -> cb.onViewEvent(e));

            itemView.setOnClickListener(v -> cb.onViewEvent(e));
        }

        /**
         * Returns the string if not null/empty, otherwise returns the fallback.
         *
         * @param s The string to check
         * @param fallback The fallback string to return if s is null or empty
         * @return s if not null/empty, otherwise fallback
         */
        private static String nullSafe(String s, String fallback) {
            return s == null || s.isEmpty() ? fallback : s;
        }
    }
}