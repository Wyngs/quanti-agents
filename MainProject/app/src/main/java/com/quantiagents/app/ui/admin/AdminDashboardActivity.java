package com.quantiagents.app.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.quantiagents.app.R;

/**
 * Activity that serves as the main dashboard for admin users.
 * Provides navigation to manage events, images, notifications, and profiles.
 */
public class AdminDashboardActivity extends AppCompatActivity {

    /**
     * Initializes the activity and sets up navigation buttons to various admin management screens.
     *
     * @param savedInstanceState The saved instance state bundle
     */
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

        manageEventsButton.setOnClickListener(v ->
                startActivity(new Intent(this, ManageEventsActivity.class)));

        manageImagesButton.setOnClickListener(v ->
                startActivity(new Intent(this, ManageImagesActivity.class)));

        manageNotificationsButton.setOnClickListener(v ->
                startActivity(new Intent(this, ManageNotificationsActivity.class)));

        manageProfilesButton.setOnClickListener(v ->
                startActivity(new Intent(this, ManageProfilesActivity.class)));
    }
}