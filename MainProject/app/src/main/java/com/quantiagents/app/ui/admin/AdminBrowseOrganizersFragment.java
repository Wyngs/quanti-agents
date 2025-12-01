package com.quantiagents.app.ui.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.quantiagents.app.R;
import com.quantiagents.app.models.UserSummary;
import com.quantiagents.app.ui.admin.viewmodel.AdminEventsViewModel;

/**
 * Fragment that displays ONLY users who are Organizers (have created events).
 */
public class AdminBrowseOrganizersFragment extends Fragment {

    private AdminEventsViewModel viewModel;
    private RecyclerView recyclerView;
    private AdminProfileAdapter adapter;
    private EditText searchInput;

    public static AdminBrowseOrganizersFragment newInstance() {
        return new AdminBrowseOrganizersFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_browse, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(AdminEventsViewModel.class);

        TextView title = view.findViewById(R.id.text_admin_title);
        if (title != null) title.setText("Manage Organizers");

        recyclerView = view.findViewById(R.id.admin_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new AdminProfileAdapter(
                profile -> {
                    viewModel.deleteProfile(profile);
                },
                profile -> {
                    showProfileDetails(profile);
                }
        );
        recyclerView.setAdapter(adapter);

        searchInput = view.findViewById(R.id.input_search);
        if (searchInput != null) {
            searchInput.setHint("Search Organizers...");
            searchInput.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    viewModel.searchOrganizers(s.toString());
                }

                @Override public void afterTextChanged(Editable s) {}
            });
        }

        viewModel.getOrganizers().observe(getViewLifecycleOwner(), organizers -> {
            adapter.submitList(organizers);
            if (organizers == null || organizers.isEmpty()) {
            }
        });

        viewModel.getToastMessage().observe(getViewLifecycleOwner(), message -> {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        });

        viewModel.loadOrganizers();
    }
    private void showProfileDetails(UserSummary profile) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Organizer Details")
                .setMessage("Name: " + profile.getName() + "\n" +
                        "Email: " + profile.getEmail() + "\n" +
                        "Username: " + profile.getUsername() + "\n" +
                        "User ID: " + profile.getUserId())
                .setPositiveButton("Close", null)
                .setNegativeButton("Delete", (dialog, which) -> viewModel.deleteProfile(profile))
                .show();
    }
}