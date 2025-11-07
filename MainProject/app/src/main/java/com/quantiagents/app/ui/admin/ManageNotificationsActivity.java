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
import com.quantiagents.app.Services.NotificationService;
import com.quantiagents.app.models.Notification;
import java.util.ArrayList;
import java.util.List;

public class ManageNotificationsActivity extends AppCompatActivity {

    private NotificationService notificationService;
    private NotificationAdapter adapter;
    private List<Notification> notificationList = new ArrayList<>();
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private View rootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_notifications);

        rootView = findViewById(android.R.id.content);
        progressBar = findViewById(R.id.progress_bar_notifications);
        recyclerView = findViewById(R.id.recycler_view_notifications);

        Toolbar toolbar = findViewById(R.id.toolbar_manage_notifications);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Manage All Notifications");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        App app = (App) getApplication();
        notificationService = app.locator().notificationService();

        setupRecyclerView();
        loadNotifications();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void setupRecyclerView() {
        adapter = new NotificationAdapter(notificationList, this::deleteNotification);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadNotifications() {
        progressBar.setVisibility(View.VISIBLE);
        notificationService.getAllNotifications().addOnSuccessListener(querySnapshot -> {
            progressBar.setVisibility(View.GONE);
            List<Notification> loadedNotifications = querySnapshot.toObjects(Notification.class);
            if (loadedNotifications != null && !loadedNotifications.isEmpty()) {
                notificationList.clear();
                notificationList.addAll(loadedNotifications);
                adapter.notifyDataSetChanged();
            } else {
                Toast.makeText(this, "No notifications found.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Error loading notifications: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void deleteNotification(Notification notification, int position) {
        notificationService.deleteNotification(notification.getNotificationId())
                .addOnSuccessListener(aVoid -> {
                    notificationList.remove(position);
                    adapter.notifyItemRemoved(position);
                    adapter.notifyItemRangeChanged(position, notificationList.size());
                    Snackbar.make(rootView, "Notification deleted", Snackbar.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    Snackbar.make(rootView, "Failed to delete notification", Snackbar.LENGTH_LONG).show();
                });
    }
}