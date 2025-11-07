package com.quantiagents.app.ui.myevents;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.quantiagents.app.App;
import com.quantiagents.app.R;
import com.quantiagents.app.Services.EventService;
import com.quantiagents.app.Services.RegistrationHistoryService;
import com.quantiagents.app.models.Event;
import com.quantiagents.app.Constants.constant;

import java.util.ArrayList;
import java.util.List;

/**
 * Browse all events (not user-scoped). Uses SingleEventFragment to view details.
 */
public class BrowseEventsFragment extends Fragment implements BrowseEventsAdapter.OnEventClick {

    private EventService eventService;
    private RegistrationHistoryService regService;

    private SwipeRefreshLayout swipe;
    private ProgressBar progress;
    private RecyclerView list;
    private TextView empty;
    private EditText search;

    private final List<Event> all = new ArrayList<>();
    private final BrowseEventsAdapter adapter = new BrowseEventsAdapter(new ArrayList<>(), this);

    public static BrowseEventsFragment newInstance() {
        return new BrowseEventsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_browse_events, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        eventService = ((App) requireActivity().getApplication()).locator().eventService();
        regService = ((App) requireActivity().getApplication()).locator().registrationHistoryService();

        swipe = v.findViewById(R.id.swipe);
        progress = v.findViewById(R.id.progress);
        list = v.findViewById(R.id.recycler);
        empty = v.findViewById(R.id.text_empty);
        search = v.findViewById(R.id.input_search);

        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);

        swipe.setOnRefreshListener(this::load);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filter(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });

        load();
    }

    private void load() {
        progress.setVisibility(View.VISIBLE);
        swipe.setRefreshing(false);

        // Synchronous read via your EventService repository interface.
        List<Event> items = eventService.getAllEvents();
        all.clear();
        if (items != null) all.addAll(items);

        applyFilterAndState();
        progress.setVisibility(View.GONE);
    }

    private void filter(String q) {
        applyFilterAndState(q == null ? "" : q.trim().toLowerCase());
    }

    private void applyFilterAndState() {
        applyFilterAndState(search.getText() == null ? "" : search.getText().toString().trim().toLowerCase());
    }

    private void applyFilterAndState(String q) {
        List<Event> filtered = new ArrayList<>();
        for (Event e : all) {
            if (e == null) continue;
            String title = safe(e.getTitle());
            if (q.isEmpty() || title.toLowerCase().contains(q)) {
                filtered.add(e);
            }
        }
        adapter.replace(filtered);
        empty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private static String safe(String s) { return s == null ? "" : s; }

    // Adapter callbacks

    @Override
    public void onJoinWaitlist(@NonNull Event event) {
        // Delegate to SingleEventFragment so the behavior matches "Selected/Waiting" flows
        openDetails(event);
    }

    @Override
    public void onViewEvent(@NonNull Event event) {
        openDetails(event);
    }

    private void openDetails(@NonNull Event event) {
        Fragment details = SingleEventFragment.newInstance();
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_container, details)
                .addToBackStack(null)
                .commit();
    }

    /**
     * Utility for consumers if your model exposes a status enum.
     */
    public static boolean isOpen(@Nullable Event e) {
        if (e == null) return false;
        // If your Event exposes an enum: e.getStatus() == constant.EventStatus.OPEN
        try {
            Object status = Event.class.getMethod("getStatus").invoke(e);
            return status == constant.EventStatus.OPEN;
        } catch (Exception ignore) {
            return false;
        }
    }
}
