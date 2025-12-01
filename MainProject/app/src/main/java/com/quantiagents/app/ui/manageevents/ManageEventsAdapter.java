package com.quantiagents.app.ui.manageevents;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.quantiagents.app.R;
import com.quantiagents.app.models.Event;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for the "My Organized Events" list.
 */
public class ManageEventsAdapter extends RecyclerView.Adapter<ManageEventsAdapter.ViewHolder> {

    /**
     * Listener used to forward row actions.
     */
    public interface OnManageEventInfoClickListener {

        /** "Manage Event Info" pressed. */
        void onManageEventInfoClicked(@NonNull Event event);

        /** "Show QR" pressed. */
        void onShowQrClicked(@NonNull Event event);

        /** Trash icon pressed to delete/cancel the event. */
        void onDeleteEventClicked(@NonNull Event event);
    }

    private final List<Event> data = new ArrayList<>();
    private final OnManageEventInfoClickListener listener;

    public ManageEventsAdapter(@NonNull OnManageEventInfoClickListener listener) {
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView status;
        final TextView waitingLine;
        final TextView capacityLine;
        final Button manageInfoButton;
        final Button showQrButton;
        final ImageButton deleteEventButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.text_event_title);
            status = itemView.findViewById(R.id.text_event_status);
            waitingLine = itemView.findViewById(R.id.text_waiting_list);
            capacityLine = itemView.findViewById(R.id.text_capacity);
            manageInfoButton = itemView.findViewById(R.id.button_manage_event_info);
            showQrButton = itemView.findViewById(R.id.button_show_qr);
            deleteEventButton = itemView.findViewById(R.id.button_delete_event);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_manage_event, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Event event = data.get(position);

        String titleText = event.getTitle() == null ? "(Untitled event)" : event.getTitle();
        holder.title.setText(titleText);

        String statusText = event.getStatus() == null ? "" : String.valueOf(event.getStatus());
        holder.status.setText(statusText);

        long waitingCount = event.getWaitingList() == null ? 0L : event.getWaitingList().size();
        holder.waitingLine.setText("Waiting list: " + waitingCount + " entrants");

        long capacity = (long) event.getEventCapacity();
        holder.capacityLine.setText("Capacity: " + capacity + " participants");

        holder.manageInfoButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onManageEventInfoClicked(event);
            }
        });

        holder.showQrButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onShowQrClicked(event);
            }
        });

        holder.deleteEventButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteEventClicked(event);
            }
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    /**
     * Replaces the entire dataset; adequate for current list sizes.
     */
    public void submit(@NonNull List<Event> events) {
        data.clear();
        data.addAll(events);
        notifyDataSetChanged();
    }
}
