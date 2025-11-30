package com.quantiagents.app.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.badge.ExperimentalBadgeUtils;
import com.quantiagents.app.App;
import com.quantiagents.app.R;
import com.quantiagents.app.Services.EventService;
import com.quantiagents.app.Services.NotificationService;
import com.quantiagents.app.Services.UserService;
import com.quantiagents.app.models.Event;
import com.quantiagents.app.models.Notification;
import com.quantiagents.app.models.User;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ExperimentalBadgeUtils
public class NotificationCenterFragment extends Fragment {

    private NotificationService notificationService;
    private UserService userService;
    private EventService eventService;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private RecyclerView recyclerView;
    private TextView emptyStateTitle;
    private TextView emptyStateMessage;
    private View emptyStateView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView unreadBadge;
    private View headerView;

    private NotificationCenterAdapter adapter;
    private List<Notification> allNotifications = new ArrayList<>();
    private List<Event> allEvents = new ArrayList<>();
    private User currentUser;

    public static NotificationCenterFragment newInstance() {
        return new NotificationCenterFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notification_center, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        App app = (App) requireActivity().getApplication();
        notificationService = app.locator().notificationService();
        userService = app.locator().userService();
        eventService = app.locator().eventService();

        // Initialize views
        recyclerView = view.findViewById(R.id.recycler_notifications);
        emptyStateView = view.findViewById(R.id.empty_state);
        emptyStateTitle = view.findViewById(R.id.empty_state_title);
        emptyStateMessage = view.findViewById(R.id.empty_state_message);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        unreadBadge = view.findViewById(R.id.unread_badge);
        headerView = view.findViewById(R.id.header_view);

        // Setup RecyclerView
        adapter = new NotificationCenterAdapter(new ArrayList<>(), this::onMarkAsRead);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        // Setup swipe to refresh
        swipeRefreshLayout.setOnRefreshListener(this::loadNotifications);

        // Load data
        loadNotifications();
    }

    private void loadNotifications() {
        // First get current user
        userService.getCurrentUser(
                user -> {
                    if (user == null) {
                        showError("Please log in to view notifications");
                        swipeRefreshLayout.setRefreshing(false);
                        return;
                    }
                    currentUser = user;

                    // Load events and notifications in parallel
                    executor.execute(() -> {
                        // Load events
                        allEvents = eventService.getAllEvents();

                        // Convert userId (String) to recipientId (int) for filtering
                        int recipientId = userIdToInt(user.getUserId());

                        // Load notifications for this user
                        allNotifications = notificationService.getNotificationsByRecipientId(recipientId);

                        // Sort by timestamp (newest first)
                        Collections.sort(allNotifications, (n1, n2) -> {
                            Date d1 = n1.getTimestamp();
                            Date d2 = n2.getTimestamp();
                            if (d1 == null && d2 == null) return 0;
                            if (d1 == null) return 1;
                            if (d2 == null) return -1;
                            return d2.compareTo(d1); // Descending order
                        });

                        // Update UI on main thread
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> {
                                updateUI();
                                swipeRefreshLayout.setRefreshing(false);
                            });
                        }
                    });
                },
                e -> {
                    showError("Failed to load user profile");
                    swipeRefreshLayout.setRefreshing(false);
                }
        );
    }

    private void updateUI() {
        // Calculate unread count
        long unreadCount = allNotifications.stream()
                .filter(n -> !n.isHasRead())
                .count();

        // Update unread badge
        if (unreadCount > 0) {
            unreadBadge.setVisibility(View.VISIBLE);
            unreadBadge.setText(getString(R.string.notification_new_count, unreadCount));
        } else {
            unreadBadge.setVisibility(View.GONE);
        }

        // Update adapter with notifications and events
        adapter.updateNotifications(allNotifications, allEvents);

        // Show/hide empty state
        if (allNotifications.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyStateView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateView.setVisibility(View.GONE);
        }
    }

    private void onMarkAsRead(Notification notification) {
        notificationService.markNotificationAsRead(
                notification.getNotificationId(),
                aVoid -> {
                    // Update local state
                    notification.setHasRead(true);
                    updateUI();
                },
                e -> {
                    Toast.makeText(requireContext(), "Failed to mark notification as read", Toast.LENGTH_SHORT).show();
                }
        );
    }

    /**
     * Converts userId (String) to recipientId (int) for notification filtering.
     * Uses hashCode to convert, ensuring positive value.
     */
    private int userIdToInt(String userId) {
        if (userId == null || userId.isEmpty()) {
            return 0;
        }
        return Math.abs(userId.hashCode());
    }

    /**
     * Helper method to find event by ID (converting int to String for comparison)
     */
    public Event findEventById(int eventId) {
        String eventIdStr = String.valueOf(eventId);
        for (Event event : allEvents) {
            if (event != null && eventIdStr.equals(event.getEventId())) {
                return event;
            }
        }
        return null;
    }

    private void showError(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}

