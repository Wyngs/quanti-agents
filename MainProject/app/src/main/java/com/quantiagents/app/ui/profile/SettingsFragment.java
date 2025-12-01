package com.quantiagents.app.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.quantiagents.app.App;
import com.quantiagents.app.R;
import com.quantiagents.app.Services.LoginService;
import com.quantiagents.app.Services.UserService;
import com.quantiagents.app.ui.auth.SignUpActivity;

/**
 * Houses the simple profile settings: notification toggle and the nuke profile button.
 */
public class SettingsFragment extends Fragment {

    private UserService userService;
    private LoginService loginService;
    private SwitchMaterial notificationSwitch;
    private boolean suppressSwitchListener;

    /**
     * Creates a new instance of SettingsFragment.
     *
     * @return A new SettingsFragment instance
     */
    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    /**
     * Hooks up the toggle and delete button once the view hierarchy exists.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        App app = (App) requireActivity().getApplication();
        userService = app.locator().userService();
        loginService = app.locator().loginService();
        notificationSwitch = view.findViewById(R.id.switch_notifications);
        MaterialButton deleteButton = view.findViewById(R.id.button_delete_profile);
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

    /**
     * Refreshes the switch state whenever we return from another screen.
     */
    @Override
    public void onResume() {
        super.onResume();
        userService.getCurrentUser(
                user -> {
                    if (user != null) {
                        suppressSwitchListener = true;
                        notificationSwitch.setChecked(user.hasNotificationsOn());
                        suppressSwitchListener = false;
                    }
                },
                e -> notificationSwitch.setChecked(true)
        );
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
