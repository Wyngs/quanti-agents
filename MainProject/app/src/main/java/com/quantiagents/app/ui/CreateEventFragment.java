package com.quantiagents.app.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
//import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.materialswitch.MaterialSwitch;
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
import java.util.List;
import java.util.Locale;
import java.util.UUID;


public class CreateEventFragment extends Fragment {

    public static CreateEventFragment newInstance() {
        return new CreateEventFragment();
    }

    private EventService eventService;
    private UserService userService;
    private ImageService imageService;

    // Form fields
    private TextInputLayout nameLayout;
    private TextInputLayout descriptionLayout;
    private TextInputLayout startDateLayout;
    private TextInputLayout endDateLayout;
    private TextInputLayout regStartDateLayout;
    private TextInputLayout regEndDateLayout;
    private TextInputLayout capacityLayout;
    private TextInputLayout priceLayout;
    private TextInputLayout waitingListLayout;
    private TextInputLayout posterLayout;

    private TextInputEditText nameField;
    private TextInputEditText descriptionField;
    private TextInputEditText startDateField;
    private TextInputEditText endDateField;
    private TextInputEditText regStartDateField;
    private TextInputEditText regEndDateField;
    private TextInputEditText capacityField;
    private TextInputEditText priceField;
    private TextInputEditText waitingListField;
    private TextInputEditText posterField;

    //private MaterialSwitch geolocationSwitch;
    private SwitchMaterial geolocationSwitch;
    private MaterialButton createButton;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private final Calendar calendar = Calendar.getInstance();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
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

        // Bind views
        bindViews(view);

        // Set up date pickers
        setupDatePickers();

