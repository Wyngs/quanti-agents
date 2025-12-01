package com.quantiagents.app.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.quantiagents.app.R;
import com.quantiagents.app.models.Notification;
import java.util.List;

/**
 * Adapter for displaying notifications in admin management screens.
 * Shows notification type, ID, and recipient information.
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private final List<Notification> notificationList;

    /**
     * Constructor that initializes the adapter with notifications.
     *
     * @param notificationList The list of notifications to display
     */
    public NotificationAdapter(List<Notification> notificationList) {
        this.notificationList = notificationList;
    }

    @NonNull @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification_admin, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        holder.bind(notificationList.get(position));
    }

    @Override
    public int getItemCount() { return notificationList.size(); }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        final TextView titleTextView;
        final TextView detailsTextView;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.text_view_notification_title);
            detailsTextView = itemView.findViewById(R.id.text_view_notification_details);
        }

        /**
         * Binds a notification to the view holder, displaying notification type and details.
         *
         * @param notification The notification to display
         */
        void bind(final Notification notification) {
            String type = notification.getType() != null ? notification.getType().toString() : "UNKNOWN";
            titleTextView.setText(type);

            String details = "ID: " + notification.getNotificationId() + " | To: " + notification.getRecipientId();
            detailsTextView.setText(details);
        }
    }
}