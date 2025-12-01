package com.quantiagents.app.ui.messages;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.quantiagents.app.R;
import com.quantiagents.app.models.Chat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Adapter for displaying a list of chats in a RecyclerView.
 */
public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatViewHolder> {

    private final List<Chat> chats;
    private final OnChatClickListener listener;
    private Map<String, Integer> unreadCounts = new HashMap<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    /**
     * Interface for handling chat click events.
     */
    public interface OnChatClickListener {
        /**
         * Called when a chat item is clicked.
         *
         * @param chat The chat that was clicked
         */
        void onChatClick(Chat chat);
    }

    /**
     * Constructor that initializes the adapter with chats and click listener.
     *
     * @param chats The list of chats to display
     * @param listener The callback interface for handling chat clicks
     */
    public ChatListAdapter(List<Chat> chats, OnChatClickListener listener) {
        this.chats = chats;
        this.listener = listener;
    }

    /**
     * Sets the unread message counts for chats.
     *
     * @param unreadCounts Map of chat ID to unread message count
     */
    public void setUnreadCounts(Map<String, Integer> unreadCounts) {
        this.unreadCounts = unreadCounts != null ? unreadCounts : new HashMap<>();
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_list, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Chat chat = chats.get(position);
        if (chat == null) return;
        int unreadCount = unreadCounts.getOrDefault(chat.getChatId(), 0);
        holder.bind(chat, unreadCount, listener);
    }

    @Override
    public int getItemCount() {
        return chats.size();
    }

    class ChatViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardChat;
        private final TextView textEventName;
        private final TextView textLastMessage;
        private final TextView textMemberCount;
        private final TextView textUnreadBadge;

        ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            cardChat = itemView.findViewById(R.id.card_chat);
            textEventName = itemView.findViewById(R.id.text_event_name);
            textLastMessage = itemView.findViewById(R.id.text_last_message);
            textMemberCount = itemView.findViewById(R.id.text_member_count);
            textUnreadBadge = itemView.findViewById(R.id.text_unread_badge);
        }

        /**
         * Binds a chat to the view holder, displaying event name, last message time, member count, and unread badge.
         *
         * @param chat The chat to display
         * @param unreadCount The number of unread messages in this chat
         * @param listener The click listener for handling chat selection
         */
        void bind(Chat chat, int unreadCount, OnChatClickListener listener) {
            // Set event name
            String eventName = chat.getEventName();
            if (TextUtils.isEmpty(eventName)) {
                eventName = "Event Chat";
            }
            textEventName.setText(eventName);

            // Set last message time
            Date lastMessageTime = chat.getLastMessageTime();
            if (lastMessageTime != null) {
                String timeText = formatLastMessageTime(lastMessageTime);
                textLastMessage.setText(timeText);
                textLastMessage.setVisibility(View.VISIBLE);
            } else {
                textLastMessage.setText("No messages yet");
                textLastMessage.setVisibility(View.VISIBLE);
            }

            // Set member count
            if (chat.getMemberIds() != null && !chat.getMemberIds().isEmpty()) {
                int memberCount = chat.getMemberIds().size();
                textMemberCount.setText(memberCount + " member" + (memberCount != 1 ? "s" : ""));
                textMemberCount.setVisibility(View.VISIBLE);
            } else {
                textMemberCount.setVisibility(View.GONE);
            }

            // Set unread badge
            if (unreadCount > 0 && textUnreadBadge != null) {
                textUnreadBadge.setText(String.valueOf(unreadCount));
                textUnreadBadge.setVisibility(View.VISIBLE);
            } else if (textUnreadBadge != null) {
                textUnreadBadge.setVisibility(View.GONE);
            }

            // Set click listener
            cardChat.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onChatClick(chat);
                }
            });
        }

        private String formatLastMessageTime(Date date) {
            Date now = new Date();
            long diff = now.getTime() - date.getTime();
            long days = diff / (24 * 60 * 60 * 1000);

            if (days == 0) {
                // Today - show time
                return "Today " + timeFormat.format(date);
            } else if (days == 1) {
                // Yesterday
                return "Yesterday " + timeFormat.format(date);
            } else if (days < 7) {
                // This week - show day and time
                return dateFormat.format(date) + " " + timeFormat.format(date);
            } else {
                // Older - show date
                return dateFormat.format(date);
            }
        }
    }
}