        // Set up button click listener
        createButton.setOnClickListener(v -> handleCreateEvent());
    }

    private void bindViews(View view) {
        // TextInputLayouts
        nameLayout = view.findViewById(R.id.input_name_layout);
        descriptionLayout = view.findViewById(R.id.input_description_layout);
        startDateLayout = view.findViewById(R.id.input_start_date_layout);
        endDateLayout = view.findViewById(R.id.input_end_date_layout);
        regStartDateLayout = view.findViewById(R.id.input_reg_start_date_layout);
        regEndDateLayout = view.findViewById(R.id.input_reg_end_date_layout);
        capacityLayout = view.findViewById(R.id.input_capacity_layout);
        priceLayout = view.findViewById(R.id.input_price_layout);
        waitingListLayout = view.findViewById(R.id.input_waiting_list_layout);
        posterLayout = view.findViewById(R.id.input_poster_layout);

        // TextInputEditTexts
        nameField = view.findViewById(R.id.input_name);
        descriptionField = view.findViewById(R.id.input_description);
        startDateField = view.findViewById(R.id.input_start_date);
        endDateField = view.findViewById(R.id.input_end_date);
        regStartDateField = view.findViewById(R.id.input_reg_start_date);
        regEndDateField = view.findViewById(R.id.input_reg_end_date);
        capacityField = view.findViewById(R.id.input_capacity);
        priceField = view.findViewById(R.id.input_price);
        waitingListField = view.findViewById(R.id.input_waiting_list);
        posterField = view.findViewById(R.id.input_poster);

        // Switch and Button
        geolocationSwitch = view.findViewById(R.id.switch_geolocation);
        createButton = view.findViewById(R.id.button_create_event);
    }

    private void setupDatePickers() {
        // Event Start Date
        startDateField.setOnClickListener(v -> showDatePicker(startDateField, "Event Start Date"));

        // Event End Date
        endDateField.setOnClickListener(v -> showDatePicker(endDateField, "Event End Date"));

        // Registration Start Date
        regStartDateField.setOnClickListener(v -> showDatePicker(regStartDateField, "Registration Start Date"));

        // Registration End Date
        regEndDateField.setOnClickListener(v -> showDatePicker(regEndDateField, "Registration End Date"));
    }

    private void showDatePicker(TextInputEditText field, String title) {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Try to parse existing date if present
        String existingDate = safeText(field);
        if (!TextUtils.isEmpty(existingDate)) {
            try {
                Date date = dateFormat.parse(existingDate);
                if (date != null) {
                    calendar.setTime(date);
                    year = calendar.get(Calendar.YEAR);
                    month = calendar.get(Calendar.MONTH);
                    day = calendar.get(Calendar.DAY_OF_MONTH);
                }
            } catch (ParseException e) {
                // Use current date if parsing fails
            }
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    calendar.set(selectedYear, selectedMonth, selectedDay);
                    field.setText(dateFormat.format(calendar.getTime()));
                },
                year, month, day
        );

        datePickerDialog.setTitle(title);
        datePickerDialog.show();
    }

    private void handleCreateEvent() {
        // Clear previous errors
        clearErrors();

        // Validate and collect form data
        if (!validateForm()) {
            return;
        }

        // Get current user
        userService.getCurrentUser(
                user -> {
                    if (user == null) {
                        Toast.makeText(requireContext(), "User not found. Please log in again.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    createEvent(user);
                },
                e -> {
                    Toast.makeText(requireContext(), "Error getting user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
        );
    }

    private boolean validateForm() {
        boolean isValid = true;

        // Validate Event Name
        String name = safeText(nameField);
        if (TextUtils.isEmpty(name)) {
            nameLayout.setError("Event name is required");
            isValid = false;
        }

        // Validate Description
        String description = safeText(descriptionField);
        if (TextUtils.isEmpty(description)) {
            descriptionLayout.setError("Description is required");
            isValid = false;
        }

        // Validate Event Start Date
        String startDate = safeText(startDateField);
        if (TextUtils.isEmpty(startDate)) {
            startDateLayout.setError("Event start date is required");
            isValid = false;
        }

        // Validate Event End Date
        String endDate = safeText(endDateField);
        if (TextUtils.isEmpty(endDate)) {
            endDateLayout.setError("Event end date is required");
            isValid = false;
        }

        // Validate Registration Start Date
        String regStartDate = safeText(regStartDateField);
        if (TextUtils.isEmpty(regStartDate)) {
            regStartDateLayout.setError("Registration start date is required");
            isValid = false;
        }

        // Validate Registration End Date
        String regEndDate = safeText(regEndDateField);
        if (TextUtils.isEmpty(regEndDate)) {
            regEndDateLayout.setError("Registration end date is required");
            isValid = false;
        }

        // Validate Capacity
        String capacityStr = safeText(capacityField);
        if (TextUtils.isEmpty(capacityStr)) {
            capacityLayout.setError("Capacity is required");
            isValid = false;
        } else {
            try {
                int capacity = Integer.parseInt(capacityStr);
                if (capacity < 1) {
                    capacityLayout.setError("Capacity must be at least 1");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                capacityLayout.setError("Invalid capacity");
                isValid = false;
            }
        }

        // Validate Price
        String priceStr = safeText(priceField);
        if (TextUtils.isEmpty(priceStr)) {
            priceLayout.setError("Price is required");
            isValid = false;
        } else {
            try {
                double price = Double.parseDouble(priceStr);
                if (price < 0) {
                    priceLayout.setError("Price cannot be negative");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                priceLayout.setError("Invalid price");
                isValid = false;
            }
        }

        // Validate Waiting List Limit (optional)
        String waitingListStr = safeText(waitingListField);
        if (!TextUtils.isEmpty(waitingListStr)) {
            try {
                int waitingList = Integer.parseInt(waitingListStr);
                if (waitingList < 1) {
                    waitingListLayout.setError("Waiting list limit must be at least 1");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                waitingListLayout.setError("Invalid waiting list limit");
                isValid = false;
            }
        }

        // Validate date relationships
        if (isValid && !TextUtils.isEmpty(startDate) && !TextUtils.isEmpty(endDate)) {
            try {
                Date start = dateFormat.parse(startDate);
                Date end = dateFormat.parse(endDate);
                if (start != null && end != null && end.before(start)) {
                    endDateLayout.setError("End date must be after start date");
                    isValid = false;
                }
            } catch (ParseException e) {
                // Already validated above
            }
        }

        if (isValid && !TextUtils.isEmpty(regStartDate) && !TextUtils.isEmpty(regEndDate)) {
            try {
                Date regStart = dateFormat.parse(regStartDate);
                Date regEnd = dateFormat.parse(regEndDate);
                if (regStart != null && regEnd != null && regEnd.before(regStart)) {
                    regEndDateLayout.setError("Registration end date must be after start date");
                    isValid = false;
                }
            } catch (ParseException e) {
                // Already validated above
            }
        }

        return isValid;
    }

    private void createEvent(User user) {
        try {
            // Create new Event object
            Event event = new Event();

            // Make Firebase Generate new event ID
            // event.setEventId(UUID.randomUUID().toString());
            
            // Set basic information
            event.setTitle(safeText(nameField));
            event.setDescription(safeText(descriptionField));
            event.setOrganizerId(user.getUserId());
            
            // Parse and set dates
            String startDateStr = safeText(startDateField);
            String endDateStr = safeText(endDateField);
            String regStartDateStr = safeText(regStartDateField);
            String regEndDateStr = safeText(regEndDateField);
            
            try {
                Date startDate = dateFormat.parse(startDateStr);
                Date endDate = dateFormat.parse(endDateStr);
                Date regStartDate = dateFormat.parse(regStartDateStr);
                Date regEndDate = dateFormat.parse(regEndDateStr);
                
                event.setEventStartDate(startDate);
                event.setEventEndDate(endDate);
                event.setRegistrationStartDate(regStartDate);
                event.setRegistrationEndDate(regEndDate);
            } catch (ParseException e) {
                Toast.makeText(requireContext(), "Error parsing dates: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Set capacity and price
            event.setEventCapacity(Double.parseDouble(safeText(capacityField)));
            event.setCost(Double.parseDouble(safeText(priceField)));
            
            // Set waiting list limit (optional)
            String waitingListStr = safeText(waitingListField);
            if (!TextUtils.isEmpty(waitingListStr)) {
                event.setWaitingListLimit(Double.parseDouble(waitingListStr));
            } else {
                event.setWaitingListLimit(0); // 0 means unlimited
            }

            // Set geolocation requirement
            event.setGeoLocationOn(geolocationSwitch.isChecked());
            
            // Set initial status
            event.setStatus(constant.EventStatus.OPEN);
            
            // Initialize lists
            event.setWaitingList(new ArrayList<>());
            event.setSelectedList(new ArrayList<>());
            event.setConfirmedList(new ArrayList<>());
            event.setCancelledList(new ArrayList<>());
            
            // Set lottery status
            event.setFirstLotteryDone(false);
            
            // Save event
            eventService.saveEvent(event,
                    eventId -> {
                        Toast.makeText(requireContext(), "Event created successfully!", Toast.LENGTH_SHORT).show();

                        event.setEventId(eventId);

                        // Create poster image if URL is provided
                        String posterUrl = safeText(posterField);
                        if (!TextUtils.isEmpty(posterUrl)) {
                            createPoster(posterUrl, eventId, event.getOrganizerId(),
                                    imageId -> {
                                        Log.d("CreateEvent", "Poster image saved with ID: " + imageId);
                                        event.setPosterImageId(imageId);
                                        eventService.updateEvent(event,
                                                aVoid -> {
                                                    Log.d("UpdateEvent", "Updated Event with new Image Id: " + imageId);
                                                },
                                                e -> {
                                                    Log.e("UpdateEvent", "Failed Updating Event with new Image Id: " + imageId, e);
                                                });
                                    },
                                    e -> {
                                        Log.e("CreateEvent", "Failed to save poster image: " + e.getMessage());
                                    });
                        }
                        resetForm();
                    },
                    e -> {
                        Toast.makeText(requireContext(), "Failed to create event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
            );


        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error creating event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void createPoster(String posterUrl, String eventId, String userId, 
                              OnSuccessListener<String> onSuccess, OnFailureListener onFailure) {
        Image newPoster = new Image();
        newPoster.setUri(posterUrl);
        newPoster.setEventId(eventId);
        newPoster.setUploadedBy(userId);
        // imageId will be auto-generated by repository when saved

        imageService.saveImage(newPoster, 
            imageId -> {
                // Image saved successfully, imageId is returned in callback
                Log.d("CreateEvent", "Poster image saved with ID: " + imageId);
                onSuccess.onSuccess(imageId);
            }, 
            e -> {
                Log.e("CreateEvent", "Failed to save poster image: " + e.getMessage());
                onFailure.onFailure(e);
            }
        );
    }

    private void clearErrors() {
        nameLayout.setError(null);
        descriptionLayout.setError(null);
        startDateLayout.setError(null);
        endDateLayout.setError(null);
        regStartDateLayout.setError(null);
        regEndDateLayout.setError(null);
        capacityLayout.setError(null);
        priceLayout.setError(null);
        waitingListLayout.setError(null);
        posterLayout.setError(null);
    }

    private void resetForm() {
        nameField.setText("");
        descriptionField.setText("");
        startDateField.setText("");
        endDateField.setText("");
        regStartDateField.setText("");
        regEndDateField.setText("");
        capacityField.setText("");
        priceField.setText("");
        waitingListField.setText("");
        posterField.setText("");
        geolocationSwitch.setChecked(false);
        clearErrors();
    }

    private String safeText(TextInputEditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }
}
