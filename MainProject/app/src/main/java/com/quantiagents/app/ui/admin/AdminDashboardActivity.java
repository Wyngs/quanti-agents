package com.quantiagents.app.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.quantiagents.app.R;

public class AdminDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        Toolbar toolbar = findViewById(R.id.toolbar_admin_dashboard);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Admin Dashboard");
        }

        Button manageEventsButton = findViewById(R.id.button_manage_events);
        Button manageImagesButton = findViewById(R.id.button_manage_posters);
        Button manageNotificationsButton = findViewById(R.id.button_manage_notifications);
        Button manageProfilesButton = findViewById(R.id.button_manage_profiles);

        manageEventsButton.setOnClickListener(v -> {
            startActivity(new Intent(AdminDashboardActivity.this, ManageEventsActivity.class));
        });

        manageImagesButton.setOnClickListener(v -> {
            startActivity(new Intent(AdminDashboardActivity.this, ManageImagesActivity.class));
        });

        manageNotificationsButton.setOnClickListener(v -> {
            startActivity(new Intent(AdminDashboardActivity.this, ManageNotificationsActivity.class));
        });

        manageProfilesButton.setOnClickListener(v -> {
            startActivity(new Intent(AdminDashboardActivity.this, ManageProfilesActivity.class));
        });
    }
}