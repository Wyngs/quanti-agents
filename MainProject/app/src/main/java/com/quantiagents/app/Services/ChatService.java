package com.quantiagents.app.Services;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.quantiagents.app.Constants.constant;
import com.quantiagents.app.Repository.ChatRepository;
import com.quantiagents.app.Repository.FireBaseRepository;
import com.quantiagents.app.Repository.MessageRepository;
import com.quantiagents.app.models.Chat;
import com.quantiagents.app.models.Message;
import com.quantiagents.app.models.Notification;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Service layer for Chat and Message operations.
 * <p>
 * Handles business logic for creating chats, adding members, and sending messages.
 * </p>
 */
public class ChatService {

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final UserService userService;
    private final NotificationService notificationService;

    /**
     * Constructor that initializes the ChatService with required dependencies.
     *
     * @param context The Android context used to initialize services
     */
    public ChatService(Context context) {
        FireBaseRepository fireBaseRepository = new FireBaseRepository();
        this.chatRepository = new ChatRepository(fireBaseRepository);
        this.messageRepository = new MessageRepository(fireBaseRepository);
        this.userService = new UserService(context);
        this.notificationService = new NotificationService(context);
    }

    /**
     * Creates a new group chat for an event when lottery is drawn.
     * The organizer is automatically added as a member.
     * Other members will be added when they accept the invitation.
     *
     * @param eventId    The event ID for which the chat is created.
     * @param eventName  The name of the event.
     * @param organizerId The user ID of the event organizer.
     * @param onSuccess  Callback receiving the created Chat ID.
     * @param onFailure  Callback receiving any error.
     */
    public void createEventChat(String eventId, String eventName, String organizerId,
                                OnSuccessListener<String> onSuccess,
                                OnFailureListener onFailure) {
        if (TextUtils.isEmpty(eventId)) {
            onFailure.onFailure(new IllegalArgumentException("Event ID is required"));
            return;
        }

        // Check if chat already exists for this event
        chatRepository.getChatByEventId(eventId,
                existingChat -> {
                    if (existingChat != null) {
                        // Chat already exists, ensure organizer is added
                        Log.d("ChatService", "Chat already exists for event: " + eventId);
                        if (!TextUtils.isEmpty(organizerId)) {
                            addUserToChat(existingChat.getChatId(), organizerId,
                                    v -> onSuccess.onSuccess(existingChat.getChatId()),
                                    e -> onSuccess.onSuccess(existingChat.getChatId()) // Continue even if add fails
                            );
                        } else {
                            onSuccess.onSuccess(existingChat.getChatId());
                        }
                    } else {
                        // Create new chat with organizer as initial member
                        Chat chat = new Chat();
                        chat.setEventId(eventId);
                        chat.setEventName(eventName != null ? eventName : "Event");
                        
                        // Add organizer to member list
                        List<String> initialMembers = new ArrayList<>();
                        if (!TextUtils.isEmpty(organizerId)) {
                            initialMembers.add(organizerId);
                        }
                        chat.setMemberIds(initialMembers);
                        chat.setCreatedAt(new Date());

                        chatRepository.saveChat(chat,
                                chatId -> {
                                    Log.d("ChatService", "Chat created for event: " + eventId + ", chatId: " + chatId + ", organizer added");
                                    onSuccess.onSuccess(chatId);
                                },
                                onFailure
                        );
                    }
                },
                onFailure
        );
    }

    /**
     * Adds a user to a chat when they accept the lottery invitation.
     *
     * @param chatId    The chat ID.
     * @param userId    The user ID to add.
     * @param onSuccess Callback invoked on success.
     * @param onFailure Callback invoked on failure.
     */
    public void addUserToChat(String chatId, String userId,
                              OnSuccessListener<Void> onSuccess,
                              OnFailureListener onFailure) {
        if (TextUtils.isEmpty(chatId) || TextUtils.isEmpty(userId)) {
            onFailure.onFailure(new IllegalArgumentException("Chat ID and User ID are required"));
            return;
        }

        chatRepository.getChatById(chatId,
                chat -> {
                    if (chat == null) {
                        onFailure.onFailure(new IllegalArgumentException("Chat not found"));
                        return;
                    }

                    List<String> members = chat.getMemberIds();
                    if (members == null) {
                        members = new ArrayList<>();
                    }

                    // Add user if not already a member
                    if (!members.contains(userId)) {
                        members.add(userId);
                        chat.setMemberIds(members);

                        chatRepository.updateChat(chat,
                                aVoid -> {
                                    Log.d("ChatService", "User " + userId + " added to chat " + chatId);
                                    onSuccess.onSuccess(aVoid);
                                },
                                onFailure
                        );
                    } else {
                        // User already in chat
                        Log.d("ChatService", "User " + userId + " already in chat " + chatId);
                        onSuccess.onSuccess(null);
                    }
                },
                onFailure
        );
    }

