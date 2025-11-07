package com.quantiagents.app.ui.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.snackbar.Snackbar;
import com.quantiagents.app.App;
import com.quantiagents.app.R;
import com.quantiagents.app.Services.AdminService;
import com.quantiagents.app.models.User;
import java.util.ArrayList;
import java.util.List;

public class ManageProfilesActivity extends AppCompatActivity {

    private AdminService adminService;
    private ProfileAdapter adapter;
    private final List<User> profileList = new ArrayList<>();
    private ProgressBar progressBar;
    private View rootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_profiles);

        App app = (App) getApplication();
        adminService = app.locator().adminService();

        rootView = findViewById(android.R.id.content);
        progressBar = findViewById(R.id.progress_bar_profiles);
        RecyclerView recyclerView = findViewById(R.id.recycler_view_profiles);
        Toolbar toolbar = findViewById(R.id.toolbar_manage_profiles);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Manage Profiles");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        setupRecyclerView(recyclerView);
        loadProfiles();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void setupRecyclerView(RecyclerView recyclerView) {
        adapter = new ProfileAdapter(profileList, this::deleteProfile);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadProfiles() {
        progressBar.setVisibility(View.VISIBLE);
        adminService.listAllProfiles().addOnSuccessListener(querySnapshot -> {
            progressBar.setVisibility(View.GONE);
            List<User> loadedProfiles = querySnapshot.toObjects(User.class);
            if (loadedProfiles != null && !loadedProfiles.isEmpty()) {
                profileList.clear();
                profileList.addAll(loadedProfiles);
                adapter.notifyDataSetChanged();
            } else {
                Toast.makeText(this, "No profiles found.", Toast.LENGTH_LONG).show();
            }
        }).addOnFailureListener(e -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Error loading profiles.", Toast.LENGTH_LONG).show();
        });
    }

    private void deleteProfile(User user, int position) {
        adminService.removeProfile(user.getUserId(), true, "Admin deleted profile")
                .addOnSuccessListener(aVoid -> {
                    profileList.remove(position);
                    adapter.notifyItemRemoved(position);
                    adapter.notifyItemRangeChanged(position, profileList.size());
                    Snackbar.make(rootView, "Deleted: " + user.getName(), Snackbar.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    Snackbar.make(rootView, "Failed to delete profile.", Snackbar.LENGTH_LONG).show();
                });
    }
}