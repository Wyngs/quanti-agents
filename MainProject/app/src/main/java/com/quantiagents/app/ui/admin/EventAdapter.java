package com.quantiagents.app.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.quantiagents.app.R;
import com.quantiagents.app.models.Event;
import java.util.List;

/**
 * Adapter for displaying events in admin management screens.
 * Allows admins to view event details and delete events.
 */
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private final List<Event> eventList;
    private final OnDeleteClickListener listener;

    /**
     * Interface for handling event deletion.
     */
    public interface OnDeleteClickListener {
        /**
         * Called when admin wants to delete an event.
         *
         * @param event The event to delete
         * @param position The position of the event in the list
         */
        void onDeleteClick(Event event, int position);
    }

    /**
     * Constructor that initializes the adapter with events and a delete listener.
     *
     * @param eventList The list of events to display
     * @param listener The callback interface for handling event deletion
     */
    public EventAdapter(List<Event> eventList, OnDeleteClickListener listener) {
        this.eventList = eventList;
        this.listener = listener;
    }

    @NonNull @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event_admin, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        holder.bind(eventList.get(position), listener);
    }

    @Override
    public int getItemCount() { return eventList.size(); }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        final TextView eventName, eventDetails;
        final ImageButton deleteButton;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            eventName = itemView.findViewById(R.id.text_view_event_name);
            eventDetails = itemView.findViewById(R.id.text_view_event_details);
            deleteButton = itemView.findViewById(R.id.button_delete_event);
        }

        /**
         * Binds an event to the view holder, displaying event details and delete button.
         *
         * @param event The event to display
         * @param listener The listener for delete actions
         */
        void bind(final Event event, final OnDeleteClickListener listener) {
            eventName.setText(event.getTitle()); // Turn 1 model uses getTitle()
            eventDetails.setText("ID: " + event.getEventId());

            deleteButton.setOnClickListener(v -> {
                if (getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onDeleteClick(event, getAdapterPosition());
                }
            });
        }
    }
}