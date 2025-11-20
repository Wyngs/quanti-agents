package com.quantiagents.app.ui.manageevents;

import android.content.Intent;
import android.os.Bundle;
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

import com.quantiagents.app.App;
import com.quantiagents.app.R;
import com.quantiagents.app.Services.EventService;
import com.quantiagents.app.Services.UserService;
import com.quantiagents.app.models.Event;
import com.quantiagents.app.models.User;
import com.quantiagents.app.ui.manageeventinfo.ManageEventInfoHostActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen that shows all events organized by the current user.
 *
 * Flow:
 *   MainActivity navigation → ManageEventsFragment
 *   → list of "My Organized Events"
 *   → "Manage Event Info" button launches ManageEventInfoHostActivity.
 */
public class ManageEventsFragment extends Fragment implements ManageEventsAdapter.OnManageEventInfoClickListener {

    private TextView totalEventsValue;
    private ProgressBar progressBar;
    private TextView emptyView;
    private RecyclerView recyclerView;
    private ManageEventsAdapter adapter;

    private EventService eventService;
    private UserService userService;

    public static ManageEventsFragment newInstance() {
        return new ManageEventsFragment();
    }

    public ManageEventsFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_manage_events, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        App app = (App) requireActivity().getApplication();
        eventService = app.locator().eventService();
        userService = app.locator().userService();

        totalEventsValue = view.findViewById(R.id.text_total_events_value);
        progressBar = view.findViewById(R.id.progress_manage_events);
        emptyView = view.findViewById(R.id.text_manage_events_empty);
        recyclerView = view.findViewById(R.id.recycler_manage_events);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ManageEventsAdapter(this);
        recyclerView.setAdapter(adapter);

        loadEvents();
    }

    /**
     * Loads all events, filters them to ones organized by the current user,
     * then updates the summary box and the list.
     */
    private void loadEvents() {
        showLoading(true);
        userService.getCurrentUser(
                user -> {
                    if (user == null) {
                        showLoading(false);
                        showEmpty("Profile not found.");
                        return;
                    }
                    String organizerId = user.getUserId();
                    eventService.getAllEvents(
                            events -> {
                                List<Event> mine = filterByOrganizer(events, organizerId);
                                bindEvents(mine);
                            },
                            e -> {
                                showLoading(false);
                                showEmpty("Error loading events.");
                                Toast.makeText(getContext(), "Failed to load events.", Toast.LENGTH_LONG).show();
                            }
                    );
                },
                e -> {
                    showLoading(false);
                    showEmpty("Error loading profile.");
                    Toast.makeText(getContext(), "Failed to load profile.", Toast.LENGTH_LONG).show();
                }
        );
    }

    private static List<Event> filterByOrganizer(@Nullable List<Event> events, @NonNull String organizerId) {
        List<Event> out = new ArrayList<>();
        if (events == null) {
            return out;
        }
        for (Event ev : events) {
            if (ev == null) continue;
            if (organizerId.equals(ev.getOrganizerId())) {
                out.add(ev);
            }
        }
        return out;
    }

    private void bindEvents(@NonNull List<Event> events) {
        showLoading(false);
        totalEventsValue.setText(String.valueOf(events.size()));

        if (events.isEmpty()) {
            showEmpty(getString(R.string.manage_events_empty_default));
            return;
        }

        emptyView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        adapter.submit(events);
    }

    private void showLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.GONE);
        }
    }

    private void showEmpty(@NonNull String message) {
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
        emptyView.setText(message);
    }

    /**
     * Callback from adapter when the row button "Manage Event Info" is pressed.
     * This launches the existing ManageEventInfoHostActivity with the event id.
     */
    @Override
    public void onManageEventInfoClicked(@NonNull Event event) {
        if (event.getEventId() == null || event.getEventId().trim().isEmpty()) {
            Toast.makeText(getContext(), "Event id is missing.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(requireContext(), ManageEventInfoHostActivity.class);
        intent.putExtra(ManageEventInfoHostActivity.EXTRA_EVENT_ID, event.getEventId());
        startActivity(intent);
    }
}
