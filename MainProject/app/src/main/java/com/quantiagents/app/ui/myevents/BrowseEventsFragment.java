package com.quantiagents.app.ui.myevents;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.quantiagents.app.Services.GeoLocationService;
import com.quantiagents.app.models.GeoLocation;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.quantiagents.app.App;
import com.quantiagents.app.Constants.constant;
import com.quantiagents.app.R;
import com.quantiagents.app.Services.EventService;
import com.quantiagents.app.Services.RegistrationHistoryService;
import com.quantiagents.app.Services.UserService;
import com.quantiagents.app.models.Event;
import com.quantiagents.app.models.RegistrationHistory;
import com.quantiagents.app.models.User;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BrowseEventsFragment extends Fragment implements BrowseEventsAdapter.OnEventClick {

    public static BrowseEventsFragment newInstance() {
        return new BrowseEventsFragment();
    }

    private EventService eventService;
    private RegistrationHistoryService regService;
    private UserService userService;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private GeoLocationService geoLocationService;
    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;
    private Event pendingEvent;
    private User pendingUser;


    private SwipeRefreshLayout swipe;
    private ProgressBar progress;
    private RecyclerView list;
    private TextView empty;
    private EditText search;
    private MaterialButton filterButton;

    private final List<Event> allEvents = new ArrayList<>();
    private BrowseEventsAdapter adapter;

    // Filter State
    private String filterCategory = "";
    private Date filterDate = null;
    private boolean filterAvailableOnly = false;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_browse_events, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        App app = (App) requireActivity().getApplication();
        eventService = app.locator().eventService();
        regService = app.locator().registrationHistoryService();
        userService = app.locator().userService();

        geoLocationService = app.locator().geoLocationService();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean granted = Boolean.TRUE.equals(result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false))
                            || Boolean.TRUE.equals(result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false));
                    if (granted) {
                        continueJoinWithLocation();
                    } else {
                        progress.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Location required for this event.", Toast.LENGTH_LONG).show();
                    }
                });

        swipe = v.findViewById(R.id.swipe);
        progress = v.findViewById(R.id.progress);
        list = v.findViewById(R.id.recycler);
        empty = v.findViewById(R.id.text_empty);
        search = v.findViewById(R.id.input_search);
        filterButton = v.findViewById(R.id.button_filter);

        adapter = new BrowseEventsAdapter(new ArrayList<>(), this);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);

        swipe.setOnRefreshListener(this::loadEvents);

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filter(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        filterButton.setOnClickListener(view -> showFilterDialog());

        loadEvents();
    }

    private void loadEvents() {
        progress.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            List<Event> fetched = eventService.getAllEvents();
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    swipe.setRefreshing(false);
                    allEvents.clear();
                    if (fetched != null) allEvents.addAll(fetched);
                    filter();
                });
            }
        });
    }

    /**
     * Shows a BottomSheetDialog to allow the user to configure filters.
     */
    private void showFilterDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_event_filters, null);
        dialog.setContentView(view);

        TextInputEditText categoryInput = view.findViewById(R.id.filter_input_category);
        TextInputEditText dateInput = view.findViewById(R.id.filter_input_date);
        SwitchMaterial availableSwitch = view.findViewById(R.id.filter_switch_available);
        MaterialButton btnClear = view.findViewById(R.id.filter_btn_clear);
        MaterialButton btnApply = view.findViewById(R.id.filter_btn_apply);

        // Pre-fill current values
        categoryInput.setText(filterCategory);
        availableSwitch.setChecked(filterAvailableOnly);
        if (filterDate != null) {
            dateInput.setText(dateFormat.format(filterDate));
        }

        // Date Picker Logic
        dateInput.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            if (filterDate != null) c.setTime(filterDate);
            new DatePickerDialog(requireContext(), (dView, year, month, day) -> {
                c.set(year, month, day);
                dateInput.setText(dateFormat.format(c.getTime()));
                // We store the selected date temporarily until Apply is clicked,
                // but simply updating the text is enough to parse it on Apply.
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Clear Logic
        btnClear.setOnClickListener(v -> {
            filterCategory = "";
            filterDate = null;
            filterAvailableOnly = false;
            filter();
            dialog.dismiss();
        });

        // Apply Logic
        btnApply.setOnClickListener(v -> {
            filterCategory = categoryInput.getText() != null ? categoryInput.getText().toString().trim() : "";
            filterAvailableOnly = availableSwitch.isChecked();

            String dateStr = dateInput.getText() != null ? dateInput.getText().toString() : "";
            if (!TextUtils.isEmpty(dateStr)) {
                try {
                    filterDate = dateFormat.parse(dateStr);
                } catch (Exception e) {
                    filterDate = null; // Fallback
                }
            } else {
                filterDate = null;
            }

            filter();
            dialog.dismiss();
        });

        dialog.show();
    }

    /**
     * Filters the allEvents list based on search query and active filters.
     */
    private void filter() {
        String query = search.getText().toString().toLowerCase().trim();
        List<Event> filtered = new ArrayList<>();

        for (Event e : allEvents) {
            // 1. Text Search (Title)
            boolean matchesSearch = query.isEmpty() ||
                    (e.getTitle() != null && e.getTitle().toLowerCase().contains(query));

            // 2. Category Filter
            boolean matchesCategory = filterCategory.isEmpty() ||
                    (e.getCategory() != null && e.getCategory().toLowerCase().contains(filterCategory.toLowerCase()));

            // 3. Date Filter (Events starting on or after filterDate)
            boolean matchesDate = filterDate == null ||
                    (e.getEventStartDate() != null && !e.getEventStartDate().before(filterDate));

            // 4. Availability Filter
            boolean matchesAvail = !filterAvailableOnly || isAvailable(e);

            if (matchesSearch && matchesCategory && matchesDate && matchesAvail) {
                filtered.add(e);
            }
        }

        adapter.replace(filtered);
        empty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private boolean isAvailable(Event e) {
        // Must be open
        if (!isOpen(e)) return false;

        // Must not be full (Waiting list limit check)
        double limit = e.getWaitingListLimit();
        // If limit > 0, check current count
        if (limit > 0) {
            int currentCount = e.getWaitingList() != null ? e.getWaitingList().size() : 0;
            if (currentCount >= limit) {
                return false;
            }
        }
        return true;
    }
    @SuppressLint("MissingPermission")
    @Override
    public void onJoinWaitlist(@NonNull Event event) {
        progress.setVisibility(View.VISIBLE);

        userService.getCurrentUser(
                user -> {
                    if (user == null) {
                        progress.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Please log in", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String userId = user.getUserId();
                    if (userId.equals(event.getOrganizerId())) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            progress.setVisibility(View.GONE);
                            Toast.makeText(getContext(), "Organizers cannot join their own events.", Toast.LENGTH_SHORT).show();
                        });
                    }
                    return;
                }

                if (event.isGeoLocationOn()) {
                    if (!hasLocationPermission()) {
                        requestLocationThenJoin(event, user);
                        return;
                    }
                    fusedLocationClient.getLastLocation()
                            .addOnSuccessListener(loc -> {
                                if (loc == null) {
                                    progress.setVisibility(View.GONE);
                                    Toast.makeText(getContext(), "Location required to join this event.", Toast.LENGTH_LONG).show();
                                    return;
                                }
                                actuallyJoin(event, user, loc);
                            })
                            .addOnFailureListener(err -> {
                                progress.setVisibility(View.GONE);
                                Toast.makeText(getContext(), "Could not get location.", Toast.LENGTH_LONG).show();
                            });
                } else {
                    // Non-geo events can join without location
                    actuallyJoin(event, user, null);
                }
            },
            e -> {
                progress.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Failed to load user profile", Toast.LENGTH_SHORT).show();
            }
        );
    }

    @Override
    public void onViewEvent(@NonNull Event event) {
        try {
            Class<?> clazz = Class.forName("com.quantiagents.app.ui.ViewEventDetailsFragment");
            java.lang.reflect.Method method = clazz.getMethod("newInstance", String.class);
            Fragment detailsFragment = (Fragment) method.invoke(null, event.getEventId());

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.content_container, detailsFragment)
                    .addToBackStack(null)
                    .commit();
        } catch (Exception e) {
            Toast.makeText(getContext(), "View Details: " + event.getTitle(), Toast.LENGTH_SHORT).show();
        }
    }

    public static boolean isOpen(Event e) {
        return e != null && e.getStatus() == constant.EventStatus.OPEN;
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationThenJoin(Event event, User user) {
        pendingEvent = event;
        pendingUser = user;
        locationPermissionLauncher.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }
    @SuppressLint("MissingPermission")
    private void continueJoinWithLocation() {
        if (pendingEvent == null || pendingUser == null) {
            progress.setVisibility(View.GONE);
            return;
        }
        if (!hasLocationPermission()) {
            progress.setVisibility(View.GONE);
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(loc -> actuallyJoin(pendingEvent, pendingUser, loc))
                .addOnFailureListener(e -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Could not get location.", Toast.LENGTH_LONG).show();
                });
    }

    private void actuallyJoin(Event event, User user, Location loc) {
        String userId = user.getUserId();
        String eventId = event.getEventId();

        executor.execute(() -> {
            if (event.isGeoLocationOn() && loc == null) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        progress.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Location required to join this event.", Toast.LENGTH_LONG).show();
                    });
                }
                return;
            }
            RegistrationHistory existing = regService.getRegistrationHistoryByEventIdAndUserId(eventId, userId);
            if (existing != null) {
                if (isAdded()) requireActivity().runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Already registered!", Toast.LENGTH_SHORT).show();
                });
                return;
            }

            RegistrationHistory newReg = new RegistrationHistory(
                    eventId,
                    userId,
                    constant.EventRegistrationStatus.WAITLIST,
                    new Date()
            );

            regService.saveRegistrationHistory(newReg,
                    aVoid -> {
                        if (event.getWaitingList() == null) event.setWaitingList(new ArrayList<>());
                        if (!event.getWaitingList().contains(userId)) {
                            event.getWaitingList().add(userId);
                            eventService.updateEvent(event, v -> {}, e -> Log.e("BrowseEvents", "Failed to sync waiting list", e));
                        }

                        if (event.isGeoLocationOn() && loc != null && geoLocationService != null) {
                            GeoLocation geo = new GeoLocation(loc.getLatitude(), loc.getLongitude(), userId, eventId);
                            geoLocationService.saveGeoLocation(geo, id -> {}, err -> {});
                        }

                        if (isAdded()) requireActivity().runOnUiThread(() -> {
                            progress.setVisibility(View.GONE);
                            Toast.makeText(getContext(), "Joined Waitlist!", Toast.LENGTH_SHORT).show();
                            loadEvents();
                        });
                    },
                    e -> {
                        if (isAdded()) requireActivity().runOnUiThread(() -> {
                            progress.setVisibility(View.GONE);
                            Toast.makeText(getContext(), "Failed to join", Toast.LENGTH_SHORT).show();
                        });
                    });
        });
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
