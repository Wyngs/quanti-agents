package com.quantiagents.app.ui.manageeventinfo;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.quantiagents.app.App;
import com.quantiagents.app.Constants.constant;
import com.quantiagents.app.R;
import com.quantiagents.app.Services.EventService;
import com.quantiagents.app.Services.GeoLocationService;
import com.quantiagents.app.Services.LotteryResultService;
import com.quantiagents.app.Services.RegistrationHistoryService;
import com.quantiagents.app.Services.UserService;
import com.quantiagents.app.models.Event;
import com.quantiagents.app.models.GeoLocation;
import com.quantiagents.app.models.RegistrationHistory;
import com.quantiagents.app.models.User;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.net.Uri;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;

import com.quantiagents.app.Services.ImageService;
import com.quantiagents.app.models.Image;


/**
 * Edit + manage screen for a single Event, including top-level fields
 * and entrant lists via tabs.
 */
public class ManageEventInfoFragment extends Fragment {

    /** FragmentResult key used to notify child pages to reload. */
    public static final String RESULT_REFRESH = "manageeventinfo:refresh";

    private static final String ARG_EVENT_ID = "eventId";

    // Base tab labels (without counts)
    private static final String[] TAB_LABELS = {"Waiting", "Selected", "Confirmed", "Cancelled"};

    // Header inputs
    private TextInputLayout nameLayout;
    private TextInputLayout descriptionLayout;
    private TextInputLayout capacityLayout;
    private TextInputLayout waitingLayout;
    private TextInputLayout priceLayout;

    // NEW layouts to match CreateEventFragment
    private TextInputLayout categoryLayout;
    private TextInputLayout startDateLayout;
    private TextInputLayout endDateLayout;
    private TextInputLayout regStartDateLayout;
    private TextInputLayout regEndDateLayout;
    private TextInputLayout locationLayout;

    private TextInputEditText nameField;
    private TextInputEditText descriptionField;
    private TextInputEditText capacityField;
    private TextInputEditText waitingField;
    private TextInputEditText priceField;

    // NEW input fields
    private TextInputEditText categoryField;
    private TextInputEditText startDateField;
    private TextInputEditText endDateField;
    private TextInputEditText regStartDateField;
    private TextInputEditText regEndDateField;
    private TextInputEditText locationField;

    // Geolocation toggle
    private SwitchMaterial geolocationSwitch;

    // Draw card inputs
    private TextInputLayout drawCountLayout;
    private TextInputEditText drawCountField;

    // Tabs + pager
    private TabLayout tabs;
    private ViewPager2 pager;

    // Services for counts / visibility
    private RegistrationHistoryService regSvc;

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private volatile Event loadedEvent;
    private String eventId;

    // Map-related
    private MapView mapView;
    private GoogleMap googleMap;
    private ClusterManager<EntrantItem> clusterManager;
    private GeoLocationService geoSvc;

    private UserService userService;

    // Date helpers (same format as CreateEvent)
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private final Calendar calendar = Calendar.getInstance();

    // Poster editing
    private ImageService imageService;
    private Uri selectedPosterUri;
    private ActivityResultLauncher<PickVisualMediaRequest> pickPosterLauncher;


    /**
     * Factory method to create a new ManageEventInfoFragment.
     *
     * @param eventId The unique identifier of the event to manage
     * @return A new ManageEventInfoFragment instance
     */
    public static ManageEventInfoFragment newInstance(@NonNull String eventId) {
        Bundle b = new Bundle();
        b.putString(ARG_EVENT_ID, eventId);
        ManageEventInfoFragment f = new ManageEventInfoFragment();
        f.setArguments(b);
        return f;
    }

    /**
     * Required empty public constructor for Fragment instantiation.
     */
    public ManageEventInfoFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_manage_event_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Args
        Bundle args = getArguments();
        String argEventId = (args != null) ? args.getString(ARG_EVENT_ID) : null;
        if (TextUtils.isEmpty(argEventId)) {
            Toast.makeText(getContext(), "Missing event id", Toast.LENGTH_LONG).show();
            return;
        }
        this.eventId = argEventId;