    /**
     * Gets a chat by event ID.
     */
    public void getChatByEventId(String eventId,
                                 OnSuccessListener<Chat> onSuccess,
                                 OnFailureListener onFailure) {
        chatRepository.getChatByEventId(eventId, onSuccess, onFailure);
    }

    /**
     * Gets a chat by chat ID.
     */
    public void getChatById(String chatId,
                           OnSuccessListener<Chat> onSuccess,
                           OnFailureListener onFailure) {
        chatRepository.getChatById(chatId, onSuccess, onFailure);
    }

    /**
     * Ensures a chat exists for an event and adds a user to it.
     * If the chat doesn't exist, it will be created with the organizer and the user.
     * This is useful for adding users who accepted invitations to events where
     * the chat might not have been created yet (e.g., older events).
     *
     * @param eventId    The event ID.
     * @param eventName  The event name.
     * @param organizerId The organizer ID.
     * @param userId     The user ID to add.
     * @param onSuccess  Callback invoked on success.
     * @param onFailure  Callback invoked on failure.
     */
    public void ensureChatExistsAndAddUser(String eventId, String eventName, String organizerId, String userId,
                                          OnSuccessListener<Void> onSuccess,
                                          OnFailureListener onFailure) {
        if (TextUtils.isEmpty(eventId) || TextUtils.isEmpty(userId)) {
            onFailure.onFailure(new IllegalArgumentException("Event ID and User ID are required"));
            return;
        }

        // First, try to get existing chat
        chatRepository.getChatByEventId(eventId,
                existingChat -> {
                    if (existingChat != null) {
                        // Chat exists, just add the user
                        addUserToChat(existingChat.getChatId(), userId, onSuccess, onFailure);
                    } else {
                        // Chat doesn't exist, create it with organizer and user
                        Chat chat = new Chat();
                        chat.setEventId(eventId);
                        chat.setEventName(eventName != null ? eventName : "Event");
                        
                        List<String> initialMembers = new ArrayList<>();
                        if (!TextUtils.isEmpty(organizerId)) {
                            initialMembers.add(organizerId);
                        }
                        if (!initialMembers.contains(userId)) {
                            initialMembers.add(userId);
                        }
                        chat.setMemberIds(initialMembers);
                        chat.setCreatedAt(new Date());

                        chatRepository.saveChat(chat,
                                chatId -> {
                                    Log.d("ChatService", "Chat created and user added for event: " + eventId);
                                    onSuccess.onSuccess(null);
                                },
                                onFailure
                        );
                    }
                },
                onFailure
        );
    }

    /**
     * Synchronously gets a chat by event ID.
     */
    public Chat getChatByEventId(String eventId) {
        return chatRepository.getChatByEventId(eventId);
    }

    /**
     * Gets all chats for a user (where user is a member).
     */
    public List<Chat> getChatsByUserId(String userId) {
        return chatRepository.getChatsByUserId(userId);
    }

