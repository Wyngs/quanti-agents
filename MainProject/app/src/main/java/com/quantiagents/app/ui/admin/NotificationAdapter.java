package com.quantiagents.app.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.quantiagents.app.R;
import com.quantiagents.app.models.Notification;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private final List<Notification> notificationList;
    private final OnDeleteClickListener listener;

    public interface OnDeleteClickListener {
        void onDeleteClick(Notification notification, int position);
    }

    public NotificationAdapter(List<Notification> notificationList, OnDeleteClickListener listener) {
        this.notificationList = notificationList;
        this.listener = listener;
    }

    @NonNull @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification_admin, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        holder.bind(notificationList.get(position), listener);
    }

    @Override
    public int getItemCount() { return notificationList.size(); }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        final TextView titleTextView;
        final TextView detailsTextView;
        final ImageButton deleteButton;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.text_view_notification_title);
            detailsTextView = itemView.findViewById(R.id.text_view_notification_details);
            deleteButton = itemView.findViewById(R.id.button_delete_notification);
        }

        void bind(final Notification notification, final OnDeleteClickListener listener) {
            String type = notification.getType() != null ? notification.getType().toString() : "UNKNOWN";
            titleTextView.setText(type);

            String details = "ID: " + notification.getNotificationId() + " | To: " + notification.getRecipientId();
            detailsTextView.setText(details);

            deleteButton.setOnClickListener(v -> {
                if (getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onDeleteClick(notification, getAdapterPosition());
                }
            });
        }
    }
}