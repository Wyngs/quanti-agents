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

public class SettingsFragment extends Fragment {

    private UserService userService;
    private MaterialSwitch notificationSwitch;

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        App app = (App) requireActivity().getApplication();
        userService = app.locator().userService();

        notificationSwitch = view.findViewById(R.id.switch_notifications);

        loadSettings();

        notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateNotificationPreference(isChecked);
        });
    }

    private void loadSettings() {
        userService.getCurrentUser().addOnSuccessListener(user -> {
            if (user != null) {
                notificationSwitch.setChecked(user.hasNotificationsOn());
            }
        }).addOnFailureListener(e -> {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Failed to load settings", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateNotificationPreference(boolean isEnabled) {
        userService.getCurrentUser().addOnSuccessListener(user -> {
            if (user != null) {
                user.setNotificationsOn(isEnabled);
                userService.updateUser(user).addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to save preference", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}