package com.quantiagents.app.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.quantiagents.app.R;
import com.quantiagents.app.models.Event;

import java.util.Objects;

public class AdminEventAdapter extends ListAdapter<Event, AdminEventAdapter.EventViewHolder> {

    private final OnEventDeleteListener deleteListener;

    public interface OnEventDeleteListener {
        void onDelete(Event event);
    }

    public AdminEventAdapter(OnEventDeleteListener deleteListener) {
        super(new EventDiffCallback());
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.admin_event_list_item, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = getItem(position);
        holder.bind(event, deleteListener);
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleView;
        private final TextView idView;
        private final Button deleteButton;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.item_event_title);
            idView = itemView.findViewById(R.id.item_event_id);
            deleteButton = itemView.findViewById(R.id.item_delete_button);
        }

        void bind(Event event, OnEventDeleteListener deleteListener) {
            titleView.setText(event.getTitle());
            idView.setText(event.getEventId());
            deleteButton.setOnClickListener(v -> deleteListener.onDelete(event));
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