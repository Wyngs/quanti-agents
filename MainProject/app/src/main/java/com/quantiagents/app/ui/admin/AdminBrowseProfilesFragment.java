package com.quantiagents.app.ui.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.app.AlertDialog;

import com.quantiagents.app.R;
import com.quantiagents.app.ui.admin.viewmodel.AdminEventsViewModel;
import com.quantiagents.app.models.UserSummary;

public class AdminBrowseProfilesFragment extends Fragment {

    private AdminEventsViewModel viewModel;
    private RecyclerView recyclerView;
    private AdminProfileAdapter adapter;
    private EditText searchInput;

    public static AdminBrowseProfilesFragment newInstance() {
        return new AdminBrowseProfilesFragment();
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
            searchInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                    viewModel.searchProfiles(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        viewModel.getProfiles().observe(getViewLifecycleOwner(), profiles -> {
            adapter.submitList(profiles);
        });

        viewModel.getToastMessage().observe(getViewLifecycleOwner(), message -> {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        });

        viewModel.loadProfiles();
    }

    // helper method to show profile details
    private void showProfileDetails(UserSummary profile) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Profile Details")
                .setMessage("Name: " + profile.getName() + "\n" +
                        "Email: " + profile.getEmail() + "\n" +
                        "Username: " + profile.getUsername() + "\n" +
                        "User ID: " + profile.getUserId())
                .setPositiveButton("Close", null)
                .setNegativeButton("Delete", (dialog, which) -> viewModel.deleteProfile(profile))
                .show();
    }
}