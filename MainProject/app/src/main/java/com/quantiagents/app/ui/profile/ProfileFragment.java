package com.quantiagents.app.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.quantiagents.app.App;
import com.quantiagents.app.R;
import com.quantiagents.app.Services.LoginService; // ADDED
import com.quantiagents.app.models.User;
import com.quantiagents.app.Services.UserService;
import com.quantiagents.app.ui.auth.SignUpActivity;

import java.text.DateFormat;
import java.util.Date;

/**
 * Fragment that displays the current user's profile information.
 * Shows user name, email, phone, device ID, and account creation date.
 * Provides access to edit profile functionality.
 */
public class ProfileFragment extends Fragment {

    private UserService userService;
    private LoginService loginService;
    private TextView nameView;
    private TextView emailView;
    private TextView phoneView;
    private TextView deviceView;
    private TextView createdView;
    private SwitchMaterial notificationSwitch;
    private boolean suppressSwitchListener;

    /**
     * Creates a new instance of ProfileFragment.
     *
     * @return A new ProfileFragment instance
     */
    public static ProfileFragment newInstance() {
        return new ProfileFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        App app = (App) requireActivity().getApplication();
        userService = app.locator().userService();
        loginService = app.locator().loginService();

        nameView = view.findViewById(R.id.text_profile_name);
        emailView = view.findViewById(R.id.text_profile_email);
        phoneView = view.findViewById(R.id.text_profile_phone);
        deviceView = view.findViewById(R.id.text_profile_device);
        createdView = view.findViewById(R.id.text_profile_created);
        MaterialButton editButton = view.findViewById(R.id.button_edit_profile);
        editButton.setOnClickListener(v -> openEdit());

        // Initialize notification switch and delete button
        notificationSwitch = view.findViewById(R.id.switch_notifications);
        MaterialButton deleteButton = view.findViewById(R.id.button_delete_profile);
        
        // Load user data first and set switch state before attaching listener
        // This prevents the listener from firing with incorrect default values
        User cachedUser = loginService.getActiveUser();
        if (cachedUser != null) {
            suppressSwitchListener = true;
            notificationSwitch.setChecked(cachedUser.hasNotificationsOn());
            suppressSwitchListener = false;
        } else {
            // Set to false as safe default until user data loads
            suppressSwitchListener = true;
            notificationSwitch.setChecked(false);
            suppressSwitchListener = false;
        }
        
        notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (suppressSwitchListener) {
                return;
            }
            buttonView.setEnabled(false);
            userService.updateNotificationPreference(
                    isChecked,
                    aVoid -> buttonView.post(() -> buttonView.setEnabled(true)),
                    e -> buttonView.post(() -> {
                        buttonView.setEnabled(true);
                        suppressSwitchListener = true;
                        notificationSwitch.setChecked(!isChecked);
                        suppressSwitchListener = false;
                        Toast.makeText(requireContext(), R.string.error_profile_missing, Toast.LENGTH_SHORT).show();
                    })
            );
        });
        
        deleteButton.setOnClickListener(v -> confirmDeletion());
    }

    @Override
    public void onResume() {
        super.onResume();
        bindUser();
    }

    /**
     * Binds user data to the UI.
     * Always fetches fresh user data from database to ensure notification preference is up-to-date.
     */
    private void bindUser() {
        // Always fetch from database to ensure we have the latest notification preference
        userService.getCurrentUser(
                user -> {
                    if (user == null) {
                        Toast.makeText(requireContext(), R.string.error_profile_missing, Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(requireContext(), SignUpActivity.class));
                        requireActivity().finish();
                        return;
                    }
                    populateUI(user);
                },
                e -> {
                    Toast.makeText(requireContext(), R.string.error_profile_missing, Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(requireContext(), SignUpActivity.class));
                    requireActivity().finish();
                }
        );
    }

    /**
     * Populates the UI with user information.
     *
     * @param user The user object to display
     */
    private void populateUI(User user) {
        nameView.setText(user.getName());
        emailView.setText(user.getEmail());
        if (TextUtils.isEmpty(user.getPhone())) {
            phoneView.setText(R.string.profile_phone_placeholder);
        } else {
            phoneView.setText(user.getPhone());
        }
        deviceView.setText(user.getDeviceId());

        // Handling potential null/string conversion for date
        if (user.getCreatedOn() != null) {
            try {
                DateFormat format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
                createdView.setText(format.format(new Date(System.currentTimeMillis())));
            } catch (Exception e) {
                createdView.setText("N/A");
            }
        }

        // Update notification switch based on user preference
        // Only update if the current state differs to avoid unnecessary changes
        if (notificationSwitch != null) {
            boolean shouldBeChecked = user.hasNotificationsOn();
            suppressSwitchListener = true;
            notificationSwitch.setChecked(shouldBeChecked);
            suppressSwitchListener = false;
        }
    }

    /**
     * Opens the EditProfileActivity to allow the user to edit their profile.
     */
    private void openEdit() {
        startActivity(new Intent(requireContext(), EditProfileActivity.class));
    }

    /**
     * Shows the confirmation dialog before deleting the profile.
     */
    private void confirmDeletion() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_profile_title)
                .setMessage(R.string.delete_profile_body)
                .setPositiveButton(R.string.delete_profile_confirm, (dialog, which) -> deleteProfile())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /**
     * Clears local session, deletes the profile, and routes back to sign-up.
     */
    private void deleteProfile() {
        loginService.logout();
        userService.deleteUserProfile(
                aVoid -> {
                    ((App) requireActivity().getApplication()).locator().deviceIdManager().reset();
                    Toast.makeText(requireContext(), R.string.message_profile_deleted, Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(requireContext(), SignUpActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    requireActivity().finish();
                },
                e -> Toast.makeText(requireContext(), R.string.error_profile_missing, Toast.LENGTH_SHORT).show()
        );
    }
}