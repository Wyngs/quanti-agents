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

public class ManageEventsActivity extends AppCompatActivity {

    private AdminService adminService;
    private EventAdapter adapter;
    private final List<Event> eventList = new ArrayList<>();
    private ProgressBar progressBar;
    private View rootView;

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

        setupRecyclerView(recyclerView);
        loadEvents();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void setupRecyclerView(RecyclerView recyclerView) {
        adapter = new EventAdapter(eventList, this::deleteEvent);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadEvents() {
        progressBar.setVisibility(View.VISIBLE);

        adminService.listAllEvents().addOnSuccessListener(querySnapshot -> {
            progressBar.setVisibility(View.GONE);
            List<Event> loadedEvents = querySnapshot.toObjects(Event.class);

            if (loadedEvents != null && !loadedEvents.isEmpty()) {
                eventList.clear();
                eventList.addAll(loadedEvents);
                adapter.notifyDataSetChanged();
            } else {
                Toast.makeText(this, "No events found.", Toast.LENGTH_LONG).show();
            }
        }).addOnFailureListener(e -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Error loading events: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    private void deleteEvent(Event event, int position) {
        adminService.removeEvent(event.getEventId(), true, "Admin deleted event")
                .addOnSuccessListener(aVoid -> {
                    eventList.remove(position);
                    adapter.notifyItemRemoved(position);
                    adapter.notifyItemRangeChanged(position, eventList.size());
                    Snackbar.make(rootView, "Deleted: " + event.getEventName(), Snackbar.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    Snackbar.make(rootView, "Failed to delete event.", Snackbar.LENGTH_LONG).show();
                });
    }
}