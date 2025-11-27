package com.quantiagents.app.ui.manageeventinfo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Fragment showing a single status list (WAITLIST / SELECTED / CONFIRMED / CANCELLED)
 * for the ManageEventInfo screen.
 */
public class ManageEventInfoListFragment extends Fragment {

    private static final String ARG_EVENT = "eventId";
    private static final String ARG_STATUS = "status";

    private String eventId;
    private String status;
    private SwipeRefreshLayout swipe;
    private TextView empty;
    private ManageEventInfoUserAdapter adapter;
    private RegistrationHistoryService regSvc;
    private UserService userSvc;

    /**
     * Factory method creating a list fragment for a given event and status.
     */
    public static ManageEventInfoListFragment newInstance(String eventId, String status) {
        Bundle b = new Bundle();
        b.putString(ARG_EVENT, eventId);
        b.putString(ARG_STATUS, status);
        ManageEventInfoListFragment f = new ManageEventInfoListFragment();
        f.setArguments(b);
        return f;
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
        eventId = requireArguments().getString(ARG_EVENT);
        status = requireArguments().getString(ARG_STATUS);

        RecyclerView rv = view.findViewById(R.id.list);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ManageEventInfoUserAdapter();
        rv.setAdapter(adapter);

        swipe = view.findViewById(R.id.swipe);
        empty = view.findViewById(R.id.empty);

        App app = (App) requireActivity().getApplication();
        regSvc = app.locator().registrationHistoryService();
        userSvc = app.locator().userService();

        swipe.setOnRefreshListener(this::load);
        getParentFragmentManager().setFragmentResultListener(
                ManageEventInfoFragment.RESULT_REFRESH,
                this,
                (k, b) -> load()
        );

        load();
    }

    /**
     * Loads registration histories and corresponding user info for this status.
     * Runs DB work on a background thread and posts results to the adapter on the UI thread.
     */
    private void load() {
        swipe.setRefreshing(true);

        new Thread(() -> {
            List<RegistrationHistory> all = regSvc.getRegistrationHistoriesByEventId(eventId);
            List<ManageEventInfoUserAdapter.Row> rows = new ArrayList<>();
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

            for (RegistrationHistory r : all) {
                if (r == null || r.getEventRegistrationStatus() == null) continue;

                // Filter by status enum name (WAITLIST, SELECTED, CONFIRMED, CANCELLED)
                if (!r.getEventRegistrationStatus().name().equalsIgnoreCase(status)) {
                    continue;
                }

                String userId = r.getUserId();
                String displayName = userId;
                String username = "";
                String joinedText = "";

                // Fetch user details
                try {
                    User u = userSvc.getUserById(userId);
                    if (u != null) {
                        if (u.getName() != null && !u.getName().isEmpty()) {
                            displayName = u.getName();
                        }
                        if (u.getUsername() != null) {
                            username = u.getUsername();
                        }
                    }
                } catch (Exception ignored) { }

                if (r.getRegisteredAt() != null) {
                    joinedText = fmt.format(r.getRegisteredAt());
                }

                rows.add(new ManageEventInfoUserAdapter.Row(
                        displayName,
                        username,
                        joinedText
                ));
            }

            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    swipe.setRefreshing(false);
                    adapter.submit(rows);
                    empty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
                });
            }
        }).start();
    }
}
