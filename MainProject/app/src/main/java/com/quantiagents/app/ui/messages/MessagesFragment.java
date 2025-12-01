package com.quantiagents.app.ui.messages;

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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.quantiagents.app.App;
import com.quantiagents.app.Constants.constant;
import com.quantiagents.app.R;
import com.quantiagents.app.Services.BadgeService;
import com.quantiagents.app.Services.ChatService;
import com.quantiagents.app.Services.EventService;
import com.quantiagents.app.Services.RegistrationHistoryService;
import com.quantiagents.app.Services.UserService;
import com.quantiagents.app.ui.main.MainActivity;
import com.quantiagents.app.models.Chat;
import com.quantiagents.app.models.Event;
import com.quantiagents.app.models.RegistrationHistory;
import com.quantiagents.app.models.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Fragment that displays all group chats the user is part of.
 * Users can tap on a chat to open it.
 */
public class MessagesFragment extends Fragment implements ChatListAdapter.OnChatClickListener {

    private ChatService chatService;
    private UserService userService;
    private EventService eventService;
    private RegistrationHistoryService registrationHistoryService;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private View emptyState;
    private ChatListAdapter adapter;
    private List<Chat> chats = new ArrayList<>();
    private User currentUser;

    public static MessagesFragment newInstance() {
        return new MessagesFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_messages, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        App app = (App) requireActivity().getApplication();
        chatService = app.locator().chatService();
        userService = app.locator().userService();
        eventService = app.locator().eventService();
        registrationHistoryService = app.locator().registrationHistoryService();

        bindViews(view);
        setupRecyclerView();
        setupSwipeRefresh();

        loadUser();
    }

    private void bindViews(@NonNull View view) {
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        recyclerView = view.findViewById(R.id.recycler_chats);
        progressBar = view.findViewById(R.id.progress_loading);
        emptyState = view.findViewById(R.id.empty_state);
    }

