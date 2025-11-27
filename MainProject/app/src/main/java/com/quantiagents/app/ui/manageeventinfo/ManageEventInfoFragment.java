package com.quantiagents.app.ui.manageeventinfo;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.gms.maps.model.Marker;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.quantiagents.app.App;
import com.quantiagents.app.R;
import com.quantiagents.app.Services.EventService;
import com.quantiagents.app.Services.LotteryResultService;
import com.quantiagents.app.Services.UserService;
import com.quantiagents.app.models.Event;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.quantiagents.app.Services.GeoLocationService;
import com.quantiagents.app.models.GeoLocation;
import java.util.ArrayList;
import java.util.List;

import com.quantiagents.app.models.User;
import java.util.HashMap;
import java.util.Map;



public class ManageEventInfoFragment extends Fragment {

    public static final String RESULT_REFRESH = "manageeventinfo:refresh";
    private static final String ARG_EVENT_ID = "eventId";

    private TextInputLayout nameLayout, descriptionLayout, capacityLayout, waitingLayout, priceLayout;
    private TextInputEditText nameField, descriptionField, capacityField, waitingField, priceField;

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private volatile Event loadedEvent;

    private MapView mapView;
    private GoogleMap googleMap;
    private GeoLocationService geoSvc;

    private UserService userService;
    private final Map<String, User> markerUsers = new HashMap<>();



