package com.quantiagents.app.ui.myevents;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * SingleEventFragment
 *
 * Renders one event row for "My Events".
 * - Reads "eventId" and "registrationStatus" from arguments.
 * - Optional "assignable" boolean argument. If true, buttons allow reassignment.
 * - Uses Services only. No direct Firebase calls here.
 *
 * Args:
 *   - eventId: String (required)
 *   - registrationStatus: String of constant.EventRegistrationStatus (required)
 *   - assignable: boolean (optional, default false)
 */
public class SingleEventFragment extends Fragment {

    public static SingleEventFragment newInstance() {
        return new SingleEventFragment();
    }

    // Services
    private UserService userService;
    private EventService eventService;
    private RegistrationHistoryService regService;

    // UI
    private View root;
    private ProgressBar progress;
    private TextView title;
    private TextView statusChip;
    private TextView dateRange;
    private TextView regWindow;
    private TextView price;
    private TextView capacity;
    private TextView desc;
    private View imageStub; // TODO image later
    private View actionsContainer;
    private MaterialButton primaryAction;
    private MaterialButton secondaryAction;
    private MaterialButton neutralAction;

    // Model
    private String eventId;
    private constant.EventRegistrationStatus currentStatus;
    private boolean assignable;

    private Event event;                  // resolved event
    private RegistrationHistory history;  // resolved registration
    private User currentUser;             // resolved user

    private final DateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        // fragment_single_event lives in res/layout
        root = inflater.inflate(R.layout.fragment_single_event, container, false);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        App app = (App) requireActivity().getApplication();
        userService = app.locator().userService();
        eventService = app.locator().eventService();
        regService = app.locator().registrationHistoryService();

