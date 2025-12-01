package com.quantiagents.app.ui.chat;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.quantiagents.app.App;
import com.quantiagents.app.R;
import com.quantiagents.app.Services.BadgeService;
import com.quantiagents.app.Services.ChatService;
import com.quantiagents.app.Services.UserService;
import com.quantiagents.app.ui.main.MainActivity;
import com.quantiagents.app.models.Chat;
import com.quantiagents.app.models.Message;
import com.quantiagents.app.models.User;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fragment for displaying and managing group chat for an event.
 * Shows messages and allows users to send new messages.
 */
public class ChatFragment extends Fragment {

    private static final String ARG_EVENT_ID = "event_id";
    private static final String ARG_CHAT_ID = "chat_id";

    private String eventId;
    private String chatId;
    private ChatService chatService;
    private UserService userService;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private RecyclerView recyclerView;
    private TextInputEditText editMessage;
    private MaterialButton buttonSend;
    private ProgressBar progressBar;
    private View emptyState;
    private TextView textChatTitle;
    private TextView textChatSubtitle;
    private View headerBackButton;

    private MessageAdapter adapter;
    private List<Message> messages = new ArrayList<>();
    private User currentUser;
    private Chat chat;
    private ListenerRegistration messagesListener; // Real-time listener for messages

    public static ChatFragment newInstance(String eventId) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        fragment.setArguments(args);
        return fragment;
    }

    public static ChatFragment newInstanceWithChatId(String chatId) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CHAT_ID, chatId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            eventId = args.getString(ARG_EVENT_ID);
            chatId = args.getString(ARG_CHAT_ID);
        }

        App app = (App) requireActivity().getApplication();
        chatService = app.locator().chatService();
        userService = app.locator().userService();

        bindViews(view);
        setupRecyclerView();
        setupClickListeners();

        loadUser();
    }

    private void bindViews(@NonNull View view) {
        recyclerView = view.findViewById(R.id.recycler_messages);
        editMessage = view.findViewById(R.id.edit_message);
        buttonSend = view.findViewById(R.id.button_send);
        progressBar = view.findViewById(R.id.progress_loading);
        emptyState = view.findViewById(R.id.empty_state);
        textChatTitle = view.findViewById(R.id.text_chat_title);
        textChatSubtitle = view.findViewById(R.id.text_chat_subtitle);
        headerBackButton = view.findViewById(R.id.header_back_button);
    }

    private void setupRecyclerView() {
        adapter = new MessageAdapter(messages, currentUser != null ? currentUser.getUserId() : null);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupClickListeners() {
        headerBackButton.setOnClickListener(v -> {
            if (isAdded()) {
                requireActivity().getOnBackPressedDispatcher().onBackPressed();
            }
        });

        buttonSend.setOnClickListener(v -> sendMessage());

        editMessage.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });
    }

    private void loadUser() {
        userService.getCurrentUser(
                user -> {
                    currentUser = user;
                    if (user == null) {
                        Toast.makeText(getContext(), "User not signed in", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    loadChat();
                },
                e -> {
                    Toast.makeText(getContext(), "Failed to load user", Toast.LENGTH_SHORT).show();
                }
        );
    }

    private void loadChat() {
        setLoading(true);

        if (!TextUtils.isEmpty(chatId)) {
            // Load by chat ID
            chatService.getChatById(chatId,
                    loadedChat -> {
                        if (loadedChat != null) {
                            this.chat = loadedChat;
                            this.eventId = loadedChat.getEventId();
                            updateUI();
                            loadMessages();
                        } else {
                            setLoading(false);
                            Toast.makeText(getContext(), "Chat not found", Toast.LENGTH_SHORT).show();
                        }
                    },
                    e -> {
                        setLoading(false);
                        Toast.makeText(getContext(), "Failed to load chat", Toast.LENGTH_SHORT).show();
                    }
            );
        } else if (!TextUtils.isEmpty(eventId)) {
            // Load by event ID
            chatService.getChatByEventId(eventId,
                    loadedChat -> {
                        this.chat = loadedChat;
                        updateUI();
                        if (loadedChat != null) {
                            this.chatId = loadedChat.getChatId();
                            loadMessages();
                        } else {
                            setLoading(false);
                            emptyState.setVisibility(View.VISIBLE);
                            Toast.makeText(getContext(), "No chat found for this event", Toast.LENGTH_SHORT).show();
                        }
                    },
                    e -> {
                        setLoading(false);
                        Toast.makeText(getContext(), "Failed to load chat", Toast.LENGTH_SHORT).show();
                    }
            );
        } else {
            setLoading(false);
            Toast.makeText(getContext(), "Event ID or Chat ID required", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateUI() {
        if (chat != null) {
            String title = chat.getEventName() != null ? chat.getEventName() : "Event Chat";
            textChatTitle.setText(title);
            textChatSubtitle.setText("Group chat");
        }
    }

    private void loadMessages() {
        if (TextUtils.isEmpty(chatId)) {
            setLoading(false);
            return;
        }

        // Remove existing listener if any
        if (messagesListener != null) {
            messagesListener.remove();
            messagesListener = null;
        }

        setLoading(true);

        // Set up real-time listener for messages
        messagesListener = chatService.listenToMessages(chatId, (querySnapshot, e) -> {
                if (e != null) {
                    Log.e("ChatFragment", "Error listening to messages", e);
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            setLoading(false);
                            Toast.makeText(getContext(), "Error loading messages", Toast.LENGTH_SHORT).show();
                        });
                    }
                    return;
                }

                if (querySnapshot != null) {
                    // Update messages list with real-time data
                    messages.clear();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        Message message = document.toObject(Message.class);
                        if (message != null) {
                            if (message.getMessageId() == null || message.getMessageId().trim().isEmpty()) {
                                message.setMessageId(document.getId());
                            }
                            messages.add(message);
                        }
                    }

                    // Update UI on main thread
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            adapter.notifyDataSetChanged();
                            setLoading(false);
                            updateEmptyState();
                            scrollToBottom();

                            // Mark messages as read when user views the chat
                            if (currentUser != null && !TextUtils.isEmpty(currentUser.getUserId())) {
                                chatService.markMessagesAsRead(chatId, currentUser.getUserId(),
                                        aVoid -> {
                                            Log.d("ChatFragment", "Messages marked as read");
                                            // Update app icon badge (unread chat messages affect badge)
                                            BadgeService badgeService = new BadgeService(requireContext());
                                            badgeService.updateBadgeCount();

                                            // Update navigation menu badges
                                            if (requireActivity() instanceof MainActivity) {
                                                ((MainActivity) requireActivity()).updateNavigationMenuBadges();
                                            }
                                        },
                                        err -> Log.w("ChatFragment", "Failed to mark messages as read", err)
                                );
                            }
                        });
                    }
                }
        });
    }

    private void sendMessage() {
        String text = editMessage.getText() != null ? editMessage.getText().toString().trim() : "";
        if (TextUtils.isEmpty(text)) {
            return;
        }

        if (TextUtils.isEmpty(chatId)) {
            Toast.makeText(getContext(), "Chat not available", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUser == null || TextUtils.isEmpty(currentUser.getUserId())) {
            Toast.makeText(getContext(), "User not signed in", Toast.LENGTH_SHORT).show();
            return;
        }

        buttonSend.setEnabled(false);
        editMessage.setText("");

        chatService.sendMessage(chatId, currentUser.getUserId(), text,
                messageId -> {
                    buttonSend.setEnabled(true);
                    // No need to reload - real-time listener will automatically update the UI
                    Log.d("ChatFragment", "Message sent successfully");
                },
                e -> {
                    buttonSend.setEnabled(true);
                    Log.e("ChatFragment", "Failed to send message", e);
                    Toast.makeText(getContext(), "Failed to send message", Toast.LENGTH_SHORT).show();
                }
        );
    }

    private void updateEmptyState() {
        if (messages.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void scrollToBottom() {
        if (recyclerView != null && adapter != null && adapter.getItemCount() > 0) {
            recyclerView.post(() -> recyclerView.smoothScrollToPosition(adapter.getItemCount() - 1));
        }
    }

    private void setLoading(boolean loading) {
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (buttonSend != null) {
            buttonSend.setEnabled(!loading);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Detach listener when fragment is paused to save resources
        if (messagesListener != null) {
            messagesListener.remove();
            messagesListener = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Re-attach listener when fragment resumes
        if (!TextUtils.isEmpty(chatId) && currentUser != null) {
            loadMessages();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Clean up listener
        if (messagesListener != null) {
            messagesListener.remove();
            messagesListener = null;
        }
        executor.shutdownNow();
    }
}