        // Services
        App app = (App) requireActivity().getApplication();
        final EventService evtSvc = app.locator().eventService();
        final LotteryResultService lottoSvc = app.locator().lotteryResultService();
        final RegistrationHistoryService regSvcLocal = app.locator().registrationHistoryService();
        this.regSvc = regSvcLocal;
        this.geoSvc = app.locator().geoLocationService();
        userService = app.locator().userService();
        imageService = app.locator().imageService();


        // MapView
        mapView = view.findViewById(R.id.map_join_locations);
        if (mapView != null) {
            mapView.onCreate(savedInstanceState);
            mapView.getMapAsync(map -> {
                googleMap = map;
                loadGeoMarkers(eventId);
            });
        }

        // Bind header inputs
        nameLayout        = view.findViewById(R.id.input_name_layout);
        descriptionLayout = view.findViewById(R.id.input_description_layout);
        capacityLayout    = view.findViewById(R.id.input_capacity_layout);
        waitingLayout     = view.findViewById(R.id.input_waiting_list_layout);
        priceLayout       = view.findViewById(R.id.input_price_layout);

        nameField         = view.findViewById(R.id.input_name);
        descriptionField  = view.findViewById(R.id.input_description);
        capacityField     = view.findViewById(R.id.input_capacity);
        waitingField      = view.findViewById(R.id.input_waiting_list);
        priceField        = view.findViewById(R.id.input_price);

        // NEW: extra layouts + fields
        categoryLayout     = view.findViewById(R.id.input_category_layout);
        startDateLayout    = view.findViewById(R.id.input_start_date_layout);
        endDateLayout      = view.findViewById(R.id.input_end_date_layout);
        regStartDateLayout = view.findViewById(R.id.input_reg_start_date_layout);
        regEndDateLayout   = view.findViewById(R.id.input_reg_end_date_layout);
        locationLayout     = view.findViewById(R.id.input_location_layout);

        categoryField      = view.findViewById(R.id.input_category);
        startDateField     = view.findViewById(R.id.input_start_date);
        endDateField       = view.findViewById(R.id.input_end_date);
        regStartDateField  = view.findViewById(R.id.input_reg_start_date);
        regEndDateField    = view.findViewById(R.id.input_reg_end_date);
        locationField      = view.findViewById(R.id.input_location);

        geolocationSwitch  = view.findViewById(R.id.switch_geolocation);

        // Wire up date pickers (same UX as CreateEventFragment)
        if (startDateField != null) {
            startDateField.setOnClickListener(v -> showDatePicker(startDateField));
        }
        if (endDateField != null) {
            endDateField.setOnClickListener(v -> showDatePicker(endDateField));
        }
        if (regStartDateField != null) {
            regStartDateField.setOnClickListener(v -> showDatePicker(regStartDateField));
        }
        if (regEndDateField != null) {
            regEndDateField.setOnClickListener(v -> showDatePicker(regEndDateField));
        }

        // Optional: stub for poster editing (keeps layout button from being “dead”)
        MaterialButton posterButton = view.findViewById(R.id.button_edit_poster);
        if (posterButton != null) {
            posterButton.setOnClickListener(v ->
                    pickPosterLauncher.launch(
                            new PickVisualMediaRequest.Builder()
                                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                                    .build()
                    )
            );
        }


        // Draw card inputs
        drawCountLayout   = view.findViewById(R.id.input_draw_count_layout);
        drawCountField    = view.findViewById(R.id.input_draw_count);
        MaterialButton drawButton = view.findViewById(R.id.btnDrawLottery);

        // Buttons
        MaterialButton btnSave   = view.findViewById(R.id.btnSaveEventDetails);
        MaterialButton btnRedraw = view.findViewById(R.id.btnRedrawCanceled);
        MaterialButton btnBack   = view.findViewById(R.id.btnBackToManageEvents);