        bindViews(view);
        parseArgsOrFail();
        loadAll();
    }

    private void bindViews(@NonNull View v) {
        progress = v.findViewById(R.id.single_progress);
        title = v.findViewById(R.id.single_title);
        statusChip = v.findViewById(R.id.single_status_chip);
        dateRange = v.findViewById(R.id.single_date_range);
        regWindow = v.findViewById(R.id.single_reg_window);
        price = v.findViewById(R.id.single_price);
        capacity = v.findViewById(R.id.single_capacity);
        desc = v.findViewById(R.id.single_description);
        imageStub = v.findViewById(R.id.single_poster); // TODO implement image later
        actionsContainer = v.findViewById(R.id.single_actions_container);
        primaryAction = v.findViewById(R.id.single_action_primary);
        secondaryAction = v.findViewById(R.id.single_action_secondary);
        neutralAction = v.findViewById(R.id.single_action_neutral);
    }

    private void parseArgsOrFail() {
        Bundle args = getArguments();
        if (args == null) {
            throw new IllegalStateException("SingleEventFragment requires arguments");
        }
        eventId = args.getString("eventId", "");
        String statusStr = args.getString("registrationStatus", "");
        assignable = args.getBoolean("assignable", false);

        if (TextUtils.isEmpty(eventId) || TextUtils.isEmpty(statusStr)) {
            throw new IllegalStateException("SingleEventFragment missing required args");
        }
        currentStatus = constant.EventRegistrationStatus.valueOf(statusStr);
    }

    /**
     * Load user, registration, and event. Keep it simple. Background thread, then paint UI.
     */
    private void loadAll() {
        setBusy(true);

        userService.getCurrentUser(
                user -> {
                    currentUser = user;
                    new Thread(() -> {
                        // Resolve registration and event
                        history = regService.getRegistrationHistoryByEventIdAndUserId(eventId, user.getUserId());
                        event = eventService.getEventById(eventId);

                        requireActivity().runOnUiThread(() -> {
                            setBusy(false);
                            bindModelToUi();
                            bindActions();
                        });
                    }).start();
                },
                e -> {
                    // No user, just show nothing helpful
                    setBusy(false);
                    title.setText("Failed to load user.");
                    actionsContainer.setVisibility(View.GONE);
                }
        );
    }

    private void setBusy(boolean busy) {
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        root.setAlpha(busy ? 0.7f : 1f);
    }

    /**
     * Paints the known model values into the card.
     */
    private void bindModelToUi() {
        // Title
        title.setText(event != null && !TextUtils.isEmpty(event.getTitle()) ? event.getTitle() : "Untitled event");

        // Status chip
        constant.EventStatus evStatus = event != null ? event.getStatus() : null;
        String chip = labelForStatus(currentStatus);
        if (evStatus == constant.EventStatus.CLOSED) {
            chip = chip + " • Closed";
        }
        statusChip.setText(chip);

        // Date range
        String dr = fmtRange(event != null ? event.getEventStartDate() : null, event != null ? event.getEventEndDate() : null);
        dateRange.setText("Event: " + dr);

        // Registration window
        String rw = fmtRange(event != null ? event.getRegistrationStartDate() : null, event != null ? event.getRegistrationEndDate() : null);
        regWindow.setText("Registration: " + rw);

        // Price
        price.setText(event != null && event.getCost() != null ? "$" + trimDouble(event.getCost()) : "$0");

        // Capacity
        String cap = "Capacity: " + (event != null ? (int) event.getEventCapacity() : 0);
        capacity.setText(cap);

        // Desc
        desc.setText(event != null && !TextUtils.isEmpty(event.getDescription()) ? event.getDescription() : "No description.");

        // Poster
        imageStub.setVisibility(View.GONE); // TODO will implement image later per your note
    }

    private String fmtRange(@Nullable Date a, @Nullable Date b) {
        if (a == null && b == null) return "TBD";
        if (a == null) return "Until " + dateFmt.format(b);
        if (b == null) return dateFmt.format(a);
        return dateFmt.format(a) + " to " + dateFmt.format(b);
    }

    private String trimDouble(double d) {
        // Show "10" instead of "10.0" where possible
        long asLong = (long) d;
        return (asLong == d) ? String.valueOf(asLong) : String.valueOf(d);
    }

    private String labelForStatus(constant.EventRegistrationStatus s) {
        switch (s) {
            case WAITLIST: return "Waiting";
            case SELECTED: return "Selected";
            case CONFIRMED: return "Confirmed";
            case CANCELLED: return "Cancelled";
        }
        return "Unknown";
    }

    /**
     * Sets up action buttons for the current status.
     * If assignable is false, the row is read-only.
     */
    /**
     * Sets up action buttons for the current status.
     * If assignable is false, the row is read-only.
     * Buttons currently do nothing – click listeners contain // TODO hooks.
     */
    private void bindActions() {
        if (!assignable) {
            actionsContainer.setVisibility(View.GONE);
            return;
        }

        // Default hidden
        actionsContainer.setVisibility(View.VISIBLE);
        primaryAction.setVisibility(View.GONE);
        secondaryAction.setVisibility(View.GONE);
        neutralAction.setVisibility(View.GONE);

        // Status-driven actions (labels match the mock)
        if (currentStatus == constant.EventRegistrationStatus.WAITLIST) {
            // Leave waiting list
            primaryAction.setText("Leave Waiting List");
            primaryAction.setVisibility(View.VISIBLE);
            primaryAction.setOnClickListener(v -> {
                // TODO: hook up "leave waiting list" action
                // e.g., applyStatusChange(constant.EventRegistrationStatus.CANCELLED);
            });

        } else if (currentStatus == constant.EventRegistrationStatus.SELECTED) {
            // Accept or Decline
            primaryAction.setText("Accept");
            secondaryAction.setText("Decline");
            primaryAction.setVisibility(View.VISIBLE);
            secondaryAction.setVisibility(View.VISIBLE);

            primaryAction.setOnClickListener(v -> {
                // TODO: hook up "accept" action
                // e.g., applyStatusChange(constant.EventRegistrationStatus.CONFIRMED);
            });
            secondaryAction.setOnClickListener(v -> {
                // TODO: hook up "decline" action
                // e.g., applyStatusChange(constant.EventRegistrationStatus.CANCELLED);
            });

        } else if (currentStatus == constant.EventRegistrationStatus.CONFIRMED) {
            // Cancel
            primaryAction.setText("Cancel");
            primaryAction.setVisibility(View.VISIBLE);
            primaryAction.setOnClickListener(v -> {
                // TODO: hook up "cancel" action
                // e.g., applyStatusChange(constant.EventRegistrationStatus.CANCELLED);
            });

        } else {
            // CANCELLED or anything else: no actions
            actionsContainer.setVisibility(View.GONE);
        }
    }


    /**
     * Main handler to reassign the registration status and keep Event lists in sync.
     * Uses Services synchronously on a background thread.
     */
    private void applyStatusChange(@NonNull constant.EventRegistrationStatus next) {
        setBusy(true);
        new Thread(() -> {
            // Refresh fresh copies
            RegistrationHistory rh = regService.getRegistrationHistoryByEventIdAndUserId(eventId, currentUser.getUserId());
            Event ev = eventService.getEventById(eventId);

            if (rh == null || ev == null) {
                runUi(() -> setBusy(false));
                return;
            }

            // 1) Update RegistrationHistory
            rh.setEventRegistrationStatus(next);
            // If you later add timestamps (selectedAt etc.) update them here.

            regService.updateRegistrationHistory(rh,
                    aVoid -> {
                        // 2) Update Event lists to reflect the new status
                        updateEventMembership(ev, currentUser.getUserId(), next);

                        // Persist event
                        eventService.updateEvent(ev,
                                ok -> runUi(() -> {
                                    // Update local state and repaint
                                    history = rh;
                                    event = ev;
                                    currentStatus = next;
                                    setBusy(false);
                                    bindModelToUi();
                                    bindActions();
                                }),
                                err -> runUi(() -> {
                                    setBusy(false);
                                    // Rollback in UI? keep simple. User can retry.
                                })
                        );
                    },
                    err -> runUi(() -> setBusy(false))
            );
        }).start();
    }

    /**
     * Moves the user id between Event lists based on the new status.
     * This is a simple helper that edits the four arrays and avoids duplicates.
     */
    private void updateEventMembership(@NonNull Event ev, @NonNull String userId, @NonNull constant.EventRegistrationStatus next) {
        // Remove from all lists first
        ev.setWaitingList(removeIfPresent(ev.getWaitingList(), userId));
        ev.setSelectedList(removeIfPresent(ev.getSelectedList(), userId));
        ev.setConfirmedList(removeIfPresent(ev.getConfirmedList(), userId));
        ev.setCancelledList(removeIfPresent(ev.getCancelledList(), userId));

        // Add to the target list
        if (next == constant.EventRegistrationStatus.WAITLIST) {
            ev.setWaitingList(addOnce(ev.getWaitingList(), userId));
        } else if (next == constant.EventRegistrationStatus.SELECTED) {
            ev.setSelectedList(addOnce(ev.getSelectedList(), userId));
        } else if (next == constant.EventRegistrationStatus.CONFIRMED) {
            ev.setConfirmedList(addOnce(ev.getConfirmedList(), userId));
        } else if (next == constant.EventRegistrationStatus.CANCELLED) {
            ev.setCancelledList(addOnce(ev.getCancelledList(), userId));
        }
    }

    private List<String> removeIfPresent(@Nullable List<String> list, @NonNull String id) {
        if (list == null) return new ArrayList<>();
        ArrayList<String> copy = new ArrayList<>(list.size());
        for (String s : list) if (s != null && !s.equals(id)) copy.add(s);
        return copy;
    }

    private List<String> addOnce(@Nullable List<String> list, @NonNull String id) {
        ArrayList<String> out = new ArrayList<>();
        if (list != null) {
            for (String s : list) {
                if (s == null || s.equals(id)) continue;
                out.add(s);
            }
        }
        out.add(id);
        return out;
    }

    // UI helper used by background threads
    private void runUi(@NonNull Runnable r) {
        if (!isAdded()) return;                 // fragment not attached
        requireActivity().runOnUiThread(r);     // hop to main thread
    }


}
