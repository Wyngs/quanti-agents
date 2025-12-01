package com.quantiagents.app.ui.myevents;

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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.quantiagents.app.Constants.constant;
import com.quantiagents.app.R;
import com.quantiagents.app.models.RegistrationHistory;
import com.quantiagents.app.ui.ViewEventDetailsFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment that displays the user's registered events organized by status.
 * Shows events in tabs: Waiting, Selected, Confirmed, and Past events.
 * Allows users to view event details, accept/decline lottery invitations, and leave waitlists.
 */
public class MyEventFragment extends Fragment implements MyEventsAdapter.OnEventActionListener {

    private MyEventsViewModel viewModel;
    // Updated to match the XML component
    private MaterialButtonToggleGroup tabs;
    private RecyclerView recycler;
    private ProgressBar progress;
    private TextView empty;
    private MyEventsAdapter adapter;

    /**
     * Enum representing the different event status tabs.
     */
    private enum Tab { WAITING, SELECTED, CONFIRMED, PAST }
    private Tab currentTab = Tab.WAITING;

    /**
     * Creates a new instance of MyEventFragment.
     *
     * @return A new MyEventFragment instance
     */
    public static MyEventFragment newInstance() {
        return new MyEventFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_events, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(MyEventsViewModel.class);

        bindViews(view);
        setupRecycler();
        setupTabs();
        observeViewModel();

        viewModel.loadData();
    }

    /**
     * Binds all view references from the layout.
     *
     * @param root The root view of the fragment
     */
    private void bindViews(@NonNull View root) {
        // Binds to the MaterialButtonToggleGroup in XML
        tabs = root.findViewById(R.id.my_events_tabs);
        recycler = root.findViewById(R.id.my_events_recycler);
        progress = root.findViewById(R.id.my_events_progress);
        empty = root.findViewById(R.id.my_events_empty);
    }

    /**
     * Sets up the RecyclerView with layout manager and adapter.
     */
    private void setupRecycler() {
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new MyEventsAdapter(this);
        recycler.setAdapter(adapter);
    }

