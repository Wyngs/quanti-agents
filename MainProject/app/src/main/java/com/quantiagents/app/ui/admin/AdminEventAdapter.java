package com.quantiagents.app.ui.admin;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.quantiagents.app.R;
import com.quantiagents.app.models.Event;
import com.quantiagents.app.ui.myevents.BrowseEventsFragment;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;

public class AdminEventAdapter extends ListAdapter<Event, AdminEventAdapter.EventViewHolder> {

    private final OnEventDeleteListener deleteListener;
    private final OnEventViewListener viewListener;

    public interface OnEventDeleteListener {
        void onDelete(Event event);
    }
    public interface OnItemClickListener {
        void onItemClick(Event event);
    }

    public interface OnEventViewListener {
        void onViewEvent(Event event);
    }

    public AdminEventAdapter(OnEventDeleteListener deleteListener, OnEventViewListener viewListener) {
        super(new EventDiffCallback());
        this.deleteListener = deleteListener;
        this.viewListener = viewListener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                // Reuse the entrant-facing single event card for admin view
                .inflate(R.layout.fragment_single_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = getItem(position);
        holder.bind(event, deleteListener, viewListener);
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleView;
        private final TextView subtitleView;
        private final MaterialButton primaryActionButton;
        private final MaterialButton secondaryActionButton;
        private final MaterialButton viewEventAction;
        private final ProgressBar progressBar;
        private final TextView statusChip;
        private final TextView textDate;
        private final TextView textRegStatus;
        private final TextView textPrice;
        private final TextView textCapacity;
        private final TextView textOrganizer;
        private final DateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            // Map to views in fragment_single_event.xml
            titleView = itemView.findViewById(R.id.single_title);
            subtitleView = itemView.findViewById(R.id.single_description);
            primaryActionButton = itemView.findViewById(R.id.single_action_primary);
            secondaryActionButton = itemView.findViewById(R.id.single_action_secondary);
            viewEventAction = itemView.findViewById(R.id.single_action_view);
            progressBar = itemView.findViewById(R.id.single_progress);

            statusChip = itemView.findViewById(R.id.single_status_chip);
            textDate = itemView.findViewById(R.id.single_text_date);
            textRegStatus = itemView.findViewById(R.id.single_text_reg_status);
            textPrice = itemView.findViewById(R.id.single_text_price);
            textCapacity = itemView.findViewById(R.id.single_text_capacity);
            textOrganizer = itemView.findViewById(R.id.single_text_organizer);

        }

        /**
         * Binds an event to the view holder, displaying event details and action buttons.
         *
         * @param event The event to display
         * @param deleteListener The listener for delete actions
         * @param viewListener The listener for view actions
         */
        void bind(Event event, OnEventDeleteListener deleteListener, OnEventViewListener viewListener) {
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }

            // Title and ID in the card
            titleView.setText(event.getTitle() != null ? event.getTitle() : "Untitled event");
            String idText = String.format(Locale.US, "ID: %s", event.getEventId());
            subtitleView.setText(idText);

            // Details
            String dateStr = event.getEventStartDate() != null ? dateFmt.format(event.getEventStartDate()) : "--";
            textDate.setText(dateStr);

            boolean open = BrowseEventsFragment.isOpen(event);
            textRegStatus.setText(open ? "Registration Open" : "Registration Closed");

            Double cost = event.getCost();
            String priceLabel = (cost == null || cost == 0) ? "Free"
                    : String.format(Locale.US, "$%.2f", cost);
            textPrice.setText(priceLabel);

            double capacity = event.getEventCapacity();
            if (capacity > 0) {
                textCapacity.setText(String.format(Locale.US, "%.0f capacity", capacity));
            } else {
                textCapacity.setText("Capacity not set");
            }

            textOrganizer.setText("Organizer: Unknown");

            // Status chip (simple open/closed/new)
            if (BrowseEventsFragment.isNew(event)) {
                statusChip.setText("New");
            } else {
                statusChip.setText(open ? "Open" : "Closed");
            }

            // Admin-specific actions:
            // - Primary button becomes "Delete event"
            // - No join waitlist / accept / decline actions
            primaryActionButton.setVisibility(View.VISIBLE);
            primaryActionButton.setText(R.string.delete_event);
            primaryActionButton.setOnClickListener(v -> deleteListener.onDelete(event));
            primaryActionButton.setIconResource(R.drawable.ic_cancel_circle);
            primaryActionButton.setBackgroundTintList(ColorStateList.valueOf(0xFFF44336)); // Red
            primaryActionButton.setTextColor(ColorStateList.valueOf(0xFFFFFFFF));

            // Hide other entrant actions for admin
            if (secondaryActionButton != null) {
                secondaryActionButton.setVisibility(View.GONE);
            }

            viewEventAction.setVisibility(View.VISIBLE);
            viewEventAction.setText("View Event");
            viewEventAction.setOnClickListener(v -> {
                if (viewListener != null) {
                    viewListener.onViewEvent(event);
                }
            });

        }
    }
    private static class EventDiffCallback extends DiffUtil.ItemCallback<Event> {
        @Override
        public boolean areItemsTheSame(@NonNull Event oldItem, @NonNull Event newItem) {
            return Objects.equals(oldItem.getEventId(), newItem.getEventId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Event oldItem, @NonNull Event newItem) {
            return Objects.equals(oldItem.getTitle(), newItem.getTitle());
        }
    }
}