        // Load Event (blocking read off main thread)
        io.execute(() -> {
            try {
                Event ev = evtSvc.getEventById(eventId);
                loadedEvent = ev;
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> prefill(ev));
            } catch (Exception e) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(
                        () -> Toast.makeText(getContext(), "Load failed", Toast.LENGTH_SHORT).show()
                );
            }
        });

        // Save Button
        btnSave.setOnClickListener(v -> onClickSave(evtSvc));

        // Back Button
        btnBack.setOnClickListener(v -> requireActivity().finish());

        // Draw Button: normal lottery draw from WAITLIST
        drawButton.setOnClickListener(v -> onClickDraw(lottoSvc, eventId));

        // Redraw Button
        btnRedraw.setOnClickListener(v -> onClickRedraw(lottoSvc, regSvcLocal, eventId, btnRedraw));
        updateRedrawVisibility(regSvcLocal, eventId, btnRedraw);

        // Pager + tabs
        pager = view.findViewById(R.id.pager);
        ManageEventInfoPagerAdapter adapter = new ManageEventInfoPagerAdapter(this, eventId);
        pager.setAdapter(adapter);

        tabs = view.findViewById(R.id.tabs);
        new TabLayoutMediator(tabs, pager, (tab, pos) -> tab.setText(TAB_LABELS[pos])).attach();

        // Initial counts for the tabs
        updateTabCounts();
    }

    /**
     * Apply Event values into input fields.
     */
    private void prefill(@Nullable Event ev) {
        if (ev == null) return;

        // Original fields
        if (!TextUtils.isEmpty(ev.getTitle()) && nameField != null) {
            nameField.setText(ev.getTitle());
        }
        if (!TextUtils.isEmpty(ev.getDescription()) && descriptionField != null) {
            descriptionField.setText(ev.getDescription());
        }
        if (capacityField != null) {
            capacityField.setText(String.valueOf((long) ev.getEventCapacity()));
        }
        if (waitingField != null) {
            waitingField.setText(String.valueOf((long) ev.getWaitingListLimit()));
        }
        if (ev.getCost() != null && priceField != null) {
            priceField.setText(String.valueOf(ev.getCost()));
        }

        // NEW: category
        if (!TextUtils.isEmpty(ev.getCategory()) && categoryField != null) {
            categoryField.setText(ev.getCategory());
        }

        // NEW: dates
        if (ev.getEventStartDate() != null && startDateField != null) {
            startDateField.setText(dateFormat.format(ev.getEventStartDate()));
        }
        if (ev.getEventEndDate() != null && endDateField != null) {
            endDateField.setText(dateFormat.format(ev.getEventEndDate()));
        }
        if (ev.getRegistrationStartDate() != null && regStartDateField != null) {
            regStartDateField.setText(dateFormat.format(ev.getRegistrationStartDate()));
        }
        if (ev.getRegistrationEndDate() != null && regEndDateField != null) {
            regEndDateField.setText(dateFormat.format(ev.getRegistrationEndDate()));
        }

        // NEW: location
        if (!TextUtils.isEmpty(ev.getLocation()) && locationField != null) {
            locationField.setText(ev.getLocation());
        }

        // NEW: geolocation toggle
        if (geolocationSwitch != null && ev != null) {
            geolocationSwitch.setChecked(ev.isGeoLocationOn());
        }
    }

    /**
     * Handle Save button.
     */
    private void onClickSave(@NonNull EventService evtSvc) {
        if (loadedEvent == null) {
            Toast.makeText(getContext(), "Event not loaded yet.", Toast.LENGTH_LONG).show();
            return;
        }

        String name      = safe(nameField);
        String desc      = safe(descriptionField);
        String capS      = safe(capacityField);
        String waitS     = safe(waitingField);
        String priceS    = safe(priceField);
        String category  = safe(categoryField);
        String startS    = safe(startDateField);
        String endS      = safe(endDateField);
        String regStartS = safe(regStartDateField);
        String regEndS   = safe(regEndDateField);
        String location  = safe(locationField);

        if (name.isEmpty() || desc.isEmpty()) {
            Toast.makeText(getContext(), "Name and description are required.", Toast.LENGTH_LONG).show();
            return;
        }

        loadedEvent.setTitle(name);
        loadedEvent.setDescription(desc);
        loadedEvent.setCategory(category);
        loadedEvent.setLocation(location);
        if (geolocationSwitch != null) {
            loadedEvent.setGeoLocationOn(geolocationSwitch.isChecked());
        }

        try {
            if (!startS.isEmpty()) {
                loadedEvent.setEventStartDate(dateFormat.parse(startS));
            }
            if (!endS.isEmpty()) {
                loadedEvent.setEventEndDate(dateFormat.parse(endS));
            }
            if (!regStartS.isEmpty()) {
                loadedEvent.setRegistrationStartDate(dateFormat.parse(regStartS));
            }
            if (!regEndS.isEmpty()) {
                loadedEvent.setRegistrationEndDate(dateFormat.parse(regEndS));
            }
        } catch (ParseException e) {
            Toast.makeText(
                    getContext(),
                    "Invalid date format. Use yyyy-mm-dd.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        try {
            if (!capS.isEmpty()) {
                loadedEvent.setEventCapacity(Double.parseDouble(capS));
            }
            if (!waitS.isEmpty()) {
                loadedEvent.setWaitingListLimit(Double.parseDouble(waitS));
            }
            if (!priceS.isEmpty()) {
                loadedEvent.setCost(Double.parseDouble(priceS));
            }
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid numeric input.", Toast.LENGTH_LONG).show();
            return;
        }

        evtSvc.updateEvent(
                loadedEvent,
                aVoid -> {
                    if (selectedPosterUri != null && imageService != null && loadedEvent.getEventId() != null) {
                        savePosterAndAttach(selectedPosterUri, loadedEvent, evtSvc);
                    } else {
                        Toast.makeText(getContext(), "Saved", Toast.LENGTH_SHORT).show();
                        getParentFragmentManager().setFragmentResult(RESULT_REFRESH, new Bundle());
                        updateTabCounts();
                    }
                },
                e -> Toast.makeText(getContext(), "Error saving", Toast.LENGTH_SHORT).show()
        );

    }

    /**
     * Handle the Draw button.
     */
    private void onClickDraw(@NonNull LotteryResultService lottoSvc,
                             @NonNull String eventId) {
        String raw = safe(drawCountField);
        if (raw.isEmpty()) {
            drawCountLayout.setError("Required");
            return;
        }
        int count;
        try {
            count = Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            drawCountLayout.setError("Invalid number");
            return;
        }
        if (count <= 0) {
            drawCountLayout.setError("Must be at least 1");
            return;
        }
        drawCountLayout.setError(null);

        // 🔑 Run the lottery OFF the main thread
        io.execute(() -> {
            lottoSvc.runLottery(
                    eventId,
                    count,
                    result -> {
                        int filled = (result != null && result.getEntrantIds() != null)
                                ? result.getEntrantIds().size()
                                : 0;
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(
                                    getContext(),
                                    "Selected " + filled + " entrant(s) from the waiting list.",
                                    Toast.LENGTH_SHORT
                            ).show();
                            getParentFragmentManager()
                                    .setFragmentResult(RESULT_REFRESH, new Bundle());
                            updateTabCounts();
                        });
                    },
                    e -> {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(
                                        getContext(),
                                        e.getMessage() != null ? e.getMessage() : "Draw failed",
                                        Toast.LENGTH_LONG
                                ).show()
                        );
                    }
            );
        });
    }

    /**
     * Handle "Redraw Canceled" button.
     */
    private void onClickRedraw(@NonNull LotteryResultService lottoSvc,
                               @NonNull RegistrationHistoryService regSvc,
                               @NonNull String eventId,
                               @NonNull MaterialButton btnRedraw) {

        io.execute(() -> {
            List<RegistrationHistory> regs =
                    regSvc.getRegistrationHistoriesByEventId(eventId);

            int cancelledCount = 0;
            if (regs != null) {
                for (RegistrationHistory r : regs) {
                    if (r != null &&
                            r.getEventRegistrationStatus() == constant.EventRegistrationStatus.CANCELLED) {
                        cancelledCount++;
                    }
                }
            }

            if (cancelledCount <= 0) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    btnRedraw.setVisibility(View.GONE);
                    Toast.makeText(
                            getContext(),
                            "No cancelled entrants to redraw.",
                            Toast.LENGTH_SHORT
                    ).show();
                });
                return;
            }

            int drawCount = cancelledCount;

            lottoSvc.runLottery(
                    eventId,
                    drawCount,
                    result -> {
                        int filled = (result != null && result.getEntrantIds() != null)
                                ? result.getEntrantIds().size()
                                : 0;
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(
                                    getContext(),
                                    "Refilled " + filled + " slot(s)",
                                    Toast.LENGTH_SHORT
                            ).show();
                            getParentFragmentManager().setFragmentResult(RESULT_REFRESH, new Bundle());
                            updateRedrawVisibility(regSvc, eventId, btnRedraw);
                            updateTabCounts();
                        });
                    },
                    e -> {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(
                                        getContext(),
                                        e.getMessage() != null ? e.getMessage() : "Refill failed",
                                        Toast.LENGTH_LONG
                                ).show()
                        );
                    }
            );
        });
    }

    /**
     * Updates the visibility of the "Redraw Canceled" button.
     */
    private void updateRedrawVisibility(@NonNull RegistrationHistoryService regSvc,
                                        @NonNull String eventId,
                                        @NonNull MaterialButton btnRedraw) {

        io.execute(() -> {
            List<RegistrationHistory> regs =
                    regSvc.getRegistrationHistoriesByEventId(eventId);

            int cancelledCount = 0;
            if (regs != null) {
                for (RegistrationHistory r : regs) {
                    if (r != null &&
                            r.getEventRegistrationStatus() == constant.EventRegistrationStatus.CANCELLED) {
                        cancelledCount++;
                    }
                }
            }

            if (!isAdded()) return;
            final boolean show = cancelledCount > 0;
            requireActivity().runOnUiThread(() ->
                    btnRedraw.setVisibility(show ? View.VISIBLE : View.GONE)
            );
        });
    }

    /**
     * Compute counts for WAITING / SELECTED / CONFIRMED / CANCELLED and update tab titles.
     */
    private void updateTabCounts() {
        if (regSvc == null || tabs == null || eventId == null) {
            return;
        }

        io.execute(() -> {
            List<RegistrationHistory> regs =
                    regSvc.getRegistrationHistoriesByEventId(eventId);

            int waiting = 0;
            int selected = 0;
            int confirmed = 0;
            int cancelled = 0;

            if (regs != null) {
                for (RegistrationHistory r : regs) {
                    if (r == null || r.getEventRegistrationStatus() == null) continue;
                    switch (r.getEventRegistrationStatus()) {
                        case WAITLIST:
                            waiting++;
                            break;
                        case SELECTED:
                            selected++;
                            break;
                        case CONFIRMED:
                            confirmed++;
                            break;
                        case CANCELLED:
                            cancelled++;
                            break;
                        default:
                            break;
                    }
                }
            }

            if (!isAdded()) return;
            final int fWaiting = waiting;
            final int fSelected = selected;
            final int fConfirmed = confirmed;
            final int fCancelled = cancelled;

            requireActivity().runOnUiThread(() -> {
                setTabLabel(0, TAB_LABELS[0], fWaiting);
                setTabLabel(1, TAB_LABELS[1], fSelected);
                setTabLabel(2, TAB_LABELS[2], fConfirmed);
                setTabLabel(3, TAB_LABELS[3], fCancelled);
            });
        });
    }

    private void setTabLabel(int index, String base, int count) {
        if (tabs == null) return;
        TabLayout.Tab tab = tabs.getTabAt(index);
        if (tab != null) {
            tab.setText(base + " (" + count + ")");
        }
    }

    private static String safe(@Nullable TextInputEditText f) {
        return (f == null || f.getText() == null) ? "" : f.getText().toString().trim();
    }

    // --- Map helpers ---

    private void loadGeoMarkers(String eventId) {
        if (geoSvc == null || googleMap == null) return;

        io.execute(() -> {
            List<GeoLocation> points = geoSvc.getGeoLocationsByEventId(eventId);
            if (points == null || points.isEmpty()) return;

            // Pre-fetch user info off the UI thread so we can show names/emails in clusters.
            List<Pair<GeoLocation, User>> entries = new ArrayList<>();
            for (GeoLocation g : points) {
                User u = userService != null ? userService.getUserById(g.getUserId()) : null;
                entries.add(new Pair<>(g, u));
            }

            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                googleMap.clear();
                clusterManager = new ClusterManager<>(requireContext(), googleMap);
                clusterManager.setRenderer(new DefaultClusterRenderer<EntrantItem>(requireContext(), googleMap, clusterManager) {
                    @Override
                    protected void onBeforeClusterItemRendered(EntrantItem item, MarkerOptions markerOptions) {
                        super.onBeforeClusterItemRendered(item, markerOptions);
                        markerOptions.draggable(false);
                    }

                    @Override
                    protected void onClusterItemRendered(EntrantItem clusterItem, Marker marker) {
                        super.onClusterItemRendered(clusterItem, marker);
                        marker.setDraggable(false);
                    }
                });
                googleMap.setOnCameraIdleListener(clusterManager);
                googleMap.setOnMarkerClickListener(clusterManager);
                googleMap.setOnInfoWindowClickListener(clusterManager);
                googleMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
                    @Override public void onMarkerDragStart(Marker marker) {}
                    @Override public void onMarkerDrag(Marker marker) {
                        marker.setPosition(marker.getPosition()); // snap back
                    }
                    @Override public void onMarkerDragEnd(Marker marker) {
                        marker.setPosition(marker.getPosition()); // snap back
                    }
                });

                LatLngBounds.Builder bounds = new LatLngBounds.Builder();
                for (Pair<GeoLocation, User> entry : entries) {
                    GeoLocation g = entry.first;
                    User u = entry.second;
                    LatLng latLng = new LatLng(g.getLatitude(), g.getLongitude());
                    String title = u != null ? u.getName() : "";
                    String snippet = u != null ? u.getEmail() : "";
                    clusterManager.addItem(new EntrantItem(latLng, title, snippet));
                    bounds.include(latLng);
                }
                clusterManager.cluster();
                googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 80));
            });
        });
    }

    private static class EntrantItem implements ClusterItem {
        private final LatLng position;
        private final String title;
        private final String snippet;

        EntrantItem(LatLng position, String title, String snippet) {
            this.position = position;
            this.title = title;
            this.snippet = snippet;
        }

        @Override public LatLng getPosition() { return position; }
        @Override public String getTitle() { return title; }
        @Override public String getSnippet() { return snippet; }
        @Override public Float getZIndex() { return 0f; }
    }

    private void showDatePicker(@NonNull TextInputEditText field) {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        String existing = safe(field);
        if (!existing.isEmpty()) {
            try {
                Date parsed = dateFormat.parse(existing);
                if (parsed != null) {
                    Calendar c = Calendar.getInstance();
                    c.setTime(parsed);
                    year = c.get(Calendar.YEAR);
                    month = c.get(Calendar.MONTH);
                    day = c.get(Calendar.DAY_OF_MONTH);
                }
            } catch (ParseException ignored) { }
        }

        new DatePickerDialog(
                requireContext(),
                (view, y, m, d) -> {
                    calendar.set(y, m, d);
                    field.setText(dateFormat.format(calendar.getTime()));
                },
                year, month, day
        ).show();
    }

    // --- Fragment / MapView lifecycle wiring ---

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    public void onPause() {
        if (mapView != null) {
            mapView.onPause();
        }
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        if (mapView != null) {
            mapView.onDestroy();
        }
        super.onDestroyView();
        io.shutdownNow();
    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pickPosterLauncher = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    if (uri != null) {
                        selectedPosterUri = uri;

                        // If view is already created, update the button UI
                        View root = getView();
                        if (root != null) {
                            MaterialButton posterButton = root.findViewById(R.id.button_edit_poster);
                            if (posterButton != null) {
                                posterButton.setText("Poster Selected");
                                posterButton.setIconResource(R.drawable.ic_check);
                            }
                        }
                    }
                }
        );
    }
    private void savePosterAndAttach(@NonNull Uri uri,
                                     @NonNull Event event,
                                     @NonNull EventService evtSvc) {

        if (imageService == null || event.getEventId() == null) {
            return;
        }

        Image img = new Image();
        img.setUri(uri.toString());
        img.setEventId(event.getEventId());
        // Optional: track who uploaded it – fall back to organizer id
        img.setUploadedBy(event.getOrganizerId());

        imageService.saveImage(
                img,
                imageId -> {
                    event.setPosterImageId(imageId);
                    evtSvc.updateEvent(
                            event,
                            v -> {
                                Toast.makeText(getContext(), "Saved", Toast.LENGTH_SHORT).show();
                                getParentFragmentManager().setFragmentResult(RESULT_REFRESH, new Bundle());
                                updateTabCounts();
                            },
                            e -> Toast.makeText(
                                    getContext(),
                                    "Poster upload failed after save.",
                                    Toast.LENGTH_SHORT
                            ).show()
                    );
                },
                e -> Toast.makeText(
                        getContext(),
                        "Event saved, but poster upload failed.",
                        Toast.LENGTH_SHORT
                ).show()
        );
    }


}
