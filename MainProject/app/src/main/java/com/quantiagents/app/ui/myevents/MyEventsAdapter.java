package com.quantiagents.app.ui.myevents;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.core.widget.TextViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.quantiagents.app.Constants.constant;
import com.quantiagents.app.R;
import com.quantiagents.app.models.Event;
import com.quantiagents.app.models.RegistrationHistory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MyEventsAdapter extends RecyclerView.Adapter<MyEventsAdapter.ViewHolder> {

    public interface OnEventActionListener {
        void onViewEvent(String eventId);
        void onLeaveWaitlist(String eventId);
        void onAccept(RegistrationHistory history);
        void onDecline(RegistrationHistory history);
    }

    public static class MyEventItem {
        public final RegistrationHistory history;
        public final Event event;
        public final String organizerName;

        public MyEventItem(@NonNull RegistrationHistory history, @NonNull Event event, String organizerName) {
            this.history = history;
            this.event = event;
            this.organizerName = organizerName;
        }
    }

    private final List<MyEventItem> items = new ArrayList<>();
    private final OnEventActionListener listener;
    private final DateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    public MyEventsAdapter(OnEventActionListener listener) {
        this.listener = listener;
    }

    public void setItems(List<MyEventItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_single_event, parent, false);
        // Ensure correct layout params
        if (v.getLayoutParams() == null) {
            v.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
        } else {
            v.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
            v.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
        }
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position), listener, dateFmt);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, statusChip, description;
        TextView textDate, textRegStatus, textPrice, textCapacity, textOrganizer;
        MaterialButton primaryAction, secondaryAction, viewEventAction;
        ProgressBar progress;

        ViewHolder(@NonNull View v) {
            super(v);
            title = v.findViewById(R.id.single_title);
            statusChip = v.findViewById(R.id.single_status_chip);
            description = v.findViewById(R.id.single_description);
            textDate = v.findViewById(R.id.single_text_date);
            textRegStatus = v.findViewById(R.id.single_text_reg_status);
            textPrice = v.findViewById(R.id.single_text_price);
            textCapacity = v.findViewById(R.id.single_text_capacity);
            textOrganizer = v.findViewById(R.id.single_text_organizer);

            primaryAction = v.findViewById(R.id.single_action_primary);
            secondaryAction = v.findViewById(R.id.single_action_secondary);
            viewEventAction = v.findViewById(R.id.single_action_view);
            progress = v.findViewById(R.id.single_progress);
        }

        void bind(MyEventItem item, OnEventActionListener listener, DateFormat dateFmt) {
            if (item == null || item.event == null) return;

            Event e = item.event;
            RegistrationHistory h = item.history;

            if (progress != null) progress.setVisibility(View.GONE);

            // Basic Info
            title.setText(e.getTitle() != null ? e.getTitle() : "Untitled Event");
            description.setText(e.getDescription() != null ? e.getDescription() : "No description provided.");

            // Details
            String dateStr = e.getEventStartDate() != null ? dateFmt.format(e.getEventStartDate()) : "--";
            textDate.setText(dateStr);

            boolean regOpen = isRegistrationOpen(e);
            textRegStatus.setText(regOpen ? "Registration Open" : "Registration Closed");

            textPrice.setText(e.getCost() == null || e.getCost() == 0 ? "Free" : String.format(Locale.US, "$%.2f", e.getCost()));
            textCapacity.setText(String.format(Locale.US, "0/%.0f capacity", e.getEventCapacity())); // Placeholder for enrolled count
            textOrganizer.setText("Organized by " + (item.organizerName != null ? item.organizerName : "Unknown"));

            // Status Badge
            constant.EventRegistrationStatus status = h.getEventRegistrationStatus();
            if (e.getStatus() == constant.EventStatus.CLOSED) {
                bindStatus(null, true); // Override as closed
            } else {
                bindStatus(status, false);
            }

            // Actions
            bindActions(h, e, listener);
        }

        private void bindStatus(constant.EventRegistrationStatus status, boolean isClosed) {
            String label = "Unknown";
            int color = 0xFFEEEEEE; // Default Gray
            int textColor = 0xFF212121; // Default Black

            if (isClosed) {
                label = "Closed";
                color = 0xFFEEEEEE;
                textColor = 0xFF757575;
            } else if (status != null) {
                switch (status) {
                    case WAITLIST:
                        label = "Waiting";
                        color = 0xFFEEEEEE; // Gray background
                        textColor = 0xFF212121;
                        break;
                    case SELECTED:
                        label = "Selected";
                        color = 0xFFFFD740; // Amber/Yellow background
                        textColor = 0xFF212121;
                        break;
                    case CONFIRMED:
                        label = "Confirmed";
                        color = 0xFF4CAF50; // Green background
                        textColor = 0xFFFFFFFF; // White text
                        break;
                    case CANCELLED:
                        label = "Cancelled";
                        color = 0xFFE57373; // Red background
                        textColor = 0xFFFFFFFF; // White text
                        break;
                }
            }

            statusChip.setText(label);
            statusChip.setTextColor(textColor);
            ViewCompat.setBackgroundTintList(statusChip, ColorStateList.valueOf(color));
        }

        private void bindActions(RegistrationHistory h, Event e, OnEventActionListener listener) {
            primaryAction.setVisibility(View.GONE);
            secondaryAction.setVisibility(View.GONE);
            viewEventAction.setVisibility(View.GONE);

            // Always allow viewing the event details unless purely actionable state dictates otherwise?
            // Design implies "View Event" is always there at the bottom except for "Selected" which has Accept/Decline

            viewEventAction.setVisibility(View.VISIBLE);
            viewEventAction.setOnClickListener(v -> listener.onViewEvent(e.getEventId()));

            if (e.getStatus() == constant.EventStatus.CLOSED) {
                return;
            }

            if (h.getEventRegistrationStatus() == null) return;

            switch (h.getEventRegistrationStatus()) {
                case WAITLIST:
                    primaryAction.setVisibility(View.VISIBLE);
                    primaryAction.setText("Leave Waiting List");
                    primaryAction.setIconResource(R.drawable.ic_cancel_circle);
                    primaryAction.setBackgroundTintList(ColorStateList.valueOf(0xFFF44336)); // Red
                    primaryAction.setTextColor(ColorStateList.valueOf(0xFFFFFFFF));
                    primaryAction.setOnClickListener(v -> listener.onLeaveWaitlist(e.getEventId()));
                    break;

                case SELECTED:
                    // For selected, we show Accept and Decline. View Event is hidden or pushed down?
                    // Image implies just Accept/Decline taking focus.
                    viewEventAction.setVisibility(View.GONE);

                    primaryAction.setVisibility(View.VISIBLE);
                    primaryAction.setText("Accept");
                    primaryAction.setIconResource(R.drawable.ic_check);
                    primaryAction.setBackgroundTintList(ColorStateList.valueOf(0xFF4CAF50)); // Green
                    primaryAction.setOnClickListener(v -> listener.onAccept(h));

                    secondaryAction.setVisibility(View.VISIBLE);
                    secondaryAction.setText("Decline");
                    secondaryAction.setIconResource(R.drawable.ic_cancel_circle);
                    secondaryAction.setBackgroundTintList(ColorStateList.valueOf(0xFFF44336)); // Red
                    secondaryAction.setOnClickListener(v -> listener.onDecline(h));
                    break;

                case CONFIRMED:
                    primaryAction.setVisibility(View.VISIBLE);
                    primaryAction.setText("Cancel");
                    primaryAction.setIconResource(R.drawable.ic_cancel_circle);
                    primaryAction.setBackgroundTintList(ColorStateList.valueOf(0xFFF44336)); // Red
                    primaryAction.setOnClickListener(v -> listener.onDecline(h)); // Maps to Cancel
                    break;
            }
        }

        private boolean isRegistrationOpen(Event e) {
            if (e == null) return false;
            Date now = new Date();
            return e.getRegistrationStartDate() != null && e.getRegistrationEndDate() != null
                    && now.after(e.getRegistrationStartDate()) && now.before(e.getRegistrationEndDate());
        }
    }
}