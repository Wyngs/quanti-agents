package com.quantiagents.app.ui.manageeventinfo;

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

import com.quantiagents.app.App;
import com.quantiagents.app.R;
import com.quantiagents.app.Services.RegistrationHistoryService;
import com.quantiagents.app.Services.UserService;
import com.quantiagents.app.models.RegistrationHistory;
import com.quantiagents.app.models.User;

import java.util.ArrayList;
import java.util.List;

/**
 * A single tab page that displays entrants filtered by status for one event.
 *
 * Uses RegistrationHistoryService to fetch histories scoped to an eventId,
 * then joins with UserService to show name + username + joined date.
 */
public class ManageEventInfoListFragment extends Fragment {

    private static final String ARG_EVENT = "eventId";
    private static final String ARG_STATUS = "status";

    private String eventId;
    private String status;

    private SwipeRefreshLayout swipe;
    private TextView empty;
    private ManageEventInfoUserAdapter adapter;
    private RegistrationHistoryService registrationHistoryService;
    private UserService userService;

    public static ManageEventInfoListFragment newInstance(@NonNull String eventId,
                                                          @NonNull String status) {
        Bundle b = new Bundle();
        b.putString(ARG_EVENT, eventId);
        b.putString(ARG_STATUS, status);
        ManageEventInfoListFragment f = new ManageEventInfoListFragment();
        f.setArguments(b);
        return f;
    }

    public ManageEventInfoListFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_manage_event_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        eventId = requireArguments().getString(ARG_EVENT);
        status  = requireArguments().getString(ARG_STATUS);

        RecyclerView rv = view.findViewById(R.id.list);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ManageEventInfoUserAdapter();
        rv.setAdapter(adapter);

        swipe = view.findViewById(R.id.swipe);
        empty = view.findViewById(R.id.empty);

        App app = (App) requireActivity().getApplication();
        registrationHistoryService = app.locator().registrationHistoryService();
        userService = app.locator().userService();

        swipe.setOnRefreshListener(this::load);

        // Listen for parent broadcasts to refresh after save/redraw.
        getParentFragmentManager().setFragmentResultListener(
                ManageEventInfoFragment.RESULT_REFRESH,
                this,
                (key, bundle) -> load()
        );

        load();
    }

    /**
     * Trigger a background fetch + join with user profiles,
     * then bind results to the adapter.
     */
    private void load() {
        swipe.setRefreshing(true);
        new Thread(() -> {
            try {
                // 1) Get all histories for this event
                List<RegistrationHistory> all =
                        registrationHistoryService.getRegistrationHistoriesByEventId(eventId);

                // 2) Filter by requested status (WAITLIST, SELECTED, etc.)
                List<RegistrationHistory> filtered = new ArrayList<>();
                if (all != null) {
                    for (RegistrationHistory r : all) {
                        if (r == null || r.getEventRegistrationStatus() == null) {
                            continue;
                        }
                        if (r.getEventRegistrationStatus().name().equalsIgnoreCase(status)) {
                            filtered.add(r);
                        }
                    }
                }

                // 3) Build UI rows: name, username, joined
                List<ManageEventInfoUserAdapter.Row> rows = new ArrayList<>();
                for (RegistrationHistory r : filtered) {
                    String uid = r.getUserId();

                    // Adjust this call if your UserService uses a different method name
                    User u = null;
                    try {
                        u = userService.getUserById(uid);
                    } catch (Exception ignored) {
                        // keep u == null and just fall back to uid
                    }

                    String name = (u != null && u.getName() != null && !u.getName().isEmpty())
                            ? u.getName()
                            : uid;

                    String username = (u != null && u.getUsername() != null)
                            ? u.getUsername()
                            : "";

                    Object reg = r.getRegisteredAt();
                    String joined = reg != null ? reg.toString() : "";

                    rows.add(new ManageEventInfoUserAdapter.Row(name, username, joined));
                }

                if (!isAdded()) return;
                List<ManageEventInfoUserAdapter.Row> finalRows = rows;
                requireActivity().runOnUiThread(() -> {
                    swipe.setRefreshing(false);
                    adapter.submit(finalRows);
                    empty.setVisibility(finalRows.isEmpty() ? View.VISIBLE : View.GONE);
                    if (finalRows.isEmpty()) {
                        empty.setText(R.string.manage_events_empty_default);
                    }
                });
            } catch (Exception e) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    swipe.setRefreshing(false);
                    adapter.submit(new ArrayList<>());
                    empty.setVisibility(View.VISIBLE);
                    empty.setText("Failed to load entrants.");
                    Toast.makeText(getContext(),
                            "Error loading entrants: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
}
