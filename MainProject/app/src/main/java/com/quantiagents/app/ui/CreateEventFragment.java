package com.quantiagents.app.ui;

import android.app.DatePickerDialog;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.quantiagents.app.App;
import com.quantiagents.app.Constants.constant;
import com.quantiagents.app.R;
import com.quantiagents.app.Services.EventService;
import com.quantiagents.app.Services.ImageService;
import com.quantiagents.app.Services.UserService;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.quantiagents.app.models.Event;
import com.quantiagents.app.models.Image;
import com.quantiagents.app.models.User;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Fragment for creating new events.
 * Handles form validation, poster image selection, and saving to Firestore.
 */
public class CreateEventFragment extends Fragment {

    public static CreateEventFragment newInstance() {
        return new CreateEventFragment();
    }

    private EventService eventService;
    private UserService userService;
    private ImageService imageService;

    // Form fields
    private TextInputLayout nameLayout, descriptionLayout, categoryLayout, startDateLayout, endDateLayout,
            regStartDateLayout, regEndDateLayout, capacityLayout, priceLayout, waitingListLayout;

    private TextInputEditText nameField, descriptionField, categoryField, startDateField, endDateField,
            regStartDateField, regEndDateField, capacityField, priceField, waitingListField, locationField;

    private SwitchMaterial geolocationSwitch;
    private MaterialButton createButton;
    private MaterialButton uploadPosterButton;

