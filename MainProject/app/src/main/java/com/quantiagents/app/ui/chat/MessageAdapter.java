package com.quantiagents.app.ui.chat;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.quantiagents.app.R;
import com.quantiagents.app.models.Message;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying messages in a chat RecyclerView.
 */
public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private final List<Message> messages;
    private final String currentUserId;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    /**
     * Constructor that initializes the adapter with messages and current user ID.
     *
     * @param messages The list of messages to display
     * @param currentUserId The ID of the current user (used to determine message alignment)
     */
    public MessageAdapter(List<Message> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messages.get(position);
        if (message == null) return;

        boolean isCurrentUser = !TextUtils.isEmpty(currentUserId) && 
                                currentUserId.equals(message.getSenderId());

        holder.bind(message, isCurrentUser);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    class MessageViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardMessage;
        private final TextView textSenderName;
        private final TextView textSenderNameAbove;
        private final TextView textMessage;
        private final TextView textTimestamp;
        private final ViewGroup parentLayout;
        private final View spacerSent;
        private final View spacerReceived;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            parentLayout = (ViewGroup) itemView;
            cardMessage = itemView.findViewById(R.id.card_message);
            textSenderName = itemView.findViewById(R.id.text_sender_name);
            textSenderNameAbove = itemView.findViewById(R.id.text_sender_name_above);
            textMessage = itemView.findViewById(R.id.text_message);
            textTimestamp = itemView.findViewById(R.id.text_timestamp);
            spacerSent = itemView.findViewById(R.id.spacer_sent);
            spacerReceived = itemView.findViewById(R.id.spacer_received);
        }

        /**
         * Binds a message to the view holder, configuring layout based on sender.
         *
         * @param message The message to display
         * @param isCurrentUser True if the message is from the current user, false otherwise
         */
        void bind(Message message, boolean isCurrentUser) {
            // Set message text
            textMessage.setText(message.getText() != null ? message.getText() : "");

            // Set timestamp
            Date timestamp = message.getTimestamp();
            if (timestamp != null) {
                String timeText = timeFormat.format(timestamp);
                textTimestamp.setText(timeText);
            } else {
                textTimestamp.setText("");
            }

            // Configure layout based on sender
            int marginSmall = (int) (8 * itemView.getContext().getResources().getDisplayMetrics().density);
            int marginLarge = (int) (64 * itemView.getContext().getResources().getDisplayMetrics().density);

            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) parentLayout.getLayoutParams();
            if (layoutParams == null) {
                layoutParams = new ViewGroup.MarginLayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
            }

            if (isCurrentUser) {
                // Sent message - align right, primary color
                layoutParams.setMarginStart(marginLarge);
                layoutParams.setMarginEnd(marginSmall);
                cardMessage.setCardBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.colorPrimary));
                textMessage.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.colorOnPrimary));
                textTimestamp.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.colorOnPrimary));
                textSenderName.setVisibility(View.GONE);
                textSenderNameAbove.setVisibility(View.GONE);
                spacerSent.setVisibility(View.VISIBLE);
                spacerReceived.setVisibility(View.GONE);
            } else {
                // Received message - align left, surface color
                layoutParams.setMarginStart(marginSmall);
                layoutParams.setMarginEnd(marginLarge);
                cardMessage.setCardBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.colorSurfaceVariant));
                textMessage.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.colorOnSurface));
                textTimestamp.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.colorOnSurfaceVariant));
                
                // Show sender name above the card
                String senderNameText = message.getSenderName();
                if (!TextUtils.isEmpty(senderNameText)) {
                    textSenderNameAbove.setText(senderNameText);
                    textSenderNameAbove.setVisibility(View.VISIBLE);
                } else {
                    textSenderNameAbove.setVisibility(View.GONE);
                }
                textSenderName.setVisibility(View.GONE);
                spacerSent.setVisibility(View.GONE);
                spacerReceived.setVisibility(View.VISIBLE);
            }

            parentLayout.setLayoutParams(layoutParams);
        }
    }
}

