package com.quantiagents.app.ui.manageeventinfo;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.quantiagents.app.App;
import com.quantiagents.app.Constants.constant;
import com.quantiagents.app.R;
import com.quantiagents.app.Services.EventService;
import com.quantiagents.app.Services.NotificationService;
import com.quantiagents.app.Services.RegistrationHistoryService;
import com.quantiagents.app.Services.UserService;
import com.quantiagents.app.models.Event;
import com.quantiagents.app.models.Notification;
import com.quantiagents.app.models.RegistrationHistory;
import com.quantiagents.app.models.User;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * One tab in ManageEventInfo: shows WAITING / SELECTED / CONFIRMED / CANCELLED
 * and adds "Notify all" + "Export CSV" actions for that list.
 */
public class ManageEventInfoListFragment extends Fragment {

    private static final String ARG_EVENT = "eventId";
    private static final String ARG_STATUS = "status";

    private String eventId;
    private String statusArg;
    private constant.EventRegistrationStatus statusFilter;

    private SwipeRefreshLayout swipe;
    private TextView empty;
    private ManageEventInfoUserAdapter adapter;
    private RegistrationHistoryService regSvc;

    private MaterialButton notifyAllButton;
    private MaterialButton exportCsvButton;

    private NotificationService notificationService;
    private EventService eventService;
    private UserService userService;

    // The registrations currently displayed in this tab
    private final List<RegistrationHistory> currentRegistrations = new ArrayList<>();

    public static ManageEventInfoListFragment newInstance(@NonNull String eventId,
                                                          @NonNull String status) {
        Bundle b = new Bundle();
        b.putString(ARG_EVENT, eventId);
        b.putString(ARG_STATUS, status);
        ManageEventInfoListFragment f = new ManageEventInfoListFragment();
        f.setArguments(b);
        return f;
    }

