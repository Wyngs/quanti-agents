package com.quantiagents.app.ui.myevents;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
import com.quantiagents.app.Services.EventService;
import com.quantiagents.app.Services.RegistrationHistoryService;
import com.quantiagents.app.Services.UserService;
import com.quantiagents.app.models.Event;
import com.quantiagents.app.models.RegistrationHistory;
import com.quantiagents.app.models.User;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BrowseEventsFragment extends Fragment implements BrowseEventsAdapter.OnEventClick {

    public static BrowseEventsFragment newInstance() {
        return new BrowseEventsFragment();
    }

    private EventService eventService;
    private RegistrationHistoryService regService;
    private UserService userService;
    // Executor to run blocking service calls off the main thread
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private SwipeRefreshLayout swipe;
    private ProgressBar progress;
    private RecyclerView list;
    private TextView empty;
    private EditText search;

    private final List<Event> allEvents = new ArrayList<>();
    private BrowseEventsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_browse_events, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        App app = (App) requireActivity().getApplication();
        eventService = app.locator().eventService();
        regService = app.locator().registrationHistoryService();
        userService = app.locator().userService();

        swipe = v.findViewById(R.id.swipe);
        progress = v.findViewById(R.id.progress);
        list = v.findViewById(R.id.recycler);
        empty = v.findViewById(R.id.text_empty);
        search = v.findViewById(R.id.input_search);

        adapter = new BrowseEventsAdapter(new ArrayList<>(), this);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);

        swipe.setOnRefreshListener(this::loadEvents);

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filter(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });

        loadEvents();
    }

    private void loadEvents() {
        progress.setVisibility(View.VISIBLE);
        // Run synchronous getAllEvents on background thread
        executor.execute(() -> {
            List<Event> fetched = eventService.getAllEvents();
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    swipe.setRefreshing(false);
                    allEvents.clear();
                    if (fetched != null) allEvents.addAll(fetched);
                    filter(search.getText().toString());
                });
            }
        });
    }

    private void filter(String query) {
        List<Event> filtered = new ArrayList<>();
        String q = query == null ? "" : query.trim().toLowerCase();

        for (Event e : allEvents) {
            // Only show events that match the search
            String title = e.getTitle() == null ? "" : e.getTitle();
            if (q.isEmpty() || title.toLowerCase().contains(q)) {
                filtered.add(e);
            }
        }

        adapter.replace(filtered);
        empty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onJoinWaitlist(@NonNull Event event) {
        progress.setVisibility(View.VISIBLE);

        // Use callback structure compatible with your existing UserService
        userService.getCurrentUser(
                user -> {
                    if (user == null) {
                        progress.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Please log in", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String userId = user.getUserId();
                    // Fix: Prevent organizer from joining their own event
                    if (userId.equals(event.getOrganizerId())) {
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> {
                                progress.setVisibility(View.GONE);
                                Toast.makeText(getContext(), "Organizers cannot join their own events.", Toast.LENGTH_SHORT).show();
                            });
                        }
                        return;
                    }

                    String eventId = event.getEventId();

                    // Check existing registration on background thread
                    executor.execute(() -> {
                        RegistrationHistory existing = regService.getRegistrationHistoryByEventIdAndUserId(eventId, userId);

                        if (existing != null) {
                            if (isAdded()) {
                                requireActivity().runOnUiThread(() -> {
                                    progress.setVisibility(View.GONE);
                                    Toast.makeText(getContext(), "Already registered!", Toast.LENGTH_SHORT).show();
                                });
                            }
                            return;
                        }

                        // Create new registration
                        RegistrationHistory newReg = new RegistrationHistory(
                                eventId,
                                userId,
                                constant.EventRegistrationStatus.WAITLIST,
                                new Date()
                        );

                        // Save using service callback
                        regService.saveRegistrationHistory(newReg,
                                aVoid -> {
                                    // Fix: Sync Event waiting list in Event object
                                    if (event.getWaitingList() == null) event.setWaitingList(new ArrayList<>());
                                    if (!event.getWaitingList().contains(userId)) {
                                        event.getWaitingList().add(userId);
                                        eventService.updateEvent(event, v -> {}, e -> Log.e("BrowseEvents", "Failed to sync waiting list", e));
                                    }

                                    if (isAdded()) {
                                        requireActivity().runOnUiThread(() -> {
                                            progress.setVisibility(View.GONE);
                                            Toast.makeText(getContext(), "Joined Waitlist!", Toast.LENGTH_SHORT).show();
                                        });
                                    }
                                },
                                e -> {
                                    if (isAdded()) {
                                        requireActivity().runOnUiThread(() -> {
                                            progress.setVisibility(View.GONE);
                                            Toast.makeText(getContext(), "Failed to join", Toast.LENGTH_SHORT).show();
                                        });
                                    }
                                });
                    });
                },
                e -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Failed to load user profile", Toast.LENGTH_SHORT).show();
                }
        );
    }

    @Override
    public void onViewEvent(@NonNull Event event) {
        // Reuse the existing ViewEventDetailsFragment for viewing details
        try {
            // We use reflection or direct class reference if available in your scope.
            // Based on your initial files, ViewEventDetailsFragment exists in com.quantiagents.app.ui
            Class<?> clazz = Class.forName("com.quantiagents.app.ui.ViewEventDetailsFragment");
            java.lang.reflect.Method method = clazz.getMethod("newInstance", String.class);
            Fragment detailsFragment = (Fragment) method.invoke(null, event.getEventId());

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.content_container, detailsFragment)
                    .addToBackStack(null)
                    .commit();
        } catch (Exception e) {
            Toast.makeText(getContext(), "View Details: " + event.getTitle(), Toast.LENGTH_SHORT).show();
        }
    }

    public static boolean isOpen(Event e) {
        return e != null && e.getStatus() == constant.EventStatus.OPEN;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}