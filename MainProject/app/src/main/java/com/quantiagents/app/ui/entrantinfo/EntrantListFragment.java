package com.quantiagents.app.ui.entrantinfo;

import android.os.Bundle;
import android.util.Log;
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

import com.google.firebase.firestore.ListenerRegistration;
import com.quantiagents.app.R;
import com.quantiagents.app.Services.RegistrationHistoryService;
import com.quantiagents.app.app.App;
import com.quantiagents.app.models.UserSummary;

import java.util.ArrayList;
import java.util.List;

/**
 * Reusable list fragment for a single registration status.
 * Responsibilities:
 * - One-shot load via service (pull-to-refresh supported)
 * - Realtime updates via service watch (removes listener on destroy)
 * - Simple empty-state handling
 */
public class EntrantListFragment extends Fragment {

    private static final String ARG_EVENT = "eventId";
    private static final String ARG_STATUS = "status";

    private EntrantUserAdapter adapter;
    private ListenerRegistration watcher;

    /**
     * Factory method to create a list for a specific (eventId, status) pair.
     *
     * @param eventId event identifier used by queries.
     * @param status  status string ("WAITING", "SELECTED", "CONFIRMED", "CANCELED").
     */
    public static EntrantListFragment newInstance(String eventId, String status) {
        Bundle b = new Bundle();
        b.putString(ARG_EVENT, eventId);
        b.putString(ARG_STATUS, status);
        EntrantListFragment f = new EntrantListFragment();
        f.setArguments(b);
        return f;
    }

    /** Required public ctor. */
    public EntrantListFragment() { /* no-op */ }

    /** Inflates {@code fragment_entrant_list} (SwipeRefreshLayout + RecyclerView + empty label). */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_entrant_list, container, false);
    }

    /**
     * Wires RecyclerView, sets up one-shot load and attaches a realtime watcher.
     * Why both? Pull-to-refresh gives manual control; watcher keeps the list hot/live.
     */
    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        String eventId = requireArguments().getString(ARG_EVENT);
        String status = requireArguments().getString(ARG_STATUS);

        RecyclerView rv = v.findViewById(R.id.list);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new EntrantUserAdapter(new ArrayList<>());
        rv.setAdapter(adapter);

        SwipeRefreshLayout swipe = v.findViewById(R.id.swipe);
        TextView empty = v.findViewById(R.id.empty);

        RegistrationHistoryService svc =
                ((App) requireActivity().getApplication()).locator().registrationHistoryService();

        // Shared render helper: update adapter + empty-state
        final java.util.function.Consumer<List<UserSummary>> render = users -> {
            adapter.submit(users);
            empty.setVisibility(users.isEmpty() ? View.VISIBLE : View.GONE);
        };

        // Initial load + manual refresh path
        Runnable loadOnce = () -> {
            swipe.setRefreshing(true);
            svc.listEntrantsByStatus(eventId, status,
                    users -> {
                        swipe.setRefreshing(false);
                        render.accept(users);
                    },
                    e -> {
                        swipe.setRefreshing(false);
                        Toast.makeText(getContext(), "Load failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        };
        swipe.setOnRefreshListener(loadOnce);
        loadOnce.run();

        // Realtime updates — fulfills "updates automatically" user story
        watcher = svc.watchEntrantsByStatus(eventId, status,
                render::accept,
                e -> Log.e("EntrantList", "watch error", e));
    }

    /**
     * Lifecycle clean-up: remove Firestore listener to avoid leaks after the view is gone.
     */
    @Override
    public void onDestroyView() {
        if (watcher != null) watcher.remove();
        super.onDestroyView();
    }
}
