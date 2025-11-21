package com.quantiagents.app.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.quantiagents.app.R;
import com.quantiagents.app.ui.admin.viewmodel.AdminEventsViewModel;
import com.quantiagents.app.models.Event;

public class AdminBrowseEventsFragment extends Fragment {

    private AdminEventsViewModel viewModel;
    private RecyclerView recyclerView;
    private AdminEventAdapter adapter;

    public static AdminBrowseEventsFragment newInstance() {
        return new AdminBrowseEventsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_browse, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView titleView = view.findViewById(R.id.text_admin_title);
        titleView.setText("Manage Events");

        SearchView searchView = view.findViewById(R.id.admin_search_view);
        viewModel = new ViewModelProvider(this).get(AdminEventsViewModel.class);

        recyclerView = view.findViewById(R.id.admin_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new AdminEventAdapter(event -> {
            viewModel.deleteEvent(event);
        });
        recyclerView.setAdapter(adapter);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                viewModel.searchEvents(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                viewModel.searchEvents(newText);
                return true;
            }
        });

        viewModel.getEvents().observe(getViewLifecycleOwner(), events -> {
            adapter.submitList(events);
        });

        viewModel.getToastMessage().observe(getViewLifecycleOwner(), message -> {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        });

        viewModel.loadEvents();
    }
}