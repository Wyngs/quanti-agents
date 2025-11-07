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

import com.quantiagents.app.R;
import com.quantiagents.app.App;
import com.quantiagents.app.Services.RegistrationHistoryService;
import com.quantiagents.app.models.RegistrationHistory;

import java.util.ArrayList;
import java.util.List;

/**
 * A single tab page that displays entrants filtered by status for one event.
 *
 * <p>Contract: fetch all histories for the event on background (Service may block);
 * filter in-memory by status; then bind to adapter on main thread.</p>
 */
public class ManageEventInfoListFragment extends Fragment {

    private static final String ARG_EVENT = "eventId";
    private static final String ARG_STATUS = "status";

    private String eventId;
    private String status;

    private SwipeRefreshLayout swipe;
    private TextView empty;
    private ManageEventInfoUserAdapter adapter;
    private RegistrationHistoryService svc;

    /**
     * Factory method to create a page for a specific status.
     *
     * @param eventId Non-null event identifier used to scope queries.
     * @param status  One of "WAITING", "SELECTED", "CONFIRMED", or "CANCELED".
     */
    public static ManageEventInfoListFragment newInstance(@NonNull String eventId, @NonNull String status) {
        Bundle b = new Bundle();
        b.putString(ARG_EVENT, eventId);
        b.putString(ARG_STATUS, status);
        ManageEventInfoListFragment f = new ManageEventInfoListFragment();
        f.setArguments(b);
        return f;
    }

    /** Required empty public constructor. */
    public ManageEventInfoListFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_manage_event_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        eventId = requireArguments().getString(ARG_EVENT);
        status  = requireArguments().getString(ARG_STATUS);

        RecyclerView rv = view.findViewById(R.id.list);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ManageEventInfoUserAdapter();
        rv.setAdapter(adapter);

        swipe = view.findViewById(R.id.swipe);
        empty = view.findViewById(R.id.empty);

        svc = ((App) requireActivity().getApplication()).locator().registrationHistoryService();

        swipe.setOnRefreshListener(this::load);

        // Listen for parent broadcasts to refresh after save/redraw.
        getParentFragmentManager().setFragmentResultListener(
                ManageEventInfoFragment.RESULT_REFRESH,
                this,
                (key, bundle) -> load()
        );

        load();
    }

    /** Trigger a background fetch + in-memory filter, then bind results to the adapter. */
    private void load() {
        swipe.setRefreshing(true);
        new Thread(() -> {
            try {
                List<RegistrationHistory> all = svc.getRegistrationHistoriesByEventId(eventId);
                List<RegistrationHistory> filtered = filterByStatus(all, status);
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> bind(filtered));
            } catch (Exception e) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    bind(new ArrayList<>());
                    Toast.makeText(getContext(), "Load failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    /**
     * Returns only rows that match the requested status, being tolerant of enum/string storage.
     */
    private static List<RegistrationHistory> filterByStatus(@NonNull List<RegistrationHistory> src,
                                                            @NonNull String want) {
        List<RegistrationHistory> out = new ArrayList<>();
        for (RegistrationHistory rh : src) {
            if (rh == null) continue;
            Object st = rh.getEventRegistrationStatus();
            String have = (st == null) ? null : String.valueOf(st);
            if (have != null && have.equalsIgnoreCase(want)) out.add(rh);
        }
        return out;
    }

    /** Bind rows to adapter and toggle empty state. */
    private void bind(@NonNull List<RegistrationHistory> rows) {
        swipe.setRefreshing(false);
        adapter.submit(rows);
        empty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