    public ManageEventInfoListFragment() {
        // Required empty constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context ctx = requireContext();
        notificationService = new NotificationService(ctx);
        eventService = new EventService(ctx);
        userService = new UserService(ctx);
        regSvc = ((App) requireActivity().getApplication()).locator().registrationHistoryService();

        Bundle args = getArguments();
        if (args != null) {
            eventId = args.getString(ARG_EVENT);
            statusArg = args.getString(ARG_STATUS);
        }

        if (statusArg != null) {
            try {
                statusFilter = constant.EventRegistrationStatus.valueOf(
                        statusArg.toUpperCase(Locale.US)
                );
            } catch (IllegalArgumentException e) {
                statusFilter = null; // fallback to string compare if needed
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_manage_event_info_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView rv = view.findViewById(R.id.list);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ManageEventInfoUserAdapter();
        adapter.setStatusFilter(statusFilter);
        adapter.setOnCancelClickListener(this::onCancelEntrant);
        rv.setAdapter(adapter);

        swipe = view.findViewById(R.id.swipe);
        empty = view.findViewById(R.id.empty);

        notifyAllButton = view.findViewById(R.id.button_notify_all);
        exportCsvButton = view.findViewById(R.id.button_export_csv);

        swipe.setOnRefreshListener(this::load);

        // Refresh when parent fragment (ManageEventInfoFragment) tells us to
        getParentFragmentManager().setFragmentResultListener(
                ManageEventInfoFragment.RESULT_REFRESH,
                this,
                (k, b) -> load()
        );

        notifyAllButton.setOnClickListener(v -> notifyAllEntrants());
        exportCsvButton.setOnClickListener(v -> exportListToCsv());

        load();
    }

    // --- Data loading ---

    private void load() {
        if (eventId == null || regSvc == null) {
            return;
        }

        swipe.setRefreshing(true);

        new Thread(() -> {
            List<RegistrationHistory> all = regSvc.getRegistrationHistoriesByEventId(eventId);
            List<RegistrationHistory> filtered = new ArrayList<>();

            if (all != null) {
                for (RegistrationHistory r : all) {
                    if (r == null || r.getEventRegistrationStatus() == null) continue;

                    if (statusFilter != null) {
                        if (r.getEventRegistrationStatus() == statusFilter) {
                            filtered.add(r);
                        }
                    } else if (statusArg != null &&
                            r.getEventRegistrationStatus().name().equalsIgnoreCase(statusArg)) {
                        filtered.add(r);
                    }
                }
            }

            // Update our in-memory list used by the buttons
            synchronized (currentRegistrations) {
                currentRegistrations.clear();
                currentRegistrations.addAll(filtered);
            }

            if (!isAdded()) return;

            requireActivity().runOnUiThread(() -> {
                swipe.setRefreshing(false);
                adapter.submit(filtered);
                empty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);

                boolean hasItems = !filtered.isEmpty();
                notifyAllButton.setEnabled(hasItems);
                exportCsvButton.setEnabled(hasItems);
            });
        }).start();
    }

    // --- Cancel one entrant (trash icon) ---

    private void onCancelEntrant(@NonNull RegistrationHistory history) {
        // Only meaningful for SELECTED tab, but adapter already hides the icon elsewhere.
        history.setEventRegistrationStatus(constant.EventRegistrationStatus.CANCELLED);

        regSvc.updateRegistrationHistory(
                history,
                aVoid -> {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(
                                requireContext(),
                                "Entrant moved to cancelled.",
                                Toast.LENGTH_SHORT
                        ).show();
                        load(); // refresh lists
                    });
                },
                e -> {
                    Log.e("ManageEventInfo", "Failed to cancel entrant", e);
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(
                                    requireContext(),
                                    "Failed to cancel entrant.",
                                    Toast.LENGTH_SHORT
                            ).show()
                    );
                }
        );
    }

    // --- Notify All ---

    private void notifyAllEntrants() {
        List<RegistrationHistory> snapshot;
        synchronized (currentRegistrations) {
            snapshot = new ArrayList<>(currentRegistrations);
        }

        if (snapshot.isEmpty()) {
            Toast.makeText(requireContext(), R.string.notify_all_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        if (eventId == null) {
            Toast.makeText(requireContext(), R.string.notify_all_event_missing, Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            Event event = eventService.getEventById(eventId);
            if (event == null || event.getOrganizerId() == null) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(),
                                    R.string.notify_all_event_missing,
                                    Toast.LENGTH_SHORT).show()
                    );
                }
                return;
            }

            String eventName = (event.getTitle() != null && !event.getTitle().trim().isEmpty())
                    ? event.getTitle().trim()
                    : "Event";

            int eventIdInt = Math.abs(eventId.hashCode());
            int organizerIdInt = Math.abs(event.getOrganizerId().hashCode());

            // Decide canned message + type based on which tab this is
            constant.NotificationType type;
            String statusText;
            String detailsText;

            switch (statusFilter) {
                case WAITLIST:
                    type = constant.NotificationType.GOOD;
                    statusText = "Waiting List Update";
                    detailsText = "You are currently on the waiting list for event: "
                            + eventName + ". We will notify you if a spot opens up.";
                    break;
                case SELECTED:
                    type = constant.NotificationType.GOOD;
                    statusText = "Selected for Event";
                    detailsText = "You have been selected for event: "
                            + eventName + ". Please open the app to accept or decline your spot.";
                    break;
                case CONFIRMED:
                    type = constant.NotificationType.GOOD;
                    statusText = "Event Reminder";
                    detailsText = "You are confirmed for event: "
                            + eventName + ". We look forward to seeing you there!";
                    break;
                case CANCELLED:
                    type = constant.NotificationType.BAD;
                    statusText = "Registration Cancelled";
                    detailsText = "Your registration for event: "
                            + eventName
                            + " has been cancelled. Please contact the organizer if you have questions.";
                    break;
                default:
                    type = constant.NotificationType.REMINDER;
                    statusText = "Event Update";
                    detailsText = "There is an update for event: " + eventName + ".";
                    break;
            }

            for (RegistrationHistory history : snapshot) {
                if (history == null || history.getUserId() == null) continue;

                String userId = history.getUserId().trim();
                if (userId.isEmpty()) continue;

                int recipientIdInt = Math.abs(userId.hashCode());

                Notification notification = new Notification(
                        0,          // auto-generate ID
                        type,
                        recipientIdInt,
                        organizerIdInt,
                        eventIdInt,
                        statusText,
                        detailsText
                );

                notificationService.saveNotification(
                        notification,
                        aVoid -> { /* best effort, no UI needed */ },
                        e -> Log.e("ManageEventInfo", "Failed to send bulk notification", e)
                );
            }

            if (isAdded()) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(),
                                R.string.notify_all_success,
                                Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    // --- Export CSV ---

    private void exportListToCsv() {
        List<RegistrationHistory> snapshot;
        synchronized (currentRegistrations) {
            snapshot = new ArrayList<>(currentRegistrations);
        }

        if (snapshot.isEmpty()) {
            Toast.makeText(requireContext(), R.string.notify_all_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            StringBuilder sb = new StringBuilder();
            sb.append("Name,Email,Status\n");

            for (RegistrationHistory history : snapshot) {
                if (history == null || history.getUserId() == null) continue;

                String userId = history.getUserId().trim();
                if (userId.isEmpty()) continue;

                User user = userService.getUserById(userId);

                String name = "";
                String email = "";

                if (user != null) {
                    if (user.getName() != null && !user.getName().trim().isEmpty()) {
                        name = user.getName().trim();
                    } else if (user.getUsername() != null && !user.getUsername().trim().isEmpty()) {
                        name = user.getUsername().trim();
                    }

                    if (user.getUsername() != null && !user.getUsername().trim().isEmpty()) {
                        email = user.getUsername().trim();
                    }
                }

                sb.append(escapeCsv(name)).append(",")
                        .append(escapeCsv(email)).append(",")
                        .append(statusFilter != null ? statusFilter.name() : "")
                        .append("\n");
            }

            File dir = requireContext().getExternalFilesDir(null);
            if (dir == null) {
                dir = requireContext().getFilesDir();
            }

            String safeStatus = statusFilter != null
                    ? statusFilter.name().toLowerCase(Locale.US)
                    : "list";

            String fileName = "entrants_" + safeStatus + "_" + System.currentTimeMillis() + ".csv";
            File csvFile = new File(dir, fileName);

            try (FileWriter writer = new FileWriter(csvFile)) {
                writer.write(sb.toString());
                writer.flush();

                if (isAdded()) {
                    final String path = csvFile.getAbsolutePath();
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(
                                    requireContext(),
                                    getString(R.string.export_csv_success) + "\n" + path,
                                    Toast.LENGTH_LONG
                            ).show()
                    );
                }
            } catch (IOException e) {
                Log.e("ManageEventInfo", "Failed to write CSV", e);
                if (isAdded()) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(),
                                    R.string.export_csv_error,
                                    Toast.LENGTH_SHORT).show()
                    );
                }
            }
        }).start();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        String v = value.replace("\"", "\"\"");
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            return "\"" + v + "\"";
        }
        return v;
    }
}
