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
import com.google.android.material.materialswitch.MaterialSwitch;
import com.quantiagents.app.App;
import com.quantiagents.app.R;
import com.quantiagents.app.models.User;
import com.quantiagents.app.Services.UserService;
import com.quantiagents.app.ui.auth.SignUpActivity;

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
        userService = ((App) requireActivity().getApplication()).locator().userService();
        notificationSwitch = view.findViewById(R.id.switch_notifications);
        MaterialButton deleteButton = view.findViewById(R.id.button_delete_profile);
        // Toggle directly writes back to the stored profile.
        notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> userService.updateNotificationPreference(isChecked));
        deleteButton.setOnClickListener(v -> confirmDeletion());
    }

    @Override
    public void onResume() {
        super.onResume();
        User user = userService.getCurrentUser();
        if (user != null) {
            notificationSwitch.setChecked(user.hasNotificationsOn());
        }
    }

    private void confirmDeletion() {
        // Quick confirmation before wiping the profile.
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_profile_title)
                .setMessage(R.string.delete_profile_body)
                .setPositiveButton(R.string.delete_profile_confirm, (dialog, which) -> deleteProfile())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void deleteProfile() {
        userService.deleteUserProfile();
        Toast.makeText(requireContext(), R.string.message_profile_deleted, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(requireContext(), SignUpActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}
