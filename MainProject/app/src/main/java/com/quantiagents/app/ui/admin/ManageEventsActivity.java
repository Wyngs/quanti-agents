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
import com.quantiagents.app.models.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Activity for managing events as an admin.
 * Displays all events and allows admins to delete them.
 */
public class ManageEventsActivity extends AppCompatActivity {

    private AdminService adminService;
    private EventAdapter adapter;
    private final List<Event> eventList = new ArrayList<>();
    private ProgressBar progressBar;
    private View rootView;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * Initializes the activity and sets up the event list with delete functionality.
     *
     * @param savedInstanceState The saved instance state bundle
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_events);

        App app = (App) getApplication();
        adminService = app.locator().adminService();

        rootView = findViewById(android.R.id.content);
        progressBar = findViewById(R.id.progress_bar_events);
        RecyclerView recyclerView = findViewById(R.id.recycler_view_events);
        Toolbar toolbar = findViewById(R.id.toolbar_manage_events);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Manage Events");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        adapter = new EventAdapter(eventList, this::deleteEvent);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadEvents();
    }

    /**
     * Loads all events from the database and updates the adapter.
     */
    private void loadEvents() {
        progressBar.setVisibility(View.VISIBLE);

        // FIX: Use the asynchronous getAllEvents method provided by AdminService
        adminService.getAllEvents(
                events -> {
                    // Success Callback
                    progressBar.setVisibility(View.GONE);
                    eventList.clear();
                    if (events != null) {
                        eventList.addAll(events);
                    }
                    adapter.notifyDataSetChanged();
                    if (eventList.isEmpty()) {
                        Toast.makeText(this, "No events found.", Toast.LENGTH_SHORT).show();
                    }
                },
                e -> {
                    // Failure Callback
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error loading events.", Toast.LENGTH_SHORT).show();
                }
        );
    }

    /**
     * Deletes an event and updates the UI.
     *
     * @param event The event to delete
     * @param position The position of the event in the list
     */
    private void deleteEvent(Event event, int position) {
        adminService.removeEvent(
                event.getEventId(),
                true,
                "Admin deleted",
                aVoid -> {
                    eventList.remove(position);
                    adapter.notifyItemRemoved(position);
                    adapter.notifyItemRangeChanged(position, eventList.size());
                    Snackbar.make(rootView, "Deleted: " + event.getTitle(), Snackbar.LENGTH_LONG).show();
                },
                e -> {
                    Snackbar.make(rootView, "Failed to delete event.", Snackbar.LENGTH_LONG).show();
                }
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}