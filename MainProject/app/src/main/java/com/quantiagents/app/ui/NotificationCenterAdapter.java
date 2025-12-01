package com.quantiagents.app.ui;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.quantiagents.app.Constants.constant;
import com.quantiagents.app.R;
import com.quantiagents.app.models.Event;
import com.quantiagents.app.models.Notification;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationCenterAdapter extends RecyclerView.Adapter<NotificationCenterAdapter.NotificationViewHolder> {

    private List<Notification> notifications = new ArrayList<>();
    private List<Event> events = new ArrayList<>();
    private final OnMarkAsReadListener markAsReadListener;

    public interface OnMarkAsReadListener {
        void onMarkAsRead(Notification notification);
    }

    public NotificationCenterAdapter(List<Notification> notifications, OnMarkAsReadListener markAsReadListener) {
        this.notifications = new ArrayList<>(notifications);
        this.markAsReadListener = markAsReadListener;
    }

    public void updateNotifications(List<Notification> notifications, List<Event> events) {
        this.notifications = new ArrayList<>(notifications);
        this.events = new ArrayList<>(events != null ? events : new ArrayList<>());
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification_center, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notifications.get(position);
        Event event = findEventById(notification.getAffiliatedEventId());
        holder.bind(notification, event, markAsReadListener);
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    private Event findEventById(int eventId) {
        String eventIdStr = String.valueOf(eventId);
        for (Event event : events) {
            if (event != null && eventIdStr.equals(event.getEventId())) {
                return event;
            }
        }
        return null;
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final ImageView iconView;
        private final TextView messageView;
        private final TextView eventNameView;
        private final TextView timestampView;
        private final MaterialButton markAsReadButton;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_notification);
            iconView = itemView.findViewById(R.id.icon_notification);
            messageView = itemView.findViewById(R.id.text_notification_message);
            eventNameView = itemView.findViewById(R.id.text_event_name);
            timestampView = itemView.findViewById(R.id.text_timestamp);
            markAsReadButton = itemView.findViewById(R.id.button_mark_as_read);
        }

        void bind(Notification notification, Event event, OnMarkAsReadListener listener) {
            // Set message based on notification type
            // Fix: Using the actual details from the notification object
            // and if details are empty, fallback to the type-based message
            String message = notification.getDetails();
            if (message == null || message.trim().isEmpty()) {
                message = getNotificationMessage(notification);
            }
            messageView.setText(message);

            // Set event name if available, fallback to Notification status (title)
            if (event != null && event.getTitle() != null) {
                eventNameView.setVisibility(View.VISIBLE);
                eventNameView.setText("Event: " + event.getTitle());
            } else if (notification.getStatus() != null && !notification.getStatus().isEmpty()) {
                eventNameView.setVisibility(View.VISIBLE);
                eventNameView.setText(notification.getStatus());
            } else {
                eventNameView.setVisibility(View.GONE);
            }

            // Set timestamp
            Date timestamp = notification.getTimestamp();
            if (timestamp != null) {
                DateFormat format = SimpleDateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault());
                timestampView.setText(format.format(timestamp));
            } else {
                timestampView.setText("");
            }

            // Set icon and color based on type
            constant.NotificationType type = notification.getType();
            if (type != null) {
                setNotificationIcon(iconView, type);
                setNotificationColor(cardView, type);
            }

            // Set read/unread state
            boolean isRead = notification.isHasRead();
            if (isRead) {
                cardView.setAlpha(0.7f);
                markAsReadButton.setVisibility(View.GONE);
            } else {
                cardView.setAlpha(1.0f);
                markAsReadButton.setVisibility(View.VISIBLE);
                markAsReadButton.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onMarkAsRead(notification);
                    }
                });
            }
        }

        // Updated fallback messages to be generic and safe
        private String getNotificationMessage(Notification notification) {
            constant.NotificationType type = notification.getType();
            if (type == null) {
                return "You have a new notification";
            }

            switch (type) {
                case GOOD:
                    return "Good news regarding your event status."; // Generic positive
                case WARNING:
                    return "Important update regarding your event status.";
                case REMINDER:
                    return "This event has been updated. Check the details.";
                case BAD:
                    return "This event has been cancelled.";
                default:
                    return "You have a new notification";
            }
        }

        private void setNotificationIcon(ImageView iconView, constant.NotificationType type) {
            switch (type) {
                case GOOD:
                    iconView.setImageResource(R.drawable.ic_check_circle);
                    iconView.setColorFilter(Color.parseColor("#16a34a")); // green-600
                    break;
                case WARNING:
                    iconView.setImageResource(R.drawable.ic_cancel);
                    iconView.setColorFilter(Color.parseColor("#dc2626")); // red-600
                    break;
                case REMINDER:
                    iconView.setImageResource(R.drawable.ic_info);
                    iconView.setColorFilter(Color.parseColor("#2563eb")); // blue-600
                    break;
                case BAD:
                    iconView.setImageResource(R.drawable.ic_cancel);
                    iconView.setColorFilter(Color.parseColor("#ea580c")); // orange-600
                    break;
                default:
                    iconView.setImageResource(R.drawable.ic_notifications);
                    iconView.setColorFilter(Color.parseColor("#6b7280")); // gray-600
                    break;
            }
        }

        private void setNotificationColor(MaterialCardView cardView, constant.NotificationType type) {
            int backgroundColor;
            int strokeColor;

            switch (type) {
                case GOOD:
                    backgroundColor = Color.parseColor("#f0fdf4"); // green-50
                    strokeColor = Color.parseColor("#bbf7d0"); // green-200
                    break;
                case WARNING:
                    backgroundColor = Color.parseColor("#fef2f2"); // red-50
                    strokeColor = Color.parseColor("#fecaca"); // red-200
                    break;
                case REMINDER:
                    backgroundColor = Color.parseColor("#eff6ff"); // blue-50
                    strokeColor = Color.parseColor("#bfdbfe"); // blue-200
                    break;
                case BAD:
                    backgroundColor = Color.parseColor("#fff7ed"); // orange-50
                    strokeColor = Color.parseColor("#fed7aa"); // orange-200
                    break;
                default:
                    backgroundColor = Color.parseColor("#f9fafb"); // gray-50
                    strokeColor = Color.parseColor("#e5e7eb"); // gray-200
                    break;
            }

            cardView.setCardBackgroundColor(backgroundColor);
            cardView.setStrokeColor(strokeColor);
            cardView.setStrokeWidth(2);
        }
    }
}