    /**
     * Sends a message to a chat.
     *
     * @param chatId    The chat ID.
     * @param senderId  The user ID of the sender.
     * @param text      The message text.
     * @param onSuccess Callback receiving the message ID.
     * @param onFailure Callback receiving any error.
     */
    public void sendMessage(String chatId, String senderId, String text,
                            OnSuccessListener<String> onSuccess,
                            OnFailureListener onFailure) {
        if (TextUtils.isEmpty(chatId) || TextUtils.isEmpty(senderId) || TextUtils.isEmpty(text)) {
            onFailure.onFailure(new IllegalArgumentException("Chat ID, Sender ID, and message text are required"));
            return;
        }

        // Get sender name asynchronously
        userService.getUserById(senderId,
                sender -> {
                    String senderName = "User";
                    if (sender != null) {
                        if (!TextUtils.isEmpty(sender.getName())) {
                            senderName = sender.getName();
                        } else if (!TextUtils.isEmpty(sender.getUsername())) {
                            senderName = sender.getUsername();
                        }
                    }
                    
                    // Store in final variable for use in nested lambda
                    final String finalSenderName = senderName;
                    
                    Message message = new Message();
                    message.setChatId(chatId);
                    message.setSenderId(senderId);
                    message.setSenderName(finalSenderName);
                    message.setText(text);
                    message.setTimestamp(new Date());

                    messageRepository.saveMessage(message,
                            messageId -> {
                                // Update chat's last message time
                                chatRepository.getChatById(chatId,
                                        chat -> {
                                            if (chat != null) {
                                                chat.setLastMessageTime(new Date());
                                                chatRepository.updateChat(chat, v -> {}, err -> {});
                                                
                                                // Send notifications to all chat members (except sender)
                                                sendMessageNotifications(chat, senderId, finalSenderName, text);
                                            }
                                        },
                                        err -> {}
                                );

                                Log.d("ChatService", "Message sent: " + messageId);
                                onSuccess.onSuccess(messageId);
                            },
                            onFailure
                    );
                },
                userError -> {
                    // If user lookup fails, use default name and continue
                    Log.w("ChatService", "Could not get sender name, using default", userError);
                    String senderName = "User";
                    
                    Message message = new Message();
                    message.setChatId(chatId);
                    message.setSenderId(senderId);
                    message.setSenderName(senderName);
                    message.setText(text);
                    message.setTimestamp(new Date());

                    messageRepository.saveMessage(message,
                            messageId -> {
                                // Update chat's last message time
                                chatRepository.getChatById(chatId,
                                        chat -> {
                                            if (chat != null) {
                                                chat.setLastMessageTime(new Date());
                                                chatRepository.updateChat(chat, v -> {}, err -> {});
                                                
                                                // Send notifications to all chat members (except sender)
                                                sendMessageNotifications(chat, senderId, senderName, text);
                                            }
                                        },
                                        err -> {}
                                );

                                Log.d("ChatService", "Message sent: " + messageId);
                                onSuccess.onSuccess(messageId);
                            },
                            onFailure
                    );
                }
        );
    }

    /**
     * Sends notifications to all chat members when a new message is sent.
     * Excludes the sender from receiving a notification.
     *
     * @param chat       The chat object.
     * @param senderId   The ID of the user who sent the message.
     * @param senderName The name of the user who sent the message.
     * @param messageText The text of the message.
     */
    private void sendMessageNotifications(Chat chat, String senderId, String senderName, String messageText) {
        if (chat == null || chat.getMemberIds() == null || chat.getMemberIds().isEmpty()) {
            return;
        }

        String eventId = chat.getEventId();
        String eventName = chat.getEventName() != null ? chat.getEventName() : "Event";
        
        // Convert event ID to int for notification
        int eventIdInt = eventId != null ? Math.abs(eventId.hashCode()) : 0;
        int senderIdInt = senderId != null ? Math.abs(senderId.hashCode()) : 0;

        // Prepare message preview (truncate if too long)
        String messagePreview = messageText != null ? messageText : "";
        if (messagePreview.length() > 100) {
            messagePreview = messagePreview.substring(0, 97) + "...";
        }

        // Send Firestore notifications to all members (except sender)
        for (String memberId : chat.getMemberIds()) {
            if (memberId == null || memberId.equals(senderId)) {
                continue; // Skip sender
            }

            int recipientIdInt = Math.abs(memberId.hashCode());

            String status = "New Message";
            String details = senderName + " sent a message in " + eventName + ": " + messagePreview;

            Notification notification = new Notification(
                    0, // Auto-generate ID
                    constant.NotificationType.REMINDER,
                    recipientIdInt,
                    senderIdInt,
                    eventIdInt,
                    status,
                    details
            );

            notificationService.saveNotification(notification,
                    aVoid -> Log.d("ChatService", "Firestore notification sent to user: " + memberId),
                    e -> Log.w("ChatService", "Failed to send Firestore notification to user: " + memberId, e)
            );
        }
    }

    /**
     * Gets all messages for a chat.
     *
     * @param chatId    The chat ID.
     * @param onSuccess Callback receiving the list of messages.
     * @param onFailure Callback receiving any error.
     */
    public void getMessages(String chatId,
                            OnSuccessListener<List<Message>> onSuccess,
                            OnFailureListener onFailure) {
        messageRepository.getMessagesByChatId(chatId, onSuccess, onFailure);
    }

