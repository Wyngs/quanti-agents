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

public class QRQuickAccessAdapter extends RecyclerView.Adapter<QRQuickAccessAdapter.ViewHolder> {

    public interface OnEventScanListener {
        void onScanEvent(Event event, String qrCodeValue);
    }

    private final List<EventQRPair> items = new ArrayList<>();
    private final OnEventScanListener listener;

    public static class EventQRPair {
        public final Event event;
        public final String qrCodeValue;

        public EventQRPair(Event event, String qrCodeValue) {
            this.event = event;
            this.qrCodeValue = qrCodeValue;
        }
    }

    public QRQuickAccessAdapter(OnEventScanListener listener) {
        this.listener = listener;
    }

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

