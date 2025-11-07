package com.quantiagents.app.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.quantiagents.app.App;
import com.quantiagents.app.R;
import com.quantiagents.app.Services.UserService;
import com.quantiagents.app.models.User;

/**
 * Profile settings: currently just a notifications on/off toggle.
 * Uses Task-based async calls so it’s consistent with the shared UserService.
 */
public class SettingsFragment extends Fragment {

    private UserService userService;
    private MaterialSwitch notificationSwitch;
    private boolean suppressSwitchListener = false;

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        App app = (App) requireActivity().getApplication();
        userService = app.locator().userService();

        notificationSwitch = view.findViewById(R.id.switch_notifications);

        // Load current settings into the UI
        loadSettings();

        // Persist changes when the user toggles the switch
        notificationSwitch.setOnCheckedChangeListener((button, isChecked) -> {
            if (suppressSwitchListener) return;
            persistNotificationPref(isChecked);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh toggle when returning to this screen
        loadSettings();
    }

    private void loadSettings() {
        userService.getCurrentUser()
                .addOnSuccessListener(user -> {
                    if (user != null) {
                        suppressSwitchListener = true;
                        notificationSwitch.setChecked(user.hasNotificationsOn());
                        suppressSwitchListener = false;
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to load settings", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void persistNotificationPref(boolean enabled) {
        // Fetch, mutate, save
        userService.getCurrentUser()
                .addOnSuccessListener(user -> {
                    if (user == null) return;

                    user.setNotificationsOn(enabled);
                    userService.updateUser(user)
                            .addOnFailureListener(e -> {
                                if (getContext() != null) {
                                    Toast.makeText(getContext(), "Failed to save preference", Toast.LENGTH_SHORT).show();
                                }
                                // Revert UI if save fails
                                suppressSwitchListener = true;
                                notificationSwitch.setChecked(!enabled);
                                suppressSwitchListener = false;
                            });
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to save preference", Toast.LENGTH_SHORT).show();
                    }
                    // Revert UI if lookup fails
                    suppressSwitchListener = true;
                    notificationSwitch.setChecked(!enabled);
                    suppressSwitchListener = false;
                });
    }
}