    public static ManageEventInfoFragment newInstance(@NonNull String eventId) {
        Bundle b = new Bundle();
        b.putString(ARG_EVENT_ID, eventId);
        ManageEventInfoFragment f = new ManageEventInfoFragment();
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_manage_event_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Bundle args = getArguments();
        String eventId = (args != null) ? args.getString(ARG_EVENT_ID) : null;
        if (TextUtils.isEmpty(eventId)) {
            Toast.makeText(getContext(), "Missing event id", Toast.LENGTH_LONG).show();
            return;
        }

        App app = (App) requireActivity().getApplication();
        final EventService evtSvc = app.locator().eventService();
        final LotteryResultService lottoSvc = app.locator().lotteryResultService();
        geoSvc = app.locator().geoLocationService();
        userService = app.locator().userService();


        mapView = view.findViewById(R.id.map_join_locations);
        if (mapView != null) {
            mapView.onCreate(savedInstanceState);
            mapView.getMapAsync(map -> {
                googleMap = map;
                loadGeoMarkers(eventId);
            });
        }


        // Bind inputs
        nameLayout = view.findViewById(R.id.input_name_layout);
        descriptionLayout = view.findViewById(R.id.input_description_layout);
        capacityLayout = view.findViewById(R.id.input_capacity_layout);
        waitingLayout = view.findViewById(R.id.input_waiting_list_layout);
        priceLayout = view.findViewById(R.id.input_price_layout);

        nameField = view.findViewById(R.id.input_name);
        descriptionField = view.findViewById(R.id.input_description);
        capacityField = view.findViewById(R.id.input_capacity);
        waitingField = view.findViewById(R.id.input_waiting_list);
        priceField = view.findViewById(R.id.input_price);

        // Load Event (Background)
        io.execute(() -> {
            try {
                Event ev = evtSvc.getEventById(eventId);
                loadedEvent = ev;
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> prefill(ev));
                }
            } catch (Exception e) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Load failed", Toast.LENGTH_SHORT).show());
                }
            }
        });

        // Save Button
        view.findViewById(R.id.btnSaveEventDetails).setOnClickListener(v -> onClickSave(evtSvc));

        // Redraw/Refill Button
        view.findViewById(R.id.btnRedrawCanceled).setOnClickListener(v -> onClickRedraw(lottoSvc, eventId));

        // Back Button
        view.findViewById(R.id.btnBackToManageEvents).setOnClickListener(v -> requireActivity().finish());

        // Pager
        ViewPager2 pager = view.findViewById(R.id.pager);
        ManageEventInfoPagerAdapter adapter = new ManageEventInfoPagerAdapter(this, eventId);
        pager.setAdapter(adapter);

        TabLayout tabs = view.findViewById(R.id.tabs);
        final String[] labels = {"Waiting", "Selected", "Confirmed", "Canceled"};
        new TabLayoutMediator(tabs, pager, (tab, pos) -> tab.setText(labels[pos])).attach();
    }

    private void prefill(@Nullable Event ev) {
        if (ev == null) return;
        nameField.setText(ev.getTitle());
        descriptionField.setText(ev.getDescription());
        capacityField.setText(String.valueOf(ev.getEventCapacity()));
        waitingField.setText(String.valueOf(ev.getWaitingListLimit()));
        if (ev.getCost() != null) priceField.setText(String.valueOf(ev.getCost()));
    }

    private void onClickSave(EventService evtSvc) {
        if (loadedEvent == null) return;

        // Validation simplified for brevity
        loadedEvent.setTitle(nameField.getText().toString().trim());
        loadedEvent.setDescription(descriptionField.getText().toString().trim());
        try {
            loadedEvent.setEventCapacity(Integer.parseInt(capacityField.getText().toString().trim()));
            loadedEvent.setWaitingListLimit(Integer.parseInt(waitingField.getText().toString().trim()));
            loadedEvent.setCost(Double.parseDouble(priceField.getText().toString().trim()));
        } catch (Exception ignored) {}

        evtSvc.updateEvent(loadedEvent,
                aVoid -> {
                    Toast.makeText(getContext(), "Saved", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().setFragmentResult(RESULT_REFRESH, new Bundle());
                },
                e -> Toast.makeText(getContext(), "Error saving", Toast.LENGTH_SHORT).show()
        );
    }

    private void onClickRedraw(LotteryResultService lottoSvc, String eventId) {
        // Logic adapted from old EntrantInfoFragment: calls refillCanceledSlots
        lottoSvc.refillCanceledSlots(eventId,
                result -> {
                    int count = (result != null && result.getEntrantIds() != null) ? result.getEntrantIds().size() : 0;
                    Toast.makeText(getContext(), "Refilled " + count + " slots", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().setFragmentResult(RESULT_REFRESH, new Bundle());
                },
                e -> Toast.makeText(getContext(), "Refill failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
        );
    }

    private void loadGeoMarkers(String eventId) {
        if (geoSvc == null || googleMap == null) return;
        new Thread(() -> {
            List<GeoLocation> points = geoSvc.getGeoLocationsByEventId(eventId);
            if (points == null) points = new ArrayList<>();

            // Pre-fetch users off the UI thread to avoid blocking the main thread.
            List<Pair<GeoLocation, User>> entries = new ArrayList<>();
            for (GeoLocation g : points) {
                User u = userService.getUserById(g.getUserId());
                entries.add(new Pair<>(g, u));
            }

            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    googleMap.clear();
                    markerUsers.clear();
                    googleMap.setOnMarkerClickListener(marker -> {
                        marker.showInfoWindow();
                        return true; // consume the click
                    });

                    LatLngBounds.Builder bounds = new LatLngBounds.Builder();
                    for (Pair<GeoLocation, User> entry : entries) {
                        GeoLocation g = entry.first;
                        User u = entry.second;
                        LatLng latLng = new LatLng(g.getLatitude(), g.getLongitude());
                        Marker marker = googleMap.addMarker(new MarkerOptions().position(latLng));
                        markerUsers.put(marker.getId(), u);
                        if (u != null) {
                            marker.setTitle(u.getName());
                            marker.setSnippet(u.getEmail()); // or phone/username if preferred
                        }
                        bounds.include(latLng);
                    }
                    if (!entries.isEmpty()) {
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 80));
                    }
                });
            }
        }).start();
    }


    @Override
    public void onDestroyView() {
        if (mapView != null) mapView.onDestroy();
        super.onDestroyView();
        io.shutdownNow();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    public void onPause() {
        if (mapView != null) mapView.onPause();
        super.onPause();
    }


    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) mapView.onLowMemory();
    }

}