    private void setupRecyclerView() {
        adapter = new ChatListAdapter(chats, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(this::loadChats);
    }

    private void loadUser() {
        userService.getCurrentUser(
                user -> {
                    currentUser = user;
                    if (user == null) {
                        Toast.makeText(getContext(), "User not signed in", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    loadChats();
                },
                e -> {
                    Toast.makeText(getContext(), "Failed to load user", Toast.LENGTH_SHORT).show();
                }
        );
    }

    private void loadChats() {
        if (currentUser == null || TextUtils.isEmpty(currentUser.getUserId())) {
            setLoading(false);
            return;
        }

        setLoading(true);
        executor.execute(() -> {
            try {
                // First, sync confirmed users and organizers with chats
                // Wait for sync to complete before loading chats
                syncUsersAndOrganizersWithChats(() -> {
                    // After sync completes, wait a brief moment for Firestore to propagate writes
                    // Then load all chats for the user
                    try {
                        Thread.sleep(500); // Small delay to ensure Firestore writes are propagated
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    
                    try {
                        List<Chat> userChats = chatService.getChatsByUserId(currentUser.getUserId());
                        
                        Log.d("MessagesFragment", "Found " + userChats.size() + " chats for user: " + currentUser.getUserId());
                        
                        // Calculate unread counts for each chat
                        Map<String, Integer> unreadCounts = new HashMap<>();
                        for (Chat chat : userChats) {
                            if (chat != null && chat.getChatId() != null) {
                                int unreadCount = chatService.getUnreadCount(chat.getChatId(), currentUser.getUserId());
                                unreadCounts.put(chat.getChatId(), unreadCount);
                                Log.d("MessagesFragment", "Chat: " + chat.getChatId() + " - " + chat.getEventName() + " - Unread: " + unreadCount);
                            }
                        }
                        
                        // Sort by last message time (most recent first)
                        userChats.sort((c1, c2) -> {
                            if (c1.getLastMessageTime() == null && c2.getLastMessageTime() == null) {
                                return 0;
                            }
                            if (c1.getLastMessageTime() == null) {
                                return 1;
                            }
                            if (c2.getLastMessageTime() == null) {
                                return -1;
                            }
                            return c2.getLastMessageTime().compareTo(c1.getLastMessageTime());
                        });

                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> {
                                chats.clear();
                                chats.addAll(userChats);
                                adapter.setUnreadCounts(unreadCounts);
                                adapter.notifyDataSetChanged();
                                updateEmptyState();
                                setLoading(false);
                                Log.d("MessagesFragment", "Displaying " + chats.size() + " chats");
                                
                                // Update app icon badge with unread messages
                                BadgeService badgeService = new BadgeService(requireContext());
                                badgeService.updateBadgeCount();
                                
                                // Update navigation menu badges
                                if (requireActivity() instanceof MainActivity) {
                                    ((MainActivity) requireActivity()).updateNavigationMenuBadges();
                                }
                            });
                        }
                    } catch (Exception e) {
                        Log.e("MessagesFragment", "Failed to load chats", e);
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> {
                                setLoading(false);
                                Toast.makeText(getContext(), "Failed to load chats", Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
                });
            } catch (Exception e) {
                Log.e("MessagesFragment", "Failed to sync or load chats", e);
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        setLoading(false);
                        Toast.makeText(getContext(), "Failed to load chats", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    /**
     * Syncs confirmed users and organizers with chats - ensures all confirmed users and organizers
     * are added to their event chats. This handles cases where users were confirmed or events were
     * created before chats were created or before they were added.
     * @param onComplete Callback invoked when all sync operations complete.
     */
    private void syncUsersAndOrganizersWithChats(Runnable onComplete) {
        // First sync confirmed users, then sync organizers
        syncConfirmedUsersWithChats(() -> {
            syncOrganizersWithChats(onComplete);
        });
    }

    /**
     * Syncs confirmed users with chats - ensures all confirmed users are added to their event chats.
     * This handles cases where users were confirmed before chats were created or before they were added.
     * @param onComplete Callback invoked when all sync operations complete.
     */
    private void syncConfirmedUsersWithChats(Runnable onComplete) {
        try {
            // Get all registration histories for the current user
            List<RegistrationHistory> registrations = registrationHistoryService.getRegistrationHistoriesByUserId(currentUser.getUserId());
            
            if (registrations == null || registrations.isEmpty()) {
                // No registrations, complete immediately
                if (onComplete != null) {
                    onComplete.run();
                }
                return;
            }

            // Find all confirmed registrations that need syncing
            List<RegistrationHistory> toSync = new ArrayList<>();
            for (RegistrationHistory reg : registrations) {
                if (reg == null || reg.getEventRegistrationStatus() != constant.EventRegistrationStatus.CONFIRMED) {
                    continue;
                }

                String eventId = reg.getEventId();
                if (eventId == null || eventId.trim().isEmpty()) {
                    continue;
                }

                // Check if user is already in the chat
                Chat existingChat = chatService.getChatByEventId(eventId);
                if (existingChat != null && existingChat.getMemberIds() != null) {
                    if (existingChat.getMemberIds().contains(currentUser.getUserId())) {
                        // User already in chat, skip
                        continue;
                    }
                }

                // Need to sync this one
                toSync.add(reg);
            }

            if (toSync.isEmpty()) {
                // Nothing to sync, complete immediately
                if (onComplete != null) {
                    onComplete.run();
                }
                return;
            }

            // Use CountDownLatch to wait for all async operations
            CountDownLatch latch = new CountDownLatch(toSync.size());
            final int[] completed = {0};
            final int[] failed = {0};

            for (RegistrationHistory reg : toSync) {
                String eventId = reg.getEventId();
                
                // Get the event to find organizer and name
                Event event = eventService.getEventById(eventId);
                if (event == null) {
                    latch.countDown();
                    continue;
                }

                String eventName = event.getTitle() != null ? event.getTitle() : "Event";
                String organizerId = event.getOrganizerId();
                
                chatService.ensureChatExistsAndAddUser(
                        eventId,
                        eventName,
                        organizerId,
                        currentUser.getUserId(),
                        aVoid -> {
                            completed[0]++;
                            Log.d("MessagesFragment", "Synced user to chat for event: " + eventId);
                            latch.countDown();
                        },
                        e -> {
                            failed[0]++;
                            Log.w("MessagesFragment", "Failed to sync user to chat for event: " + eventId, e);
                            latch.countDown();
                        }
                );
            }

            // Wait for all sync operations to complete (with timeout)
            try {
                boolean finished = latch.await(10, TimeUnit.SECONDS);
                if (!finished) {
                    Log.w("MessagesFragment", "Sync timeout - some operations may not have completed");
                }
                Log.d("MessagesFragment", "Sync completed: " + completed[0] + " succeeded, " + failed[0] + " failed");
            } catch (InterruptedException e) {
                Log.e("MessagesFragment", "Sync interrupted", e);
                Thread.currentThread().interrupt();
            }

            // Invoke completion callback
            if (onComplete != null) {
                onComplete.run();
            }
        } catch (Exception e) {
            Log.e("MessagesFragment", "Error syncing confirmed users with chats", e);
            // Still invoke completion callback even on error
            if (onComplete != null) {
                onComplete.run();
            }
        }
    }

    /**
     * Syncs organizers with chats - ensures all organizers are added to their event chats.
     * This handles cases where events were created before chats were created or before organizers were added.
     * @param onComplete Callback invoked when all sync operations complete.
     */
    private void syncOrganizersWithChats(Runnable onComplete) {
        try {
            // Get all events
            List<Event> allEvents = eventService.getAllEvents();
            
            if (allEvents == null || allEvents.isEmpty()) {
                // No events, complete immediately
                if (onComplete != null) {
                    onComplete.run();
                }
                return;
            }

            // Find all events where current user is the organizer
            List<Event> organizerEvents = new ArrayList<>();
            for (Event event : allEvents) {
                if (event != null && event.getOrganizerId() != null && 
                    event.getOrganizerId().equals(currentUser.getUserId())) {
                    organizerEvents.add(event);
                }
            }

            if (organizerEvents.isEmpty()) {
                // User is not an organizer of any events, complete immediately
                if (onComplete != null) {
                    onComplete.run();
                }
                return;
            }

            // Check which events need syncing (organizer not in chat)
            List<Event> toSync = new ArrayList<>();
            for (Event event : organizerEvents) {
                String eventId = event.getEventId();
                if (eventId == null || eventId.trim().isEmpty()) {
                    continue;
                }

                // Check if organizer is already in the chat
                Chat existingChat = chatService.getChatByEventId(eventId);
                if (existingChat != null && existingChat.getMemberIds() != null) {
                    if (existingChat.getMemberIds().contains(currentUser.getUserId())) {
                        // Organizer already in chat, skip
                        continue;
                    }
                }

                // Need to sync this one
                toSync.add(event);
            }

            if (toSync.isEmpty()) {
                // Nothing to sync, complete immediately
                if (onComplete != null) {
                    onComplete.run();
                }
                return;
            }

            // Use CountDownLatch to wait for all async operations
            CountDownLatch latch = new CountDownLatch(toSync.size());
            final int[] completed = {0};
            final int[] failed = {0};

            for (Event event : toSync) {
                String eventId = event.getEventId();
                String eventName = event.getTitle() != null ? event.getTitle() : "Event";
                String organizerId = event.getOrganizerId();
                
                chatService.ensureChatExistsAndAddUser(
                        eventId,
                        eventName,
                        organizerId,
                        currentUser.getUserId(),
                        aVoid -> {
                            completed[0]++;
                            Log.d("MessagesFragment", "Synced organizer to chat for event: " + eventId);
                            latch.countDown();
                        },
                        e -> {
                            failed[0]++;
                            Log.w("MessagesFragment", "Failed to sync organizer to chat for event: " + eventId, e);
                            latch.countDown();
                        }
                );
            }

            // Wait for all sync operations to complete (with timeout)
            try {
                boolean finished = latch.await(10, TimeUnit.SECONDS);
                if (!finished) {
                    Log.w("MessagesFragment", "Organizer sync timeout - some operations may not have completed");
                }
                Log.d("MessagesFragment", "Organizer sync completed: " + completed[0] + " succeeded, " + failed[0] + " failed");
            } catch (InterruptedException e) {
                Log.e("MessagesFragment", "Organizer sync interrupted", e);
                Thread.currentThread().interrupt();
            }

            // Invoke completion callback
            if (onComplete != null) {
                onComplete.run();
            }
        } catch (Exception e) {
            Log.e("MessagesFragment", "Error syncing organizers with chats", e);
            // Still invoke completion callback even on error
            if (onComplete != null) {
                onComplete.run();
            }
        }
    }

    private void updateEmptyState() {
        if (chats.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void setLoading(boolean loading) {
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(loading);
        }
    }

    @Override
    public void onChatClick(Chat chat) {
        if (chat == null || TextUtils.isEmpty(chat.getChatId())) {
            Toast.makeText(getContext(), "Chat not available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Open the chat fragment
        Fragment chatFragment = com.quantiagents.app.ui.chat.ChatFragment.newInstanceWithChatId(chat.getChatId());
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_container, chatFragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (currentUser != null) {
            loadChats();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}