    /**
     * Sets up the tab toggle group and handles tab selection changes.
     */
    private void setupTabs() {
        // Listener for ToggleGroup changes
        tabs.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.tab_waiting) {
                    updateCurrentTab(0);
                } else if (checkedId == R.id.tab_selected) {
                    updateCurrentTab(1);
                } else if (checkedId == R.id.tab_confirmed) {
                    updateCurrentTab(2);
                } else if (checkedId == R.id.tab_past) {
                    updateCurrentTab(3);
                }
                updateUiFromViewModel();
            }
        });

        // Ensure initial state matches the checked button in XML
        int checkedId = tabs.getCheckedButtonId();
        if (checkedId == R.id.tab_waiting) updateCurrentTab(0);
        else if (checkedId == R.id.tab_selected) updateCurrentTab(1);
        else if (checkedId == R.id.tab_confirmed) updateCurrentTab(2);
        else if (checkedId == R.id.tab_past) updateCurrentTab(3);
        else {
            // Fallback if nothing checked
            tabs.check(R.id.tab_waiting);
        }
    }

    /**
     * Updates the current tab based on the selected index.
     *
     * @param index The tab index (0=WAITING, 1=SELECTED, 2=CONFIRMED, 3=PAST)
     */
    private void updateCurrentTab(int index) {
        switch (index) {
            case 0: currentTab = Tab.WAITING; break;
            case 1: currentTab = Tab.SELECTED; break;
            case 2: currentTab = Tab.CONFIRMED; break;
            case 3: currentTab = Tab.PAST; break;
            default: currentTab = Tab.WAITING; break;
        }
    }

    /**
     * Observes ViewModel LiveData and updates UI accordingly.
     */
    private void observeViewModel() {
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            progress.setVisibility(loading ? View.VISIBLE : View.GONE);
            if (loading && adapter.getItemCount() == 0) {
                empty.setVisibility(View.GONE);
                recycler.setVisibility(View.GONE);
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) {
                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                if (adapter.getItemCount() == 0) {
                    empty.setText(msg);
                    empty.setVisibility(View.VISIBLE);
                }
            }
        });

        // Update dynamic counts on the buttons
        viewModel.getWaitingList().observe(getViewLifecycleOwner(), list -> {
            updateTabTitle(0, "Waiting", list != null ? list.size() : 0);
            if (currentTab == Tab.WAITING) updateUiList(list);
        });

        viewModel.getSelectedList().observe(getViewLifecycleOwner(), list -> {
            updateTabTitle(1, "Selected", list != null ? list.size() : 0);
            if (currentTab == Tab.SELECTED) updateUiList(list);
        });

        viewModel.getConfirmedList().observe(getViewLifecycleOwner(), list -> {
            updateTabTitle(2, "Confirm", list != null ? list.size() : 0);
            if (currentTab == Tab.CONFIRMED) updateUiList(list);
        });

        viewModel.getPastList().observe(getViewLifecycleOwner(), list -> {
            updateTabTitle(3, "Past", list != null ? list.size() : 0);
            if (currentTab == Tab.PAST) updateUiList(list);
        });
    }

    private void updateTabTitle(int index, String baseTitle, int count) {
        int viewId = -1;
        switch(index) {
            case 0: viewId = R.id.tab_waiting; break;
            case 1: viewId = R.id.tab_selected; break;
            case 2: viewId = R.id.tab_confirmed; break;
            case 3: viewId = R.id.tab_past; break;
        }

        if (viewId != -1 && tabs != null) {
            View view = tabs.findViewById(viewId);
            if (view instanceof MaterialButton) {
                MaterialButton btn = (MaterialButton) view;
                if (count > 0) {
                    btn.setText(baseTitle + " (" + count + ")");
                } else {
                    btn.setText(baseTitle);
                }
            }
        }
    }

    private void updateUiFromViewModel() {
        switch (currentTab) {
            case WAITING:
                updateUiList(viewModel.getWaitingList().getValue());
                break;
            case SELECTED:
                updateUiList(viewModel.getSelectedList().getValue());
                break;
            case CONFIRMED:
                updateUiList(viewModel.getConfirmedList().getValue());
                break;
            case PAST:
                updateUiList(viewModel.getPastList().getValue());
                break;
        }
    }

    private void updateUiList(List<MyEventsAdapter.MyEventItem> list) {
        if (list == null) list = new ArrayList<>();
        adapter.setItems(list);

        if (list.isEmpty()) {
            recycler.setVisibility(View.GONE);
            empty.setText(R.string.my_events_empty_generic);
            empty.setVisibility(View.VISIBLE);
        } else {
            recycler.setVisibility(View.VISIBLE);
            empty.setVisibility(View.GONE);
        }
    }

    public void refreshData() {
        if (viewModel != null) {
            viewModel.loadData();
        }
    }

    @Override
    public void onViewEvent(String eventId) {
        try {
            Fragment details = ViewEventDetailsFragment.newInstance(eventId);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.content_container, details)
                    .addToBackStack(null)
                    .commit();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Unable to open event details", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onLeaveWaitlist(String eventId) {
        viewModel.leaveWaitlist(eventId, () ->
                Toast.makeText(getContext(), "Left waitlist", Toast.LENGTH_SHORT).show()
        );
    }

    @Override
    public void onAccept(RegistrationHistory history) {
        viewModel.updateStatus(history, constant.EventRegistrationStatus.CONFIRMED, () ->
                Toast.makeText(getContext(), "Accepted!", Toast.LENGTH_SHORT).show()
        );
    }

    @Override
    public void onDecline(RegistrationHistory history) {
        viewModel.updateStatus(history, constant.EventRegistrationStatus.CANCELLED, () ->
                Toast.makeText(getContext(), "Declined/Cancelled", Toast.LENGTH_SHORT).show()
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.loadData();
    }
}