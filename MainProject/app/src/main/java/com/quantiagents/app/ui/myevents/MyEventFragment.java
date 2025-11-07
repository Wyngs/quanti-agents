package com.quantiagents.app.ui.myevents;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.FragmentTransaction;

/**
 * MyEventFragment
 *
 * Shows four buckets for the user's registrations. No swipe refresh.
 * Data reloads whenever the selected tab changes. Also loads on first render.
 */
public class MyEventFragment extends Fragment {

    // UI
    private MaterialButtonToggleGroup tabs;
    private MaterialButton tabWaiting;
    private MaterialButton tabSelected;
    private MaterialButton tabConfirmed;
    private MaterialButton tabPast;
    private RecyclerView recycler;
    private ProgressBar progress;
    private TextView empty;

    // Services
    private UserService userService;
    private RegistrationHistoryService regService;
    private EventService eventService;

    // Buckets
    private final List<Item> waitingItems = new ArrayList<>();
    private final List<Item> selectedItems = new ArrayList<>();
    private final List<Item> confirmedItems = new ArrayList<>();
    private final List<Item> pastItems = new ArrayList<>();

    // Adapter
    private HostAdapter adapter;

    // Tracks which tab is currently selected
    private enum Tab { WAITING, SELECTED, CONFIRMED, PAST }
    private Tab currentTab = Tab.WAITING;

    public static MyEventFragment newInstance() {
        return new MyEventFragment();
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_my_events, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        App app = (App) requireActivity().getApplication();
        userService = app.locator().userService();
        regService = app.locator().registrationHistoryService();
        eventService = app.locator().eventService();

        bindViews(view);
        setupRecycler();
        setupTabs();

        // First load
        reloadDataForCurrentTab(true);
    }

    private void bindViews(@NonNull View root) {
        tabs = root.findViewById(R.id.my_events_tabs);
        tabWaiting = root.findViewById(R.id.tab_waiting);
        tabSelected = root.findViewById(R.id.tab_selected);
        tabConfirmed = root.findViewById(R.id.tab_confirmed);
        tabPast = root.findViewById(R.id.tab_past);
        recycler = root.findViewById(R.id.my_events_recycler);
        progress = root.findViewById(R.id.my_events_progress);
        empty = root.findViewById(R.id.my_events_empty);
    }

    private void setupRecycler() {
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new HostAdapter(getChildFragmentManager());
        recycler.setAdapter(adapter);
    }

