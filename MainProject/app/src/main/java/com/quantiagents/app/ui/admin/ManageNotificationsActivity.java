package com.quantiagents.app.ui.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.quantiagents.app.App;
import com.quantiagents.app.R;
import com.quantiagents.app.Services.NotificationService;
import com.quantiagents.app.models.Notification;
import java.util.ArrayList;
import java.util.List;

public class ManageNotificationsActivity extends AppCompatActivity {

    private NotificationService notificationService;
    private NotificationAdapter adapter;
    private List<Notification> notificationList = new ArrayList<>();
    private ProgressBar progressBar;
    private View rootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_notifications);

        rootView = findViewById(android.R.id.content);
        progressBar = findViewById(R.id.progress_bar_notifications);
        RecyclerView recyclerView = findViewById(R.id.recycler_view_notifications);
        Toolbar toolbar = findViewById(R.id.toolbar_manage_notifications);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Manage Notifications");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        App app = (App) getApplication();
        notificationService = app.locator().notificationService();

        adapter = new NotificationAdapter(notificationList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadNotifications();
    }

    private void loadNotifications() {
        progressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            List<Notification> list = notificationService.getAllNotifications();
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                notificationList.clear();
                if(list != null) notificationList.addAll(list);
                adapter.notifyDataSetChanged();
                if (notificationList.isEmpty()) {
                    Toast.makeText(this, "No notifications.", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}