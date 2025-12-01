package com.quantiagents.app.ui.myevents;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.quantiagents.app.App;
import com.quantiagents.app.Constants.constant;
import com.quantiagents.app.R;
import com.quantiagents.app.Services.EventService;
import com.quantiagents.app.Services.RegistrationHistoryService;
import com.quantiagents.app.Services.UserService;
import com.quantiagents.app.models.Event;
import com.quantiagents.app.models.RegistrationHistory;
import com.quantiagents.app.models.User;
import com.quantiagents.app.ui.ViewEventDetailsFragment;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fragment that displays details for a single event.
 * Kept for compatibility, but logic mirrors Adapter ViewHolder.
 * Shows event information and registration status with action buttons.
 */
public class SingleEventFragment extends Fragment {

    /**
     * Creates a new instance of SingleEventFragment.
     *
     * @return A new SingleEventFragment instance
     */
    public static SingleEventFragment newInstance() { return new SingleEventFragment(); }

    private UserService userService;
    private EventService eventService;
    private RegistrationHistoryService regService;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private ProgressBar progress;
    private TextView title, statusChip, description;
    private TextView textDate, textRegStatus, textPrice, textCapacity, textOrganizer;
    private MaterialButton primaryAction, secondaryAction, viewEventAction;

    private String eventId;
    private constant.EventRegistrationStatus currentStatus;
    private Event event;
    private RegistrationHistory history;
    private User currentUser;
    private User organizerUser;
    private final DateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_single_event, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        App app = (App) requireActivity().getApplication();
        userService = app.locator().userService();
        eventService = app.locator().eventService();
        regService = app.locator().registrationHistoryService();

        bindViews(view);

        if (getArguments() != null) {
            eventId = getArguments().getString("eventId", "");
            String statusStr = getArguments().getString("registrationStatus", "");
            if (!TextUtils.isEmpty(statusStr)) {
                try {
                    currentStatus = constant.EventRegistrationStatus.valueOf(statusStr);
                } catch (Exception ignored) {}
            }
        }

        if (!TextUtils.isEmpty(eventId)) loadAll();
    }

    /**
     * Binds all view references from the layout.
     *
     * @param v The root view of the fragment
     */
    private void bindViews(View v) {
        progress = v.findViewById(R.id.single_progress);
        title = v.findViewById(R.id.single_title);
        statusChip = v.findViewById(R.id.single_status_chip);
        description = v.findViewById(R.id.single_description);
        textDate = v.findViewById(R.id.single_text_date);
        textRegStatus = v.findViewById(R.id.single_text_reg_status);
        textPrice = v.findViewById(R.id.single_text_price);
        textCapacity = v.findViewById(R.id.single_text_capacity);
        textOrganizer = v.findViewById(R.id.single_text_organizer);
        primaryAction = v.findViewById(R.id.single_action_primary);
        secondaryAction = v.findViewById(R.id.single_action_secondary);
        viewEventAction = v.findViewById(R.id.single_action_view);

        viewEventAction.setOnClickListener(l -> {
            if(eventId != null) {
                Fragment f = ViewEventDetailsFragment.newInstance(eventId);
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.content_container, f).addToBackStack(null).commit();
            }
        });
    }

    private void loadAll() {
        if(progress != null) progress.setVisibility(View.VISIBLE);
        userService.getCurrentUser(user -> {
            currentUser = user;
            executor.execute(() -> {
                if (!isAdded()) return;
                event = eventService.getEventById(eventId);
                history = regService.getRegistrationHistoryByEventIdAndUserId(eventId, user.getUserId());
                if(event != null) organizerUser = userService.getUserById(event.getOrganizerId());

                requireActivity().runOnUiThread(this::bindModel);
            });
        }, e -> {});
    }

    private void bindModel() {
        if (!isAdded() || event == null) return;
        if(progress != null) progress.setVisibility(View.GONE);

        title.setText(event.getTitle());
        description.setText(event.getDescription());
        textDate.setText(dateFmt.format(event.getEventStartDate()));
        textPrice.setText(String.format(Locale.US, "$%.2f", event.getCost()));
        textCapacity.setText(String.format(Locale.US, "0/%d capacity", (int)event.getEventCapacity()));
        textOrganizer.setText("Organized by " + (organizerUser != null ? organizerUser.getName() : "Unknown"));

        // Basic status chip logic
        statusChip.setText(currentStatus != null ? currentStatus.toString() : "Unknown");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}