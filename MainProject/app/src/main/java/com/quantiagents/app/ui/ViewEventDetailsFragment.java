package com.quantiagents.app.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.quantiagents.app.App;
import com.quantiagents.app.Constants.constant;
import com.quantiagents.app.R;
import com.quantiagents.app.Services.EventService;
import com.quantiagents.app.Services.GeoLocationService;
import com.quantiagents.app.Services.ImageService;
import com.quantiagents.app.Services.NotificationService;
import com.quantiagents.app.Services.RegistrationHistoryService;
import com.quantiagents.app.Services.ServiceLocator;
import com.quantiagents.app.Services.QRCodeService;
import com.quantiagents.app.Services.UserService;
import com.quantiagents.app.models.Event;
import com.quantiagents.app.models.GeoLocation;
import com.quantiagents.app.models.Image;
import com.quantiagents.app.models.Notification;
import com.quantiagents.app.models.QRCode;
import com.quantiagents.app.models.RegistrationHistory;
import com.quantiagents.app.models.User;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.content.res.ColorStateList;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import android.annotation.SuppressLint;



/**
 * Fragment that displays entrant-facing details for a single event, including waiting-list actions.
 */
public class ViewEventDetailsFragment extends Fragment {

    private static final String ARG_EVENT_ID = "event_id";
    private static final double RANDOM_OFFSET_RANGE = 0.02; // ~2km jitter for mock geolocation

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final DateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());

    private String eventId;

    private EventService eventService;
    private UserService userService;
    private RegistrationHistoryService registrationHistoryService;
    private NotificationService notificationService;
    private QRCodeService qrCodeService;
    private GeoLocationService geoLocationService;
    private ImageService imageService;

    // Views
    private MaterialButton buttonBack;
    private MaterialButton buttonErrorBack;
    private MaterialButton buttonToggleQr;
    private MaterialButton buttonJoin;
    private MaterialButton buttonLeave;
    private MaterialButton buttonRegistrationClosed;
    private MaterialButton buttonViewPoster;
    private ProgressBar progressBar;
    private View cardContent;
    private View layoutError;
    private LinearLayout qrContainer;
    private ImageView imageQrCode;
    private TextView textQrValue;
    private TextView textTitle;
    private TextView textDescription;
    private TextView textDateRange;
    private TextView textPrice;
    private TextView textCapacity;
    private TextView textOrganizer;
    private TextView textRegistrationOpen;
    private TextView textRegistrationClose;
    private TextView textWaitingListCount;
    private MaterialCardView cardUserStatus;
    private TextView textUserStatus;
    private TextView textUserStatusDetail;

    // Cached state
    private Event currentEvent;
    private User currentUser;
    private User organizerUser;
    private RegistrationHistory currentEntry;
    private List<RegistrationHistory> waitingEntries = new ArrayList<>();
    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;
    private Location pendingJoinLocation;

    private String qrCodeValue;
    private boolean showQr;

    public ViewEventDetailsFragment() {
        // Required empty public constructor
    }

    /**
     * Factory method for creating a ViewEventDetailsFragment.
     *
     * @param eventId Firestore identifier of the event to display.
     */
    public static ViewEventDetailsFragment newInstance(@NonNull String eventId) {
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        ViewEventDetailsFragment fragment = new ViewEventDetailsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_view_event_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindViews(view);
        setupClickListeners();

        Bundle args = getArguments();
        eventId = args != null ? args.getString(ARG_EVENT_ID) : null;

        App app = (App) requireActivity().getApplication();
        ServiceLocator locator = app.locator();
        eventService = locator.eventService();
        userService = locator.userService();
        registrationHistoryService = locator.registrationHistoryService();
        notificationService = locator.notificationService();
        qrCodeService = locator.qrCodeService();
        geoLocationService = locator.geoLocationService();
        imageService = locator.imageService();
        notificationService = locator.notificationService();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean granted = Boolean.TRUE.equals(result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false))
                            || Boolean.TRUE.equals(result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false));
                    if (granted) {
                        fetchLocationAndJoin();
                    } else {
                        setActionEnabled(true);
                        Toast.makeText(getContext(), "Location is required to join this event.", Toast.LENGTH_LONG).show();
                    }
                });

        if (TextUtils.isEmpty(eventId)) {
            showError(getString(R.string.view_event_error_message));
            return;
        }

        loadContent();
    }

    private void bindViews(@NonNull View view) {
        buttonBack = view.findViewById(R.id.button_back);
        buttonErrorBack = view.findViewById(R.id.button_error_back);
        buttonToggleQr = view.findViewById(R.id.button_toggle_qr);
        buttonJoin = view.findViewById(R.id.button_join_waitlist);
        buttonLeave = view.findViewById(R.id.button_leave_waitlist);
        buttonRegistrationClosed = view.findViewById(R.id.button_registration_closed);
        buttonViewPoster = view.findViewById(R.id.button_view_poster);
        progressBar = view.findViewById(R.id.progress_loading);
        cardContent = view.findViewById(R.id.card_content);
        layoutError = view.findViewById(R.id.layout_error);
        qrContainer = view.findViewById(R.id.layout_qr_container);
        imageQrCode = view.findViewById(R.id.image_qr_code);
        textQrValue = view.findViewById(R.id.text_qr_code_value);
        textTitle = view.findViewById(R.id.text_event_title);
        textDescription = view.findViewById(R.id.text_event_description);
        textDateRange = view.findViewById(R.id.text_event_date_range);
        textPrice = view.findViewById(R.id.text_event_price);
        textCapacity = view.findViewById(R.id.text_event_capacity);
        textOrganizer = view.findViewById(R.id.text_event_organizer);
        textRegistrationOpen = view.findViewById(R.id.text_registration_open);
        textRegistrationClose = view.findViewById(R.id.text_registration_close);
        textWaitingListCount = view.findViewById(R.id.text_waiting_list_count);
        cardUserStatus = view.findViewById(R.id.card_user_status);
        textUserStatus = view.findViewById(R.id.text_user_status);
        textUserStatusDetail = view.findViewById(R.id.text_user_status_detail);
    }

    private void setupClickListeners() {
        buttonBack.setOnClickListener(v -> handleBackNavigation());
        buttonErrorBack.setOnClickListener(v -> handleBackNavigation());
        buttonToggleQr.setOnClickListener(v -> {
            showQr = !showQr;
            updateQrSection();
        });
        buttonJoin.setOnClickListener(v -> joinWaitingList());
        buttonLeave.setOnClickListener(v -> leaveWaitingList());
        buttonViewPoster.setOnClickListener(v -> showPoster());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        io.shutdownNow();
    }

    private void handleBackNavigation() {
        if (!isAdded()) {
            return;
        }
        requireActivity().getOnBackPressedDispatcher().onBackPressed();
    }

    private void loadContent() {
        setLoading(true);
        io.execute(() -> {
            Event event = null;
            User organizer = null;
            User activeUser = null;
            RegistrationHistory userEntry = null;
            List<RegistrationHistory> histories = new ArrayList<>();
            List<QRCode> qrCodes = new ArrayList<>();

            try {
                event = eventService.getEventById(eventId);
                if (event != null) {
                    if (!TextUtils.isEmpty(event.getOrganizerId())) {
                        organizer = userService.getUserById(event.getOrganizerId());
                    }
                    activeUser = userService.getCurrentUser();
                    histories = registrationHistoryService.getRegistrationHistoriesByEventId(eventId);
                    if (activeUser != null && !TextUtils.isEmpty(activeUser.getUserId())) {
                        userEntry = registrationHistoryService.getRegistrationHistoryByEventIdAndUserId(eventId, activeUser.getUserId());
                        if (userEntry == null) {
                            userEntry = findEntryByUser(histories, activeUser.getUserId());
                        }
                    }
                    qrCodes = qrCodeService.getQRCodesByEventId(eventId);
                }
            } catch (Exception e) {
                final String message = e.getMessage();
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    setLoading(false);
                    showError(message == null ? getString(R.string.view_event_error_message) : message);
                });
                return;
            }

            final Event loadedEvent = event;
            final User loadedOrganizer = organizer;
            final User loadedUser = activeUser;
            final RegistrationHistory loadedEntry = userEntry;
            final List<RegistrationHistory> loadedHistories = histories;
            final String loadedQr = (!qrCodes.isEmpty() ? qrCodes.get(0).getQrCodeValue() : null);

            if (!isAdded()) {
                return;
            }

            requireActivity().runOnUiThread(() ->
                    bindData(loadedEvent, loadedOrganizer, loadedUser, loadedHistories, loadedEntry, loadedQr));
        });
    }

    private void bindData(@Nullable Event event,
                          @Nullable User organizer,
                          @Nullable User user,
                          @NonNull List<RegistrationHistory> histories,
                          @Nullable RegistrationHistory entry,
                          @Nullable String qrValue) {
        setLoading(false);

        if (event == null) {
            showError(getString(R.string.view_event_error_message));
            return;
        }

        this.currentEvent = event;
        this.organizerUser = organizer;
        this.currentUser = user;
        this.currentEntry = entry;
        this.waitingEntries = histories;
        this.qrCodeValue = qrValue;
        setActionEnabled(true);

        layoutError.setVisibility(View.GONE);
        cardContent.setVisibility(View.VISIBLE);

        // Title & description
        String title = TextUtils.isEmpty(event.getTitle())
                ? getString(R.string.view_event_untitled)
                : event.getTitle();
        textTitle.setText(title);
        if (TextUtils.isEmpty(event.getDescription())) {
            textDescription.setVisibility(View.GONE);
        } else {
            textDescription.setVisibility(View.VISIBLE);
            textDescription.setText(event.getDescription());
        }

        // Dates
        textDateRange.setText(formatDateRange(event.getEventStartDate(), event.getEventEndDate()));
        textRegistrationOpen.setText(getString(R.string.view_event_registration_opens, formatDate(event.getRegistrationStartDate())));
        textRegistrationClose.setText(getString(R.string.view_event_registration_closes, formatDate(event.getRegistrationEndDate())));

        // Price
        String priceText = event.getCost() == null || event.getCost() <= 0
                ? getString(R.string.view_event_price_free)
                : currencyFormat.format(event.getCost());
        textPrice.setText(getString(R.string.view_event_price_label, priceText));

        // Capacity
        double capacityRaw = event.getEventCapacity();
        String capacityValue;
        if (capacityRaw > 0) {
            capacityValue = getString(R.string.view_event_capacity_format, trimTrailingZeros(capacityRaw));
        } else {
            capacityValue = getString(R.string.view_event_capacity_unknown);
        }
        textCapacity.setText(getString(R.string.view_event_capacity_label, capacityValue));

        // Organizer
        if (organizer != null && !TextUtils.isEmpty(organizer.getName())) {
            textOrganizer.setVisibility(View.VISIBLE);
            textOrganizer.setText(getString(R.string.view_event_organizer_format, organizer.getName()));
        } else {
            textOrganizer.setVisibility(View.GONE);
        }

        // Waiting list count
        int waitingCount = countWaiting(histories);
        textWaitingListCount.setText(getString(R.string.view_event_waiting_list_count, waitingCount));

        // Poster Button Visibility
        if (event.getPosterImageId() != null && !event.getPosterImageId().isEmpty()) {
            buttonViewPoster.setVisibility(View.VISIBLE);
        } else {
            buttonViewPoster.setVisibility(View.GONE);
        }

        // Update status card & action buttons
        updateUserStatusCard(entry);
        updateActionButtons(waitingCount);
        updateQrSection();
    }

    private void updateUserStatusCard(@Nullable RegistrationHistory entry) {
        if (entry == null) {
            cardUserStatus.setVisibility(View.GONE);
            return;
        }
        constant.EventRegistrationStatus status = normalizeStatus(entry.getEventRegistrationStatus());
        if (status == null) {
            cardUserStatus.setVisibility(View.GONE);
            return;
        }
        cardUserStatus.setVisibility(View.VISIBLE);
        switch (status) {
            case WAITLIST:
                textUserStatus.setText(R.string.view_event_status_waiting_heading);
                textUserStatusDetail.setText(R.string.view_event_status_waiting_detail);
                break;
            case SELECTED:
                textUserStatus.setText(R.string.view_event_status_selected_heading);
                textUserStatusDetail.setText(R.string.view_event_status_selected_detail);
                break;
            case CONFIRMED:
                textUserStatus.setText(R.string.view_event_status_confirmed_heading);
                textUserStatusDetail.setText(R.string.view_event_status_confirmed_detail);
                break;
            case CANCELLED:
                textUserStatus.setText(R.string.view_event_status_cancelled_heading);
                textUserStatusDetail.setText(R.string.view_event_status_cancelled_detail);
                break;
            default:
                cardUserStatus.setVisibility(View.GONE);
                break;
        }
    }

    private void updateActionButtons(int waitingCount) {
        boolean registrationOpen = isRegistrationOpen(currentEvent);
        boolean hasEntry = currentEntry != null;
        constant.EventRegistrationStatus status = hasEntry ? normalizeStatus(currentEntry.getEventRegistrationStatus()) : null;

        double waitLimit = currentEvent != null ? currentEvent.getWaitingListLimit() : 0;
        boolean waitListFull = waitLimit > 0 && waitingCount >= waitLimit;

        boolean canJoin = registrationOpen && !hasEntry && !waitListFull;
        boolean canLeave = status == constant.EventRegistrationStatus.WAITLIST;

        buttonJoin.setVisibility(canJoin ? View.VISIBLE : View.GONE);
        buttonJoin.setEnabled(canJoin);

        buttonLeave.setVisibility(canLeave ? View.VISIBLE : View.GONE);
        buttonLeave.setEnabled(canLeave);

        boolean showClosed = !registrationOpen && !hasEntry;
        buttonRegistrationClosed.setVisibility(showClosed ? View.VISIBLE : View.GONE);
        buttonRegistrationClosed.setEnabled(false);
    }

    private void updateQrSection() {
        qrContainer.setVisibility(showQr ? View.VISIBLE : View.GONE);
        buttonToggleQr.setText(showQr ? R.string.view_event_hide_qr : R.string.view_event_show_qr);

        if (!showQr) {
            return;
        }

        if (!TextUtils.isEmpty(qrCodeValue)) {
            textQrValue.setVisibility(View.VISIBLE);
            textQrValue.setText(qrCodeValue);
        } else {
            textQrValue.setVisibility(View.VISIBLE);
            textQrValue.setText(R.string.view_event_qr_not_available);
        }

        imageQrCode.setVisibility(View.GONE); // Placeholder: no QR bitmap generation yet.
    }
    @SuppressLint("MissingPermission")
    private void joinWaitingList() {
        if (currentEvent == null) return;
        if (currentUser == null || TextUtils.isEmpty(currentUser.getUserId())) {
            Toast.makeText(getContext(), R.string.view_event_error_missing_user, Toast.LENGTH_LONG).show();
            return;
        }

        // Fix: Prevent organizer from joining their own event
        if (currentUser.getUserId().equals(currentEvent.getOrganizerId())) {
            Toast.makeText(getContext(), "Organizers cannot join their own events.", Toast.LENGTH_LONG).show();
            return;
        }

        int waitingCount = countWaiting(waitingEntries);
        double waitLimit = currentEvent.getWaitingListLimit();
        if (waitLimit > 0 && waitingCount >= waitLimit) {
            Toast.makeText(getContext(), R.string.view_event_toast_waitlist_full, Toast.LENGTH_SHORT).show();
            return;
        }

        if (eventRequiresLocation() && !hasLocationPermission()) {
            setActionEnabled(false);
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
            return;
        }


        setActionEnabled(false);

        // Get location if event requires it, otherwise proceed without location
        if (eventRequiresLocation() && hasLocationPermission()) {
            // Event requires location and we have permission - get location first
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(loc -> {
                        pendingJoinLocation = loc;
                        proceedWithRegistration();
                    })
                    .addOnFailureListener(e -> {
                        if (!isAdded()) return;
                        setActionEnabled(true);
                        Toast.makeText(getContext(), "Could not get location.", Toast.LENGTH_LONG).show();
                    });
        } else {
            // Event doesn't require location - proceed without location
            pendingJoinLocation = null;
            proceedWithRegistration();
        }
    }

    private void proceedWithRegistration() {
        if (currentEvent == null || currentUser == null) {
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> setActionEnabled(true));
            }
            return;
        }

        RegistrationHistory history = new RegistrationHistory(
                currentEvent.getEventId(),
                currentUser.getUserId(),
                constant.EventRegistrationStatus.WAITLIST,
                new Date()
        );

        registrationHistoryService.saveRegistrationHistory(history,
                aVoid -> {
                    if (currentEvent.getWaitingList() == null) currentEvent.setWaitingList(new ArrayList<>());
                    if (!currentEvent.getWaitingList().contains(currentUser.getUserId())) {
                        currentEvent.getWaitingList().add(currentUser.getUserId());
                        eventService.updateEvent(currentEvent, v -> {}, e -> Log.e("ViewEvent", "Failed to sync waiting list", e));
                    }

                    // Save real geo only if required and location available
                    if (eventRequiresLocation() && pendingJoinLocation != null) {
                        handleGeoLocationJoin(pendingJoinLocation);
                    }

                    // Send notifications to organizer and user
                    sendRegistrationNotifications(currentEvent, currentUser);

                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), R.string.view_event_toast_join_success, Toast.LENGTH_SHORT).show();
                        loadContent();
                    });
                },
                e -> {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        setActionEnabled(true);
                        Toast.makeText(getContext(), R.string.view_event_toast_join_failure, Toast.LENGTH_LONG).show();
                    });
                });
    }
    private boolean eventRequiresLocation() {
        return currentEvent != null && currentEvent.isGeoLocationOn();
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("MissingPermission")
    private void fetchLocationAndJoin() {
        if (!hasLocationPermission()) {
            setActionEnabled(true);
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(loc -> {
                    pendingJoinLocation = loc;
                    doJoinWithLocation();
                })
                .addOnFailureListener(e -> {
                    setActionEnabled(true);
                    Toast.makeText(getContext(), "Could not get location.", Toast.LENGTH_LONG).show();
                });
    }

    private void doJoinWithLocation() {
        if (pendingJoinLocation == null) {
            Toast.makeText(getContext(), "Location unavailable. Please try again.", Toast.LENGTH_LONG).show();
            setActionEnabled(true);
            return;
        }
        handleGeoLocationJoin(pendingJoinLocation);
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> {
            Toast.makeText(getContext(), R.string.view_event_toast_join_success, Toast.LENGTH_SHORT).show();
            loadContent();
        });
    }


    private void handleGeoLocationJoin(Location loc) {
        if (currentEvent == null || !currentEvent.isGeoLocationOn()) return;
        if (geoLocationService == null || currentUser == null || TextUtils.isEmpty(currentUser.getUserId())) return;
        if (loc == null) return;

        GeoLocation geo = new GeoLocation(loc.getLatitude(), loc.getLongitude(), currentUser.getUserId(), currentEvent.getEventId());
        geoLocationService.saveGeoLocation(geo, docId -> { }, e -> { });
    }


    private void leaveWaitingList() {
        if (currentEvent == null || currentUser == null || TextUtils.isEmpty(currentUser.getUserId())) {
            return;
        }
        setActionEnabled(false);
        registrationHistoryService.deleteRegistrationHistory(currentEvent.getEventId(), currentUser.getUserId(),
                aVoid -> {
                    // Fix: Sync Event waiting list remove
                    if (currentEvent.getWaitingList() != null && currentEvent.getWaitingList().contains(currentUser.getUserId())) {
                        currentEvent.getWaitingList().remove(currentUser.getUserId());
                        eventService.updateEvent(currentEvent, v -> {}, e -> Log.e("ViewEvent", "Failed to sync waiting list remove", e));
                    }

                    if (!isAdded()) return;
                    if (currentEvent.isGeoLocationOn() && geoLocationService != null) {
                        geoLocationService.deleteGeoLocation(currentUser.getUserId(), currentEvent.getEventId(),
                                unused -> { },
                                err -> { });
                    }
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), R.string.view_event_toast_leave_success, Toast.LENGTH_SHORT).show();
                        loadContent();
                    });
                },
                e -> {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        setActionEnabled(true);
                        Toast.makeText(getContext(), R.string.view_event_toast_leave_failure, Toast.LENGTH_LONG).show();
                    });
                });
    }

    /**
     * Fetches the image object and displays the URI in a dialog.
     */
    private void showPoster() {
        if (currentEvent == null || currentEvent.getPosterImageId() == null) return;

        io.execute(() -> {
            Image img = imageService.getImageById(currentEvent.getPosterImageId());
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    if (img != null && img.getUri() != null) {
                        showImageDialog(img.getUri());
                    } else {
                        Toast.makeText(getContext(), "Poster not found", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void showImageDialog(String uri) {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_image_viewer);

        ImageView imageView = dialog.findViewById(R.id.image_preview);
        if (imageView != null) {
            Glide.with(this).load(uri).into(imageView);
        }

        dialog.show();
        // Make dialog larger
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void setActionEnabled(boolean enabled) {
        if (buttonJoin != null) {
            buttonJoin.setEnabled(enabled);
        }
        if (buttonLeave != null) {
            buttonLeave.setEnabled(enabled);
        }
        if (buttonToggleQr != null) {
            buttonToggleQr.setEnabled(enabled);
        }
        if (buttonViewPoster != null) {
            buttonViewPoster.setEnabled(enabled);
        }
    }

    private void showError(@Nullable String message) {
        cardContent.setVisibility(View.GONE);
        layoutError.setVisibility(View.VISIBLE);
        TextView messageView = layoutError.findViewById(R.id.text_error_message);
        if (!TextUtils.isEmpty(message)) {
            messageView.setText(message);
        }
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) {
            cardContent.setVisibility(View.GONE);
            layoutError.setVisibility(View.GONE);
            setActionEnabled(false);
        }
    }

    private String formatDateRange(@Nullable Date start, @Nullable Date end) {
        String startText = formatDate(start);
        String endText = formatDate(end);
        if (TextUtils.isEmpty(startText) && TextUtils.isEmpty(endText)) {
            return "";
        }
        return getString(R.string.view_event_date_range, startText, endText);
    }

    private String formatDate(@Nullable Date date) {
        return date == null ? "--" : dateFormat.format(date);
    }

    private boolean isRegistrationOpen(@Nullable Event event) {
        if (event == null) return false;
        Date now = new Date();
        Date start = event.getRegistrationStartDate();
        Date end = event.getRegistrationEndDate();
        if (start == null || end == null) return false;
        return !now.before(start) && !now.after(end);
    }

    private int countWaiting(@NonNull List<RegistrationHistory> histories) {
        int count = 0;
        for (RegistrationHistory history : histories) {
            if (history == null) continue;
            if (normalizeStatus(history.getEventRegistrationStatus()) == constant.EventRegistrationStatus.WAITLIST) {
                count++;
            }
        }
        return count;
    }

    private constant.EventRegistrationStatus normalizeStatus(@Nullable Object status) {
        if (status instanceof constant.EventRegistrationStatus) {
            return (constant.EventRegistrationStatus) status;
        }
        if (status == null) {
            return null;
        }
        try {
            String value = String.valueOf(status).toUpperCase(Locale.US);
            if ("WAITING".equals(value)) {
                value = "WAITLIST";
            } else if ("CANCELED".equals(value)) {
                value = "CANCELLED";
            }
            return constant.EventRegistrationStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Nullable
    private RegistrationHistory findEntryByUser(@NonNull List<RegistrationHistory> histories, @NonNull String userId) {
        for (RegistrationHistory history : histories) {
            if (history == null) continue;
            if (userId.equals(history.getUserId())) {
                return history;
            }
        }
        return null;
    }

    private GeoLocation generateMockLocation(@NonNull String userId, @NonNull String eventId) {
        double baseLat = 53.5461;
        double baseLng = -113.4938;
        double latOffset = (Math.random() - 0.5) * RANDOM_OFFSET_RANGE;
        double lngOffset = (Math.random() - 0.5) * RANDOM_OFFSET_RANGE;
        return new GeoLocation(baseLat + latOffset, baseLng + lngOffset, userId, eventId);
    }

    private String trimTrailingZeros(double value) {
        long longValue = (long) value;
        if (value == longValue) {
            return String.valueOf(longValue);
        }
        return String.format(Locale.getDefault(), "%.2f", value);
    }

    /**
     * Sends notifications to both the organizer and the user when a user signs up for an event.
     */
    private void sendRegistrationNotifications(Event event, User user) {
        if (event == null || user == null) return;

        String eventId = event.getEventId();
        String userId = user.getUserId();
        String organizerId = event.getOrganizerId();

        if (eventId == null || userId == null || organizerId == null) return;

        // Convert String IDs to int for notifications
        int eventIdInt = Math.abs(eventId.hashCode());
        int userIdInt = Math.abs(userId.hashCode());
        int organizerIdInt = Math.abs(organizerId.hashCode());

        // Notification for the organizer (REMINDER type)
        Notification organizerNotification = new Notification(
                0, // Auto-generate ID
                constant.NotificationType.REMINDER,
                organizerIdInt,
                0, // senderId (system)
                eventIdInt
        );

        // Notification for the user (GOOD type - confirmation)
        Notification userNotification = new Notification(
                0, // Auto-generate ID
                constant.NotificationType.GOOD,
                userIdInt,
                0, // senderId (system)
                eventIdInt
        );

        // Save organizer notification
        notificationService.saveNotification(organizerNotification,
                aVoid -> Log.d("ViewEvent", "Notification sent to organizer"),
                e -> Log.e("ViewEvent", "Failed to send notification to organizer", e)
        );

        // Save user notification
        notificationService.saveNotification(userNotification,
                aVoid -> Log.d("ViewEvent", "Notification sent to user"),
                e -> Log.e("ViewEvent", "Failed to send notification to user", e)
        );
    }
}