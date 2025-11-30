package com.quantiagents.app.ui.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.app.AlertDialog;

import com.quantiagents.app.R;
import com.quantiagents.app.ui.admin.viewmodel.AdminEventsViewModel;
import com.quantiagents.app.models.Event;

import java.util.ArrayList;

public class AdminBrowseEventsFragment extends Fragment {

    private AdminEventsViewModel viewModel;
    private RecyclerView recyclerView;
    private AdminEventAdapter adapter;
    private EditText searchInput;

    public static AdminBrowseEventsFragment newInstance() {
        return new AdminBrowseEventsFragment();
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_browse_events, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(AdminEventsViewModel.class);

        recyclerView = view.findViewById(R.id.recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new AdminEventAdapter(
                event -> {
                    viewModel.deleteEvent(event);
                },
                event -> {
                    // Navigate to event details
                    Fragment detailsFragment = com.quantiagents.app.ui.ViewEventDetailsFragment.newInstance(event.getEventId());
                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.content_container, detailsFragment)
                            .addToBackStack(null)
                            .commit();
                }
        );
        recyclerView.setAdapter(adapter);

        // Search setup
        searchInput = view.findViewById(R.id.input_search);
        if (searchInput != null) {
            searchInput.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    viewModel.searchEvents(s.toString());
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        // Observer for Events List
        viewModel.getEvents().observe(getViewLifecycleOwner(), events -> {
            if (events != null) {
                adapter.submitList(events);
            }
        });

        // Observer for Toast Messages
        viewModel.getToastMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) {
                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });

        // Load Data
        viewModel.loadEvents();
    }

    // helper method to show event details
    private void showEventDetails(Event event) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Event Details")
                .setMessage("Title: " + event.getTitle() + "\n" +
                        "ID: " + event.getEventId() + "\n" +
                        "Description: " + (event.getDescription() != null ? event.getDescription() : "N/A") + "\n" +
                        "Organizer ID: " + event.getOrganizerId())
                .setPositiveButton("Close", null)
                .setNegativeButton("Delete", (dialog, which) -> viewModel.deleteEvent(event))
                .show();
    }
}