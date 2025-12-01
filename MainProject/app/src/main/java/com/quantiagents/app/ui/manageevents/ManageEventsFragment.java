package com.quantiagents.app.ui.manageevents;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.quantiagents.app.App;
import com.quantiagents.app.Constants.constant;
import com.quantiagents.app.R;
import com.quantiagents.app.Services.EventService;
import com.quantiagents.app.Services.NotificationService;
import com.quantiagents.app.Services.QRCodeService;
import com.quantiagents.app.Services.RegistrationHistoryService;
import com.quantiagents.app.Services.UserService;
import com.quantiagents.app.models.Event;
import com.quantiagents.app.models.Notification;
import com.quantiagents.app.models.QRCode;
import com.quantiagents.app.models.RegistrationHistory;
import com.quantiagents.app.models.User;
import com.quantiagents.app.ui.manageeventinfo.ManageEventInfoHostActivity;

import android.content.Intent;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen that shows all events organized by the current user.
 *
 * Flow:
 *   MainActivity navigation → ManageEventsFragment
 *   → list of "My Organized Events"
 *   → buttons per row: Show QR / Manage Event Info / Delete event.
 */
public class ManageEventsFragment extends Fragment
        implements ManageEventsAdapter.OnManageEventInfoClickListener {

    private TextView totalEventsValue;
    private ProgressBar progressBar;
    private TextView emptyView;
    private RecyclerView recyclerView;
    private ManageEventsAdapter adapter;

    private EventService eventService;
    private UserService userService;
    private QRCodeService qrCodeService;
    private RegistrationHistoryService registrationHistoryService;
    private NotificationService notificationService;

    public static ManageEventsFragment newInstance() {
        return new ManageEventsFragment();
    }

    public ManageEventsFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_manage_events, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        App app = (App) requireActivity().getApplication();
        eventService = app.locator().eventService();
        userService = app.locator().userService();
        qrCodeService = app.locator().qrCodeService();
        registrationHistoryService = app.locator().registrationHistoryService();
        notificationService = app.locator().notificationService();

        totalEventsValue = view.findViewById(R.id.text_total_events_value);
        progressBar = view.findViewById(R.id.progress_manage_events);
        emptyView = view.findViewById(R.id.text_manage_events_empty);
        recyclerView = view.findViewById(R.id.recycler_manage_events);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ManageEventsAdapter(this);
        recyclerView.setAdapter(adapter);

        loadEvents();
    }

    /**
     * Loads all events, filters them to ones organized by the current user,
     * then updates the summary box and the list.
     */
    private void loadEvents() {
        showLoading(true);
        userService.getCurrentUser(
                user -> {
                    if (user == null) {
                        showLoading(false);
                        showEmpty("Profile not found.");
                        return;
                    }
                    String organizerId = user.getUserId();
                    eventService.getAllEvents(
                            events -> {
                                List<Event> mine = filterByOrganizer(events, organizerId);
                                bindEvents(mine);
                            },
                            e -> {
                                showLoading(false);
                                showEmpty("Error loading events.");
                                Toast.makeText(getContext(), "Failed to load events.", Toast.LENGTH_LONG).show();
                            }
                    );
                },
                e -> {
                    showLoading(false);
                    showEmpty("Error loading profile.");
                    Toast.makeText(getContext(), "Failed to load profile.", Toast.LENGTH_LONG).show();
                }
        );
    }

    /**
     * Keep only events for this organizer, and drop ones already CANCELLED.
     */
    private static List<Event> filterByOrganizer(@Nullable List<Event> events, @NonNull String organizerId) {
        List<Event> out = new ArrayList<>();
        if (events == null) {
            return out;
        }
        for (Event ev : events) {
            if (ev == null) continue;
            if (!organizerId.equals(ev.getOrganizerId())) continue;
            if (ev.getStatus() == constant.EventStatus.CANCELLED) continue;
            out.add(ev);
        }
        return out;
    }

    private void bindEvents(@NonNull List<Event> events) {
        showLoading(false);
        totalEventsValue.setText(String.valueOf(events.size()));

        if (events.isEmpty()) {
            showEmpty(getString(R.string.manage_events_empty_default));
            return;
        }

        emptyView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        adapter.submit(events);
    }

    private void showLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.GONE);
        }
    }

    private void showEmpty(@NonNull String message) {
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
        emptyView.setText(message);
    }

    // ------------------------------------------------------------------------
    // Row callbacks
    // ------------------------------------------------------------------------

    /**
     * "Manage Event Info" pressed → launch ManageEventInfoHostActivity.
     */
    @Override
    public void onManageEventInfoClicked(@NonNull Event event) {
        if (event.getEventId() == null || event.getEventId().trim().isEmpty()) {
            Toast.makeText(getContext(), "Event id is missing.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(requireContext(), ManageEventInfoHostActivity.class);
        intent.putExtra(ManageEventInfoHostActivity.EXTRA_EVENT_ID, event.getEventId());
        startActivity(intent);
    }

    /**
     * "Show QR" pressed → fetch QR for this event and show dialog.
     */
    @Override
    public void onShowQrClicked(@NonNull Event event) {
        if (qrCodeService == null) {
            Toast.makeText(requireContext(), R.string.manage_events_qr_error, Toast.LENGTH_SHORT).show();
            return;
        }
        String eventId = event.getEventId();
        if (eventId == null || eventId.trim().isEmpty()) {
            Toast.makeText(requireContext(), R.string.manage_events_qr_error, Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            List<QRCode> codes;
            try {
                codes = qrCodeService.getQRCodesByEventId(eventId);
            } catch (Exception e) {
                codes = null;
            }

            if (!isAdded()) return;

            String qrValue = null;
            if (codes != null && !codes.isEmpty()) {
                qrValue = codes.get(0).getQrCodeValue();
            }

            final String finalQrValue = qrValue;
            requireActivity().runOnUiThread(() -> {
                if (finalQrValue == null || finalQrValue.trim().isEmpty()) {
                    Toast.makeText(requireContext(),
                            R.string.manage_events_qr_not_found,
                            Toast.LENGTH_SHORT).show();
                } else {
                    showQrDialog(finalQrValue, event.getTitle());
                }
            });
        }).start();
    }

    /**
     * Trash icon pressed → confirm, then cancel event + notify entrants.
     */
    @Override
    public void onDeleteEventClicked(@NonNull Event event) {
        String eventId = event.getEventId();
        if (eventId == null || eventId.trim().isEmpty()) {
            Toast.makeText(requireContext(), R.string.manage_events_delete_error, Toast.LENGTH_SHORT).show();
            return;
        }

        String eventName = (event.getTitle() != null && !event.getTitle().trim().isEmpty())
                ? event.getTitle().trim()
                : getString(R.string.manage_events_default_event_name);

        String notificationTitle = getString(R.string.manage_events_delete_notification_title);
        String notificationBodyTemplate = getString(R.string.manage_events_delete_notification_body);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.manage_events_delete_dialog_title)
                .setMessage(R.string.manage_events_delete_dialog_message)
                .setNegativeButton(R.string.manage_events_delete_dialog_cancel, null)
                .setPositiveButton(R.string.manage_events_delete_dialog_confirm,
                        (dialog, which) ->
                                performDeleteEvent(event, eventName, notificationTitle, notificationBodyTemplate))
                .show();
    }

    /**
     * Runs on background thread: notify entrants + mark histories CANCELLED,
     * then on main thread mark the Event as CANCELLED and refresh list.
     */
    private void performDeleteEvent(@NonNull Event event,
                                    @NonNull String eventName,
                                    @NonNull String notificationTitle,
                                    @NonNull String notificationBodyTemplate) {

        showLoading(true);

        new Thread(() -> {
            try {
                String eventId = event.getEventId();
                String organizerId = event.getOrganizerId();

                // 1) Notify WAITLIST / SELECTED / CONFIRMED entrants + mark them CANCELLED
                if (registrationHistoryService != null && notificationService != null &&
                        eventId != null && organizerId != null) {

                    List<RegistrationHistory> regs =
                            registrationHistoryService.getRegistrationHistoriesByEventId(eventId);

                    if (regs != null) {
                        int eventIdInt = Math.abs(eventId.hashCode());
                        int organizerIdInt = Math.abs(organizerId.hashCode());

                        for (RegistrationHistory r : regs) {
                            if (r == null || r.getUserId() == null) continue;

                            constant.EventRegistrationStatus status = r.getEventRegistrationStatus();
                            if (status != constant.EventRegistrationStatus.WAITLIST &&
                                    status != constant.EventRegistrationStatus.SELECTED &&
                                    status != constant.EventRegistrationStatus.CONFIRMED) {
                                continue;
                            }

                            int recipientIdInt = Math.abs(r.getUserId().hashCode());
                            String details = String.format(notificationBodyTemplate, eventName);

                            Notification notification = new Notification(
                                    0,
                                    constant.NotificationType.BAD,
                                    recipientIdInt,
                                    organizerIdInt,
                                    eventIdInt,
                                    notificationTitle,
                                    details
                            );

                            // Fire-and-forget notifications + history updates
                            notificationService.saveNotification(notification, aVoid -> { }, e -> { });

                            r.setEventRegistrationStatus(constant.EventRegistrationStatus.CANCELLED);
                            registrationHistoryService.updateRegistrationHistory(r, aVoid -> { }, e -> { });
                        }
                    }
                }

                if (!isAdded()) return;

                // 2) On main thread: mark Event CANCELLED and refresh
                requireActivity().runOnUiThread(() -> {
                    event.setStatus(constant.EventStatus.CANCELLED);
                    eventService.updateEvent(
                            event,
                            aVoid -> {
                                Toast.makeText(requireContext(),
                                        R.string.manage_events_delete_success,
                                        Toast.LENGTH_SHORT).show();
                                loadEvents();
                            },
                            e -> {
                                showLoading(false);
                                Toast.makeText(requireContext(),
                                        R.string.manage_events_delete_error,
                                        Toast.LENGTH_LONG).show();
                            }
                    );
                });
            } catch (Exception e) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(requireContext(),
                            R.string.manage_events_delete_error,
                            Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    // ------------------------------------------------------------------------
    // QR dialog helpers
    // ------------------------------------------------------------------------

    private void showQrDialog(@NonNull String qrValue, @Nullable String eventTitle) {
        if (!isAdded()) return;

        ImageView imageView = new ImageView(requireContext());
        int size = (int) (300 * getResources().getDisplayMetrics().density / 3); // ~300dp
        imageView.setLayoutParams(new ViewGroup.LayoutParams(size, size));

        Bitmap bitmap = generateQrBitmap(qrValue, size);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                .setView(imageView)
                .setPositiveButton(android.R.string.ok, null);

        String title = (eventTitle == null || eventTitle.trim().isEmpty())
                ? getString(R.string.manage_events_qr_dialog_title)
                : getString(R.string.manage_events_qr_dialog_title_with_name, eventTitle);
        builder.setTitle(title);

        builder.show();
    }

    @Nullable
    private Bitmap generateQrBitmap(@NonNull String value, int sizePx) {
        try {
            BarcodeEncoder encoder = new BarcodeEncoder();
            return encoder.encodeBitmap(value, BarcodeFormat.QR_CODE, sizePx, sizePx);
        } catch (WriterException e) {
            return null;
        }
    }
}