    // Image Selection
    private Uri selectedPosterUri;
    private ActivityResultLauncher<PickVisualMediaRequest> pickMedia;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private final Calendar calendar = Calendar.getInstance();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Register the image picker
        pickMedia = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if (uri != null) {
                selectedPosterUri = uri;
                uploadPosterButton.setText("Poster Selected");
                uploadPosterButton.setIconResource(R.drawable.ic_check); // Assumes ic_check exists
            } else {
                Log.d("CreateEvent", "No media selected");
            }
        });
        return inflater.inflate(R.layout.fragment_create_event, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize services
        App app = (App) requireActivity().getApplication();
        eventService = app.locator().eventService();
        userService = app.locator().userService();
        imageService = app.locator().imageService();

        bindViews(view);
        setupDatePickers();

        uploadPosterButton.setOnClickListener(v ->
                pickMedia.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()));

        createButton.setOnClickListener(v -> handleCreateEvent());
    }

    private void bindViews(View view) {
        nameLayout = view.findViewById(R.id.input_name_layout);
        descriptionLayout = view.findViewById(R.id.input_description_layout);
        categoryLayout = view.findViewById(R.id.input_category_layout); // Bind new layout
        startDateLayout = view.findViewById(R.id.input_start_date_layout);
        endDateLayout = view.findViewById(R.id.input_end_date_layout);
        regStartDateLayout = view.findViewById(R.id.input_reg_start_date_layout);
        regEndDateLayout = view.findViewById(R.id.input_reg_end_date_layout);
        capacityLayout = view.findViewById(R.id.input_capacity_layout);
        priceLayout = view.findViewById(R.id.input_price_layout);
        waitingListLayout = view.findViewById(R.id.input_waiting_list_layout);

        nameField = view.findViewById(R.id.input_name);
        descriptionField = view.findViewById(R.id.input_description);
        categoryField = view.findViewById(R.id.input_category); // Bind new field
        startDateField = view.findViewById(R.id.input_start_date);
        endDateField = view.findViewById(R.id.input_end_date);
        regStartDateField = view.findViewById(R.id.input_reg_start_date);
        regEndDateField = view.findViewById(R.id.input_reg_end_date);
        capacityField = view.findViewById(R.id.input_capacity);
        priceField = view.findViewById(R.id.input_price);
        waitingListField = view.findViewById(R.id.input_waiting_list);
        locationField = view.findViewById(R.id.input_location);

        geolocationSwitch = view.findViewById(R.id.switch_geolocation);
        createButton = view.findViewById(R.id.button_create_event);
        uploadPosterButton = view.findViewById(R.id.button_upload_poster);
    }

    private void setupDatePickers() {
        startDateField.setOnClickListener(v -> showDatePicker(startDateField));
        endDateField.setOnClickListener(v -> showDatePicker(endDateField));
        regStartDateField.setOnClickListener(v -> showDatePicker(regStartDateField));
        regEndDateField.setOnClickListener(v -> showDatePicker(regEndDateField));
    }

    private void showDatePicker(TextInputEditText field) {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        String existingDate = safeText(field);
        if (!TextUtils.isEmpty(existingDate)) {
            try {
                Date date = dateFormat.parse(existingDate);
                if (date != null) {
                    Calendar c = Calendar.getInstance();
                    c.setTime(date);
                    year = c.get(Calendar.YEAR);
                    month = c.get(Calendar.MONTH);
                    day = c.get(Calendar.DAY_OF_MONTH);
                }
            } catch (ParseException ignored) { }
        }

        new DatePickerDialog(requireContext(), (view, y, m, d) -> {
            calendar.set(y, m, d);
            field.setText(dateFormat.format(calendar.getTime()));
        }, year, month, day).show();
    }

    private void handleCreateEvent() {
        clearErrors();
        if (!validateForm()) return;

        userService.getCurrentUser(
                user -> {
                    if (user == null) {
                        Toast.makeText(requireContext(), "User not found.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    createEvent(user);
                },
                e -> Toast.makeText(requireContext(), "Error getting user.", Toast.LENGTH_SHORT).show()
        );
    }

    private boolean validateForm() {
        boolean isValid = true;
        if (TextUtils.isEmpty(safeText(nameField))) { nameLayout.setError("Required"); isValid = false; }
        if (TextUtils.isEmpty(safeText(descriptionField))) { descriptionLayout.setError("Required"); isValid = false; }
        // Category is optional for now, or we can make it required
        if (TextUtils.isEmpty(safeText(startDateField))) { startDateLayout.setError("Required"); isValid = false; }
        if (TextUtils.isEmpty(safeText(endDateField))) { endDateLayout.setError("Required"); isValid = false; }
        if (TextUtils.isEmpty(safeText(regStartDateField))) { regStartDateLayout.setError("Required"); isValid = false; }
        if (TextUtils.isEmpty(safeText(regEndDateField))) { regEndDateLayout.setError("Required"); isValid = false; }

        String cap = safeText(capacityField);
        if (TextUtils.isEmpty(cap)) { capacityLayout.setError("Required"); isValid = false; }
        else if (Integer.parseInt(cap) < 1) { capacityLayout.setError("Min 1"); isValid = false; }

        String pr = safeText(priceField);
        if (TextUtils.isEmpty(pr)) { priceLayout.setError("Required"); isValid = false; }

        return isValid;
    }

    private void createEvent(User user) {
        try {
            Event event = new Event();
            event.setTitle(safeText(nameField));
            event.setDescription(safeText(descriptionField));
            event.setCategory(safeText(categoryField)); // Save category
            event.setOrganizerId(user.getUserId());
            event.setLocation(safeText(locationField));

            event.setEventStartDate(dateFormat.parse(safeText(startDateField)));
            event.setEventEndDate(dateFormat.parse(safeText(endDateField)));
            event.setRegistrationStartDate(dateFormat.parse(safeText(regStartDateField)));
            event.setRegistrationEndDate(dateFormat.parse(safeText(regEndDateField)));

            event.setEventCapacity(Double.parseDouble(safeText(capacityField)));
            event.setCost(Double.parseDouble(safeText(priceField)));

            String waitStr = safeText(waitingListField);
            event.setWaitingListLimit(!waitStr.isEmpty() ? Double.parseDouble(waitStr) : 0);

            event.setGeoLocationOn(geolocationSwitch.isChecked());
            event.setStatus(constant.EventStatus.OPEN);

            // Init lists
            event.setWaitingList(new ArrayList<>());
            event.setSelectedList(new ArrayList<>());
            event.setConfirmedList(new ArrayList<>());
            event.setCancelledList(new ArrayList<>());

            eventService.saveEvent(event,
                    eventId -> {
                        event.setEventId(eventId);
                        if (selectedPosterUri != null) {
                            savePoster(selectedPosterUri.toString(), eventId, user.getUserId(),
                                    imgId -> {
                                        event.setPosterImageId(imgId);
                                        eventService.updateEvent(event, v -> {}, e -> {});
                                        finishSuccess();
                                    },
                                    e -> {
                                        Toast.makeText(getContext(), "Event created, but poster failed.", Toast.LENGTH_SHORT).show();
                                        resetForm();
                                    });
                        } else {
                            finishSuccess();
                        }
                    },
                    e -> Toast.makeText(requireContext(), "Creation failed", Toast.LENGTH_SHORT).show()
            );

        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void savePoster(String uriStr, String eventId, String userId,
                            OnSuccessListener<String> success, OnFailureListener fail) {
        Image img = new Image();
        img.setUri(uriStr);
        img.setEventId(eventId);
        img.setUploadedBy(userId);
        imageService.saveImage(img, success, fail);
    }

    private void finishSuccess() {
        Toast.makeText(requireContext(), "Event Created!", Toast.LENGTH_SHORT).show();
        resetForm();
        // Optionally navigate away
    }

    private void clearErrors() {
        nameLayout.setError(null); descriptionLayout.setError(null);
        categoryLayout.setError(null);
        startDateLayout.setError(null); endDateLayout.setError(null);
        regStartDateLayout.setError(null); regEndDateLayout.setError(null);
        capacityLayout.setError(null); priceLayout.setError(null);
    }

    private void resetForm() {
        nameField.setText(""); descriptionField.setText(""); categoryField.setText("");
        startDateField.setText(""); endDateField.setText("");
        regStartDateField.setText(""); regEndDateField.setText("");
        capacityField.setText(""); priceField.setText("");
        waitingListField.setText(""); locationField.setText("");
        geolocationSwitch.setChecked(false);
        uploadPosterButton.setText(R.string.create_event_poster_label);
        selectedPosterUri = null;
        clearErrors();
    }

    private String safeText(TextInputEditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }
}