    private void setupTabs() {
        tabs.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;

            if (checkedId == R.id.tab_waiting) currentTab = Tab.WAITING;
            else if (checkedId == R.id.tab_selected) currentTab = Tab.SELECTED;
            else if (checkedId == R.id.tab_confirmed) currentTab = Tab.CONFIRMED;
            else if (checkedId == R.id.tab_past) currentTab = Tab.PAST;

            // Each tab switch triggers a fresh load from services
            reloadDataForCurrentTab(false);
        });

        // default selection
        tabWaiting.setChecked(true);
        currentTab = Tab.WAITING;
    }

    /**
     * Reloads all buckets from services then swaps the list for the active tab.
     * @param showSpinner true to show the full screen spinner during first load
     */
    private void reloadDataForCurrentTab(boolean showSpinner) {
        if (showSpinner) {
            progress.setVisibility(View.VISIBLE);
            recycler.setVisibility(View.GONE);
            empty.setVisibility(View.GONE);
        }

        userService.getCurrentUser(
                (User user) -> {
                    if (user == null) {
                        requireActivity().runOnUiThread(() -> {
                            progress.setVisibility(View.GONE);
                            adapter.submitList(new ArrayList<>());
                            recycler.setVisibility(View.GONE);
                            empty.setText("No user found. Please log in again.");
                            empty.setVisibility(View.VISIBLE);
                        });
                        return;
                    }

                    // ---------- Do everything synchronously (no background thread) ----------
                    List<RegistrationHistory> regs =
                            regService.getRegistrationHistoriesByUserId(user.getUserId());

                    Map<String, Event> cache = new HashMap<>();
                    List<Item> waiting = new ArrayList<>();
                    List<Item> selected = new ArrayList<>();
                    List<Item> confirmed = new ArrayList<>();
                    List<Item> past = new ArrayList<>();

                    for (RegistrationHistory rh : regs) {
                        if (rh == null || rh.getEventId() == null) continue;

                        Event ev = cache.get(rh.getEventId());
                        if (ev == null) {
                            ev = eventService.getEventById(rh.getEventId());
                            if (ev != null) cache.put(ev.getEventId(), ev);
                        }
                        if (ev == null) continue;

                        Item item = new Item(rh, ev);
                        constant.EventRegistrationStatus s = rh.getEventRegistrationStatus();
                        if (s == constant.EventRegistrationStatus.WAITLIST) waiting.add(item);
                        else if (s == constant.EventRegistrationStatus.SELECTED) selected.add(item);
                        else if (s == constant.EventRegistrationStatus.CONFIRMED) confirmed.add(item);
                        else if (s == constant.EventRegistrationStatus.CANCELLED) past.add(item);

                        if (ev.getStatus() == constant.EventStatus.CLOSED) {
                            // ensure it appears in Past as well (even if already added)
                            past.add(item);
                        }
                    }

                    requireActivity().runOnUiThread(() -> {
                        // Replace buckets
                        waitingItems.clear(); waitingItems.addAll(waiting);
                        selectedItems.clear(); selectedItems.addAll(selected);
                        confirmedItems.clear(); confirmedItems.addAll(confirmed);
                        pastItems.clear(); pastItems.addAll(past);

                        // Update tab texts with counts
                        tabWaiting.setText(getString(R.string.tab_waiting_fmt, waitingItems.size()));
                        tabSelected.setText(getString(R.string.tab_selected_fmt, selectedItems.size()));
                        tabConfirmed.setText(getString(R.string.tab_confirmed_fmt, confirmedItems.size()));
                        tabPast.setText(getString(R.string.tab_past_fmt, pastItems.size()));

                        // Show the active tab's list
                        if (currentTab == Tab.WAITING) swapList(waitingItems);
                        else if (currentTab == Tab.SELECTED) swapList(selectedItems);
                        else if (currentTab == Tab.CONFIRMED) swapList(confirmedItems);
                        else swapList(pastItems);

                        progress.setVisibility(View.GONE);
                    });
                },
                e -> {
                    requireActivity().runOnUiThread(() -> {
                        progress.setVisibility(View.GONE);
                        adapter.submitList(new ArrayList<>());
                        recycler.setVisibility(View.GONE);
                        empty.setText("Failed to load user.");
                        empty.setVisibility(View.VISIBLE);
                    });
                }
        );
    }


    private void swapList(List<Item> next) {
        adapter.submitList(next);
        boolean isEmpty = next == null || next.isEmpty();
        recycler.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        empty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        if (isEmpty) empty.setText(R.string.my_events_empty_generic);
    }

    /** Tuple of registration and event. */
    private static class Item {
        final RegistrationHistory reg;
        final Event event;
        Item(RegistrationHistory reg, Event event) { this.reg = reg; this.event = event; }
        String key() { return event.getEventId() + "_" + reg.getUserId(); }
    }

    /** Hosts SingleEventFragment inside each row. */
    private static class HostAdapter extends RecyclerView.Adapter<HostVH> {
        private final androidx.fragment.app.FragmentManager fm;
        private final List<Item> items = new ArrayList<>();

        HostAdapter(androidx.fragment.app.FragmentManager fm) {
            this.fm = fm;
            setHasStableIds(true);
        }

        @Override public long getItemId(int position) { return items.get(position).key().hashCode(); }

        void submitList(List<Item> next) {
            List<Item> old = new ArrayList<>(items);
            items.clear();
            if (next != null) items.addAll(next);

            DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override public int getOldListSize() { return old.size(); }
                @Override public int getNewListSize() { return items.size(); }
                @Override public boolean areItemsTheSame(int o, int n) {
                    return old.get(o).key().equals(items.get(n).key());
                }
                @Override public boolean areContentsTheSame(int o, int n) {
                    return old.get(o).key().equals(items.get(n).key());
                }
            }).dispatchUpdatesTo(this);
        }

        @NonNull
        @Override
        public HostVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View root = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_my_event_fragment_host, parent, false);
            return new HostVH(root);
        }

        @Override
        public void onBindViewHolder(@NonNull HostVH holder, int position) {
            Item item = items.get(position);
            String tag = "single_event_" + item.key();

            // If a fragment with this tag already exists, don't recreate it
            Fragment existing = fm.findFragmentByTag(tag);
            if (existing != null) return;

            Bundle args = new Bundle();
            args.putString("eventId", item.event.getEventId());
            args.putString("registrationStatus", item.reg.getEventRegistrationStatus().name());

            Fragment child = com.quantiagents.app.ui.myevents.SingleEventFragment.newInstance();
            child.setArguments(args);

            // CRITICAL: wait until the row's container is attached
            View container = holder.containerView();
            container.post(() -> {
                // double-check the view is still attached
                if (!container.isAttachedToWindow()) return;

                FragmentTransaction tx = fm.beginTransaction()
                        .replace(holder.containerId(), child, tag)
                        .setReorderingAllowed(true);

                tx.commitNowAllowingStateLoss();
            });
        }

        @Override public int getItemCount() { return items.size(); }
    }

    private static class HostVH extends RecyclerView.ViewHolder {
        private final View container;
        HostVH(@NonNull View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.single_event_host);
            // ensure a unique id per holder
            container.setId(View.generateViewId());
        }
        int containerId() { return container.getId(); }
        View containerView() { return container; }
    }



    private void runUi(@NonNull Runnable r) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(r);
    }
}
