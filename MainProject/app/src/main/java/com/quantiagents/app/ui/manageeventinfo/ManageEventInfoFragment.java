package com.quantiagents.app.ui.manageeventinfo;

import android.os.Bundle;
import android.text.TextUtils;
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
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.quantiagents.app.App;
import com.quantiagents.app.Constants.constant;
import com.quantiagents.app.R;
import com.quantiagents.app.Services.EventService;
import com.quantiagents.app.Services.GeoLocationService;
import com.quantiagents.app.Services.LotteryResultService;
import com.quantiagents.app.Services.RegistrationHistoryService;
import com.quantiagents.app.models.Event;
import com.quantiagents.app.models.GeoLocation;
import com.quantiagents.app.models.RegistrationHistory;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    private TextInputEditText nameField;
    private TextInputEditText descriptionField;
    private TextInputEditText capacityField;
    private TextInputEditText waitingField;
    private TextInputEditText priceField;

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
    private GeoLocationService geoSvc;

    /**
     * Factory method to create a new ManageEventInfoFragment.
     */
    public static ManageEventInfoFragment newInstance(@NonNull String eventId) {
        Bundle b = new Bundle();
        b.putString(ARG_EVENT_ID, eventId);
        ManageEventInfoFragment f = new ManageEventInfoFragment();
        f.setArguments(b);
        return f;
    }

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
        if (!TextUtils.isEmpty(ev.getTitle())) {
            nameField.setText(ev.getTitle());
        }
        if (!TextUtils.isEmpty(ev.getDescription())) {
            descriptionField.setText(ev.getDescription());
        }
        capacityField.setText(String.valueOf((long) ev.getEventCapacity()));
        waitingField.setText(String.valueOf((long) ev.getWaitingListLimit()));
        if (ev.getCost() != null) {
            priceField.setText(String.valueOf(ev.getCost()));
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

        String name  = safe(nameField);
        String desc  = safe(descriptionField);
        String capS  = safe(capacityField);
        String waitS = safe(waitingField);
        String priceS = safe(priceField);

        if (name.isEmpty() || desc.isEmpty()) {
            Toast.makeText(getContext(), "Name and description are required.", Toast.LENGTH_LONG).show();
            return;
        }

        loadedEvent.setTitle(name);
        loadedEvent.setDescription(desc);

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
                    Toast.makeText(getContext(), "Saved", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().setFragmentResult(RESULT_REFRESH, new Bundle());
                    updateTabCounts();
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

            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                googleMap.clear();
                LatLngBounds.Builder bounds = new LatLngBounds.Builder();
                for (GeoLocation g : points) {
                    LatLng latLng = new LatLng(g.getLatitude(), g.getLongitude());
                    googleMap.addMarker(new MarkerOptions().position(latLng));
                    bounds.include(latLng);
                }
                googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 80));
            });
        });
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
}
