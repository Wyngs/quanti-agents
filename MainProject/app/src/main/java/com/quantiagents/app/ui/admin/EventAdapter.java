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

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private final List<Event> eventList;
    private final OnDeleteClickListener listener;

    public interface OnDeleteClickListener { void onDeleteClick(Event event, int position); }

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

    public static class EventViewHolder extends RecyclerView.ViewHolder {
        final TextView eventName, eventDetails;
        final ImageButton deleteButton;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            eventName = itemView.findViewById(R.id.text_view_event_name);
            eventDetails = itemView.findViewById(R.id.text_view_event_details);
            deleteButton = itemView.findViewById(R.id.button_delete_event);
        }

        public void bind(final Event event, final OnDeleteClickListener listener) {
            eventName.setText(event.getEventName());

            String idText = itemView.getContext().getString(R.string.event_id_label, event.getEventId());
            eventDetails.setText(idText);

            deleteButton.setOnClickListener(v -> {
                if (getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onDeleteClick(event, getAdapterPosition());
                }
            });
        }
    }
}