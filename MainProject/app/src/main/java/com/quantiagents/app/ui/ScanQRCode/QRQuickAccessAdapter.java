package com.quantiagents.app.ui.ScanQRCode;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.quantiagents.app.R;
import com.quantiagents.app.models.Event;
import com.quantiagents.app.models.QRCode;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying quick access QR codes for events in ScanQRCodeFragment.
 * Allows users to quickly scan QR codes for events they've recently accessed.
 */
public class QRQuickAccessAdapter extends RecyclerView.Adapter<QRQuickAccessAdapter.ViewHolder> {

    /**
     * Interface for handling event scan actions.
     */
    public interface OnEventScanListener {
        /**
         * Called when user wants to scan a QR code for an event.
         *
         * @param event The event associated with the QR code
         * @param qrCodeValue The QR code value to scan
         */
        void onScanEvent(Event event, String qrCodeValue);
    }

    private final List<EventQRPair> items = new ArrayList<>();
    private final OnEventScanListener listener;

    /**
     * Data class representing an event and its associated QR code value.
     */
    public static class EventQRPair {
        public final Event event;
        public final String qrCodeValue;

        /**
         * Constructor for EventQRPair.
         *
         * @param event The event object
         * @param qrCodeValue The QR code value associated with the event
         */
        public EventQRPair(Event event, String qrCodeValue) {
            this.event = event;
            this.qrCodeValue = qrCodeValue;
        }
    }

    /**
     * Constructor that initializes the adapter with a scan listener.
     *
     * @param listener The callback interface for handling scan actions
     */
    public QRQuickAccessAdapter(OnEventScanListener listener) {
        this.listener = listener;
    }

    /**
     * Sets new event-QR code pairs and notifies the adapter.
     *
     * @param newItems The new list of event-QR code pairs to display
     */
    public void setItems(List<EventQRPair> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_qr_quick_access_event, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EventQRPair pair = items.get(position);
        holder.bind(pair, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView eventNameText;
        private final TextView qrCodeText;
        private final MaterialButton scanButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            eventNameText = itemView.findViewById(R.id.eventNameText);
            qrCodeText = itemView.findViewById(R.id.qrCodeText);
            scanButton = itemView.findViewById(R.id.scanButton);
        }

        /**
         * Binds an event-QR code pair to the view holder.
         *
         * @param pair The event-QR code pair to display
         * @param listener The scan listener for handling button clicks
         */
        void bind(EventQRPair pair, OnEventScanListener listener) {
            Event event = pair.event;
            String qrCodeValue = pair.qrCodeValue;

            eventNameText.setText(event.getTitle() != null ? event.getTitle() : "Untitled Event");
            qrCodeText.setText(itemView.getContext().getString(R.string.scan_qr_quick_access_code_label, qrCodeValue));

            scanButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onScanEvent(event, qrCodeValue);
                }
            });
        }
    }
}