    /**
     * Synchronously gets all messages for a chat.
     */
    public List<Message> getMessages(String chatId) {
        return messageRepository.getMessagesByChatId(chatId);
    }

    /**
     * Sets up a real-time listener for messages in a chat.
     * The listener will be called whenever messages are added, modified, or removed.
     * 
     * @param chatId    The chat ID to listen to.
     * @param listener  Callback that receives the QuerySnapshot whenever messages change.
     *                 The listener should handle the snapshot and extract messages.
     * @return ListenerRegistration that can be used to stop listening (call remove() to detach).
     */
    public ListenerRegistration listenToMessages(String chatId, EventListener<QuerySnapshot> listener) {
        if (TextUtils.isEmpty(chatId)) {
            Log.w("ChatService", "listenToMessages called with empty chatId");
            return null;
        }
        return messageRepository.listenToMessages(chatId, listener);
    }

    /**
     * Marks all messages in a chat as read for a user.
     * Updates the chat's lastReadTimestamps to the current time.
     *
     * @param chatId    The chat ID.
     * @param userId    The user ID.
     * @param onSuccess Callback invoked on success.
     * @param onFailure Callback invoked on failure.
     */
    public void markMessagesAsRead(String chatId, String userId,
                                    OnSuccessListener<Void> onSuccess,
                                    OnFailureListener onFailure) {
        if (TextUtils.isEmpty(chatId) || TextUtils.isEmpty(userId)) {
            onFailure.onFailure(new IllegalArgumentException("Chat ID and User ID are required"));
            return;
        }

        chatRepository.getChatById(chatId,
                chat -> {
                    if (chat == null) {
                        onFailure.onFailure(new IllegalArgumentException("Chat not found"));
                        return;
                    }

                    // Update the last read timestamp to now
                    Date now = new Date();
                    chat.setLastReadTimestamp(userId, now);

                    // Update the chat in Firestore
                    chatRepository.updateChat(chat,
                            aVoid -> {
                                Log.d("ChatService", "Messages marked as read for user: " + userId + " in chat: " + chatId);
                                onSuccess.onSuccess(aVoid);
                            },
                            onFailure
                    );
                },
                onFailure
        );
    }

    /**
     * Calculates the number of unread messages for a user in a chat.
     *
     * @param chatId The chat ID.
     * @param userId The user ID.
     * @return The number of unread messages.
     */
    public int getUnreadCount(String chatId, String userId) {
        if (TextUtils.isEmpty(chatId) || TextUtils.isEmpty(userId)) {
            return 0;
        }

        Chat chat = chatRepository.getChatById(chatId);
        if (chat == null) {
            return 0;
        }

        Date lastRead = chat.getLastReadTimestamp(userId);
        List<Message> allMessages = getMessages(chatId);
        if (allMessages == null || allMessages.isEmpty()) {
            return 0;
        }

        if (lastRead == null) {
            // User has never read messages, count all messages except their own
            int unreadCount = 0;
            for (Message message : allMessages) {
                if (!userId.equals(message.getSenderId())) {
                    unreadCount++;
                }
            }
            return unreadCount;
        }

        // Count messages after the last read timestamp
        int unreadCount = 0;
        for (Message message : allMessages) {
            if (message.getTimestamp() != null && message.getTimestamp().after(lastRead)) {
                // Don't count messages sent by the user themselves
                if (!userId.equals(message.getSenderId())) {
                    unreadCount++;
                }
            }
        }

        return unreadCount;
    }

    /**
     * Gets the total number of unread messages across all chats for a user.
     *
     * @param userId The user ID.
     * @return The total number of unread messages across all chats.
     */
    public int getTotalUnreadMessageCount(String userId) {
        if (TextUtils.isEmpty(userId)) {
            return 0;
        }

        List<Chat> userChats = getChatsByUserId(userId);
        if (userChats == null || userChats.isEmpty()) {
            return 0;
        }

        int totalUnread = 0;
        for (Chat chat : userChats) {
            if (chat != null && chat.getChatId() != null) {
                totalUnread += getUnreadCount(chat.getChatId(), userId);
            }
        }

        return totalUnread;
    }
}

