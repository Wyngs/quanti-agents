package com.quantiagents.app.ui.entrantinfo;

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
import java.util.Collections;
import java.util.List;

/**
 * A single tab page that displays entrants for a given (eventId, status) pair.
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Perform an async one-shot load (with pull-to-refresh).</li>
 *   <li>Listen to a parent "refresh" signal after draw/refill to reload.</li>
 *   <li>Show a simple empty state when no rows exist.</li>
 * </ul>
 */
public class EntrantListFragment extends Fragment {

    private static final String ARG_EVENT = "eventId";
    private static final String ARG_STATUS = "status";

    private String eventId;
    private String status;

    private SwipeRefreshLayout swipe;
    private TextView empty;
    private EntrantUserAdapter adapter;
    private RegistrationHistoryService svc;

    /**
     * Factory method to create a page for a specific status.
     *
     * @param eventId Non-null event identifier used to scope queries.
     * @param status  One of "WAITING", "SELECTED", "CONFIRMED", or "CANCELED".
     * @return A configured fragment instance.
     */
    public static EntrantListFragment newInstance(@NonNull String eventId, @NonNull String status) {
        Bundle b = new Bundle();
        b.putString(ARG_EVENT, eventId);
        b.putString(ARG_STATUS, status);
        EntrantListFragment f = new EntrantListFragment();
        f.setArguments(b);
        return f;
    }

    /** Required empty public constructor. */
    public EntrantListFragment() { /* no-op */ }

    /**
     * Inflates the list container which consists of:
     * <ul>
     *   <li>{@link SwipeRefreshLayout} wrapping a {@link RecyclerView}</li>
     *   <li>A centered empty-label {@link TextView} (initially hidden)</li>
     * </ul>
     *
     * @param inflater  The {@link LayoutInflater}.
     * @param container The parent container (may be null).
     * @param savedInstanceState Prior saved state, if any (may be null).
     * @return The inflated root view.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_entrant_list, container, false);
    }

    /**
     * Wires the RecyclerView and refresh behavior, then triggers an initial load.
     * Also subscribes to a parent fragment result with key "entrantinfo:refresh"
     * so we can reload after draw/refill operations.
     *
     * @param view               Root view returned by {@link #onCreateView}.
     * @param savedInstanceState Prior saved state, if any.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        eventId = requireArguments().getString(ARG_EVENT);
        status  = requireArguments().getString(ARG_STATUS);

        RecyclerView rv = view.findViewById(R.id.list);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new EntrantUserAdapter(new ArrayList<>());
        rv.setAdapter(adapter);

        swipe = view.findViewById(R.id.swipe);
        empty = view.findViewById(R.id.empty);

        svc = ((App) requireActivity().getApplication()).locator().registrationHistoryService();

        swipe.setOnRefreshListener(this::load);

        // Reload when the host fragment broadcasts a refresh request.
        getParentFragmentManager().setFragmentResultListener(
                "entrantinfo:refresh",
                this,
                (key, bundle) -> load()
        );

        load();
    }

    /**
     * Executes an async fetch of registrations for the configured (eventId, status).
     * On success, binds rows to the adapter and toggles the empty state. On failure,
     * shows a toast and clears the list.
     */
    private void load() {
        swipe.setRefreshing(true);
        svc.getByEventAndStatus(eventId, status,
                this::bind,
                e -> {
                    bind(Collections.emptyList());
                    Toast.makeText(getContext(), "Load failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Renders the provided rows into the adapter and updates the empty state.
     *
     * @param rows Non-null list of {@link RegistrationHistory} rows to display (may be empty).
     */
    private void bind(@NonNull List<RegistrationHistory> rows) {
        swipe.setRefreshing(false);
        adapter.submit(rows);
        empty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
